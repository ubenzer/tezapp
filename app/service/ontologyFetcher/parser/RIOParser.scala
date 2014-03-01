package service.ontologyFetcher.parser

import org.openrdf.rio.{UnsupportedRDFormatException, RDFParseException, Rio, RDFFormat}
import org.openrdf.rio.helpers.RDFHandlerBase
import play.Logger
import org.openrdf.model._
import service.ontologyFetcher.storer.OntologyStorageEngine
import common.CryptoUtils
import service.FetchResult
import models.OntologyDocument
import scala.concurrent.Future
import common.ExecutionContexts.verySlowOps
import java.io.{ByteArrayInputStream, InputStream}
import scala.util.Try

class RIOParser(storer: OntologyStorageEngine) extends OntologyParser(storer) {

  override def parseStreamAsOntology(tbParsed: String, ontologyUri: String, format: RDFFormat, source: String): Future[FetchResult] = {
    def string2InputStream(value: String):InputStream = new ByteArrayInputStream(value.getBytes("UTF-8"))
    lazy val md5 = CryptoUtils.md5(tbParsed)

    def parseAndSave(): Future[FetchResult] = {
      val ontologyAsStream = string2InputStream(tbParsed)
      try {
        val handler = new RIOCustomHandler(storer, ontologyUri, source)
        val rdfParser = Rio.createParser(format)
        rdfParser.setRDFHandler(handler)

        // Save elements, triples and update document on demand. (we cant handle all list in one turn, if ontology is big.)
        rdfParser.parse(ontologyAsStream, ontologyUri)

        storer.saveDocument(ontologyUri, md5, source).map {
          x => FetchResult(success = 1)
        }
      } catch {
        case ex: RDFParseException =>
          Logger.info("Couldn't parse ontology at " + ontologyUri + "(" + ex.getMessage + ")")
          Future.successful(FetchResult(notParsable = 1))
        case ex: UnsupportedRDFormatException =>
          Logger.info("Couldn't parse ontology because it is unsupported at " + ontologyUri + "(" + ex.getMessage + ")")
          Future.successful(FetchResult(notParsable = 1))
        case ex: Throwable =>
          Logger.error("Some exception occurred while parsing ontology at " + ontologyUri, ex)
          Future.successful(FetchResult(notParsable = 1))
      } finally {
        Try {
          ontologyAsStream.close()
        }
      }
    }

    // Check if ontology exists
    OntologyDocument.isExists(ontologyUri).flatMap {
      case false => parseAndSave()
      case true  =>
        OntologyDocument.isLocked(ontologyUri).flatMap {
          case true  => Future.successful(FetchResult(duplicate = 1))
          case false =>
            OntologyDocument.isModified(ontologyUri, md5).flatMap {
              case true =>
                // If modified clean old ontology
                storer.deleteOntology(ontologyUri)
                parseAndSave()
              case false =>
                storer.saveDocument(ontologyUri, md5, source).map {
                  x => FetchResult(duplicate = 1)
                }
            }
      }
    }


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

