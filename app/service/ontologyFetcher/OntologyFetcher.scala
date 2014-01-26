package service.ontologyFetcher

import scala.concurrent.{Future}
import play.api.libs.ws.{WS, Response}
import play.api.Logger
import service.parser.RIOParser
import service.storer.SalatStorageEngine
import java.net.ConnectException
import java.util.concurrent.{TimeoutException}
import service.ontologyFetcher.parser.OntologyParser
import common.ExecutionContexts
import service.FetchResult


abstract class OntologyFetcher(parser: OntologyParser) {
  def getOntologyList(keyword: String): Future[Set[String]]

  protected def search(keyword: String, source: String): Future[FetchResult] = {
    /* Step 1: Fetch ontology list to be fetched. */
    val ontologyListF = getOntologyList(keyword)

    import ExecutionContexts.internetIOOps
    ontologyListF.flatMap {
      case ontologyList if ontologyList.isEmpty => Future.successful(FetchResult(searchEngineFailed = true))
      case ontologyList if !ontologyList.isEmpty =>
        val downloadedOntologiesF: Future[(Seq[Response], FetchResult)] = downloadOntologies(ontologyList.toSeq: _*)
        downloadedOntologiesF.map {
          tuple =>
            val responses: Seq[Response] = tuple._1
            val fetchResult: FetchResult = tuple._2

            val parseResults: FetchResult = responses.map {
              r => parser.parseResponseAsOntology(r, source)
            }.foldLeft(fetchResult) {
              (result, current) => result + current
            }
            parseResults
        }
    }
  }

  def downloadOntologies(urlList: String*): Future[(Seq[Response], FetchResult)] = {
    import ExecutionContexts.internetIOOps
    val resultFutures = urlList.map {
      url =>
        WS.url(url).withHeaders(("Accept", "application/rdf+xml, application/xml;q=0.6, text/xml;q=0.6")).get().map {
          response => response.status match {
            case num if 400 until 500 contains num => (None, FetchResult(failed = 1, failedNotFound = 1)) // TODO Make this better
            case num if 500 until 600 contains num => (None, FetchResult(failed = 1, failedServerError = 1)) // This, too.
            case _ => (Some(response), FetchResult(success = 1))
          }
        } recover {
          case ex: TimeoutException => {
            Logger.info("Fetch failed because of a timeout  for url " + url, ex)
            (None, FetchResult(failed=1, failedTimeout = 1))
          }
          case ex: ConnectException => {
            Logger.info("Fetch failed because of connection problem for url " + url, ex)
            (None, FetchResult(failed=1, failedConnection = 1))
          }
          case ex: Exception => {
            Logger.error("Fetch failed for url " + url, ex)
            (None, FetchResult(failed=1))
          }
        }
    }

    val inverted:Future[Seq[(Option[Response], FetchResult)]] = Future.sequence(resultFutures)

    val folded:Future[(Seq[Response], FetchResult)] = inverted.map {
      resultSeq =>
        resultSeq.foldLeft((List.empty[Response], FetchResult())) {
          case ((curList, totalFetchResult), (Some(result), elemResult)) =>
            (result :: curList, totalFetchResult + elemResult)
          case ((curList, totalFetchResult), (None, elemResult)) =>
            (curList, totalFetchResult + elemResult)
        }
    }
    folded
  }
}

object OntologyFetcher {
  lazy val defaultParser = new RIOParser(new SalatStorageEngine())
  lazy val SwoogleFetcher = new SwoogleFetcher(defaultParser)
}