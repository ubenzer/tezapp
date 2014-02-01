package common

/**
 * User: ub (17/11/13 - 18:56)
 */
object Utils {
  def uuid = java.util.UUID.randomUUID.toString

  def isBlank(s: String): Boolean = (s == null) || (s.length == 0) || (s.trim.length == 0)
}
