package controllers

import play.api.mvc._
import service.ontologyFetcher.{OntologyFetcher}
import scala.concurrent.Future
import common.ExecutionContexts.fastOps
import service.ontologySearch.Search
import play.api.libs.json.Json
import models.SearchResult

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
    Future {
      Search.findElementsByKeyword(keyword)
    }.map {
      r =>
        implicit val fetchResultJson = Json.writes[SearchResult]
        Ok(Json.toJson(r))
    }
  }
}
