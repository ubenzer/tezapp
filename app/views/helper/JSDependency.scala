package views.helper

import play.api.mvc.Request



object JSDependency {
  implicit def username[A](implicit request: Request[A]) : Option[String] = {
    None
  }
}
