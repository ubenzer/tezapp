package common

import org.apache.commons.validator.routines.UrlValidator

/**
 * User: ub (17/11/13 - 18:56)
 */
object Utils {
  def uuid = java.util.UUID.randomUUID.toString

  var httpSValidator: UrlValidator = new UrlValidator(Array[String]("http", "https"))

  def isBlank(s: String): Boolean = (s == null) || (s.length == 0) || (s.trim.length == 0)
}
