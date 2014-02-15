package controllers

import play.api.mvc._
import service.ontologyFetcher.OntologyFetcher
import scala.concurrent.Future
import common.ExecutionContexts.fastOps
import play.api.libs.json.Json
import models.{DisplayableElement, SearchResult}
import service.ontologySearch.Search
import org.openrdf.rio.{RDFFormat, Rio, RDFWriter}
import java.io.{PipedOutputStream, PipedInputStream}
import scala.util.Try
import play.api.libs.iteratee.Enumerator
import org.openrdf.model.Statement
import org.openrdf.model.impl.{LiteralImpl, URIImpl, StatementImpl}
import common.RDF

object Test extends Controller {

  def swoogle(keyword: String) = Action.async {
    if(keyword.length < 1) {
      Future.successful(BadRequest)
    } else {
      OntologyFetcher.SwoogleFetcher.search(keyword).map {
        result => Ok(result.toString)
      }
    }
  }

  def find(keyword: String) = Action.async {
    Search.findElementsByKeyword(keyword).map {
      r =>
        implicit val iDisplayableElement = Json.writes[DisplayableElement]
        implicit val iSearchResult = Json.writes[SearchResult]
        Ok(Json.toJson(r))
    }
  }

  def export() = Action.async(parse.json) {
    r =>
      Future.successful {

        val in = new PipedInputStream()
        val out = new PipedOutputStream(in)
        val writer: RDFWriter = Rio.createWriter(RDFFormat.RDFXML, out)

        Try {
          writer.startRDF()

          val s: Statement = new StatementImpl(new URIImpl("http://www.ubenzer.com"), new URIImpl(RDF.Label), new LiteralImpl("UBenzer"))

          writer.handleStatement(s)

          writer.endRDF()
        }
        out.close()


        val dataContent: Enumerator[Array[Byte]] = Enumerator.fromStream(in)
        Ok.chunked(dataContent)
      }
  }
}
