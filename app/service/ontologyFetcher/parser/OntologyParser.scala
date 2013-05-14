package service.ontologyFetcher.parser

import org.openrdf.rio.RDFFormat
import java.net.{URI=>JURI}
import java.io.{ByteArrayInputStream, InputStream}
import org.openrdf.model.{URI, Value, Resource}
import service.ontologyFetcher.Status
import play.api.libs.ws.Response

abstract class OntologyParser protected {

  def parseResponseAsOntology(response: Response)(afterParseCallback: ((Resource, URI, Value) => _) = null): Status.Value = {
    import service.ontologyFetcher.parser.OntologyParserImplicits._
    val inferredType: RDFFormat = inferType(response.header("Content-Type"))
    parseStreamAsOntology(response.body, response.getAHCResponse.getUri, inferredType)(afterParseCallback)
  }
  def parseStreamAsOntology(tbParsed: InputStream, baseUri: String, format: RDFFormat)(afterParseCallback: ((Resource, URI, Value) => _) = null): Status.Value

  def inferType(contentType: Option[String]): RDFFormat = {

    for(h <- contentType) {
      val mime = h.split(';')(0)
      mime match {
        case x if(x == "application/rdf+xml" || x == "application/xml") => return RDFFormat.RDFXML
        case "text/plain" => return RDFFormat.NTRIPLES
        case "text/turtle" => return RDFFormat.TURTLE
        case x if (x == "text/n3" || x == "text/rdf+n3") => return RDFFormat.N3
        case "application/trix" =>  return RDFFormat.TRIX
        case "application/x-trig" => return RDFFormat.TRIG
        case "application/x-binary-rdf" => return RDFFormat.BINARY
        case "text/x-nquads" => return RDFFormat.NQUADS
        case "application/ld+json" => return RDFFormat.JSONLD
        case "application/rdf+json" => return RDFFormat.RDFJSON
        case x if(x == "application/xhtml+xml" || x == "application/html" || x == "text/html") => return RDFFormat.RDFA
      }
    }
    RDFFormat.RDFXML
  }
}
object OntologyParserImplicits {
  implicit def URI2String(uri: JURI):String = uri.toString
  implicit def String2InputStream(value: String):InputStream = new ByteArrayInputStream(value.getBytes("UTF-8"))
}