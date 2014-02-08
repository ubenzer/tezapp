package models

case class SearchResult(
  element: DisplayableElement,
  score: Double
) {
  override def equals(o: Any) = o match {
    case that: SearchResult => that.element.equals(this.element)
    case _ => false
  }
  override def hashCode = element.hashCode
}

case class DisplayableElement(
  uri: String,
  label: Option[String] = None,
  comment: Option[String] = None,
  kind: String
) {
  override def equals(o: Any) = o match {
    case that: DisplayableElement => that.uri.equalsIgnoreCase(this.uri)
    case _ => false
  }
  override def hashCode = uri.toUpperCase.hashCode
}
