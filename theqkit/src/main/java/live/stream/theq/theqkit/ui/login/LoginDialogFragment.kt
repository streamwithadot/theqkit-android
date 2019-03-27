package live.stream.theq.theqkit.ui.login

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.theqkit_fragment_login_dialog.cancel
import kotlinx.android.synthetic.main.theqkit_fragment_login_dialog.entryGroup
import kotlinx.android.synthetic.main.theqkit_fragment_login_dialog.inputNotifier
import kotlinx.android.synthetic.main.theqkit_fragment_login_dialog.loadingGroup
import kotlinx.android.synthetic.main.theqkit_fragment_login_dialog.loadingMessage
import kotlinx.android.synthetic.main.theqkit_fragment_login_dialog.notifierProgress
import kotlinx.android.synthetic.main.theqkit_fragment_login_dialog.submit
import kotlinx.android.synthetic.main.theqkit_fragment_login_dialog.usernameEditText

import live.stream.theq.theqkit.R
import live.stream.theq.theqkit.data.sdk.AccountKitLogin
import live.stream.theq.theqkit.data.sdk.ApiError
import live.stream.theq.theqkit.data.sdk.FirebaseLogin
import live.stream.theq.theqkit.data.sdk.LoginAuthData
import live.stream.theq.theqkit.listener.LoginResponseListener
import java.lang.IllegalStateException

@Keep
internal class LoginDialogFragment : AppCompatDialogFragment() {

  private lateinit var viewModel: LoginViewModel
  private var listener: LoginResponseListener? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isCancelable = false
    setStyle(DialogFragment.STYLE_NO_TITLE, R.style.TheQKit_DialogFragment)

    val loginAuthData = arguments!!.getParcelable<LoginAuthData>(
        KEY_LOGIN_AUTH_DATA)!!
    val suggestedUsername = arguments!!.getString(
        KEY_SUGGESTED_USERNAME)

    viewModel =
      ViewModelProviders.of(this,
          LoginViewModelFactory(loginAuthData, suggestedUsername,
              listener))
          .get(LoginViewModel::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.theqkit_fragment_login_dialog, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    viewModel.loginStateLiveData.observe(this, Observer { state ->
      context?.let {
        if (state.isCompleted) {
          // not a fan of allowing state loss.
          // I don't see any other choice since we do not have control of activity.
          dismissAllowingStateLoss()
          return@Observer
        }

        loadingMessage.text = it.resources.getString(state.loadingMessage)
        if (state.isLoading) {
          loadingGroup.visibility = View.VISIBLE
          entryGroup.visibility = View.GONE
          inputNotifier.visibility = View.GONE
          notifierProgress.visibility = View.GONE
        } else {
          loadingGroup.visibility = View.GONE
          entryGroup.visibility = View.VISIBLE

          if (state.isUsernameQueryInFlight) {
            inputNotifier.visibility = View.INVISIBLE
            notifierProgress.visibility = View.VISIBLE
            submit.setTextColor(ContextCompat.getColor(it, R.color.theqkit_black_50p_transparent))
            submit.isEnabled = false
          } else {
            notifierProgress.visibility = View.INVISIBLE
            when {
              state.isUsernameQueryValid -> {
                inputNotifier.visibility = View.INVISIBLE
                submit.setTextColor(ContextCompat.getColor(it, R.color.theqkit_color_accent))
                submit.isEnabled = true
              }
              usernameEditText.text.toString().trim().isBlank() -> {
                inputNotifier.visibility = View.INVISIBLE
                submit.setTextColor(
                    ContextCompat.getColor(it, R.color.theqkit_black_50p_transparent))
                submit.isEnabled = false
              }
              else -> {
                inputNotifier.visibility = View.VISIBLE
                submit.setTextColor(
                    ContextCompat.getColor(it, R.color.theqkit_black_50p_transparent))
                submit.isEnabled = false
              }
            }
          }
        }
      }
    })

    usernameEditText.addTextChangedListener(object : TextWatcher {
      override fun afterTextChanged(s: Editable?) {
        viewModel.usernameQueryChanged(s.toString().trim())
      }

      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })

    submit.setOnClickListener {
      val typedUsername = usernameEditText.text.toString()
      viewModel.signupClicked(typedUsername)
    }

    cancel.setOnClickListener {
      viewModel.handleFailedLogin(
          ApiError("USER_CANCELLED", "User cancelled signup."))
    }
  }

  internal fun setListener(listener: LoginResponseListener?) {
    this.listener = listener
  }

  companion object {

    private const val KEY_LOGIN_AUTH_DATA = "KEY_LOGIN_AUTH_DATA"
    private const val KEY_SUGGESTED_USERNAME = "KEY_SUGGESTED_USERNAME"

    fun newInstance(accountKitLogin: AccountKitLogin? = null,
      firebaseLogin: FirebaseLogin? = null,
      suggestedUsername: String?,
      listener: LoginResponseListener
    ): LoginDialogFragment {
      val loginDialogFragment = LoginDialogFragment().apply {
        arguments = Bundle().apply {
          val loginAuthData = when {
            accountKitLogin != null -> LoginAuthData(accountKit = accountKitLogin)
            firebaseLogin != null -> LoginAuthData(firebase = firebaseLogin)
            else -> throw IllegalStateException("No login data passed")
          }
          putParcelable(KEY_LOGIN_AUTH_DATA, loginAuthData)
          putString(KEY_SUGGESTED_USERNAME, suggestedUsername)
        }
      }
      loginDialogFragment.setListener(listener)
      return loginDialogFragment
    }
  }
}

internal class LoginViewModelFactory(
  private val loginAuthData: LoginAuthData,
  private val suggestedUsername: String?,
  private val listener: LoginResponseListener?
) : ViewModelProvider.NewInstanceFactory() {

  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return LoginViewModel(loginAuthData, suggestedUsername,
        listener) as T
  }
}
