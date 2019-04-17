package live.stream.theq.theqkit.data.sdk

data class ApiError(
  val errorCode: String = "UNKNOWN_ERROR",
  val errorMessage: String = "An unknown error has occurred",
  val success: Boolean = false
) {

  /** @suppress **/
  fun toException(): ApiException {
    return ApiException(errorCode, errorMessage)
  }

}

/** @suppress **/
class ApiException(val errorCode: String, errorMessage: String) : Exception(errorMessage)
