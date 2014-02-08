package controllers

import play.api.mvc._
import service.ontologyFetcher.{OntologyFetcher}
import scala.concurrent.Future
import common.ExecutionContexts.fastOps
import play.api.libs.json.Json
import models.{DisplayableElement, SearchResult}
import service.ontologySearch.Search

object Test extends Controller {

  def swoogle(keyword: String) = Action.async {
    if(keyword.length < 1) {
      Future.successful(BadRequest)
    } else {
      OntologyFetcher.SwoogleFetcher.search(keyword).map {
        result => Ok(result.toString)
      }
    }
  }

  def find(keyword: String) = Action.async {
    Search.findElementsByKeyword(keyword).map {
      r =>
        implicit val iDisplayableElement = Json.writes[DisplayableElement]
        implicit val iSearchResult = Json.writes[SearchResult]
        Ok(Json.toJson(r))
    }
  }
}
