package controllers

import play.api.mvc._
import play.api.Logger
import service.ontologyFetcher.{OntologyFetcher}

object SwoogleScala extends Controller {

  def submit(keyword: String) = Action {
    if(keyword.length < 1) BadRequest

    val theFetcher = OntologyFetcher.SwoogleFetcher
    val list: Option[Set[String]] = theFetcher getOntologyList keyword

    for(definedList <- list) yield definedList.foreach(Logger.info(_))

    NotFound
  }
}
