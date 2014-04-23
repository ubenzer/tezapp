package service.ontologyFetcher

import scala.concurrent.Future
import play.api.libs.ws.{Response, WS}
import service.ontologyFetcher.parser.OntologyParser
import common.ExecutionContexts
import scala.util.{Success, Failure}
import play.api.libs.json.JsArray
import play.api.{Play, Logger}
import play.api.Play.current

class WatsonFetcher(parser: OntologyParser) extends OntologyFetcher(parser) {
  private val SEARCH_ONTOLOGY_API_URL = "http://watson.kmi.open.ac.uk/API/semanticcontent/keywords/"
  private val WATSON_MAX_RESULT = Play.configuration.getInt("watson.maxSearchResult").getOrElse(100)

  override def getOntologyList(keyword: String): Future[Seq[String]] = {
    import ExecutionContexts.internetIOOps
    val resultListResponseFuture: Future[Response] = fetchResults(keyword)
    resultListResponseFuture.map {
      response => toUrlList(response)
    }.recover {
      case ex: Throwable => Seq.empty[String]
    }
  }

  private def toUrlList(r: Response): Seq[String] = {
    try {
      val json = r.json
      val elements = (json \ "SemanticContent-array" \ "SemanticContent" ).asOpt[JsArray].getOrElse(JsArray())
      elements.value.map {
        element =>
          val link = element.asOpt[String]
          link
      }.flatten.filterNot {
        x => x.contains("livejournal.com") || x.contains("deadjournal.com")
      }.take(WATSON_MAX_RESULT)
    } catch {
      case ex: Throwable => Seq.empty[String]
    }
  }

  private def fetchResults(searchQuery: String): Future[Response] = {
    import ExecutionContexts.internetIOOps
    Logger.info("Fetching WATSON")
    Logger.info(SEARCH_ONTOLOGY_API_URL + "?q=" + searchQuery)

    WS.url(SEARCH_ONTOLOGY_API_URL)
      .withQueryString(("q", searchQuery))
      .withHeaders(("Accept", "application/json")).get()
      .andThen {
        case Failure(response) =>
          Logger.error("Fetching WATSON FAILED")
          response
        case Success(response) =>
          Logger.info("Fetching WATSON completed")
          response
      }
  }
}
