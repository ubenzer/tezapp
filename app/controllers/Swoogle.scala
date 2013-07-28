package controllers

import play.api.mvc._
import service.ontologyFetcher.{OntologyFetcher}

object Swoogle extends Controller {

  def submit(keyword: String) = Action {
    if(keyword.length < 1) BadRequest

    val stats = OntologyFetcher.SwoogleFetcher.doOntologyFetchingFor(keyword)

    Ok(stats.toString)
  }
}
