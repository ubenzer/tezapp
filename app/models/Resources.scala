package models

import com.mongodb.casbah.Imports._
import org.joda.time._
import service.persist.MongoDB

case class OntologyDocument (
  id            : String, // uri of document
  uDate         : DateTime,
  appearsOn     : Seq[String]  // Swoogle, Watson etc.
)

case class OntologyElement (
  id            : String,   //uri of element
  uDate         : DateTime,
  sourceOntology: Seq[String]
)

case class OntologyTriple (
  id            : ObjectId = new ObjectId,
  subject       : String,
  predicate     : String,
  objectO       : Option[String],
  objectD       : Option[String],

  sourceOntology: Seq[String],

  // lets add some redundancy
  elementUris   : Array[String]
)

object OntologyDocument {
  val collection = MongoDB.db("OntologyDocument")
}
object OntologyElement {
  val collection = MongoDB.db("OntologyElement")
}
object OntologyTriple {
  val collection = MongoDB.db("OntologyTriple")
  collection.ensureIndex(DBObject("predicate" -> 1, "subject" -> 1, "objectO" -> 1), DBObject({"sparse" -> true}, {"name" -> "tripleIdxPSO"}))
}