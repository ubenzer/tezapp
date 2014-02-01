package service.ontologyFetcher.storer

import org.openrdf.model._
import reactivemongo.bson.BSONObjectID
import scala.concurrent.Future

abstract class OntologyStorageEngine() {
  def saveDocument(uri: String, md5: String, source: String): Future[Boolean]
  def saveTriple(sourceDocument: String, subject: Resource, predicate: URI, objekt: Value, source: String)(bNodeLookup: collection.mutable.Map[String, String]): Future[Option[BSONObjectID]]

  def deleteOntology(ontologyUri: String): Future[Boolean]

  def isBlankNode(v: Value): Boolean = {
    v match {
      case r: BNode => true
      case _ => false
    }
  }
  def isData(v: Value): Boolean = {
    v match {
      case r: Literal  => true
      case _ => false
    }
  }
}