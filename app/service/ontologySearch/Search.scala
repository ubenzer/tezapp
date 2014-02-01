package service.ontologySearch

import models.{SearchResult, OntologyTriple}
import scala.concurrent.{Future}
import common.ExecutionContexts.fastOps
import reactivemongo.bson.{BSONArray, BSONDocument}
import reactivemongo.core.commands.RawCommand
import scala.util.{Success, Failure}

object Search {

  val specialElements =
   "http://www.w3.org/2000/01/rdf-schema#label"   ::
   "http://www.w3.org/2000/01/rdf-schema#comment" :: Nil

  def findElementsByKeyword(kws: String*): Future[Seq[SearchResult]] = {
    val searchCommand = BSONDocument(
      "text" -> OntologyTriple.collection.name,
      "search" -> kws.mkString(" "),
      "limit" -> 10
    )
    val futureResult: Future[BSONDocument] = OntologyTriple.collection.db.command(RawCommand(searchCommand))


    futureResult.map {
      bson =>
        println(BSONDocument.pretty(bson))
        val results = bson.getAs[BSONArray]("results").get
        results.iterator.map {
          case Failure(ex) => None
          case Success((idx, value: BSONDocument)) => {
            val score = value.getAs[Double]("score").get
            val triple = value.getAs[OntologyTriple]("obj").get

            if(specialElements.contains(triple.predicate)) {
              println("Özel durum!")

              Some(
                SearchResult(triple.subject, Some(triple.id.get.toString), None, "AHAHA", score)
              )
            } else {
              println("Değil")
              println(value)
              None
            }
          }
        }.foldLeft(List.empty[SearchResult]) {
          case (previous, Some(searchResult)) => searchResult :: previous
          case (previous, None) => previous
        }
    }
  }
//
//  def findSubject(predicate: String, objekt: String) = {
//    import reactivemongo.api._
//    import play.modules.reactivemongo.MongoController
//    import play.modules.reactivemongo.json.collection.JSONCollection
//    import play.api.libs.json._
//
//    val cursor: Cursor[JsObject] = OntologyTriple.c.find(
//        Json.obj("predicate" -> predicate, "objectO" -> objekt)
//    ).cursor[JsObject]
//
//    // gather all the JsObjects in a list
//    val futureResults: Future[List[JsObject]] = cursor.collect[List]()
//
//    // transform the list into a JsArray
//    val futurePersonsJsonArray: Future[Seq[OntologyTriple]] = futureResults.map {
//      resultList =>
//        resultList.map {
//          aResult: JsObject =>
//            OntologyTriple(aResult("id").as)
//        }
//    }
//
//  }
//  def findPredicate(predicate: String, objekt: String) = {}
//  def findObject(predicate: String, objekt: String) = {}
//
//  def recursive(limit: Int=5)() = {}
}
