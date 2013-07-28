package service.ontologyFetcher

import scala.concurrent.{Await, Future}
import play.api.libs.ws.{WS, Response}
import scala.xml.Elem
import play.api.Logger
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import service.parser.RIOParser
import service.storer.SalatStorer
import java.net.ConnectException
import java.util.concurrent.TimeoutException
import service.ontologyFetcher.parser.OntologyParser

abstract class OntologyFetcher(parser: OntologyParser) {
  val MAX_TIME_PER_ONTOLOGY_DOWNLOAD = 10 seconds
  def getOntologyList(keyword: String): Option[Set[String]]
  def getOntologyListFuture(keyword: String): Future[Option[Set[String]]]

  def doOntologyFetchingFor(keyword: String, source: String): Map[Status.Value, Int] = {

    /* Step 1: Fetch ontology list to be fetched. */
    val ontologyList = getOntologyList(keyword)
    if(!ontologyList.isDefined) Map.empty

    val tbDownloadedOntologiesFuture: Seq[Future[Either[Response, Status.Value]]] = downloadOntologiesFuture(ontologyList.get.toSeq: _*)

    val tbParsedAndStoredOntologiesFuture: Seq[Future[Status.Value]] =
      for(tbDownloadedOntology <- tbDownloadedOntologiesFuture) yield {
        tbDownloadedOntology.map {
          either => either match {
            case Left(r) => parser.parseResponseAsOntology(r, source)
            case Right(r) => r
          }
        }
      }

    val results = try {
      Await.result(Future.sequence(tbParsedAndStoredOntologiesFuture), MAX_TIME_PER_ONTOLOGY_DOWNLOAD * tbDownloadedOntologiesFuture.size)
    } catch {
      case ex: Throwable => Logger.error("Some error occurred while waiting for ontology fetching.", ex)
      return Map.empty
    }

    results.groupBy { case aStatus => aStatus}.mapValues(_.size)
  }

  def downloadOntologiesFuture(urlList: String*): Seq[Future[Either[Response, Status.Value]]] = {
    for(url <- urlList) yield
      WS.url(url).withHeaders(("Accept", "application/rdf+xml, application/xml;q=0.6, text/xml;q=0.6")).get().map {
        r: Response => {

          r.status match {
            case num if 400 until 500 contains num => Right(Status.Status400)
            case num if 500 until 600 contains num => Right(Status.Status500)
            case _ => Left(r)
          }
        }
      } recover {
        case ex: TimeoutException => {
          Logger.info("Fetch failed because of a timeout  for url " + url, ex)
          Right(Status.ConnectionProblem)
        }
        case ex: ConnectException => {
          Logger.info("Fetch failed because of connection problem for url " + url, ex)
          Right(Status.ConnectionProblem)
        }
        case ex: Exception => {
          Logger.error("Fetch failed for url " + url, ex)
          Right(Status.ConnectionProblem)
        }
      }
  }
  protected def getXMLSync(future: Future[Response]): Option[Elem] = {
    try {
      Some(Await.result(future, MAX_TIME_PER_ONTOLOGY_DOWNLOAD).xml)
    } catch {
      case e: Exception => {
        Logger.error("Can't get results.", e)
        return None
      }
    }
  }
}
object OntologyFetcher {
  lazy val defaultParser = new RIOParser(new SalatStorer())
  lazy val SwoogleFetcher = new SwoogleFetcher(defaultParser)
}
object Status extends Enumeration {
  type Status = Value
  val Ok, Duplicate, NotParsable, Spam, Status400, Status500, ConnectionProblem, Other = Value
}