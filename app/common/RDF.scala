package common

object RDF {
  val Comment = "http://www.w3.org/2000/01/rdf-schema#comment"
  val Label = "http://www.w3.org/2000/01/rdf-schema#label"
  val Type = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
  val Property = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property"
  val Ontology = "http://www.w3.org/2002/07/owl#Ontology"
  val Class = "http://www.w3.org/2002/07/owl#Class"
  val AnnotationProperty = "http://www.w3.org/2002/07/owl#AnnotationProperty"
}
object RDFNamespace {
  val RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns"
  val RDF_SCHEMA = "http://www.w3.org/2000/01/rdf-schema"
  val OWL = "http://www.w3.org/2002/07/owl"
}

object RDFExport {
  val INCLUDE_IN_ALL_EXPORTS = RDF.Comment :: RDF.Label :: RDF.Type :: RDF.Property :: RDF.Class :: RDF.AnnotationProperty :: Nil
  val COMMON_NAMESPACES = RDFNamespace.RDF :: RDFNamespace.RDF_SCHEMA :: RDFNamespace.OWL :: Nil
  def isUriACommonOntologyThing(uri: String): Boolean = {
    COMMON_NAMESPACES.exists(x => uri.startsWith(x))
  }
}
