package service.ontologySearch

import models.{SearchResult, OntologyTriple}
import scala.concurrent.{Future}
import common.ExecutionContexts.fastOps
import reactivemongo.bson.{BSONArray, BSONDocument}
import reactivemongo.core.commands.RawCommand
import scala.util.{Success, Failure}
import java.util.Locale

object Search {

  sealed trait DescriptiveElement // http://stackoverflow.com/a/18595574/158523
  object Label extends DescriptiveElement
  object Comment extends DescriptiveElement

  def findElementsByKeyword(kws: String*): Future[Seq[SearchResult]] = {
    import scala.annotation.tailrec
    def countSubstring(str1:String, str2:String):Int= {    // http://rosettacode.org/wiki/Count_occurrences_of_a_substring#Scala
      @tailrec def count(pos:Int, c:Int):Int={
        val idx=str1 indexOf(str2, pos)
        if(idx == -1) c else count(idx+str2.size, c+1)
      }
      count(0,0)
    }
    def getMatchCount(keywords: Seq[String], tbSearchedIn: String): Int = {
      keywords.foldLeft(0) {
        (aggr, kw) => aggr + countSubstring(tbSearchedIn.toLowerCase(Locale.ENGLISH), kw)
      }
    }
    def getAsDescriptiveElement(uri: String): Option[DescriptiveElement] = {

      val descriptiveElementLookup: Map[String, DescriptiveElement] = Map(
        "http://www.w3.org/2000/01/rdf-schema#label" -> Label,
        "http://www.w3.org/2000/01/rdf-schema#comment" -> Comment
      )

      descriptiveElementLookup.get(uri)
    }


    val searchCommand = BSONDocument(
      "text" -> OntologyTriple.collection.name,
      "search" -> kws.mkString(" "),
      "limit" -> 10
    )
    val futureResult: Future[BSONDocument] = OntologyTriple.collection.db.command(RawCommand(searchCommand))

    futureResult.map {
      bson =>
        val results = bson.getAs[BSONArray]("results").getOrElse(BSONArray.empty)
        results.iterator.map {
          case Failure(ex) => None
          case Success((idx, value: BSONDocument)) => {
            val score = value.getAs[Double]("score").get
            val triple = value.getAs[OntologyTriple]("obj").get

            /* Determine what matched keywords */
            val objektHitCount = getMatchCount(kws, triple.objekt)
            val predicateHitCount = getMatchCount(kws, triple.predicate)
            val subjectHitCount = getMatchCount(kws, triple.subject)

            val realHitElement = (objektHitCount :: predicateHitCount :: subjectHitCount :: Nil).view.sorted.reverse.head match {
              case `objektHitCount` => triple.objekt
              case `predicateHitCount` => triple.predicate
              case `subjectHitCount` => triple.subject
            }

            val sr = realHitElement match {
              case triple.objekt => {
                val sr = SearchResult(
                    uri = triple.objekt,
                    kind = "TODO",
                    score = score
                )

                getAsDescriptiveElement(triple.predicate).map {
                  case Label => sr.copy(
                    uri   = triple.subject,
                    label = Some(triple.objekt)
                  )

                  case Comment => sr.copy(
                    uri     = triple.subject,
                    comment = Some(triple.objekt)
                  )
                  case _ => sr
                }.getOrElse {
                  println("UMUT 1000 TODO")
                  sr
                }
              }
              case triple.predicate => SearchResult(
                uri = triple.predicate,
                kind = "TODO",
                score = score
              )
              case triple.subject => SearchResult(
                uri = triple.subject,
                kind = "TODO",
                score = score
              )
            }
            Some(sr)
          }
          case Success(_) => None
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
