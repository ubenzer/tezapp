package service.ontologyFetcher.storer

import org.openrdf.model.{Value, URI, Resource}

abstract class OntologyStorer protected {
  def saveTriple(subject: Resource, predicate: URI, objekt: Value): Unit
}
