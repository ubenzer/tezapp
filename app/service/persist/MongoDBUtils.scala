package service.persist

import com.mongodb.casbah.Imports._

// http://stackoverflow.com/a/11200484/158523
class MongoDBUtils(underlying: DBObject) {

  def getString(key: String) = underlying.as[String](key)

  def getDouble(key: String) = underlying.as[Double](key)

  def getInt(key: String) = underlying.as[Int](key)

  def getList[A](key: String) =
    (List() ++ underlying(key).asInstanceOf[BasicDBList]) map { _.asInstanceOf[A] }

  def getSet[A](key: String) =
    (Set() ++ underlying(key).asInstanceOf[BasicDBList]) map { _.asInstanceOf[A] }

  def getStringSet[A](key: String) = getList[String](key)
  def getObjectIdSet[A](key: String) = getSet[ObjectId](key)
}

object MongoDBUtils {
  implicit def toDBObjectHelper(obj: DBObject) = new MongoDBUtils(obj)
}
