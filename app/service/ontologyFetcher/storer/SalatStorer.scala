package service.storer

import service.ontologyFetcher.storer.OntologyStorer
import org.openrdf.model.{Value, URI, Resource}
import models.Resources

object SalatStorer extends OntologyStorer {
  def saveTriple(subject: Resource, predicate: URI, objekt: Value) {
    Resources.insert(new Resources(name=subject.toString))
    Resources.insert(new Resources(name=predicate.toString))
    Resources.insert(new Resources(name=objekt.toString))
  }
}
