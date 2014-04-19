package models

import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.bson._
import org.joda.time.DateTime
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.{Text, Ascending}
import reactivemongo.api.collections.default.BSONCollection
import scala.concurrent.Future
import reactivemongo.core.commands.{RawCommand, Remove, Update, FindAndModify}
import play.api.{Play, Logger}
import common.ExecutionContexts.verySlowOps
import play.api.Play.current
import common.{RDFExport, RDFType, RDF}

case class OntologyTriple (
                            id            : Option[BSONObjectID] = None,
                            subject       : String,
                            predicate     : String,
                            objekt        : String,
                            isObjectData  : Boolean,
                            objectLanguage: Option[String],
                            appearsOn     : Set[String] = Set.empty,  // Swoogle, Watson etc.
                            sourceOntology: Set[String] = Set.empty,
                            cDate         : DateTime = new DateTime
                            ) {
}
object OntologyTriple {
  val collection: BSONCollection = ReactiveMongoPlugin.db.collection[BSONCollection]("OntologyTriple")
  val MAX_TEXT_INDEX_SEARCH_COUNT = Play.configuration.getInt("search.maxSearchByTextIndexCount").getOrElse(5000)
  val MAX_URI_SEARCH_COUNT = Play.configuration.getInt("search.maxSearchByUriCount").getOrElse(500)

  implicit object OntologyTripleBSONReader extends BSONDocumentReader[OntologyTriple] {
    def read(doc: BSONDocument): OntologyTriple = {
      OntologyTriple(
        id             = doc.getAs[BSONObjectID]("_id"),
        subject        = doc.getAs[String]("subject").get,
        predicate      = doc.getAs[String]("predicate").get,
        objekt         = doc.getAs[String]("objekt").get,
        isObjectData   = doc.getAs[Boolean]("isObjectData").get,
        objectLanguage = doc.getAs[String]("objectLanguage"),
        sourceOntology = doc.getAs[Set[String]]("sourceOntology").toSet.flatten,
        appearsOn      = doc.getAs[Set[String]]("appearsOn").toSet.flatten,
        cDate          = doc.getAs[BSONDateTime]("cDate").map(dt => new DateTime(dt.value)).get
      )
    }
  }

  implicit object OntologyTripleBSONWriter extends BSONDocumentWriter[OntologyTriple] {
    def write(triple: OntologyTriple): BSONDocument = {
      val elementUris: List[String] = triple.subject :: triple.predicate :: (if(triple.isObjectData) { Nil } else { List(triple.objekt) })
      BSONDocument(
        "_id" -> triple.id.getOrElse(BSONObjectID.generate),
        "subject" -> triple.subject,
        "predicate" -> triple.predicate,
        "objekt" -> triple.objekt,
        "objectLanguage" -> triple.objectLanguage,
        "isObjectData" -> triple.isObjectData,
        "sourceOntology" -> triple.sourceOntology,
        "appearsOn" -> triple.appearsOn,
        "cDate" -> BSONDateTime(triple.cDate.getMillis),
        "elementUris" -> elementUris // redundancy for optimization
      )
    }
  }

  val tripleIdx = Index(
    key  = ("predicate" -> Ascending) :: ("subject" -> Ascending) ::
      ("objekt" -> Ascending) :: ("isObjectData", Ascending) :: Nil,
    name = Some("tripleIdxPSO")
  )
  val textIdx = Index(
    key  = ("subject" -> Text) :: ("predicate" -> Text) :: ("objekt" -> Text) :: Nil,
    name = Some("objectText")
  )

  collection.indexesManager.ensure(tripleIdx)
  collection.indexesManager.ensure(textIdx)

