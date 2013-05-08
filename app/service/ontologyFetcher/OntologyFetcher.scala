package service.ontologyFetcher

import scala.concurrent.{Await, Future}
import play.api.libs.ws.Response
import scala.xml.Elem
import play.api.Logger
import scala.concurrent.duration._

abstract trait OntologyFetcher {

  def getOntologyList(keyword: String): Option[Set[String]]
  def getOntologyListFuture(keyword: String): Future[Option[Set[String]]]

  def getXMLSync(future: Future[Response]): Option[Elem] = {
    try {
      Some(Await.result(future, 10 seconds).xml)
    } catch {
      case e: Exception => {
        Logger.error("Can't get results.", e)
        return None
      }
    }
  }
}
object OntologyFetcher {
  lazy val SwoogleFetcher = new Object with SwoogleFetcher
}