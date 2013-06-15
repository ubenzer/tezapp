package models

import play.api.Play.current
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import mongoContext._
import org.joda.time._

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

object OntologyDocument extends ModelCompanion[OntologyDocument, String] {
  val dao = new SalatDAO[OntologyDocument, String](collection = mongoCollection("OntologyDocument")) {}
}
object OntologyElement extends ModelCompanion[OntologyElement, String] {
  val dao = new SalatDAO[OntologyElement, String](collection = mongoCollection("OntologyElement")) {}
}
object OntologyTriple extends ModelCompanion[OntologyTriple, ObjectId] {
  mongoCollection("OntologyTriple").ensureIndex(DBObject("elementUris" -> 1), "elementAvailabilityIdx")
  mongoCollection("OntologyTriple").ensureIndex(DBObject("predicate" -> 1, "subject" -> 1, "objectO" -> 1), DBObject({"sparse" -> true}, {"name" -> "tripleIdxPSO"}))
  mongoCollection("OntologyTriple").ensureIndex(DBObject("predicate" -> 1, "objectO" -> 1), DBObject({"sparse" -> true}, {"name" -> "tripleIdxPO"}))
  val dao = new SalatDAO[OntologyTriple, ObjectId](collection = mongoCollection("OntologyTriple")) {}
}