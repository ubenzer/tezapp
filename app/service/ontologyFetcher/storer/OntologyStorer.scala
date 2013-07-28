package service.ontologyFetcher.storer

import org.openrdf.model.{Value, URI, Resource}
import java.io.InputStream
import com.mongodb.casbah.Imports._

abstract class OntologyStorer() {
  def saveDocument(ontologyUri: String, sourceToAppend: List[String] = Nil, elementsToAppend: List[String] = Nil, triplesToAppend: List[ObjectId] = Nil)
  def saveElement(elementUri: Value)
  def saveTriple(ontologyUri: String, subject: Resource, predicate: URI, objekt: Value): ObjectId

  def checkOntologyExists(ontologyUri: String): Boolean
  def checkOntologyModified(ontologyUri: String, content: InputStream): Boolean
  def deleteOntology(ontologyUri: String)
}