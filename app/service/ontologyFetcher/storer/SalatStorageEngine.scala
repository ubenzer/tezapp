package service.storer

import common.Utils
import service.ontologyFetcher.storer.OntologyStorageEngine
import org.openrdf.model._
import models._
import org.joda.time.DateTime
import com.mongodb.casbah.Imports._
import service.persist.MongoDBUtils._
import scala.Some

class SalatStorageEngine() extends OntologyStorageEngine {

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
      $addToSet("appearsOn"       -> source)         ++
      $addToSet("sourceOntology"  -> sourceDocument) ++
      (if(objectIsData) {
        $set({ "objectD" -> Some(objectId) }, { "objectO" -> None })
      } else {
        $set({ "objectO" -> Some(objectId) }, { "objectD" -> None })
      })

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
        $set({"_id" -> sourceDocument}),
        $set({"_id" -> sourceDocument}) ++ $set({"uDate" -> new DateTime()}) ++
        $addToSet("appearsOn" -> source) ++ $addToSet("sourceOntology" -> sourceDocument),
      upsert = true)

    addTripleActionResult.get("_id").asInstanceOf[ObjectId]
  }

  def checkOntologyExists(ontologyUri: String): Boolean = OntologyDocument.collection.findOneByID(ontologyUri).isDefined
  def checkOntologyModified(ontologyUri: String, md5: String): Boolean = {
    val document = OntologyDocument.collection.findOne(MongoDBObject("_id" -> ontologyUri))
    if(!document.isDefined) return true
    val dbmd5: String = document.get.get("md5").asInstanceOf[String]
    md5 != dbmd5
  }
  def deleteOntology(ontologyUri: String): Unit = {
    val maybeDocument = OntologyDocument.collection.findOneByID(ontologyUri)
    if(!maybeDocument.isDefined) return

    val document = maybeDocument.get

    val elementList = document.getStringSet("hasElements")
    val tripleList = document.getObjectIdSet("hasTriples")

    val reducedElementList = elementList.filter {
      element =>
        val query: DBObject = ("_id" $ne ontologyUri) ++ DBObject("hasElements" -> element)
        OntologyDocument.collection.find(query).size == 0
    }
    val reducedTripleList = tripleList.filter {
      triple =>
        val query: DBObject = ("_id" $ne ontologyUri) ++ DBObject("hasTriples" -> triple)
        OntologyDocument.collection.find(query).size == 0
    }

    OntologyDocument.collection.remove(MongoDBObject("_id" -> ontologyUri))
    if(reducedElementList.size > 0) OntologyElement.collection.remove("_id" $in reducedElementList)
    if(reducedTripleList.size > 0) OntologyTriple.collection.remove("_id" $in reducedTripleList)
  }
}