  def mergeSave(triple: OntologyTriple): Future[Option[OntologyTriple]] = {
    val selector = BSONDocument(
      "predicate"    -> triple.predicate,
      "subject"      -> triple.subject,
      "objekt"       -> triple.objekt,
      "isObjectData" -> triple.isObjectData
    )
    val elementUris: List[String] = triple.subject :: triple.predicate :: (if(triple.isObjectData) { Nil } else { List(triple.objekt) })
    val update: BSONDocument = BSONDocument(
      "$set" -> BSONDocument(
        "subject"      -> triple.subject,
        "predicate"    -> triple.predicate,
        "objekt"       -> triple.objekt,
        "isObjectData" -> triple.isObjectData,

        "elementUris" -> elementUris,
        "cDate" -> BSONDateTime(new DateTime().getMillis)
      ),
      "$addToSet" -> BSONDocument(
        "appearsOn" -> BSONDocument(
          "$each" -> BSONArray(triple.appearsOn)
        ),
        "sourceOntology" -> BSONDocument(
          "$each" -> BSONArray(triple.sourceOntology)
        )
      )
    )

    val command = FindAndModify(
      collection = collection.name,
      query      = selector,
      modify     = Update(update, true),
      upsert     = true)

    collection.db.command(command).recover {
      case _ => None
    }.map {
      case None => {
        Logger.error("OntologyTriple mergeSave failed for selector '" + selector + "' and update '" + update + "'.")
        None
      }
      case Some(bsonDocument) =>
        Some(OntologyTripleBSONReader.read(bsonDocument))
    }
  }
  def removeAllTriplesOfAnOntology(uri: String): Future[Boolean] = {
    val selector = BSONDocument(
      "sourceOntology" -> uri,
      "sourceOntology" -> BSONDocument(
        "$gt" -> 1
      )
    )
    val update = BSONDocument(
      "$pull" -> BSONDocument(
        "sourceOntology" -> uri
      )
    )

    val command = FindAndModify(
      collection = collection.name,
      query      = selector,
      modify     = Update(update, false),
      upsert     = true)
    collection.db.command(command).flatMap {
      case None => {
        Logger.error("OntologyTriple removeAllTriplesOfAnOntology failed (1) for selector '" + selector + "' and update '" + update + "'.")
        Future.successful(false)
      }
      case Some(bsonDocument) => {

        val selector = BSONDocument(
          "sourceOntology" -> uri
        )
        val command = FindAndModify(
          collection = collection.name,
          query      = selector,
          modify     = Remove
        )
        collection.db.command(command).map {
          case None => {
            Logger.error("OntologyTriple removeAllTriplesOfAnOntology failed (2) for selector '" + selector + "' and update '" + update + "'.")
            false
          }
          case Some(bsonDocument) => true
        }
      }
    }
  }

  def getSubject(predicate: String, objekt: String): Future[List[String]] = {
    getTriple(None, Some(predicate), Some(objekt)).map {
      ot => {
        ot.map {
          o => o.subject
        }
      }
    }
  }
  def getPredicate(subject: String, objekt: String): Future[List[String]] = {
    getTriple(Some(subject), None, Some(objekt)).map {
      ot => {
        ot.map {
          o => o.predicate
        }
      }
    }
  }
  def getObject(subject: String, predicate: String): Future[List[String]] = {
    getTriple(Some(subject), Some(predicate), None).map {
      ot => {
        ot.map {
          o => o.objekt
        }
      }
    }
  }

  def getTriple(subject: Option[String] = None, predicate: Option[String] = None, objekt: Option[String] = None, max: Int = 100): Future[List[OntologyTriple]] = {
    if(!subject.isDefined && !predicate.isDefined && !objekt.isDefined) {
      return Future.successful(List.empty)
    }

    val query: List[(String, BSONValue)] = subject.map(x => List("subject" -> BSONString(x))).getOrElse(List.empty) ++
                predicate.map(x => List("predicate" -> BSONString(x))).getOrElse(List.empty) ++
                objekt.map(x => List("objekt" -> BSONString(x))).getOrElse(List.empty)

    val f: Future[List[OntologyTriple]] = collection.find(
      BSONDocument(query)
    ).cursor[OntologyTriple].collect[List](upTo = max)

    f.recover {
      case e:Throwable => {
        Logger.error("getSubject failed with subject: " + subject + " predicate: " + predicate + " The error is: " + e)
        List.empty
      }
    }
  }

  def getTriplesThatIncludes(elements: List[String]): Future[List[OntologyTriple]] = {
    val f: Future[List[OntologyTriple]] = collection.find(
      BSONDocument(
        "elementUris" -> BSONDocument(
          "$in" -> elements
        )
      )
    ).cursor[OntologyTriple].collect[List](MAX_URI_SEARCH_COUNT)

    f.recover {
      case e:Throwable => {
        Logger.error("getTriplesThatIncludes failed for: " + elements.mkString(", ") + " The error is: " + e)
        List.empty
      }
    }
  }

