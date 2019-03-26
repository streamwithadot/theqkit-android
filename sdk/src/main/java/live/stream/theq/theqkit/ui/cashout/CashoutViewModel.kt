package live.stream.theq.theqkit.ui.cashout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import live.stream.theq.theqkit.R
import live.stream.theq.theqkit.TheQKit
import live.stream.theq.theqkit.data.sdk.User
import live.stream.theq.theqkit.data.sdk.UserUpdateRequest
import live.stream.theq.theqkit.util.EmailHelper

internal class CashoutViewModel : ViewModel(), CoroutineScope {
  private val userRepository = TheQKit.getInstance().userRepository
  private val prefsHelper = TheQKit.getInstance().prefsHelper

  private val job = Job()
  override val coroutineContext = job + Main

  private val _emailValidatorLiveData = MutableLiveData<Boolean>()
  val emailValidatorLiveData: LiveData<Boolean> = _emailValidatorLiveData

  private val _userLiveData = MutableLiveData<User>()
  val userLiveData: LiveData<User> = _userLiveData

  private val _loadingLiveData = MutableLiveData<Boolean>()
  val loadingLiveData: LiveData<Boolean> = _loadingLiveData

  private val _errorMessageResLiveData = MutableLiveData<Int>()
  val errorMessageResLiveData: LiveData<Int> = _errorMessageResLiveData

  private val _successMessageResLiveData = MutableLiveData<Int>()
  val successMessageResLiveData: LiveData<Int> = _successMessageResLiveData

  init {
    fetchUser()
  }

  private fun fetchUser() {
    _loadingLiveData.value = true
    val userId = prefsHelper.userId

    if (userId == null) {
      _errorMessageResLiveData.value = R.string.theq_sdk_cashout_not_logged_in
    } else {
      launch {
        val user = try {
          val userResponse = userRepository.fetchUserAsync(userId).await()
          userResponse.body()?.user
        } catch (e :Exception) {
          null
        }
        _loadingLiveData.value = false
        if (user != null) {
          _userLiveData.value = user
        } else {
          _errorMessageResLiveData.value = R.string.theq_sdk_cashout_user_load_failure
        }
      }
    }
  }

  fun requestCashout(email: String) {
    launch {
      val userId = prefsHelper.userId
      if (userId == null) {
        _errorMessageResLiveData.value = R.string.theq_sdk_cashout_not_logged_in
      } else {
        _loadingLiveData.value = true
        if (email == prefsHelper.email) {
          submitCashoutRequest(userId)
        } else {
          val successfulEmailUpdate = submitUpdateUser(userId, email)
          if (successfulEmailUpdate) {
            submitCashoutRequest(userId)
          }
        }
        _loadingLiveData.value = false
      }
    }
  }

  private suspend fun submitUpdateUser(userId: String, email: String): Boolean {
    val userUpdateRequest = UserUpdateRequest(email)
    val updatedEmail = try {
      val userResponse = userRepository.updateUserAsync(userId, userUpdateRequest)
          .await()
      userResponse.body()?.user?.email
    } catch(e: Exception) {
      null
    }

    return if (updatedEmail != null) {
      true
    } else {
      _errorMessageResLiveData.value = R.string.theq_sdk_cashout_email_update_failure
      false
    }
  }

  private suspend fun submitCashoutRequest(userId: String) {
    val isCashoutSuccessful = try {
      val cashoutResponse = userRepository.cashoutRequestAsync(userId).await()
      cashoutResponse.isSuccessful
    } catch (e: Exception) {
      false
    }

    if (isCashoutSuccessful) {
      _successMessageResLiveData.value = R.string.theq_sdk_cashout_success_message
    } else {
      _errorMessageResLiveData.value = R.string.theq_sdk_cashout_unknown_error
    }
  }

  fun validateEmail(email: String) {
    _emailValidatorLiveData.value = email.matches(Regex(EmailHelper.EMAIL_REGEX))
  }

  override fun onCleared() {
    super.onCleared()
    job.cancel()
  }
}