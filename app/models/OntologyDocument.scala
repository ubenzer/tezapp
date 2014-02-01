package models

import org.joda.time.DateTime
import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.bson._
import scala.concurrent.Future
import play.api.Logger
import reactivemongo.bson.BSONDateTime
import scala.Some
import reactivemongo.api.collections.default.BSONCollection
import common.ExecutionContexts.verySlowOps
import play.api.Play.current

case class OntologyDocument (
                              uri           : String,       // uri of document
                              appearsOn     : Set[String] = Set.empty,  // Swoogle, Watson etc.
                              md5           : String,
                              isLocked      : Boolean = false,
                              cDate         : DateTime = new DateTime
                              ) {
  def save() = OntologyDocument.save(this)
}

object OntologyDocument {
  val collection: BSONCollection = ReactiveMongoPlugin.db.collection[BSONCollection]("OntologyDocument")

  implicit object OntologyDocumentBSONReader extends BSONDocumentReader[OntologyDocument] {
    def read(doc: BSONDocument): OntologyDocument = {
      OntologyDocument(
        uri       = doc.getAs[String ]("_id").get,
        appearsOn = doc.getAs[Set[String]]("appearsOn").toSet.flatten,
        md5       = doc.getAs[String]("md5").get,
        isLocked  = doc.getAs[Boolean]("isLocked").getOrElse(false),
        cDate     = doc.getAs[BSONDateTime]("cDate").map(dt => new DateTime(dt.value)).getOrElse(new DateTime)
      )
    }
  }

  implicit object OntologyDocumentBSONWriter extends BSONDocumentWriter[OntologyDocument] {
    def write(document: OntologyDocument): BSONDocument = {
      BSONDocument(
        "_id" -> document.uri,
        "appearsOn" -> document.appearsOn,
        "md5" -> document.md5,
        "isLocked" -> document.isLocked,
        "cDate" -> BSONDateTime(document.cDate.getMillis)
      )
    }
  }

  def get(uri: String): Future[Option[OntologyDocument]] = {
    collection.find(
      BSONDocument("_id" -> uri)
    ).one[OntologyDocument]
  }
  def save(ontologyDocument: OntologyDocument): Future[Boolean] = {
    collection.save(ontologyDocument).map {
      _ => true
    } recover {
      case e:Throwable => {
        Logger.error("Save failed for ontologyDocument: " + ontologyDocument + " The error is: " + e)
        false
      }
    }
  }

  def mergeSave(ontologyDocument: OntologyDocument): Future[Boolean] = {
    val selector = BSONDocument("_id" -> ontologyDocument.uri)
    val update: BSONDocument = BSONDocument(
      "$set" -> BSONDocument(
        "cDate" -> BSONDateTime(ontologyDocument.cDate.getMillis),
        "md5"   -> ontologyDocument.md5
      ),
      "$addToSet" -> BSONDocument(
        "appearsOn" -> BSONDocument(
          "$each" -> BSONArray(ontologyDocument.appearsOn)
        )
      )
    )

    collection.update(
      selector = selector,
      update = update,
      upsert = true
    ).map {
      _ => true
    } recover {
      case e:Throwable => {
        Logger.error("mergeSave failed for ontologyDocument: " + ontologyDocument + " The error is: " + e)
        false
      }
    }
  }

  def remove(uri: String): Future[Boolean] = {
    collection.remove(
      query = BSONDocument("_id" -> uri),
      firstMatchOnly = true
    ).map {
      _ => true
    } recover {
      case e:Throwable => {
        Logger.error("Remove failed for ontologyDocument: " + uri + " The error is: " + e)
        false
      }
    }
  }


  def lock(uri: String): Future[Boolean] = {
    val selector = BSONDocument("_id" -> uri)
    val update: BSONDocument = BSONDocument(
      "$set" -> BSONDocument(
        "isLocked" -> true
      )
    )

    collection.update(
      selector = selector,
      update = update
    ).map {
      _ => true
    } recover {
      case e:Throwable => {
        Logger.error("Locking failed for ontologyDocument: " + uri + " The error is: " + e)
        false
      }
    }
  }
  def isLocked(uri: String): Future[Boolean] = {
    get(uri).map {
      case Some(ontologyDocument) => ontologyDocument.isLocked
      case None => false
    }
  }
  def isExists(uri: String): Future[Boolean] = {
    get(uri).map {
      case Some(ontologyDocument) => true
      case None => false
    }
  }
  def isModified(uri: String, md5: String): Future[Boolean] = {
    get(uri).map {
      case Some(ontologyDocument) => {
        if(ontologyDocument.md5 != md5) {
          Logger.info("Ontology '" + uri + "' is modified.")
          true
        } else { false }
      }
      case None => true
    }
  }
}