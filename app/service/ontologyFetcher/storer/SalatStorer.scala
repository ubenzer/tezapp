package service.storer

import service.ontologyFetcher.storer.OntologyStorer
import org.openrdf.model.{Value, URI, Resource}
import models._
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.commons.ValidBSONType.ObjectId
import org.bson.types.ObjectId
import models.ObjectRes
import models.DBTriple
import models.DBResource

object SalatStorer extends OntologyStorer {
  def saveTriple(subject: Resource, predicate: URI, objekt: Value) {

    /* Find if resource already exists */
    val dbSubject = DBResources.findOne(MongoDBObject({"uri" -> subject.stringValue})).getOrElse(new DBResource(uri=subject.stringValue))
    DBResources.save(dbSubject)

    val dbPredicate = DBResources.findOne(MongoDBObject({"uri" -> predicate.stringValue})).getOrElse(new DBResource(uri=predicate.stringValue))
    DBResources.save(dbPredicate)



    val dbObject: Either[DBResource, String] =

    objekt match {
      case r: Resource => {
        val dbObject = DBResources.findOne(MongoDBObject({"uri" -> r.stringValue})).getOrElse(new DBResource(uri=r.stringValue))
        DBResources.save(dbObject)
        Left(dbObject)
      }
      case r => {
        Right(r.stringValue)
      }
    }

    var theArray : Array[ObjectId] = if(dbObject.isLeft) Array(dbSubject.id, dbPredicate.id, dbObject.left.get.id) else  Array(dbSubject.id, dbPredicate.id)

    val triple = DBTriples.findOne(MongoDBObject({"resources" -> theArray})).getOrElse(new DBTriple(
      resources = theArray,

      subject   = new ObjectRes(uri = dbSubject.uri, id= dbSubject.id),
      predicate = new ObjectRes(uri = dbPredicate.uri, id= dbPredicate.id),
      `object`  = if(dbObject.isLeft) { val o = dbObject.left.get; new ObjectRes(uri=o.uri, id= o.id) } else { new VariableRes(value= dbObject.right.get) }
    ))
    DBTriples.save(triple)
  }
}
