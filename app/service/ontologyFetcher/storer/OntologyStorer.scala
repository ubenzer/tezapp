package service.ontologyFetcher.storer

import org.openrdf.model.{Value, URI, Resource}
import models.SourceType

abstract class OntologyStorer protected {
  def saveTriple(sourceDocument: String, source: SourceType, subject: Resource, predicate: URI, objekt: Value) : Unit
}
