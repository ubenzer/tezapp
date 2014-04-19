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
import play.api.libs.json.Reads._
import play.api.Logger
import common.{RDF, RDFExport}
import service.FetchResult

object Test extends Controller {
  implicit val DisplayableElementWrites = Json.writes[DisplayableElement]
  implicit val SearchResultWrites = Json.writes[SearchResult]
  implicit val fetchResultWrites = Json.writes[FetchResult]

  def swoogle(keyword: String) = Action.async {
    if(keyword.length < 1) {
      Future.successful(BadRequest)
    } else {
      OntologyFetcher.SwoogleFetcher.search(keyword).map {
        result => Ok(result.toString)
      }
    }
  }

  def watson(keyword: String) = Action.async {
    if(keyword.length < 1) {
      Future.successful(BadRequest)
    } else {
      OntologyFetcher.WatsonFetcher.search(keyword).map {
        result => Ok(result.toString)
      }
    }
  }

  def sindice(keyword: String) = Action.async {
    if(keyword.length < 1) {
      Future.successful(BadRequest)
    } else {
      OntologyFetcher.SindiceFetcher.search(keyword).map {
        result => Ok(result.toString)
      }
    }
  }

  def find(keyword: String) = Action.async {
    Search.findElementsByKeyword(keyword).map {
      r =>
        Ok(Json.toJson(r))
    }
  }

  implicit val searchObjectRead: Reads[(List[String], String, Boolean)] =
    {
      (__ \ "elements").read[List[String]] and
      (__ \ "properties" \ "format").read[String] and
      (__ \ "properties" \ "extended").read[Boolean]
    }.tupled
  def export() = Action(parse.json) {
    request =>
      request.body.validate[(List[String], String, Boolean)].map {
        case (elements: List[String], format: String, extended: Boolean) =>

          val formatObj = Option(RDFFormat.valueOf(format))

          val in = new PipedInputStream()
          val out = new PipedOutputStream(in)

          def isBlankNode(uri: String) = uri.indexOf(':') < 0

          val nonCommonElements = elements.filterNot(x => RDFExport.isUriACommonOntologyThing(x))
          OntologyTriple.getTriplesThatIncludes(nonCommonElements).flatMap {
           triples =>
            val withExportTriples: Future[Set[OntologyTriple]] = Future.sequence {
              triples.map {
                triple =>
                  val allExportedKinds: Future[List[OntologyTriple]] = Future.sequence {
                    (if(extended) { RDFExport.INCLUDE_IN_ALL_EXPORTS_EXTENDED } else { RDFExport.INCLUDE_IN_ALL_EXPORTS }).map {
                      include =>
                        OntologyTriple.getTriple(Some(triple.subject), Some(include))
                    }
                  }.map { x => x.flatten }
                  allExportedKinds
              }
            }.map { x => x.toSet.flatten }
            withExportTriples.map {
              x => x ++ triples
            }
          }.map {
            triples =>
              Logger.info("Starting writing ontology for export...")
              val writer: RDFWriter = Rio.createWriter(formatObj.getOrElse(RDFFormat.RDFXML), out)
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
      e => BadRequest(JsError.toFlatJson(e))
    }
  }
}
