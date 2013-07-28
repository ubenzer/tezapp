import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import com.mongodb.casbah.MongoClient
import play.api._

object Global extends GlobalSettings {

  override def onStart(app: Application) {

    Logger.info("Application has started")

    RegisterJodaTimeConversionHelpers()


  }
}