package service.ontologyFetcher

import play.api.Logger
import scala.concurrent.{Future}
import play.api.libs.ws.{Response, WS}
import scala.math.floor
import scala.xml.Elem
import service.ontologyFetcher.parser.OntologyParser
import common.ExecutionContexts
import service.FetchResult

class SwoogleFetcher(parser: OntologyParser) extends OntologyFetcher(parser) {
  private final val ACCESS_KEY: String = "52fc0c56ec4942e2a5268356d0b8af23"
  private final val SEARCH_ONTOLOGY_API_URL: String = "http://sparql.cs.umbc.edu/swoogle31/q"
  private final val SWOOGLE_MAX_RESULT: Int = 1000
  private final val SWOOGLE_RESULT_PER_PAGE: Int = 10

  def search(keyword: String): Future[FetchResult] = super.search(keyword, "swoogle")


  @throws[Exception]("On connection problem")
  def getOntologyList(keyword: String): Future[Set[String]] = {
    import ExecutionContexts.internetIOOps
    def getNormalizedSearchResultCount(xml: Elem): Int = {
      val resultCount = try {(xml \\ "QueryResponse" \\ "hasSearchTotalResults").text.toInt; } catch { case e: Throwable => 0 }
      if (resultCount > SWOOGLE_MAX_RESULT) SWOOGLE_MAX_RESULT else resultCount
    }
    def toUrlList(r: Response): Seq[String] = {
      try {
        val xml = r.xml
        for (node <- xml \\ "SemanticWebDocument" \\ "@{http://www.w3.org/1999/02/22-rdf-syntax-ns#}about")
        yield node.text
      } catch {
        case ex: Throwable => Seq.empty[String]
      }
    }

    val firstPagePromise: Future[Response] = fetchAPage(keyword, 1)

    // what if fails?
    val resultCount: Future[Int] = firstPagePromise.map { response =>
      val xml = response.xml
      getNormalizedSearchResultCount(xml)
    } recover {
      case ex: Throwable => {
        Logger.error("There is no XML to parse!")
        0
      }
    }

    resultCount.flatMap {
      case rc if rc > 0 => {
        val searchResultsFutures: Seq[Future[Response]] =
          Seq(firstPagePromise) ++ {
            for (i: Int <- 1 until (floor(rc / SWOOGLE_RESULT_PER_PAGE) + (if (rc % SWOOGLE_RESULT_PER_PAGE > 0) 1 else 0)).toInt)
            yield fetchAPage(keyword, (i * SWOOGLE_RESULT_PER_PAGE) + 1)
          }

        val urlListFutures: Seq[Future[Seq[String]]] = searchResultsFutures.map { responseFuture: Future[Response] =>
          responseFuture.map {
            response => toUrlList(response)
          }
        }

        val urlListFuture: Future[Seq[Seq[String]]] = Future.sequence(urlListFutures)
        val urlListFlatterned: Future[Set[String]] = urlListFuture.map {
          x => x.flatten.toSet
        }
        urlListFlatterned
      }
      case _ => Future.successful(Set.empty[String])
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
