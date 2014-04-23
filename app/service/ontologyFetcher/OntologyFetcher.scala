package service.ontologyFetcher

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.ws.{WS, Response}
import play.api.{Play, Logger}
import java.net.ConnectException
import java.util.concurrent.TimeoutException
import service.ontologyFetcher.parser.{RIOParser, OntologyParser}
import common.{BasicTimer, ExecutionContexts}
import service.FetchResult
import service.ontologyFetcher.storer.MongoStorageEngine
import play.api.Play.current

abstract class OntologyFetcher(parser: OntologyParser) {

  def getOntologyList(keyword: String): Future[Seq[String]]

  def crawlOntologies(urls: Seq[String]) = OntologyFetcher.crawlOntologies _

  def crawlOntology(uri: String) = OntologyFetcher.crawlOntology _

}

object OntologyFetcher {
  lazy val defaultParser = new RIOParser(new MongoStorageEngine())
  lazy val SwoogleFetcher = new SwoogleFetcher(defaultParser)
  lazy val WatsonFetcher = new WatsonFetcher(defaultParser)
  lazy val SindiceFetcher = new SindiceFetcher(defaultParser)

  val CHUNK_SIZE = Play.configuration.getInt("process.chunkSize").getOrElse(50)

  private def serialiseFutures[A, B](l: Iterable[A])(fn: A ⇒ Future[B])(implicit ec: ExecutionContext): Future[List[B]] = {
    l.foldLeft(Future(List.empty[B])) {
      (previousFuture, next) ⇒
        for {
          previousResults ← previousFuture
          next ← fn(next)
        } yield previousResults :+ next
    }
  }

  private def downloadOntology(url: String): Future[(Option[Response], FetchResult)] = {
    import ExecutionContexts.internetIOOps
    Logger.info("Downloading ontology: " + url)
    try {
      val timer = new BasicTimer("download", url).start()
      val wsFuture = WS.url(url).withHeaders(("Accept", "application/rdf+xml, application/xml;q=0.6, text/xml;q=0.6")).get().map {
        response => response.status match {
          case num if 404 == num => (None, FetchResult(notFound = 1))
          case num if 400 until 500 contains num => (None, FetchResult(failed400x = 1))
          case num if 500 until 600 contains num => (None, FetchResult(failed500x = 1))
          case _ => (Some(response), FetchResult(success = 1))
        }
      }.recover {
        case ex: TimeoutException =>
          Logger.info("Fetch failed because of a timeout  for url " + url)
          (None, FetchResult(timeout = 1))
        case ex: ConnectException =>
          Logger.info("Fetch failed because of connection problem for url " + url)
          (None, FetchResult(connection = 1))
        case ex: Throwable =>
          Logger.error("Fetch failed for url " + url, ex)
          (None, FetchResult(unknown = 1))
      }
      wsFuture.onComplete { _ => timer.stop() }
      wsFuture
    }
  }

  def crawlOntologies(uriList: Iterable[String]): Future[FetchResult] = {
    import ExecutionContexts.fastOps

    serialiseFutures(uriList.grouped(CHUNK_SIZE).toIterable) {
      aChunk =>
        Future.sequence {
          aChunk.map {
            uri => crawlOntology(uri)
          }
        }
    }.map {
        fr =>
          fr.flatten.foldLeft(FetchResult()) {
            (result, current) => result + current
          }
      }
  }
  def crawlOntology(uri: String): Future[FetchResult] = {
    import ExecutionContexts.fastOps

    def downloadedOntologyF: Future[(Option[Response], FetchResult)] = downloadOntology(uri)
    def processedOntologyF: Future[FetchResult] = downloadedOntologyF.flatMap {
        case (Some(ontologyResponse), fetchResult) =>
          val timer = new BasicTimer("parse", uri).start()
          val fetchResultF = defaultParser.parseResponseAsOntology(ontologyResponse)
          fetchResultF.onComplete { _ => timer.stop() }
          fetchResultF
        case (None, fetchResult) => Future.successful(fetchResult)
    }
    processedOntologyF
  }
}