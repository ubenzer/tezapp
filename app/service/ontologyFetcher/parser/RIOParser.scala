package service.parser

import org.openrdf.rio.{UnsupportedRDFormatException, RDFParseException, Rio, RDFFormat}
import org.openrdf.rio.helpers.{RDFHandlerBase}
import service.ontologyFetcher.Status
import service.ontologyFetcher.parser.OntologyParser
import play.Logger
import org.openrdf.model._
import service.ontologyFetcher.storer.OntologyStorer
import common.{RewindableByteArrayInputStream, CryptoUtils}
import com.mongodb.casbah.Imports._

class RIOParser(storer: OntologyStorer) extends OntologyParser(storer) {

  override def parseStreamAsOntology(tbParsed: RewindableByteArrayInputStream, ontologyUri: String, format: RDFFormat, source: String): Status.Value = {

    // Check if ontology exists
    val isOntologyExists = storer.checkOntologyExists(ontologyUri)
    val md5 = CryptoUtils.md5(tbParsed)

    if(isOntologyExists) {
      val isOntologyModified = storer.checkOntologyModified(ontologyUri, md5)  // Check if ontology modified
      if(isOntologyModified) {
        storer.deleteOntology(ontologyUri) // If modified clean old ontology
      } else {
        storer.addSourceToOntology(ontologyUri, source :: Nil) // We don't need to parse it again, just update results.
        return Status.Ok
      }
    }

    try {
      val handler = new RIOCustomHandler(storer, ontologyUri)
      val rdfParser = Rio.createParser(format)
      rdfParser.setRDFHandler(handler)

      // Save elements, triples and update document on demand. (we cant handle all list in one turn, if ontology is big.)
      rdfParser.parse(tbParsed, ontologyUri)

      val parseInfo = handler.getParseInfo
      storer.saveDocument(ontologyUri, md5, source :: Nil, parseInfo._1, parseInfo._2)

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
class RIOCustomHandler(storer: OntologyStorer, baseUri: String) extends RDFHandlerBase {

  private var elementList: List[String] = Nil
  private var tripleList: List[ObjectId] = Nil

  def getParseInfo: (List[String], List[ObjectId]) = (elementList, tripleList)

  override def handleStatement(st: Statement):Unit = {

    val subject: Resource = st.getSubject
    val predicate: URI = st.getPredicate
    val objekt: Value = st.getObject

    /* Save elements, we don't like blank node here */
    if(!subject.isInstanceOf[BNode]) {
      storer.saveElement(subject)
      elementList ::= subject.stringValue()
    }

    storer.saveElement(predicate)
    elementList ::= predicate.stringValue()

    if(objekt.isInstanceOf[Resource] && !objekt.isInstanceOf[BNode]) {
      storer.saveElement(objekt)
      elementList ::= objekt.stringValue()
    }

    val savedId = storer.saveTriple(baseUri, subject, predicate, objekt)
    tripleList ::= savedId
  }
}

