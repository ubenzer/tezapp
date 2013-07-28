package service.parser

import org.openrdf.rio.{UnsupportedRDFormatException, RDFParseException, Rio, RDFFormat}
import org.openrdf.rio.helpers.{RDFHandlerBase}
import java.io.{InputStream}
import service.ontologyFetcher.Status
import service.ontologyFetcher.parser.OntologyParser
import play.Logger
import org.openrdf.model._
import service.ontologyFetcher.storer.OntologyStorer


class RIOParser(storer: OntologyStorer) extends OntologyParser(storer) {

  override def parseStreamAsOntology(tbParsed: InputStream, ontologyUri: String, format: RDFFormat, source: String): Status.Value = {

    // Check if ontology exists
    val isOntologyExists = storer.checkOntologyExists(ontologyUri)
    if(isOntologyExists) {
      val isOntologyModified = storer.checkOntologyModified(ontologyUri, tbParsed)  // Check if ontology modified
      if(isOntologyModified) storer.deleteOntology(ontologyUri) // If modified clean old ontology
    }

    try {
      val handler = new RIOCustomHandler(storer, ontologyUri, source)
      val rdfParser = Rio.createParser(format)
      rdfParser.setRDFHandler(handler)

      // Save elements, triples and update document on demand. (we cant handle all list in one turn, if ontology is big.)
      rdfParser.parse(tbParsed, ontologyUri)

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
class RIOCustomHandler(storer: OntologyStorer, baseUri: String, source: String) extends RDFHandlerBase {

  override def handleStatement(st: Statement):Unit = {

    val subject: Resource = st.getSubject
    val predicate: URI = st.getPredicate
    val objekt: Value = st.getObject

    /* Save elements, we don't like blank node here */
    if(!subject.isInstanceOf[BNode]) storer.saveElement(subject)
    storer.saveElement(predicate)
    if(objekt.isInstanceOf[Resource] && !objekt.isInstanceOf[BNode]) storer.saveElement(objekt)

    val savedId = storer.saveTriple(baseUri, subject, predicate, objekt)
    storer.saveDocument(baseUri, source :: Nil,
      (if(!subject.isInstanceOf[BNode]) { subject.toString :: Nil } else { Nil }) ::: predicate.toString :: (if (objekt.isInstanceOf[Resource]) { objekt.stringValue :: Nil } else { Nil }),
      savedId :: Nil
    )
  }
}

