package live.stream.theq.theqkit.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import live.stream.theq.theqkit.data.sdk.AuthResponse
import live.stream.theq.theqkit.data.sdk.LoginAuthData
import live.stream.theq.theqkit.data.sdk.SignupAuthData
import live.stream.theq.theqkit.data.sdk.SuccessResponse
import live.stream.theq.theqkit.data.sdk.UserResponse
import live.stream.theq.theqkit.data.sdk.UserUpdateRequest
import live.stream.theq.theqkit.http.RestClient
import live.stream.theq.theqkit.util.PrefsHelper
import retrofit2.Response
import java.lang.Exception

internal class UserRepository(restClient: RestClient, private val prefsHelper: PrefsHelper) : CoroutineScope {
  private val apiService = restClient.apiService

  private val job = Job()
  override val coroutineContext = job + Main

  internal fun loginAsync(partnerCode: String, authData: LoginAuthData): Deferred<Response<AuthResponse>> {
    return apiService.partnerLoginAsync(partnerCode, authData)
  }

  internal fun signupAsync(partnerCode: String, authData: SignupAuthData): Deferred<Response<AuthResponse>> {
    return apiService.parterSignupAsync(partnerCode, authData)
  }

  internal fun logout() {
    prefsHelper.clear()
    launch {
      try {
        apiService.logoutAsync()
            .await()
      } catch (e: Exception) {}
    }
  }

  internal fun fetchUserAsync(userId: String): Deferred<Response<UserResponse>> {
    return apiService.fetchUserAsync(userId)
  }

  internal fun updateUserAsync(userId: String, userUpdateRequest: UserUpdateRequest): Deferred<Response<UserResponse>> {
    return apiService.updateUserAsync(userId, userUpdateRequest)
  }

  internal fun cashoutRequestAsync(userId: String): Deferred<Response<SuccessResponse>> {
    return apiService.cashoutRequestAsync(userId)
  }

  internal fun usernameCheckAsync(query: String): Deferred<Response<Void>> {
    return apiService.usernameCheckAsync(query)
  }

  companion object {
    internal const val ERROR_NO_SUCH_USER = "NO_SUCH_USER"
  }
}