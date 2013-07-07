package service.parser

import org.openrdf.rio.{UnsupportedRDFormatException, RDFParseException, Rio, RDFFormat}
import org.openrdf.rio.helpers.{RDFHandlerBase}
import java.io.{InputStream}
import service.ontologyFetcher.Status
import service.ontologyFetcher.parser.OntologyParser
import play.Logger
import org.openrdf.model.{Value, URI, Resource, Statement}
import service.ontologyFetcher.storer.OntologyStorer

class RIOParser(storer: OntologyStorer) extends OntologyParser(storer) {

  override def parseStreamAsOntology(tbParsed: InputStream, baseUri: String, format: RDFFormat, source: String): Status.Value = {

    try {
      val rdfParser = Rio.createParser(format)
      rdfParser.setRDFHandler(new RIOCustomHandler(storer.saveTriple _, baseUri, source))
      rdfParser.parse(tbParsed, baseUri);
    } catch {
      case ex: RDFParseException => {
        Logger.debug("Couldn't parse ontology at " + baseUri, ex)
        return Status.NotParsable
      }
      case ex: UnsupportedRDFormatException => {
        Logger.debug("Couldn't parse ontology because it is unsupported at " + baseUri, ex)
        return Status.NotParsable
      }
      case ex: Throwable => {
        Logger.error("Some exception occurred while parsing ontology at " + baseUri, ex)
        return Status.NotParsable
      }
    }
    Status.Ok
  }

}
class RIOCustomHandler(saveHandler: (String, String, Resource, URI, Value) => _, baseUri: String, source: String) extends RDFHandlerBase {

  override def handleStatement(st: Statement):Unit = {

    val subject: Resource = st.getSubject
    val predicate: URI = st.getPredicate
    val objekt: Value = st.getObject

    saveHandler(baseUri, source, subject, predicate, objekt)
  }
}

