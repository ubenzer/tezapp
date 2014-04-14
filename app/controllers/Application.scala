package controllers

import play.api.mvc._
import play.api.mvc.Controller
import play.api.libs.json._
import play.api.libs.functional.syntax._
import service.ontologyFetcher.OntologyFetcher
import scala.concurrent.Future
import common.ExecutionContexts.fastOps
import service.FetchResult
import service.ontologySearch.Search
import models.{OntologyTriple, SearchResult, DisplayableElement}
import org.openrdf.rio.RDFFormat
import common.RDF

object Application extends Controller {
  implicit val searchObjectRead: Reads[(List[String], Boolean)] =
    {
      (JsPath \ "keywords").read[List[String]] and
      (JsPath \ "offline").read[Boolean]
    }.tupled
  implicit val DisplayableElementWrites = Json.writes[DisplayableElement]
  implicit val SearchResultWrites = Json.writes[SearchResult]
  implicit val fetchResultWrites = Json.writes[FetchResult]

  def index = Action {
    Ok(views.html.main.render())
  }

  def search = Action.async(parse.json) {
    request =>
      request.body.validate[(List[String], Boolean)].map {
        case (keywords, offline) =>
          /*++ keywords.map {
            keyword => OntologyFetcher.WatsonFetcher.search(keyword.trim)
          } */
          // Update database if requested.
          (if(!offline) {
            val futureList: List[Future[FetchResult]] = keywords.map {
              keyword => OntologyFetcher.SwoogleFetcher.search(keyword.trim)
            } ++ keywords.map {
              keyword => OntologyFetcher.SindiceFetcher.search(keyword.trim)
            }
            Future.sequence(futureList)
          } else {
            Future.successful(List(FetchResult()))
          }).flatMap { fetchResult =>
            // Do the actual search
            Search.findElementsByKeyword(keywords.mkString(" ")).map {
              searchResult =>
                Ok {
                  JsObject(Seq(
                    "fetchResults"  -> Json.toJson(fetchResult),
                    "searchResults" -> Json.toJson(searchResult)
                  ))
                }
            }
          }
      }.recoverTotal {
        e => Future.successful(BadRequest("Detected error:" + JsError.toFlatJson(e)))
      }
  }

  def getExportFormats = Action {
    import scala.collection.JavaConversions._
    val json = RDFFormat.values().toIterable.map {
      aFormat =>
        Json.obj(
          "name" -> aFormat.getName,
          "mime" -> aFormat.getDefaultMIMEType,
          "extension" -> aFormat.getDefaultFileExtension
        )
    }.toSeq
    Ok(JsArray(json))
  }

  implicit val getRelatedElementsRead: Reads[(String, String)] =
    {
      (__ \ "uri").read[String] and
      (__ \ "predicate").read[String]
    }.tupled
  val searchableTypes = RDF.InverseOf :: RDF.DisjointWith :: RDF.Range :: RDF.Domain ::
    RDF.SubPropertyOf :: RDF.SubclassOf :: RDF.Type :: Nil
  def getRelatedElements(getWhat: String) = Action.async(parse.json) {
    request =>
      request.body.validate[(String, String)].map {
        case (uri: String, predicate: String) =>
          if(!searchableTypes.contains(predicate)) {
            Future.successful { BadRequest("Invalid 'type'") }
          } else {
            (
              if(getWhat == "object") {
                OntologyTriple.getObject(subject = uri, predicate = predicate)
              } else if(getWhat == "subject") {
                OntologyTriple.getSubject(predicate = predicate, objekt = uri)
              } else {
                Future.sequence {
                  OntologyTriple.getObject(subject = uri, predicate = predicate) ::
                    OntologyTriple.getSubject(predicate = predicate, objekt = uri) :: Nil
                }.map {
                  f => f.flatten
                }
              }
              ).flatMap {
              elements =>
                Future.sequence {
                  elements.map {
                    element => OntologyTriple.getDisplayableElement(element)
                  }
                }
            }.map {
              setOfMaybeDisplayableElements => setOfMaybeDisplayableElements.flatten
            }.map {
              displayableElements =>
                Ok(Json.toJson(displayableElements.toSet))
            }
          }
      }.recoverTotal {
        e => Future.successful {
          BadRequest(JsError.toFlatJson(e))
        }
      }
  }

}
