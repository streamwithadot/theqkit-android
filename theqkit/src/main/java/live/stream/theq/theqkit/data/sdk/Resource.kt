package live.stream.theq.theqkit.data.sdk

import live.stream.theq.theqkit.data.sdk.Resource.Companion.Status.ERROR
import live.stream.theq.theqkit.data.sdk.Resource.Companion.Status.LOADING
import live.stream.theq.theqkit.data.sdk.Resource.Companion.Status.SUCCESS

/** @suppress **/
data class Resource<out T> internal constructor(
    val status: Status,
    val data: T?,
    val errorMessage: String? = null,
    val errorCode: String? = null
) {

  val created = System.currentTimeMillis()

  fun isOlderThan(age: Long) = created + age < System.currentTimeMillis()

  fun isLoading() = status == LOADING

  fun isSuccess() = status == SUCCESS

  fun isError() = status == Companion.Status.ERROR

  companion object {

    fun <T> success(data: T?): Resource<T> {
      return Resource(
          SUCCESS, data
      )
    }

    fun <T> error(errorMessage: String, data: T? = null): Resource<T> {
      return Resource(
          ERROR, data, errorMessage
      )
    }

    fun <T> error(err: Exception, data: T? = null): Resource<T> {
      val (errorMessage, errorCode) = when (err) {
        is ApiException -> Pair(err.message, err.errorCode)
        else -> Pair("Unknown Error", null)
      }

      return Resource(ERROR, data, errorMessage, errorCode)
    }

    fun <T> loading(data: T?): Resource<T> {
      return Resource(
          LOADING, data
      )
    }

    enum class Status {
      SUCCESS,
      ERROR,
      LOADING
    }
  }

}
