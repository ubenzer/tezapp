package service.ontologyFetcher.parser

import org.openrdf.rio.RDFFormat
import java.net.{URI=>JURI}
import java.io.{ByteArrayInputStream, InputStream}
import play.api.libs.ws.Response
import service.ontologyFetcher.storer.OntologyStorageEngine
import common.RewindableByteArrayInputStream
import service.FetchResult

abstract class OntologyParser(storer: OntologyStorageEngine) {

  final def parseResponseAsOntology(response: Response, source: String): FetchResult = {
    import service.ontologyFetcher.parser.OntologyParserImplicits._
    val inferredType: RDFFormat = inferType(response.header("Content-Type"))
    parseStreamAsOntology(response.body, response.getAHCResponse.getUri, inferredType, source)
  }
  def parseStreamAsOntology(tbParsed: RewindableByteArrayInputStream, baseUri: String, format: RDFFormat, source: String): FetchResult

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
        case _ =>
      }
    }
    return RDFFormat.RDFXML
  }
}
object OntologyParserImplicits {
  implicit def URI2String(uri: JURI):String = uri.toString
  implicit def String2InputStream(value: String):InputStream = new ByteArrayInputStream(value.getBytes("UTF-8"))
  implicit def String2IRewindableByteArrayInputStream(value: String): RewindableByteArrayInputStream = new RewindableByteArrayInputStream(value.getBytes("UTF-8"))
}