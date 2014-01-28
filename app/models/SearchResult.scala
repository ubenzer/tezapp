package models

/**
 * User: ub (28/1/14 - 20:53)
 */
case class SearchResult(
  uri: String,
  label: Option[String],
  comment: Option[String],
  kind: String,
  score: Double
)
