package controllers

import play.api.mvc._
import service.ontologyFetcher.{OntologyFetcher}

object SwoogleScala extends Controller {

  def submit(keyword: String) = Action {
    if(keyword.length < 1) BadRequest


    val theFetcher = OntologyFetcher.SwoogleFetcher

    val stats = theFetcher.doOntologyFetchingFor(keyword, "swoogle")

    println(stats.toString)


    NotFound
  }
}
