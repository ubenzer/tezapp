package controllers

import play.api.mvc._
import service.{SwoogleFetcher, OntologyFetcher}
import play.api.Logger

object SwoogleScala extends Controller {

  def submit(keyword: String) = Action {
    if(keyword.length < 1) BadRequest

    val theFetcher = new OntologyFetcher with SwoogleFetcher
    val list: Option[Set[String]] = theFetcher getOntologyList keyword

    for(definedList <- list) yield definedList.foreach(Logger.info(_))

    NotFound
  }
}
