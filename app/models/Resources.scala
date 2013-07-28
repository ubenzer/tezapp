package models

import com.mongodb.casbah.Imports._
import org.joda.time._
import service.persist.MongoDB

case class OntologyDocument (
  id          : String, // uri of document
  hasElements : Array[String],
  hasTriples  : Array[ObjectId],
  uDate       : DateTime,
  appearsOn   : Array[String]  // Swoogle, Watson etc.
)

case class BasicOntologyElement (
  id          : String    // uri of element
)
case class OntologyElement (
  id          : String,   //uri of element
  uDate       : DateTime
)

case class OntologyTriple (
  id          : ObjectId = new ObjectId,
  subject     : BasicOntologyElement,
  predicate   : BasicOntologyElement,
  objectO     : Option[BasicOntologyElement],
  objectD     : Option[String],
  elementUris : Array[String]
)

object OntologyDocument {
  val collection = MongoDB.db("OntologyDocument")
}
object OntologyElement {
  val collection = MongoDB.db("OntologyElement")
}
object OntologyTriple {
  val collection = MongoDB.db("OntologyTriple")
  collection.ensureIndex(DBObject("elementUris" -> 1), "elementAvailabilityIdx")
  collection.ensureIndex(DBObject("predicate" -> 1, "subject" -> 1, "objectO" -> 1), DBObject({"sparse" -> true}, {"name" -> "tripleIdxPSO"}))
  collection.ensureIndex(DBObject("predicate" -> 1, "objectO" -> 1), DBObject({"sparse" -> true}, {"name" -> "tripleIdxPO"}))
}