package service.ontologyFetcher.storer

import org.openrdf.model.{Value, URI, Resource}
import com.mongodb.casbah.Imports._

abstract class OntologyStorer() {
  def saveDocument(ontologyUri: String, md5: String, sourceToAppend: List[String] = Nil, elementsToAppend: List[String] = Nil, triplesToAppend: List[ObjectId] = Nil)
  def saveElement(elementUri: Value)
  def saveTriple(ontologyUri: String, subject: Resource, predicate: URI, objekt: Value): ObjectId

  def addSourceToOntology(ontologyUri: String, sourceToAppend: List[String])

  def checkOntologyExists(ontologyUri: String): Boolean
  def checkOntologyModified(ontologyUri: String, md5: String): Boolean
  def deleteOntology(ontologyUri: String)
}