package service.storer

import service.ontologyFetcher.storer.OntologyStorer
import org.openrdf.model.{Value, URI, Resource}
import models._
import org.joda.time.DateTime
import com.mongodb.casbah.Imports._

object SalatStorer extends OntologyStorer {

  private def saveElement(uri: Value) {
    OntologyElement.update(
      MongoDBObject({"_id" -> uri.stringValue}),
      MongoDBObject({"_id" -> uri.stringValue}, {"uDate" -> new DateTime()})
      , true, false)
  }
  private def saveDocument(uri: String, sourceToAppend: List[SourceType] = Nil, elementsToAppend: List[String] = Nil, triplesToAppend: List[ObjectId] = Nil) {

    val update1: DBObject =
      MongoDBObject("_id" -> uri.toString) ++
      MongoDBObject("uDate" -> new DateTime)

    val update2: DBObject = $addToSet("appearsOn") $each(sourceToAppend:_*)

    val update3 = $addToSet("hasElements") $each(elementsToAppend.toArray:_*)

    val update4 = $addToSet("hasTriples") $each (triplesToAppend.toArray:_*)

    /* Find and replace here! */
    OntologyDocument.dao.collection.findAndModify(
      query = MongoDBObject({"_id" -> uri.toString}),
      update = update1,
      upsert = true,
      fields = null,
      sort = null,
      remove = false,
      returnNew = true
    )

    OntologyDocument.dao.collection.findAndModify(
      query = MongoDBObject({"_id" -> uri.toString}),
      update = update2,
      upsert = true,
      fields = null,
      sort = null,
      remove = false,
      returnNew = true
    )

    OntologyDocument.dao.collection.findAndModify(
      query = MongoDBObject({"_id" -> uri.toString}),
      update = update3,
      upsert = true,
      fields = null,
      sort = null,
      remove = false,
      returnNew = true
    )

    OntologyDocument.dao.collection.findAndModify(
      query = MongoDBObject({"_id" -> uri.toString}),
      update = update4,
      upsert = true,
      fields = null,
      sort = null,
      remove = false,
      returnNew = true
    )
  }
  def saveTriple(sourceDocument: String, source: SourceType, subject: Resource, predicate: URI, objekt: Value) {

    saveElement(subject)
    saveElement(predicate)
    objekt match { case r: Resource => saveElement(r); case r => }

    val subjectQ   = {"subject" -> subject.toString}
    val predicateQ = {"predicate" -> predicate.toString}
    val objectkQ   =
        objekt match {
          case r: Resource => {
            { "objectO" -> Some(r.toString) }
          }
          case r => {
            { "objectD" -> Some(r.stringValue) }
          }
        }

    val query = MongoDBObject(subjectQ :: predicateQ :: objectkQ :: Nil)

    val objectUQ = objekt match {
      case r: Resource => {
        (List({ "objectO" -> Some(r.toString) }, { "objectD" -> None }), r.toString :: Nil)
      }
      case r => {
        (List({ "objectD" -> Some(r.stringValue) }, { "objectO" -> None }), Nil)
      }
    }

    val elementUris:Array[String] = (subject.toString :: predicate.toString :: objectUQ._2) toArray


    val update = MongoDBObject(
      {"elementUris" -> elementUris } ::
      {"subject"   -> subject.toString} ::
      {"predicate" -> predicate.toString} ::
      objectUQ._1
    )

    /* Find and replace here! */
    val result = OntologyTriple.dao.collection.findAndModify(
      query = query,
      update = update,
      upsert = true,
      fields = null,
      sort = null,
      remove = false,
      returnNew = true
    )

    val objectAppendList = (subject.toString :: predicate.toString :: objectUQ._2)
    var tripleAppendList: List[ObjectId] = Nil
    if(result.isDefined) {
      tripleAppendList = result.get.get("_id").asInstanceOf[ObjectId] :: Nil
    }

    saveDocument(sourceDocument, List(source), objectAppendList, tripleAppendList)
  }
}
