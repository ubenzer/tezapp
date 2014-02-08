package controllers

import play.api.mvc._
import play.api.mvc.Controller
import play.api.libs.json._
import play.api.libs.functional.syntax._
import service.ontologyFetcher.{OntologyFetcher}
import scala.concurrent.Future
import common.ExecutionContexts.fastOps
import service.FetchResult
import service.ontologySearch.Search
import models.{SearchResult, DisplayableElement}

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
              OntologyFetcher.SwoogleFetcher.search(_)
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

          // Do search db stuff here




      }.recoverTotal {
        e => Future.successful(BadRequest("Detected error:" + JsError.toFlatJson(e)))
      }
  }
}
