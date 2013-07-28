package service.persist

import com.mongodb.casbah.MongoClient

/**
 * User: ub (21/7/13 - 2:35 PM)
 */
object MongoDB {
  val mongoClient = MongoClient()
  val db = mongoClient("mydb")
}
