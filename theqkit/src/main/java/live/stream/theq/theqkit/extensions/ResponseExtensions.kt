package live.stream.theq.theqkit.extensions

import live.stream.theq.theqkit.data.sdk.ApiError
import live.stream.theq.theqkit.http.RestClient
import retrofit2.Response

fun <T> Response<T>.getOrThrow(restClient: RestClient) : T? {
  if (this.isSuccessful) {
    return this.body()
  } else {
    this.errorBody()?.let {
      throw restClient.parseError(it).toException()
    }

    throw ApiError().toException()
  }
}
