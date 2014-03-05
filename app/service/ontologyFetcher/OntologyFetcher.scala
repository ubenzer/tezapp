package service.ontologyFetcher

import scala.concurrent.Future
import play.api.libs.ws.{WS, Response}
import play.api.Logger
import java.net.ConnectException
import java.util.concurrent.TimeoutException
import service.ontologyFetcher.parser.{RIOParser, OntologyParser}
import common.ExecutionContexts
import service.FetchResult
import service.ontologyFetcher.storer.MongoStorageEngine


abstract class OntologyFetcher(parser: OntologyParser) {
  def getOntologyList(keyword: String): Future[Set[String]]

  protected def search(keyword: String, source: String): Future[FetchResult] = {
    Logger.info("Starting search for keyword '" + keyword + "'")

    /* Step 1: Fetch ontology list to be fetched. */
    val ontologyListF = getOntologyList(keyword)

    ontologyListF.flatMap {
      case ontologyList if ontologyList.isEmpty => Future.successful(FetchResult(searchEngineFailed = true))
      case ontologyList if !ontologyList.isEmpty =>
        Logger.info("Downloading ontologies, total file count: " + ontologyList.size)
        val downloadedOntologiesFutureSeq: Seq[Future[(Option[Response], FetchResult)]] = downloadOntologies(ontologyList.toSeq: _*)
        val processedOntologiesSeq: Seq[Future[FetchResult]] = downloadedOntologiesFutureSeq.map {
          downloadedOntologiesFuture =>
            downloadedOntologiesFuture.flatMap {
              case (Some(ontologyResponse), fetchResult) =>
                val fetchResultF = parser.parseResponseAsOntology(ontologyResponse, source)
                fetchResultF
              case (None, fetchResult) => Future.successful(fetchResult)
            }(ExecutionContexts.verySlowOps)
        }
        import ExecutionContexts.fastOps
        Future.sequence(processedOntologiesSeq).map {
          fr =>
            fr.foldLeft(FetchResult()) {
              (result, current) => result + current
          }
        }
    }(ExecutionContexts.internetIOOps)
  }

  def downloadOntologies(urlList: String*): Seq[Future[(Option[Response], FetchResult)]] = {
    import ExecutionContexts.internetIOOps
    val resultFutures = urlList.map {
      url =>
        Logger.info("Downloading ontology: " + url)
        WS.url(url).withHeaders(("Accept", "application/rdf+xml, application/xml;q=0.6, text/xml;q=0.6")).get().map {
          response => response.status match {
            case num if 404 == num => (None, FetchResult(notFound = 1))
            case num if 400 until 500 contains num => (None, FetchResult(failed400x = 1))
            case num if 500 until 600 contains num => (None, FetchResult(failed500x = 1))
            case _ => (Some(response), FetchResult(success = 1))
          }
        } recover {
          case ex: TimeoutException =>
            Logger.info("Fetch failed because of a timeout  for url " + url)
            (None, FetchResult(timeout = 1))
          case ex: ConnectException =>
            Logger.info("Fetch failed because of connection problem for url " + url)
            (None, FetchResult(connection = 1))
          case ex: Exception =>
            Logger.error("Fetch failed for url " + url, ex)
            (None, FetchResult(unknown=1))
        }
    }
    resultFutures
  }
}

object OntologyFetcher {
  lazy val defaultParser = new RIOParser(new MongoStorageEngine())
  lazy val SwoogleFetcher = new SwoogleFetcher(defaultParser)
  lazy val WatsonFetcher = new WatsonFetcher(defaultParser)
}