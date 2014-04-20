package controllers

import play.api.mvc.{Action, Controller}
import scala.io.Source
import play.api.Play.current
import play.api.Play

/**
 * This file has the controllers which are used to test internals
 * of the application. These shouldn't be accessible on production!
 */
object Analyze extends Controller {
  val LOG_FILE = "/logs/application.log"

  def parseLog = Action {


    try {
      val fileUrl = Play.getFile(LOG_FILE)
      val file = Source.fromFile(fileUrl)(scala.io.Codec.UTF8)
      Ok(
        file.getLines().map {
          line =>
            if(line.startsWith("TIMER: ")) {
              val splitted = line.split("\\[")
              val source = splitted(1).split("\\]")(0).replaceAll(",","\\,")
              val thing =  splitted(2).split("\\]")(0).replaceAll(",","\\,")
              val milis = splitted(3).split("\\]")(0).replaceAll(",","\\,")
              val output = "\"" + source + "\"" + "," + "\"" + thing + "\"" + "," + "\"" + milis + "\""
              Some(output)
            } else {
              None
            }
        }.flatten.mkString("\n")
      ).as("text/csv")
    } catch {
      case e: Throwable =>
        BadRequest("Read failed.")
    }
  }
}
