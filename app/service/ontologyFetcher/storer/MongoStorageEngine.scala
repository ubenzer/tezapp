package service.ontologyFetcher.storer

import common.Utils
import service.ontologyFetcher.storer.OntologyStorageEngine
import org.openrdf.model._
import models._
import org.joda.time.DateTime
import scala.Some
import scala.concurrent.Future
import reactivemongo.bson.BSONObjectID
import play.api.Logger
import common.ExecutionContexts.verySlowOps

class MongoStorageEngine() extends OntologyStorageEngine {

  override def saveDocument(uri: String, md5: String, source: String): Future[Boolean] = {
    OntologyDocument.mergeSave(
      OntologyDocument(
        uri = uri,
        md5 = md5,
        appearsOn = Set(source),
        cDate = new DateTime
      )
    )
  }
  def getResourceId(value: Value)(bNodeLookup: collection.mutable.Map[String, String]): String = {
    val maybeId = value match {
      case r: BNode =>
        val uuid = Utils.uuid
        val id = bNodeLookup.getOrElse(r.getID, uuid)
        if(id == uuid) {
          bNodeLookup += {r.getID -> uuid}
        }
        Some(id)
      case r: URI => Some(r.stringValue)
      case r: Literal => Some(r.stringValue)
      case r => None
    }
    if(!maybeId.isDefined) { throw new RuntimeException("Can't generate an id") }
    maybeId.get
  }
  override def saveTriple(sourceDocument: String, subject: Resource, predicate: URI, objekt: Value, source: String)(bNodeLookup: collection.mutable.Map[String, String]): Future[Option[BSONObjectID]] = {
    /* What kind of object we are face with? Blank node? Damn blank node,
      we need unique id's for them.

      Data? That's ok.
     */
    val subjectId      = getResourceId(subject)(bNodeLookup)
    val predicateId    = predicate.stringValue
    val objectId       = getResourceId(objekt)(bNodeLookup)
    val objectIsData   = isData(objekt)
    val objectLanguage = if(objectIsData && objekt.isInstanceOf[Literal]) {
      Option(objekt.asInstanceOf[Literal].getLanguage)
    } else None


    /* Find and replace here! */
    OntologyTriple.mergeSave(OntologyTriple(
      subject = subjectId,
      predicate = predicateId,
      objekt = objectId,
      isObjectData = objectIsData,
      objectLanguage = objectLanguage,
      appearsOn = Set(source),
      sourceOntology = Set(sourceDocument)
    )).map {
      case None => None
      case Some(triple) => triple.id
    }
  }

  def deleteOntology(uri: String): Future[Boolean] = {
    OntologyDocument.isExists(uri).flatMap {
      exists =>
        if(exists) {
          Logger.info("Removing " + uri)
          OntologyDocument.lock(uri).map {
            lockStatus =>
              if(!lockStatus) { false } else {
                OntologyTriple.removeAllTriplesOfAnOntology(uri).map {
                  lockStatus =>
                    if(!lockStatus) { false } else {
                      OntologyDocument.remove(uri)
                    }
                }
                true
              }
          }

        } else {
          Future.successful(true)
        }
    }
  }
}
