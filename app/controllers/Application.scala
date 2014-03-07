package controllers

import play.api.mvc._
import play.api.mvc.Controller
import play.api.libs.json._
import play.api.libs.functional.syntax._
import service.ontologyFetcher.OntologyFetcher
import scala.concurrent.Future
import common.ExecutionContexts.fastOps
import service.FetchResult
import service.ontologySearch.Search
import models.{SearchResult, DisplayableElement}
import org.openrdf.rio.RDFFormat

object Application extends Controller {
  implicit val searchObjectRead: Reads[(List[String], Boolean)] =
    {
      (JsPath \ "keywords").read[List[String]] and
      (JsPath \ "offline").read[Boolean]
    }.tupled
  implicit val DisplayableElementWrites = Json.writes[DisplayableElement]
  implicit val SearchResultWrites = Json.writes[SearchResult]
  implicit val fetchResultWrites = Json.writes[FetchResult]

  def index = Action {
    Ok(views.html.main.render())
  }

  def search = Action.async(parse.json) {
    request =>
      request.body.validate[(List[String], Boolean)].map {
        case (keywords, offline) =>

          // Update database if requested.
          (if(!offline) {
            val futureList: List[Future[FetchResult]] = keywords.map {
              keyword => OntologyFetcher.SwoogleFetcher.search(keyword.trim)
            } ++ keywords.map {
              keyword => OntologyFetcher.WatsonFetcher.search(keyword.trim)
            } ++ keywords.map {
              keyword => OntologyFetcher.SindiceFetcher.search(keyword.trim)
            }
            Future.sequence(futureList)
          } else {
            Future.successful(List(FetchResult()))
          }).flatMap { fetchResult =>
            // Do the actual search
            Search.findElementsByKeyword(keywords.mkString(" ")).map {
              searchResult =>
                Ok {
                  JsObject(Seq(
                    "fetchResults"  -> Json.toJson(fetchResult),
                    "searchResults" -> Json.toJson(searchResult)
                  ))
                }
            }
          }
      }.recoverTotal {
        e => Future.successful(BadRequest("Detected error:" + JsError.toFlatJson(e)))
      }
  }

  def getExportFormats = Action {
    import scala.collection.JavaConversions._
    val json = RDFFormat.values().toIterable.map {
      aFormat =>
        Json.obj(
          "name" -> aFormat.getName,
          "mime" -> aFormat.getDefaultMIMEType,
          "extension" -> aFormat.getDefaultFileExtension
        )
    }.toSeq
    Ok(JsArray(json))
  }
}
