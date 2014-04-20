package service.ontologyFetcher

import play.api.{Play, Logger}
import scala.concurrent.Future
import play.api.libs.ws.{Response, WS}
import scala.math.floor
import scala.xml.Elem
import service.ontologyFetcher.parser.OntologyParser
import common.{BasicTimer, ExecutionContexts}
import service.FetchResult
import scala.util.{Success, Failure}
import play.api.Play.current

class SwoogleFetcher(parser: OntologyParser) extends OntologyFetcher(parser) {
  private final val ACCESS_KEY: String = Play.configuration.getString("swoogle.apiKey").getOrElse("_")
  private final val SEARCH_ONTOLOGY_API_URL: String = "http://sparql.cs.umbc.edu/swoogle31/q"
  private final val SWOOGLE_MAX_RESULT: Int = Play.configuration.getInt("swoogle.maxSearchResult").getOrElse(1000)
  private final val SWOOGLE_RESULT_PER_PAGE: Int = 10

  def search(keyword: String): Future[FetchResult] = super.search(keyword, "swoogle")

  override def getOntologyList(keyword: String): Future[Set[String]] = {
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

    val timer = new BasicTimer("fetch|swoogle", "page|all").start()

    val firstPagePromise: Future[Response] = fetchAPage(keyword, 1)

    val resultCount: Future[Int] = firstPagePromise.map { response =>
      val xml = response.xml
      getNormalizedSearchResultCount(xml)
    }

    val convertF = resultCount.flatMap {
      case rc if rc > 0 => {

        val urlListFutures: Seq[Future[Seq[String]]] =
          (Seq(firstPagePromise) ++ {
            for (i: Int <- 1 until (floor(rc / SWOOGLE_RESULT_PER_PAGE) + (if (rc % SWOOGLE_RESULT_PER_PAGE > 0) 1 else 0)).toInt)
            yield fetchAPage(keyword, (i * SWOOGLE_RESULT_PER_PAGE) + 1)
          }).map { responseFuture: Future[Response] =>
          responseFuture.map {
            response => toUrlList(response)
          }.recover {
            case ex: Throwable => Seq.empty[String]
          }
        }

        val urlListFuture: Future[Seq[Seq[String]]] = Future.sequence(urlListFutures)
        val urlListFlatterned: Future[Set[String]] = urlListFuture.map {
          x => x.flatten.toSet
        }
        urlListFlatterned
      }
      case _ => Future.successful(Set.empty[String])
    } recover {
      case ex: Throwable => {
        Logger.error("Search engine is totally failed.", ex)
        Set.empty[String]
      }
    }
    convertF.onComplete { _ => timer.stop() }
    convertF
  }

  private def fetchAPage(searchQuery: String, startResult: Int): Future[Response] = {
    import ExecutionContexts.internetIOOps
    Logger.info("Fetching SWOOGLE starting " + startResult)
    Logger.info(SEARCH_ONTOLOGY_API_URL + "?queryType=search_swd_ontology&key=" + ACCESS_KEY + "&searchString=" + searchQuery + "&searchStart=" + String.valueOf(startResult))

    val timer = new BasicTimer("fetch|swoogle", "page|" + startResult).start()
    val pageF = WS.url(SEARCH_ONTOLOGY_API_URL)
      .withQueryString(("queryType", "search_swd_ontology"))
      .withQueryString(("key", ACCESS_KEY))
      .withQueryString(("searchString", searchQuery))
      .withQueryString(("searchStart", String.valueOf(startResult))).get()
      .andThen {
        case Failure(response) =>
          Logger.error("Fetching SWOOGLE FAILED " + startResult)
          response
        case Success(response) =>
          Logger.info("Fetching SWOOGLE completed " + startResult)
          response
      }
    pageF.onComplete { _ => timer.stop() }
    pageF
  }
}
