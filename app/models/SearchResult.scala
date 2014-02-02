package models

case class SearchResult(
  uri: String,
  label: Option[String] = None,
  comment: Option[String] = None,
  kind: String,
  score: Double
) {
  override def equals(o: Any) = o match {
    case that: SearchResult => that.uri.equalsIgnoreCase(this.uri)
    case _ => false
  }
  override def hashCode = uri.toUpperCase.hashCode
}
