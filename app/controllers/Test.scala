package controllers

import play.api.mvc._
import service.ontologyFetcher.{OntologyFetcher}
import scala.concurrent.Future
import common.ExecutionContexts.fastOps
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
    Future {
      Search.findElementsByKeyword(keyword)
    }.map {
      r =>
        Ok(r._1.toString + " ***** " + r._2.toString)
    }
  }
}
