package service.ontologySearch

import models.{DisplayableElement, SearchResult, OntologyTriple}
import scala.concurrent.{Future}
import common.ExecutionContexts.fastOps
import reactivemongo.bson.{BSONArray, BSONDocument}
import reactivemongo.core.commands.RawCommand
import scala.util.{Success, Failure}
import java.util.Locale

object Search {



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

    val searchCommand = BSONDocument(
      "text" -> OntologyTriple.collection.name,
      "search" -> kws.mkString(" "),
      "limit" -> 1000
    )
    val futureResult: Future[BSONDocument] = OntologyTriple.collection.db.command(RawCommand(searchCommand))

    futureResult.flatMap {
      bson =>
        val results = bson.getAs[BSONArray]("results").getOrElse(BSONArray.empty)
        val futuresOfResults = results.iterator.flatMap {
          case Failure(ex) => None
          case Success((idx, value: BSONDocument)) => {
            val score = value.getAs[Double]("score").get
            val triple = value.getAs[OntologyTriple]("obj").get

            /* Determine what matched keywords */
            val objektHitCount = getMatchCount(kws, triple.objekt)
            val predicateHitCount = getMatchCount(kws, triple.predicate)
            val subjectHitCount = getMatchCount(kws, triple.subject)

            /* Find why we found this triple as a result */
            val realHitElement = (objektHitCount :: predicateHitCount :: subjectHitCount :: Nil).view.sorted.reverse.head match {
              case `objektHitCount` => triple.objekt
              case `predicateHitCount` => triple.predicate
              case `subjectHitCount` => triple.subject
            }

            /* Create a result object */
            val sr: Future[SearchResult] = realHitElement match {
              case triple.objekt => {
                (if(triple.isObjectData) {
                  OntologyTriple.getDisplayableElement(triple.subject)
                } else {
                  OntologyTriple.getDisplayableElement(triple.objekt)
                }).map { de: DisplayableElement =>
                  SearchResult(
                    element = de,
                    score = score
                  )
                }
              }
              case _ => {
                OntologyTriple.getDisplayableElement(realHitElement).map { de =>
                  SearchResult(
                    element = de,
                    score = score
                  )
                }
              }
            }
            Some(sr)
          }
          case Success(_) => None
        }

        Future.sequence(futuresOfResults).map {
          _.toSeq.distinct
        }
    }
  }
}
