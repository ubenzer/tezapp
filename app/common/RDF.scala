package common

object RDF {
  val Comment = "http://www.w3.org/2000/01/rdf-schema#comment"
  val Label = "http://www.w3.org/2000/01/rdf-schema#label"
  val Type = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
  val Property = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property"
  val ObjectProperty = "http://www.w3.org/2002/07/owl#ObjectProperty"
  val Ontology = "http://www.w3.org/2002/07/owl#Ontology"
  val Class = "http://www.w3.org/2002/07/owl#Class"
  val RDFClass = "http://www.w3.org/2000/01/rdf-schema#Class"
  val AnnotationProperty = "http://www.w3.org/2002/07/owl#AnnotationProperty"
  val DisjointWith = "http://www.w3.org/2002/07/owl#disjointWith"
  val SubclassOf = "http://www.w3.org/2000/01/rdf-schema#subClassOf"
  val Range = "http://www.w3.org/2000/01/rdf-schema#range"
  val Domain = "http://www.w3.org/2000/01/rdf-schema#domain"
  val Thing = "http://www.w3.org/2002/07/owl#Thing"
  val SubPropertyOf = "http://www.w3.org/2000/01/rdf-schema#subPropertyOf"
  val InverseOf = "http://www.w3.org/2002/07/owl#inverseOf"
}
object RDFNamespace {
  val RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns"
  val RDF_SCHEMA = "http://www.w3.org/2000/01/rdf-schema"
  val OWL = "http://www.w3.org/2002/07/owl"
}
object RDFType {
  val APP_SUPPORTED_TYPES = RDF.RDFClass :: RDF.Class :: RDF.Property :: RDF.Ontology :: RDF.Thing :: RDF.ObjectProperty :: Nil
}

object RDFExport {
  val INCLUDE_IN_ALL_EXPORTS = RDF.Comment :: RDF.Label :: RDF.Type :: RDF.Property :: RDF.Class :: RDF.AnnotationProperty :: Nil
  val COMMON_NAMESPACES: List[String] = RDFNamespace.RDF :: RDFNamespace.RDF_SCHEMA :: RDFNamespace.OWL :: Nil
}
