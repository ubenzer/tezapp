package service.parser

import org.openrdf.rio.{Rio, RDFFormat}
import org.openrdf.rio.helpers.{RDFHandlerBase}
import java.io.{InputStream}
import service.ontologyFetcher.Status
import service.ontologyFetcher.parser.OntologyParser
import play.Logger
import org.openrdf.model.{Value, URI, Resource, Statement}

object RIOParser extends OntologyParser {

  override def parseStreamAsOntology(tbParsed: InputStream, baseUri: String, format: RDFFormat)(afterParseCallback: ((Resource, URI, Value) => _) = null): Status.Value = {
    val rdfParser = Rio.createParser(format)
    if(afterParseCallback != null) {
      rdfParser.setRDFHandler(new RIOCustomHandler(afterParseCallback))
    }
    try {
      rdfParser.parse(tbParsed, baseUri);
    } catch {
      case ex: Throwable => {
        Logger.error("Some exception occurred while parsing ontology at " + baseUri, ex)
        return Status.NotParsable
      }
    }
    Status.Ok
  }

}
class RIOCustomHandler(saveHandler: (Resource, URI, Value) => _) extends RDFHandlerBase {

  override def handleStatement(st: Statement):Unit = {

    val subject: Resource = st.getSubject
    val predicate: URI = st.getPredicate
    val objekt: Value = st.getObject

    saveHandler(subject, predicate, objekt)
  }
}

