package models

import play.api.Play.current
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import mongoContext._

case class Resources (
  id: ObjectId = new ObjectId,
  name: String
)

object Resources extends ModelCompanion[Resources, ObjectId] {
  val dao = new SalatDAO[Resources, ObjectId](collection = mongoCollection("resources")) {}
}