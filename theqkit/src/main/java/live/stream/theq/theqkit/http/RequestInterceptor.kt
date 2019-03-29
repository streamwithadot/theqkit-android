package live.stream.theq.theqkit.http

import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Response
import com.google.gson.Gson
import live.stream.theq.theqkit.data.sdk.ApiError
import live.stream.theq.theqkit.events.Events
import live.stream.theq.theqkit.events.UserBannedEvent
import live.stream.theq.theqkit.util.PrefsHelper

internal class RequestInterceptor(private val prefsHelper: PrefsHelper, private val gson: Gson) : Interceptor {
  override fun intercept(chain: Chain): Response {
    val request = chain.request()

    val urlBuilder = request.url().newBuilder()
    prefsHelper.userId?.let { uid -> urlBuilder.addQueryParameter("uid", uid) }

    val requestBuilder = request.newBuilder().url(urlBuilder.build())
    prefsHelper.bearerToken?.let { token -> requestBuilder.addHeader("Authorization", "Bearer $token") }

    val response = chain.proceed(requestBuilder.build())
    if (response.code() == 401) {
      try {
        val apiError = gson
            .fromJson(response.peekBody(1000000).charStream(), ApiError::class.java)
        if (apiError.errorCode == "USER_BANNED") {
          val wasPreviouslyBanned = prefsHelper.banned
          prefsHelper.banned = true
          Events.publish(UserBannedEvent(wasPreviouslyBanned))
        }
      } catch (e: Exception) {}
    }
    return response
  }
}