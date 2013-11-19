package service.storer

import common.Utils
import service.ontologyFetcher.storer.OntologyStorageEngine
import org.openrdf.model._
import models._
import org.joda.time.DateTime
import com.mongodb.casbah.Imports._
import scala.Some

class SalatStorageEngine() extends OntologyStorageEngine {

  override def checkLock(ontologyURI: String): Boolean = {
    OntologyDocument.collection.find(DBObject("_id" -> ontologyURI, "locked" -> true)).size > 0
  }
  override def saveDocument(ontologyUri: String, md5: String, source: String) {
    val query = MongoDBObject({"_id" -> ontologyUri})

    val update: DBObject =
      $set({"uDate" -> new DateTime    }) ++
      $set({"md5" -> md5               }) ++
      $addToSet("appearsOn" -> source)

    OntologyDocument.collection.update(query, update, upsert = true)
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
    if(!maybeId.isDefined) { throw new RuntimeException("Can generate an id") }
    maybeId.get
  }
  override def saveTriple(sourceDocument: String, subject: Resource, predicate: URI, objekt: Value, source: String)(bNodeLookup: collection.mutable.Map[String, String]): ObjectId = {
    /* What kind of object we are face with? Blank node? Damn blank node,
      we need unique id's for them.

      Data? That's ok.
     */
    val subjectId    = getResourceId(subject)(bNodeLookup)
    val predicateId  = predicate.stringValue
    val objectId     = getResourceId(objekt)(bNodeLookup)
    val objectIsData = objekt match {
                         case r: Resource => false
                         case r: Literal  => true
                         case r => throw new RuntimeException("Can generate an id")
                       }

    val subjectQ   = MongoDBObject("subject" -> subjectId)
    val predicateQ = MongoDBObject("predicate" -> predicateId)
    val objectQ    = if(objectIsData) {
                       MongoDBObject("objectD" -> objectId)
                     } else {
                       MongoDBObject("objectO" -> objectId)
                     }

    val query  = predicateQ ++ subjectQ ++ objectQ
    val update =
      $set({"elementUris"         -> (predicateId :: subjectId :: (if(!objectIsData) { objectId } else { Nil }) :: Nil) }) ++
      $set({"subject"             -> subjectId   })  ++
      $set({"predicate"           -> predicateId })  ++
      $set({"uDate"               -> new DateTime})  ++
      (if(objectIsData) {
        $set({ "objectD" -> Some(objectId) }) ++ $set({ "objectO" -> None })
      } else {
        $set({ "objectO" -> Some(objectId) }) ++ $set({ "objectD" -> None })
      }) ++
      $addToSet("appearsOn"       -> source)         ++
      $addToSet("sourceOntology"  -> sourceDocument)


    /* Find and replace here! */
    val maybeAddTripleActionResult = OntologyTriple.collection.findAndModify(
      query = query,
      update = update,
      upsert = true,
      fields = null,
      sort = null,
      remove = false,
      returnNew = true
    )

    if(!maybeAddTripleActionResult.isDefined) { throw new RuntimeException("Can't save triple!") }
    val addTripleActionResult = maybeAddTripleActionResult.get

    /* Save element, too */
    OntologyElement.collection.update(
        DBObject({"_id" -> subjectId}),
        $set({"uDate" -> new DateTime()}) ++
        $addToSet("appearsOn" -> source) ++ $addToSet("sourceOntology" -> sourceDocument),
      upsert = true)

    OntologyElement.collection.update(
        DBObject({"_id" -> predicateId}),
        $set({"uDate" -> new DateTime()}) ++
        $addToSet("appearsOn" -> source) ++ $addToSet("sourceOntology" -> sourceDocument),
      upsert = true)

    if(!objectIsData) {
      OntologyElement.collection.update(
        DBObject({"_id" -> objectId}),
        $set({"uDate" -> new DateTime()}) ++
          $addToSet("appearsOn" -> source) ++ $addToSet("sourceOntology" -> sourceDocument),
        upsert = true)
    }

    addTripleActionResult.get("_id").asInstanceOf[ObjectId]
  }

  def checkOntologyExists(ontologyUri: String): Boolean = OntologyDocument.collection.findOneByID(ontologyUri).isDefined
  def checkOntologyModified(ontologyUri: String, md5: String): Boolean = {
    val document = OntologyDocument.collection.findOne(MongoDBObject("_id" -> ontologyUri))
    if(!document.isDefined) return true
    val dbmd5: String = document.get.get("md5").asInstanceOf[String]
    play.api.Logger.info("Ontology " + ontologyUri + " is modified: " + (md5 != dbmd5))
    md5 != dbmd5
  }
  def deleteOntology(ontologyUri: String): Unit = {
    val maybeDocument = OntologyDocument.collection.findOneByID(ontologyUri)
    if(!maybeDocument.isDefined) return

    play.api.Logger.info("Removing " + ontologyUri)

    // Lock if we are going to delete it.
    OntologyDocument.collection.update(DBObject("_id" -> ontologyUri), DBObject("locked" -> true))

    OntologyElement.collection.findAndModify(
      query  = DBObject("sourceOntology" -> ontologyUri) ++ ("sourceOntology" $gt 1),
      update = $pull("sourceOntology" -> ontologyUri),
      upsert = false,
      fields = null,
      sort = null,
      remove = false,
      returnNew = false
    )
    OntologyElement.collection.findAndModify(
      query  = DBObject("sourceOntology" -> ontologyUri),
      update = null,
      upsert = false,
      fields = null,
      sort = null,
      remove = true,
      returnNew = false
    )

    OntologyTriple.collection.findAndModify(
      query  = DBObject("sourceOntology" -> ontologyUri) ++ ("sourceOntology" $gt 1),
      update = $pull("sourceOntology" -> ontologyUri),
      upsert = false,
      fields = null,
      sort = null,
      remove = false,
      returnNew = false
    )
    OntologyTriple.collection.findAndModify(
      query  = DBObject("sourceOntology" -> ontologyUri),
      update = null,
      upsert = false,
      fields = null,
      sort = null,
      remove = true,
      returnNew = false
    )

    OntologyDocument.collection.remove(MongoDBObject("_id" -> ontologyUri))
  }
}
