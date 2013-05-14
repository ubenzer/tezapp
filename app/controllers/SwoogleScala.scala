package controllers

import play.api.mvc._
import play.api.Logger
import service.ontologyFetcher.{OntologyFetcher}
import models.Resources

object SwoogleScala extends Controller {

  def submit(keyword: String) = Action {
    if(keyword.length < 1) BadRequest


    val theFetcher = OntologyFetcher.SwoogleFetcher

    val stats = theFetcher doOntologyFetchingFor keyword

    println(stats.toString)


    NotFound
  }
}