  def getRecursive[I,O](queryElement: I, recursionCount:Int = 5)(fetchFunction: I => Future[List[O]])(transformFunction: O => List[I]): Future[List[O]] = {
    val fetchedFutureSet: Future[List[O]] = fetchFunction(queryElement)
    if(recursionCount == 0) {
      fetchedFutureSet
    } else {
      val nextPhase: Future[List[O]] = fetchedFutureSet.flatMap {
        fetchedSet: List[O] =>
          val f2: List[Future[List[O]]] = fetchedSet.map {
            fetched: O =>
              val candidates: List[I] = transformFunction(fetched)
              val candidatesProcessed: List[Future[List[O]]] = candidates.map {
                candidate: I =>
                  getRecursive(candidate, recursionCount - 1)(fetchFunction)(transformFunction)
              }
              Future.sequence(candidatesProcessed).map { flatten => flatten.flatten }
          }
          Future.sequence(f2).map { flatten => flatten.flatten }
      }
      Future.sequence(List(fetchedFutureSet, nextPhase)).map { flatten => flatten.flatten }
    }
  }

  def getType(subject: String): Future[Option[String]] = {
    getObject(subject, RDF.Type).map {
      oSet =>
        val reducedToSupported = oSet.filter( x => RDFType.APP_SUPPORTED_TYPES.contains(x))

        if(reducedToSupported.size > 1) {
          Logger.warn("Reduced type for " + subject + " has more than one supported type.")
        }

        if(reducedToSupported.isEmpty) {
          Some("__INSTANCE__")
        } else {
          reducedToSupported.headOption
        }
    }
  }
  def getLabel(subject: String): Future[Option[String]] = _getSingleObject(subject, RDF.Label, defaultToAnchor = true)
  def getComment(subject: String): Future[Option[String]] = _getSingleObject(subject, RDF.Comment)
  private def _getSingleObject(subject: String, predicate: String, defaultToAnchor: Boolean = false): Future[Option[String]] = {

    def getLabelFromUri(uri: String): Option[String] = {
      val splittedUri = uri.split("#")
      if(splittedUri.length == 2) {
        Some(splittedUri(1))
      } else {
        val splittedUri = uri.split("/")
        if(splittedUri.length > 1) {
         Some(splittedUri(splittedUri.length-1))
        } else {
          None
        }
      }
    }

    getTriple(Some(subject), Some(predicate), None).map {
      oSet =>
        if(oSet.size > 0) {
          val en: List[OntologyTriple] = oSet.filter(x => x.objectLanguage == Some("en"))
          if(en.size > 0) {
            Some(en.head.objekt)
          } else {
            if(defaultToAnchor) {
              getLabelFromUri(subject)
            } else {
              val nullLang: List[OntologyTriple] = oSet.filter(x => x.objectLanguage == None)
              if(nullLang.size > 0) {
                Some(nullLang.head.objekt)
              } else {
                Some(oSet.head.objekt)
              }
            }
          }
        } else {
          if(defaultToAnchor) {
            getLabelFromUri(subject)
          } else {
            None
          }
        }
    }
  }

  def stringSearch(searchString: String): Future[BSONArray] = {
    val searchCommand = BSONDocument(
      "text" -> OntologyTriple.collection.name,
      "search" -> searchString,
      "limit" -> MAX_TEXT_INDEX_SEARCH_COUNT
    )
    val futureResult: Future[BSONDocument] = OntologyTriple.collection.db.command(RawCommand(searchCommand))

    futureResult.map {
      bson =>
        bson.getAs[BSONArray]("results").getOrElse(BSONArray.empty)
    } recover {
      case e:Throwable => {
        Logger.error("stringSearch failed with searchString: " + searchString + " The error is: " + e)
        BSONArray.empty
      }
    }
  }

  def getDisplayableElement(uri: String, allowUnknownType: Boolean = false): Future[Option[DisplayableElement]] = {

    /*  We need to fill label, description and kind for given uri */

    val labelF = getLabel(uri)
    val commentF = getComment(uri)
    val typeF = getType(uri)

    val f =
      for {
        f1 <- labelF
        f2 <- commentF
        f3 <- typeF
      } yield (f1,f2, f3)


    f.map {
      case (label, comment, Some(kind)) =>
        Some(DisplayableElement(
          uri = uri,
          label = label,
          comment = comment,
          kind = kind
        ))
      case (label, comment, None) =>
        if(allowUnknownType) {
          Some(DisplayableElement(
            uri = uri,
            label = label,
            comment = comment,
            kind = "UNKNOWN"
          ))
        } else {
          None
        }
    }
  }
}