package controllers

import play.api.mvc._
import service.ontologyFetcher.OntologyFetcher
import scala.concurrent.Future
import common.ExecutionContexts.fastOps
import models.{OntologyTriple, DisplayableElement, SearchResult}
import service.ontologySearch.Search
import org.openrdf.rio.{RDFFormat, Rio, RDFWriter}
import java.io.{PipedOutputStream, PipedInputStream}
import play.api.libs.iteratee.Enumerator
import org.openrdf.model.Statement
import org.openrdf.model.impl.{LiteralImpl, URIImpl, StatementImpl}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.Logger

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

  implicit val searchObjectRead: Reads[(List[String], String)] =
    {
      (JsPath \ "elements").read[List[String]] and
      (JsPath \ "properties" \ "format").read[String]
    }.tupled
  def export() = Action(parse.json) {
    request =>
      request.body.validate[(List[String], String)].map {
        case (elements: List[String], format: String) =>

          val in = new PipedInputStream()
          val out = new PipedOutputStream(in)

          def isBlankNode(uri: String) = uri.indexOf(':') < 0

          OntologyTriple.getTriplesThatIncludes(elements:_*).map {
            triples =>
              val writer: RDFWriter = Rio.createWriter(RDFFormat.RDFXML, out)
              writer.startRDF()

              triples.foreach {
                triple =>
                  if(!isBlankNode(triple.subject) && (triple.isObjectData || !isBlankNode(triple.objekt))) {

                    val s: Statement = new StatementImpl(
                      new URIImpl(triple.subject),
                      new URIImpl(triple.predicate),
                      if(triple.isObjectData) {
                        new LiteralImpl(triple.objekt)
                      } else {
                        new URIImpl(triple.objekt)
                      }
                    )
                    writer.handleStatement(s)

                  }
              }

              writer.endRDF()

              out.close()
          } recover {
            case e:Throwable =>
              Logger.error("Failed exporting ontology. Reason: " + e)
              out.close()
          }

          val dataContent: Enumerator[Array[Byte]] = Enumerator.fromStream(in)
          Ok.chunked(dataContent)

    }.recoverTotal {
      e => BadRequest("Detected error:" + JsError.toFlatJson(e))
    }
  }
}
