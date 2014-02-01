package service

/**
 * This stores overall process of an ontology
 * fetch, parsing and persistence flow.
 */
case class FetchResult(
  success: Int = 0,

  // Fail cases
  searchEngineFailed: Boolean = false,
  duplicate: Int = 0,
  notFound: Int = 0,
  failed400x: Int = 0,
  failed500x: Int = 0,
  timeout: Int = 0,
  connection: Int = 0,
  notParsable: Int = 0,
  unknown: Int = 0
) {
  def +(fr: FetchResult): FetchResult = {
    FetchResult(
      success = this.success + fr.success,
      searchEngineFailed = this.searchEngineFailed || fr.searchEngineFailed,
      duplicate = this.duplicate + fr.duplicate,
      notFound = this.notFound + fr.notFound,
      failed400x = this.failed400x + fr.failed400x,
      failed500x = this.failed500x + fr.failed500x,
      timeout = this.timeout + fr.timeout,
      connection = this.connection + fr.connection,
      notParsable = this.notParsable + fr.notParsable,
      unknown = this.unknown + fr.unknown
    )
  }
}
