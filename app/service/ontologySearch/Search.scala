package service.ontologySearch

import models.{OntologyTriple, OntologyElement}
import com.mongodb.casbah.Imports._

object Search {

  val specialElements =
   "http://www.w3.org/2000/01/rdf-schema#comment" :: Nil

  def findElementsByKeyword(kw: String) = {
    val cr1: DBObject = OntologyElement.collection.db.command(DBObject("text" -> "OntologyElement") ++ DBObject("search" -> kw, "filter" -> DBObject("isBlankNode" -> false)))
    val cr2: DBObject = OntologyTriple.collection.db.command(DBObject("text" -> "OntologyTriple") ++ DBObject("search" -> kw))

    val Z = cr2.get("results").asInstanceOf[DBObject]

    (Z.size, cr2)
  }
  def findSubject(predicate: String, objekt: String) = {}
  def findPredicate(predicate: String, objekt: String) = {}
  def findObject(predicate: String, objekt: String) = {}

  def recursive(limit: Int=5)() = {}
}
