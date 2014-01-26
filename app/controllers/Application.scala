package controllers

import play.api.mvc._
import play.api.mvc.Controller
import play.api.libs.json._
import play.api.libs.functional.syntax._
import service.ontologyFetcher.{OntologyFetcher}
import scala.concurrent.Future
import common.ExecutionContexts
import service.FetchResult

object Application extends Controller {

  def index = Action {
    Ok(views.html.main.render())
  }

  implicit val searchObjectRead: Reads[(List[String], Boolean)] =
    {
      (JsPath \ "keywords").read[List[String]] and
      (JsPath \ "offline").read[Boolean]
    }.tupled
  def search = Action(parse.json) {
    request =>
      request.body.validate[(List[String], Boolean)].map {
        case (keywords, offline) =>

          // Update database
          if(!offline) {
            val futureList: List[Future[FetchResult]] = keywords.map {
              OntologyFetcher.SwoogleFetcher.search(_)
            }
            import ExecutionContexts.verySlowOps
            Future.sequence(futureList)
          } else {
            Future.successful(List(FetchResult()))
          }

          // Do search db stuff here





          Ok(Json.obj("aha" -> "ehe"))

      }.recoverTotal {
        e => BadRequest("Detected error:" + JsError.toFlatJson(e))
      }
  }
}
