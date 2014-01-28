package service.ontologySearch

import models.{SearchResult, OntologyTriple, OntologyElement}
import com.mongodb.casbah.Imports._

object Search {

  val specialElements =
   "http://www.w3.org/2000/01/rdf-schema#label" ::
   "http://www.w3.org/2000/01/rdf-schema#comment" :: Nil

  def findElementsByKeyword(kw: String): Seq[SearchResult] = {
    val searchResults: List[SearchResult] = Nil
    val uriSearchResults: DBObject = OntologyElement.collection.db.command(DBObject("text" -> "OntologyElement") ++ DBObject("search" -> kw, "filter" -> DBObject("isBlankNode" -> false)))
    val dataPropertySearchResults: DBObject = OntologyTriple.collection.db.command(DBObject("text" -> "OntologyTriple") ++ DBObject("search" -> kw))

    val uriSearch = uriSearchResults.get("results").asInstanceOf[BasicDBList]
    val dataPropertySearch = dataPropertySearchResults("results").asInstanceOf[BasicDBList]


    dataPropertySearch.map {
      case x: BasicDBObject => {
        val score = x("score").asInstanceOf[Double]
        val obj = x("obj").asInstanceOf[BasicDBObject]

        val subject = obj("subject").asInstanceOf[String]
        val predicate = obj("predicate").asInstanceOf[String]
        val objectD = obj("objectD").asInstanceOf[String]

        if(specialElements.contains(predicate)) {
          println("Özel durum!")

          Some(
            SearchResult(subject, Some(objectD), None, "AHAHA", score)
          )
        } else {
          println("Değil")
          println(obj)
          None
        }
      }
    }.foldLeft(List.empty[SearchResult]) {
      case (previous, Some(searchResult)) => searchResult :: previous
      case (previous, None) => previous
    }
  }
  def findSubject(predicate: String, objekt: String) = {}
  def findPredicate(predicate: String, objekt: String) = {}
  def findObject(predicate: String, objekt: String) = {}

  def recursive(limit: Int=5)() = {}
}
