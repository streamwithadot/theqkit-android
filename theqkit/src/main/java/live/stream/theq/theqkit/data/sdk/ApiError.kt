package live.stream.theq.theqkit.data.sdk

data class ApiError(val errorCode: String = "UNKNOWN_ERROR", val errorMessage: String = "An unknown error has occurred", val success: Boolean = false) {

  fun toException(): ApiException {
    return ApiException(errorCode, errorMessage)
  }

}

class ApiException(val errorCode: String, errorMessage: String) : Exception(errorMessage)