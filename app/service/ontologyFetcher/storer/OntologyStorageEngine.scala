package service.ontologyFetcher.storer

import org.openrdf.model.{Value, URI, Resource}
import com.mongodb.casbah.Imports._

abstract class OntologyStorageEngine() {
  def saveDocument(ontologyUri: String, md5: String, source: String)
  def saveTriple(sourceDocument: String, subject: Resource, predicate: URI, objekt: Value, source: String)(bNodeLookup: collection.mutable.Map[String, String]): ObjectId
  def checkLock(ontologyURI: String): Boolean

  def checkOntologyExists(ontologyUri: String): Boolean
  def checkOntologyModified(ontologyUri: String, md5: String): Boolean
  def deleteOntology(ontologyUri: String)
}