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

  implicit val searchObjectRead: Reads[(List[String], String)] =
    {
      (__ \ "elements").read[List[String]] and
      (__ \ "properties" \ "format").read[String]
    }.tupled
  def export() = Action(parse.json) {
    request =>
      request.body.validate[(List[String], String)].map {
        case (elements: List[String], format: String) =>

          val formatObj = Option(RDFFormat.valueOf(format))

          val in = new PipedInputStream()
          val out = new PipedOutputStream(in)

          def isBlankNode(uri: String) = uri.indexOf(':') < 0

          OntologyTriple.getTriplesThatIncludes(elements).flatMap {
           triples =>
            val withExportTriples: Future[Set[OntologyTriple]] = Future.sequence {
              triples.map {
                triple =>
                  val allExportedKinds: Future[List[OntologyTriple]] = Future.sequence {
                    RDFExport.INCLUDE_IN_ALL_EXPORTS.map {
                      include =>
                        val exportedKind: Future[List[OntologyTriple]] = OntologyTriple.getRecursive(triple.subject, 10) {
                          subject: String =>
                            OntologyTriple.getTriple(Some(subject), Some(include))
                        }{
                          x => if(x.isObjectData) { List.empty } else { List(x.objekt) }
                        }
                        exportedKind
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

  implicit val getRelatedElementsRead: Reads[(String, String)] =
    {
      (__ \ "uri").read[String] and
      (__ \ "what").read[String]
    }.tupled
  val availableRelationTypes = Map(
      "disjointWith"-> RDF.DisjointWith,
      "range" -> RDF.Range,
      "domain" -> RDF.Domain,
      "subclassOf" -> RDF.SubclassOf,

      "type" -> RDF.Type
    )
  def getRelatedElementsByObject = Action.async(parse.json) {
    request =>
      request.body.validate[(String, String)].map {
        case (uri: String, relationType: String) =>
         availableRelationTypes.get(relationType) match {
           case Some(rt: String) =>
              OntologyTriple.getObject(uri, rt).flatMap {
                elements =>
                  Future.sequence {
                    elements.map {
                      element => OntologyTriple.getDisplayableElement(element)
                    }
                  }
              }.map {
                setOfMaybeDisplayableElements =>setOfMaybeDisplayableElements.flatten
              }.map {
                displayableElements =>
                  Ok(Json.toJson(displayableElements.toSet))
              }
           case _ =>  Future.successful {
             BadRequest("Invalid type")
           }
          }
    }.recoverTotal {
      e => Future.successful {
        BadRequest(JsError.toFlatJson(e))
      }
    }
  }
  def getRelatedElementsBySubject = Action.async(parse.json) {
    request =>
      request.body.validate[(String, String)].map {
        case (uri: String, relationType: String) =>
         availableRelationTypes.get(relationType) match {
           case Some(rt: String) =>
              OntologyTriple.getSubject(rt, uri).flatMap {
                elements =>
                  Future.sequence {
                    elements.map {
                      element => OntologyTriple.getDisplayableElement(element)
                    }
                  }
              }.map {
                setOfMaybeDisplayableElements =>setOfMaybeDisplayableElements.flatten
              }.map {
                displayableElements =>
                  Ok(Json.toJson(displayableElements))
              }
           case _ => Future.successful {
             BadRequest("Invalid type")
           }
          }
    }.recoverTotal {
      e => Future.successful {
        BadRequest(JsError.toFlatJson(e))
      }
    }
  }
}
