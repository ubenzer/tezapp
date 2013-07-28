package service.storer

import service.ontologyFetcher.storer.OntologyStorer
import org.openrdf.model.{BNode, Value, URI, Resource}
import models._
import org.joda.time.DateTime
import com.mongodb.casbah.Imports._
import java.io.InputStream
import common.CryptoUtils
import service.persist.MongoDBUtils._

class SalatStorer() extends OntologyStorer {

   def saveElement(elementUri: Value) {
    if(elementUri.isInstanceOf[BNode]) return
    OntologyElement.collection.update(
      MongoDBObject({"_id" -> elementUri.stringValue}),
      MongoDBObject({"_id" -> elementUri.stringValue}, {"uDate" -> new DateTime()}),
      upsert = true)
  }
  def saveDocument(uri: String, sourceToAppend: List[String] = Nil, elementsToAppend: List[String] = Nil, triplesToAppend: List[ObjectId] = Nil) {
    val query = MongoDBObject({"_id" -> uri.toString})

    val update: DBObject =
      $set("uDate" -> new DateTime) ++
      $addToSet("appearsOn"   -> MongoDBObject("$each" -> MongoDBList(sourceToAppend:_*   )),
                "hasElements" -> MongoDBObject("$each" -> MongoDBList(elementsToAppend:_* )),
                "hasTriples"  -> MongoDBObject("$each" -> MongoDBList(triplesToAppend:_*  )))

    OntologyDocument.collection.update(query, update, upsert = true)
  }

  def saveTriple(sourceDocument: String, subject: Resource, predicate: URI, objekt: Value): ObjectId = {
    val subjectQ   = Some(MongoDBObject("subject" -> subject.toString))
    val predicateQ = MongoDBObject("predicate" -> predicate.toString)
    val objectkQ   =
        objekt match {
          /*case r: BNode => None  */
          case r: Resource => Some(MongoDBObject("objectO" -> Some(r.toString)))
          case r => Some(MongoDBObject("objectD" -> Some(r.stringValue)))
        }

    var queryQ = predicateQ
    if(subjectQ.isDefined) queryQ = queryQ ++ subjectQ.get
    if(objectkQ.isDefined) queryQ = queryQ ++ objectkQ.get

    val objectUQ = objekt match {
      case r: Resource => {
        (List({ "objectO" -> Some(r.stringValue) }, { "objectD" -> None }), Some(r))
      }
      case r => {
        (List({ "objectD" -> Some(r.stringValue) }, { "objectO" -> None }), None)
      }
    }

    var objectAppendList = List(predicate.toString)
    /*if(!subject.isInstanceOf[BNode])*/ objectAppendList ::= subject.stringValue
    if(objectUQ._2.isDefined /*&& !objectUQ._2.get.isInstanceOf[BNode]*/) objectAppendList ::= objectUQ._2.get.stringValue

    val update = MongoDBObject(
      {"elementUris" -> objectAppendList.toArray } ::
      {"subject"   -> subject.stringValue} ::
      {"predicate" -> predicate.stringValue} ::
      objectUQ._1
    )

    /* Find and replace here! */
    val result = OntologyTriple.collection.findAndModify(
      query = queryQ,
      update = update,
      upsert = true,
      fields = null,
      sort = null,
      remove = false,
      returnNew = true
    )

    result.get.get("_id").asInstanceOf[ObjectId]
  }

  def checkOntologyExists(ontologyUri: String): Boolean = OntologyDocument.collection.findOneByID(ontologyUri).isDefined
  def checkOntologyModified(ontologyUri: String, content: InputStream): Boolean = {
    val document = OntologyDocument.collection.findOne(MongoDBObject("_id" -> "ontologyUri"))
    if(!document.isDefined) return true
    val md5: String = document.get.get("md5").asInstanceOf[String]
    val calculatedMd5: String = CryptoUtils.md5(content)
    md5 == calculatedMd5
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
