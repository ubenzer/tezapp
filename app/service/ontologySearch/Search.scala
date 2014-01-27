package service.ontologySearch

import models.{OntologyTriple, OntologyElement}
import com.mongodb.casbah.Imports._

object Search {

  def findElementsByKeyword(kw: String) = {
    val cr1 = OntologyElement.collection.db.command(DBObject("text" -> "OntologyElement") ++ DBObject("search" -> kw, "filter" -> DBObject("isBlankNode" -> false)))
    val cr2 = OntologyTriple.collection.db.command(DBObject("text" -> "OntologyTriple") ++ DBObject("search" -> kw))

    cr1.result()

    (cr1, cr2)
  }
  def findSubject(predicate: String, objekt: String) = {}
  def findPredicate(predicate: String, objekt: String) = {}
  def findObject(predicate: String, objekt: String) = {}

  def recursive(limit: Int=5)() = {}
}
