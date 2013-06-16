package service.storer

import service.ontologyFetcher.storer.OntologyStorer
import org.openrdf.model.{BNode, Value, URI, Resource}
import models._
import org.joda.time.DateTime
import com.mongodb.casbah.Imports._
import play.api.Logger
import com.mongodb.casbah.Imports
import scala.Some

object SalatStorer extends OntologyStorer {

  private def saveElement(uri: Value) {
    /*if(uri.isInstanceOf[BNode]) return */
    OntologyElement.dao.collection.update(
      MongoDBObject({"_id" -> uri.stringValue}),
      MongoDBObject({"_id" -> uri.stringValue}, {"uDate" -> new DateTime()}),
      upsert = true)
  }
  private def saveDocument(uri: String, sourceToAppend: List[String] = Nil, elementsToAppend: List[String] = Nil, triplesToAppend: List[ObjectId] = Nil) {

    val query = MongoDBObject({"_id" -> uri.toString})

    val update: DBObject =
      $set("uDate" -> new DateTime) ++
      $addToSet("appearsOn"   -> MongoDBObject("$each" -> MongoDBList(sourceToAppend:_*   )),
                "hasElements" -> MongoDBObject("$each" -> MongoDBList(elementsToAppend:_* )),
                "hasTriples"  -> MongoDBObject("$each" -> MongoDBList(triplesToAppend:_*  )))

    OntologyDocument.dao.collection.update(query, update, upsert = true)
  }
  def saveTriple(sourceDocument: String, source: String, subject: Resource, predicate: URI, objekt: Value) {

    saveElement(subject)
    saveElement(predicate)
    objekt match { case r: Resource => saveElement(r); case r => }

    val subjectQ   = /*if(!subject.isInstanceOf[BNode]) {*/ Some(MongoDBObject("subject" -> subject.toString)) /*} else { None }   */
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
    val result = OntologyTriple.dao.collection.findAndModify(
      query = queryQ,
      update = update,
      upsert = true,
      fields = null,
      sort = null,
      remove = false,
      returnNew = true
    )

    var tripleAppendList: List[ObjectId] = Nil
    if(result.isDefined) {
      tripleAppendList = result.get.get("_id").asInstanceOf[ObjectId] :: Nil
    }

    saveDocument(sourceDocument, List(source), objectAppendList, tripleAppendList)
  }
}
