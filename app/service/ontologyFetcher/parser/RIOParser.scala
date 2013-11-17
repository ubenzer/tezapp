package service.parser

import org.openrdf.rio.{UnsupportedRDFormatException, RDFParseException, Rio, RDFFormat}
import org.openrdf.rio.helpers.{RDFHandlerBase}
import service.ontologyFetcher.Status
import service.ontologyFetcher.parser.OntologyParser
import play.Logger
import org.openrdf.model._
import service.ontologyFetcher.storer.OntologyStorageEngine
import common.{RewindableByteArrayInputStream, CryptoUtils}

class RIOParser(storer: OntologyStorageEngine) extends OntologyParser(storer) {

  override def parseStreamAsOntology(tbParsed: RewindableByteArrayInputStream, ontologyUri: String, format: RDFFormat, source: String): Status.Value = {

    // Check if ontology exists
    val isOntologyExists = storer.checkOntologyExists(ontologyUri)
    val md5 = CryptoUtils.md5(tbParsed)

    if(isOntologyExists) {
      val isOntologyModified = storer.checkOntologyModified(ontologyUri, md5)  // Check if ontology modified
      if(isOntologyModified) {
        storer.deleteOntology(ontologyUri) // If modified clean old ontology
      } else {
        storer.saveDocument(ontologyUri, md5, source) // We don't need to parse it again, just update results.
        return Status.Ok
      }
    }

    try {
      val handler = new RIOCustomHandler(storer, ontologyUri, source)
      val rdfParser = Rio.createParser(format)
      rdfParser.setRDFHandler(handler)

      // Save elements, triples and update document on demand. (we cant handle all list in one turn, if ontology is big.)
      rdfParser.parse(tbParsed, ontologyUri)

      storer.saveDocument(ontologyUri, md5, source)

    } catch {
      case ex: RDFParseException => {
        Logger.info("Couldn't parse ontology at " + ontologyUri, ex)
        return Status.NotParsable
      }
      case ex: UnsupportedRDFormatException => {
        Logger.info("Couldn't parse ontology because it is unsupported at " + ontologyUri, ex)
        return Status.NotParsable
      }
      case ex: Throwable => {
        Logger.error("Some exception occurred while parsing ontology at " + ontologyUri, ex)
        return Status.NotParsable
      }
    }
    Status.Ok
  }

}
class RIOCustomHandler(storer: OntologyStorageEngine, baseUri: String, source: String) extends RDFHandlerBase {

  override def handleStatement(st: Statement):Unit = {

    val subject: Resource = st.getSubject
    val predicate: URI = st.getPredicate
    val objekt: Value = st.getObject

    val bNodeLookup = collection.mutable.Map[String, String]()

    storer.saveTriple(baseUri, subject, predicate, objekt, source)(bNodeLookup)
  }
}

