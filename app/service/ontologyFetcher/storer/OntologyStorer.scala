package service.ontologyFetcher.storer

import org.openrdf.model.{Value, URI, Resource}

abstract class OntologyStorer() {
  def saveTriple(sourceDocument: String, source: String, subject: Resource, predicate: URI, objekt: Value) : Unit
}
