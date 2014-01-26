package service

case class FetchResult(
  success: Int = 0,
  searchEngineFailed: Boolean = false,

  failed: Int = 0,
  failedNotFound: Int = 0,
  failedServerError: Int = 0,
  failedTimeout: Int = 0,
  failedConnection: Int = 0,
  failedNotParsable: Int = 0,

  duplicate: Int = 0
) {
  def +(fr: FetchResult): FetchResult = {
    FetchResult(
      success = this.success + fr.success,
      searchEngineFailed = this.searchEngineFailed || fr.searchEngineFailed,
      failed = this.failed + fr.failed,
      failedNotFound = this.failedNotFound + fr.failedNotFound,
      failedServerError = this.failedServerError + fr.failedServerError,
      failedTimeout = this.failedTimeout + fr.failedTimeout,
      failedConnection = this.failedConnection + fr.failedConnection,
      failedNotParsable = this.failedNotParsable + fr.failedNotParsable,
      duplicate = this.duplicate + fr.duplicate
    )
  }
}
