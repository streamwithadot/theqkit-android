package live.stream.theq.theqkit.http

import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import live.stream.theq.theqkit.data.sdk.ApiError
import live.stream.theq.theqkit.util.PrefsHelper
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.Exception
import java.util.UUID

/** @suppress */
class RestClient private constructor(apiBase: String, prefsHelper: PrefsHelper, debug: Boolean) {

  private val logLevel =
    if (debug) HttpLoggingInterceptor.Level.BODY
    else HttpLoggingInterceptor.Level.NONE

  val gson = GsonBuilder().create()
  private val requestInterceptor =
    RequestInterceptor(prefsHelper, gson)
  private val okHttp = OkHttpClient.Builder()
      .addInterceptor(requestInterceptor)
      .addInterceptor(HttpLoggingInterceptor().setLevel(logLevel)).build()

  private val rxJava2CallAdapterFactory = RxJava2CallAdapterFactory.create()
  private val gsonConverterFactory = GsonConverterFactory.create(gson)

  val retrofit = Retrofit.Builder()
      .callFactory(okHttp)
      .addCallAdapterFactory(rxJava2CallAdapterFactory)
      .addCallAdapterFactory(CoroutineCallAdapterFactory())
      .addConverterFactory(gsonConverterFactory)
      .baseUrl(apiBase)
      .build()

  val apiService: ApiService = retrofit.create(
      ApiService::class.java)

  fun createHostApiService(host: String?): ApiService {
    return if (host != null) {
      val retrofit = Retrofit.Builder()
          .callFactory(okHttp)
          .addCallAdapterFactory(rxJava2CallAdapterFactory)
          .addCallAdapterFactory(CoroutineCallAdapterFactory())
          .addConverterFactory(gsonConverterFactory)
          .baseUrl("https://$host/v2/")
          .build()
      retrofit.create(ApiService::class.java)
    } else {
      apiService
    }
  }

  fun parseError(responseBody: ResponseBody?): ApiError {
    if (responseBody != null) {
      try {
        val converter = retrofit.responseBodyConverter<ApiError>(
            ApiError::class.java,
            arrayOf(object : Annotation {})
        )
        return converter.convert(responseBody)
      } catch (e: Exception) {
        return (ApiError(
            "UNKNOWN_ERROR", "An unknown error has occurred", false
        ))
      }
    } else {
      return (ApiError(
          "UNKNOWN_ERROR", "An unknown error has occurred", false
      ))
    }
  }

  fun createEventSourceUrl(gameHost: String, gameId: UUID): String {
    return  "https://$gameHost/v2/event-feed/games/$gameId"
  }

  internal companion object {

    @Volatile private var INSTANCE: RestClient? = null

    internal fun getInstance(apiBase: String, prefsHelper: PrefsHelper, debug: Boolean): RestClient =
        INSTANCE ?: synchronized(this) {
          INSTANCE
              ?: RestClient(
                  apiBase, prefsHelper, debug
              ).also { INSTANCE = it }
        }
  }
}