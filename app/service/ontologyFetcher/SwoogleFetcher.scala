package service.ontologyFetcher

import play.api.Logger
import scala.concurrent.{Await, Future}
import play.api.libs.ws.{Response, WS}
import scala.concurrent.duration._
import scala.xml.Elem
import service.ontologyFetcher.parser.OntologyParser
import common.ExecutionContexts.internetIOOps

class SwoogleFetcher(parser: OntologyParser) extends OntologyFetcher(parser) {
  private final val ACCESS_KEY: String = "52fc0c56ec4942e2a5268356d0b8af23"
  private final val SEARCH_ONTOLOGY_API_URL: String = "http://sparql.cs.umbc.edu/swoogle31/q"
  private final val SWOOGLE_MAX_RESULT: Int = 1000
  private final val SWOOGLE_RESULT_PER_PAGE: Int = 10

  def doOntologyFetchingFor(keyword: String): Map[Status.Value, Int] = super.doOntologyFetchingFor(keyword, "swoogle")

  @throws[Exception]("On connection problem")
  def getOntologyListFuture(keyword: String): Future[Option[Set[String]]] = {
    val firstPagePromise = fetchAPage(keyword, 1)

    def getNormalizedSearchResultCount(xml: Elem): Int = {
      val resultCount = try { (xml \\ "QueryResponse" \\ "hasSearchTotalResults").text.toInt; } catch { case e: Throwable => 0 }
      if (resultCount > SWOOGLE_MAX_RESULT) SWOOGLE_MAX_RESULT else resultCount
    }

    val resultCount =
      getXMLSync(firstPagePromise) match {
      case Some(xml) => {
         getNormalizedSearchResultCount(xml)
      }
      case None => {
        Logger.error("There is no XML to parse!")
        throw new Exception
      }
    }
    if(resultCount == 0) new Exception

    def toUrlList(toBeMapped: Future[Response]): Future[Set[String]] = {
      val mapped: Future[Set[String]] = toBeMapped.map {
        def swoogleParser(r: Response): Set[String] = {
          (for (node <- (r.xml \\ "SemanticWebDocument" \\ "@{http://www.w3.org/1999/02/22-rdf-syntax-ns#}about")) yield node.text).toSet
        }
        r: Response => swoogleParser(r)
      } recover {
        case ex => {
            Logger.error("A mapped future failed", ex)
            Set[String]()
        }
      }
      mapped
    }

    val pageFutureSet: Seq[Future[Set[String]]] =
      (for (i: Int <- 1 until (scala.math.floor (resultCount / SWOOGLE_RESULT_PER_PAGE) + (if (resultCount % SWOOGLE_RESULT_PER_PAGE > 0) 1 else 0)).toInt) yield toUrlList(fetchAPage(keyword, ((i * SWOOGLE_RESULT_PER_PAGE) + 1)))
        ) ++ Seq(toUrlList(firstPagePromise))

    return Future.sequence(pageFutureSet).map {
      results: Seq[Set[String]] => if(results.isEmpty) None else Some((results.flatten).toSet)
    }
  }

  def getOntologyList(keyword: String): Option[Set[String]] = {
    try {
      return Await.result(getOntologyListFuture(keyword), 10 minutes)
    } catch {
      case e: Exception => {
        Logger.error("Not on time!", e)
        return None
      }
    }
  }

  private def fetchAPage(searchQuery: String, startResult: Int): Future[Response] = {
    if (startResult >= SWOOGLE_MAX_RESULT) {
      throw new IllegalArgumentException
    }
    WS.url(SEARCH_ONTOLOGY_API_URL)
      .withQueryString(("queryType", "search_swd_ontology"))
      .withQueryString(("key", ACCESS_KEY))
      .withQueryString(("searchString", searchQuery))
      .withQueryString(("searchStart", String.valueOf(startResult))).get()
  }
}
