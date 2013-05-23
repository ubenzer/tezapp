package models

import play.api.Play.current
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import mongoContext._
import org.joda.time._

case class DBResource (
  id          : ObjectId = new ObjectId,
  uri         : String
)
case class OntologyFiles (
  id          : ObjectId = new ObjectId,
  uri         : String,
  source      : SourceType,
  createDate  : DateTime
)
case class DBTriple (
  id         : ObjectId = new ObjectId,
  resources  : Array[ObjectId],

  subject    : ObjectRes,
  predicate  : ObjectRes,
  `object`   : LightResource
)
class LightResource
case class ObjectRes(uri: String, id: ObjectId)   extends LightResource
case class VariableRes(value: String) extends LightResource


class SourceType()
case class Swoogle() extends SourceType
case class Watson() extends SourceType

object DBTriples extends ModelCompanion[DBTriple, ObjectId] {
  mongoCollection("triples").ensureIndex(DBObject("resources" -> 1), "res_idx", true)
  val dao = new SalatDAO[DBTriple, ObjectId](collection = mongoCollection("triples")) {}
}
object DBResources extends ModelCompanion[DBResource, ObjectId] {
  mongoCollection("resources").ensureIndex(DBObject("uri" -> 1), "uri_idx", true)
  val dao = new SalatDAO[DBResource, ObjectId](collection = mongoCollection("resources")) {}
}