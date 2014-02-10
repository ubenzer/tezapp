package models

import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.bson._
import org.joda.time.DateTime
import reactivemongo.api.indexes.{Index}
import reactivemongo.api.indexes.IndexType.{Text, Ascending}
import reactivemongo.api.collections.default.BSONCollection
import scala.concurrent.Future
import reactivemongo.core.commands.{RawCommand, Remove, Update, FindAndModify}
import play.api.Logger
import common.ExecutionContexts.verySlowOps
import play.api.Play.current
import common.RDF

case class OntologyTriple (
                            id            : Option[BSONObjectID] = None,
                            subject       : String,
                            predicate     : String,
                            objekt        : String,
                            isObjectData  : Boolean,

                            appearsOn     : Set[String] = Set.empty,  // Swoogle, Watson etc.
                            sourceOntology: Set[String] = Set.empty,
                            cDate         : DateTime = new DateTime
                            ) {
}
object OntologyTriple {
  val collection: BSONCollection = ReactiveMongoPlugin.db.collection[BSONCollection]("OntologyTriple")

  val MAX_SEARCH_QUERY = 10000 // TODO Move to conf

  implicit object OntologyTripleBSONReader extends BSONDocumentReader[OntologyTriple] {
    def read(doc: BSONDocument): OntologyTriple = {
      OntologyTriple(
        id             = doc.getAs[BSONObjectID]("_id"),
        subject        = doc.getAs[String]("subject").get,
        predicate      = doc.getAs[String]("predicate").get,
        objekt         = doc.getAs[String]("objekt").get,
        isObjectData   = doc.getAs[Boolean]("isObjectData").get,
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

  def getSubject(predicate: String, objeckt: String): Future[Set[String]] = {
    val f: Future[List[OntologyTriple]] = collection.find(
      BSONDocument(
        "predicate" -> predicate,
        "objeckt" -> objeckt
      )
    ).cursor[OntologyTriple].collect[List]()

    f.map {
      ot:List[OntologyTriple] => {
        ot.map {
          o => o.subject
        }
      }.toSet
    } recover {
      case e:Throwable => {
        Logger.error("getSubject failed with predicate: " + predicate + " object: " + objeckt + " The error is: " + e)
        Set.empty
      }
    }
  }

  def getPredicate(subject: String, objeckt: String): Future[Set[String]] = {
    val f: Future[List[OntologyTriple]] = collection.find(
      BSONDocument(
        "subject" -> subject,
        "objeckt" -> objeckt
      )
    ).cursor[OntologyTriple].collect[List]()

    f.map {
      ot:List[OntologyTriple] => {
        ot.map {
          o => o.predicate
        }
      }.toSet
    } recover {
      case e:Throwable => {
        Logger.error("getSubject failed with subject: " + subject + " object: " + objeckt + " The error is: " + e)
        Set.empty
      }
    }
  }

  def getObject(subject: String, predicate: String): Future[Set[String]] = {
    val f: Future[List[OntologyTriple]] = collection.find(
      BSONDocument(
        "subject" -> subject,
        "predicate" -> predicate
      )
    ).cursor[OntologyTriple].collect[List]()

    f.map {
      ot:List[OntologyTriple] => {
        ot.map {
          o => o.objekt
        }
      }.toSet
    } recover {
      case e:Throwable => {
        Logger.error("getSubject failed with subject: " + subject + " predicate: " + predicate + " The error is: " + e)
        Set.empty
      }
    }
  }

  def getType(subject: String): Future[Option[String]] = _getSingleObject(subject, RDF.Type)
  def getLabel(subject: String): Future[Option[String]] = _getSingleObject(subject, RDF.Label)
  def getComment(subject: String): Future[Option[String]] = _getSingleObject(subject, RDF.Comment)
  private def _getSingleObject(subject: String, predicate: String): Future[Option[String]] = {
    getObject(subject, predicate).map {
      oSet =>
        if(oSet.size > 1) {
          Logger.warn("Uri " + subject + " has more than one " + predicate)
        }
        oSet.headOption
    }
  }

  def stringSearch(searchString: String): Future[BSONArray] = {
    val searchCommand = BSONDocument(
      "text" -> OntologyTriple.collection.name,
      "search" -> searchString,
      "limit" -> MAX_SEARCH_QUERY
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

  def getDisplayableElement(uri: String): Future[Option[DisplayableElement]] = {

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
      case (label, comment, None) => None
    }
  }
}