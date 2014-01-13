package controllers

import play.api.mvc._
import play.api.mvc.Controller
import play.i18n.Lang

object Application extends Controller {
  def index = Action {
    Ok(views.html.main.render())
  }

  def javascriptRoutes = Action { implicit request =>
  /*import routes.javascript._
  Ok(play.api.Routes.javascriptRouter("routes")(
    Swoogle.submit,
    Watson.submit,
    Sindice.submit)
  ).as("text/javascript")  */
    Ok("")
  }
}
