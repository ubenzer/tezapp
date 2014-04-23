package service.ontologyFetcher

import play.api.{Play, Logger}
import scala.concurrent.Future
import play.api.libs.ws.{Response, WS}
import service.ontologyFetcher.parser.OntologyParser
import common.ExecutionContexts
import scala.util.{Success, Failure}
import play.api.Play.current
import play.api.libs.json.{JsArray, JsValue}

class SindiceFetcher(parser: OntologyParser) extends OntologyFetcher(parser) {
  private val SEARCH_ONTOLOGY_API_URL = "http://api.sindice.com/v3/search"
  private val SINDICE_MAX_RESULT_PAGE = Play.configuration.getInt("sindice.maxSearchResultPage").getOrElse(100)

  private def getNormalizedSearchResultCount(json: JsValue): Int = {
    val count = (json \ "totalResults").asOpt[Int].getOrElse(0)
    val itemsPerPage = (json \ "itemsPerPage").asOpt[Int].getOrElse(0)

    if(count < 1 || itemsPerPage < 1) {
      0
    } else {
      val realPageCount = Math.ceil(count / itemsPerPage).toInt
      if(realPageCount > SINDICE_MAX_RESULT_PAGE) {
        SINDICE_MAX_RESULT_PAGE
      } else {
        realPageCount
      }
    }
  }
  override def getOntologyList(keyword: String): Future[Seq[String]] = {
    import ExecutionContexts.internetIOOps

    val firstPageRequestF = fetchAPage(keyword, 1)

    val searchPageCountF = firstPageRequestF.map {
      firstPageRequest =>
        val json = firstPageRequest.json
        getNormalizedSearchResultCount(json)
    }

    val convertF = searchPageCountF.flatMap {
      case pc if pc > 0 =>

        val urlListResponseFutures: Seq[Future[Response]] = pc match {
          case 1 => Seq(firstPageRequestF)
          case pcc if pcc > 2 =>
            Seq(firstPageRequestF) ++ {
              for (i: Int <- 2 to pc)
              yield fetchAPage(keyword, i + 1)
            }
        }

        val urlListFutures: Seq[Future[Seq[String]]] = urlListResponseFutures.map {
          responseFuture: Future[Response] =>
            responseFuture.map {
              response => toUrlList(response)
            }.recover {
              case ex: Throwable => Seq.empty[String]
            }
          }

        val urlListFuture: Future[Seq[Seq[String]]] = Future.sequence(urlListFutures)
        val urlListFlatterned: Future[Seq[String]] = urlListFuture.map {
          x => x.flatten
        }
        urlListFlatterned
      case _ => Future.successful(Seq.empty[String])
    } recover {
      case ex: Throwable =>
        Logger.error("Search engine is totally failed.", ex)
        Seq.empty[String]
    }
    convertF
  }

  private def toUrlList(r: Response): Seq[String] = {
    try {
      val xml = r.xml
      for (node <- xml \\ "SemanticWebDocument" \\ "@{http://www.w3.org/1999/02/22-rdf-syntax-ns#}about")
      yield node.text
    } catch {
      case ex: Throwable => Seq.empty[String]
    }

    try {
      val json = r.json
      val elements = (json \ "entries").asOpt[JsArray].getOrElse(JsArray())
      elements.value.map {
        element =>
          val link = (element \ "link").asOpt[String]
          link
      }.flatten
    } catch {
      case ex: Throwable => Seq.empty[String]
    }
  }

  private def fetchAPage(searchQuery: String, page: Int): Future[Response] = {
    import ExecutionContexts.internetIOOps
    Logger.info("Fetching SINDICE starting page " + page)
    Logger.info(SEARCH_ONTOLOGY_API_URL + "?fq=format:RDF&format=json&q=" + searchQuery + "&page=" + String.valueOf(page))

    val pageF = WS.url(SEARCH_ONTOLOGY_API_URL)
      .withQueryString(("fq", "format:RDF"))
      .withQueryString(("format", "json"))
      .withQueryString(("q", searchQuery))
      .withQueryString(("page", String.valueOf(page))).get()
      .andThen {
        case Failure(response) =>
          Logger.error("Fetching SINDICE FAILED page " + page)
          response
        case Success(response) =>
          Logger.info("Fetching SINDICE completed page " + page)
          response
      }
    pageF
  }
}
