package live.stream.theq.theqkit.ui.login

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import live.stream.theq.theqkit.R
import live.stream.theq.theqkit.TheQKit
import live.stream.theq.theqkit.data.sdk.ApiError
import live.stream.theq.theqkit.data.sdk.AuthData
import live.stream.theq.theqkit.data.sdk.AuthResponse
import live.stream.theq.theqkit.data.sdk.LoginAuthData
import live.stream.theq.theqkit.data.sdk.SignupAuthData
import live.stream.theq.theqkit.http.RestClient
import live.stream.theq.theqkit.listener.LoginResponseListener
import live.stream.theq.theqkit.repository.UserRepository
import java.lang.Exception

internal class LoginViewModel(
  private val loginAuthData: LoginAuthData,
  private val suggestedUsername: String?,
  internal val listener: LoginResponseListener?
) : ViewModel(), CoroutineScope {

  private val config = TheQKit.getInstance().config
  private val userRepository = TheQKit.getInstance().userRepository
  private val restClient: RestClient = TheQKit.getInstance().restClient
  private val prefsHelper = TheQKit.getInstance().prefsHelper

  private val _loginStateLiveData = MutableLiveData<LoginState>()
  val loginStateLiveData: LiveData<LoginState> = _loginStateLiveData

  private val usernameQueryLiveData = MutableLiveData<String>()

  private val job = Job()
  override val coroutineContext = job + Main
  private var individualQueryJob: Job? = null

  init {
    _loginStateLiveData.value =
      LoginState(isLoading = true,
          loadingMessage = R.string.theqkit_logging_in)
    login()
  }

  fun login() {
    launch {
      if (config.partnerCode == null) {
        Log.d(
            TheQKit.TAG, "Login failed on null partner code. This should not be allowed to happen"
        )
        return@launch
      }

      try {
        val loginResponse = userRepository.loginAsync(config.partnerCode, loginAuthData)
            .await()
        val authResponse = loginResponse.body()
        if (loginResponse.isSuccessful && authResponse != null) {
          handleSuccessfulLogin(authResponse)
        } else {
          val apiError = restClient.parseError(loginResponse.errorBody())
          when (apiError.errorCode) {
            UserRepository.ERROR_NO_SUCH_USER -> signup(suggestedUsername)
            else -> handleFailedLogin(apiError)
          }
        }
      } catch (e: Exception) {
        handleFailedLogin(ApiError())
      }
    }
  }

  fun signupClicked(suggestedUsername: String?) {
    if (loginStateLiveData.value?.isUsernameQueryValid == true) {
      _loginStateLiveData.value = _loginStateLiveData.value?.copy(
          isLoading = true, loadingMessage = R.string.theqkit_creating_account
      )
      launch {
        signup(suggestedUsername)
      }
    }
  }

  fun usernameQueryChanged(query: String) {
    if (query != usernameQueryLiveData.value) {
      usernameQueryLiveData.value = query

      if (individualQueryJob?.isActive == true) {
        individualQueryJob?.cancel()
        individualQueryJob = null
        if (_loginStateLiveData.value?.isUsernameQueryInFlight == true) {
          _loginStateLiveData.value =
            _loginStateLiveData.value?.copy(isUsernameQueryInFlight = false)
        }
      }

      if (query.isBlank()) {
        // clear any warnings
        _loginStateLiveData.value = _loginStateLiveData.value?.copy(isUsernameQueryValid = false)
        return
      }

      _loginStateLiveData.value = _loginStateLiveData.value?.copy(isUsernameQueryInFlight = true)
      individualQueryJob = launch {
        delay(500)
        val usernameAvailable =
          try {
            val usernameCheck = userRepository.usernameCheckAsync(query)
                .await()
            usernameCheck.code() == 404
          } catch (e: Exception) {
            false
          }
        _loginStateLiveData.value = _loginStateLiveData.value?.copy(
            isUsernameQueryValid = usernameAvailable, isUsernameQueryInFlight = false
        )
      }
    }
  }

  private suspend fun signup(suggestedUsername: String?) {
    if (config.partnerCode == null) {
      Log.d(TheQKit.TAG, "Signup failed on null partner code. This should not be allowed to happen")
      return
    }
    if (suggestedUsername != null) {
      try {
        val signupAuthData =
          SignupAuthData(
            AuthData(accountKit = loginAuthData.accountKit, firebase = loginAuthData.firebase),
            suggestedUsername, autoHandleUsernameCollision = true)
        val signupResponse = userRepository.signupAsync(config.partnerCode, signupAuthData)
            .await()
        val authResponse = signupResponse.body()
        if (signupResponse.isSuccessful && authResponse != null) {
          handleSuccessfulLogin(authResponse)
        } else {
          val apiError = restClient.parseError(signupResponse.errorBody())
          handleFailedLogin(apiError)
        }
      } catch (e: Exception) {
        handleFailedLogin(ApiError())
      }
    } else {
      _loginStateLiveData.value = _loginStateLiveData.value?.copy(isLoading = false)
    }
  }

  private fun handleSuccessfulLogin(authResponse: AuthResponse) {
    prefsHelper.saveUser(authResponse)
    listener?.onSuccess()
    _loginStateLiveData.value = _loginStateLiveData.value?.copy(isCompleted = true)
  }

  fun handleFailedLogin(apiError: ApiError) {
    listener?.onFailure(apiError)
    _loginStateLiveData.value = _loginStateLiveData.value?.copy(isCompleted = true)
  }

  override fun onCleared() {
    super.onCleared()
    job.cancel()
  }
}

internal data class LoginState(
  val isCompleted: Boolean = false,
  val isLoading: Boolean = false,
  val isUsernameQueryValid: Boolean = false,
  val isUsernameQueryInFlight: Boolean = false,
  @StringRes val loadingMessage: Int = R.string.theqkit_logging_in
)