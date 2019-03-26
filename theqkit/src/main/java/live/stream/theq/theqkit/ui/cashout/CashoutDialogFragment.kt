package live.stream.theq.theqkit.ui.cashout

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
import androidx.lifecycle.ViewModelProviders
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.theq_sdk_fragment_cashout_dialog.cashoutAllowedGroup
import kotlinx.android.synthetic.main.theq_sdk_fragment_cashout_dialog.cashoutMessage
import kotlinx.android.synthetic.main.theq_sdk_fragment_cashout_dialog.dismiss
import kotlinx.android.synthetic.main.theq_sdk_fragment_cashout_dialog.emailEditText
import kotlinx.android.synthetic.main.theq_sdk_fragment_cashout_dialog.loadedGroup
import kotlinx.android.synthetic.main.theq_sdk_fragment_cashout_dialog.loadingSpinner
import kotlinx.android.synthetic.main.theq_sdk_fragment_cashout_dialog.requestCashout

import live.stream.theq.theqkit.R
import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit.SECONDS

@Keep
internal class CashoutDialogFragment : AppCompatDialogFragment() {

  private lateinit var viewModel: CashoutViewModel
  private val disposable = CompositeDisposable()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setStyle(DialogFragment.STYLE_NO_TITLE, R.style.SDKDialogFragment)

    viewModel = ViewModelProviders.of(this)
        .get(CashoutViewModel::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.theq_sdk_fragment_cashout_dialog, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    initializeViews()

    viewModel.loadingLiveData.observe(this, Observer { loading ->
      loadingSpinner.visibility = if (loading) View.VISIBLE else View.GONE
      loadedGroup.visibility = if (loading) View.INVISIBLE else View.VISIBLE
      if (loading) {
        cashoutAllowedGroup.visibility = View.GONE
      }
    })

    viewModel.successMessageResLiveData.observe(this, Observer { stringRes ->
      context?.let {
        showDismissingMessageView(it.resources.getString(stringRes))
      }
    })

    viewModel.errorMessageResLiveData.observe(this, Observer { stringRes ->
      context?.let {
        showErrorMessageView(it.resources.getString(stringRes))
      }
    })

    viewModel.userLiveData.observe(this, Observer { user ->
      context?.let { ctx ->
        if (user == null) {
          showErrorMessageView(ctx.getString(R.string.theq_sdk_cashout_user_load_failure))
          return@Observer
        }

        emailEditText.setText(user.email ?: "")
        val decimalFormat = DecimalFormat(ctx.getString(R.string.theq_sdk_full_currency_format))
        val balance = decimalFormat.format(user.balance)

        if(user.balance >= BigDecimal(resources.getInteger(R.integer.theq_sdk_cashout_minimum))) {
          showCashoutEntryView(balance)
        } else {
          showErrorMessageView(resources.getString(R.string.theq_sdk_cashout_minimum_message, balance))
        }
      }
    })

    viewModel.emailValidatorLiveData.observe(this, Observer { isValid ->
      context?.let {
        requestCashout.isEnabled = isValid
        val textColorResource =
          if(isValid) { R.color.theq_sdk_color_accent } else { R.color.theq_sdk_black }
        requestCashout.setTextColor(ContextCompat.getColor(it, textColorResource))
      }
    })
  }

  override fun onDestroyView() {
    super.onDestroyView()
    disposable.clear()
  }

  private fun initializeViews() {
    dismiss.setOnClickListener {
      dismiss()
    }

    requestCashout.setOnClickListener {
      viewModel.requestCashout(emailEditText.text.toString())
    }

    emailEditText.addTextChangedListener(object : TextWatcher {
      override fun afterTextChanged(s: Editable?) {
        viewModel.validateEmail(s.toString())
      }
      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
  }

  private fun showCashoutEntryView(balance: String) {
    context?.let {
      cashoutMessage.text = it.getString(R.string.theq_sdk_cashout_message, balance)
      cashoutAllowedGroup.visibility = View.VISIBLE
    }
  }

  private fun showErrorMessageView(message: String) {
    context?.let {
      cashoutMessage.text = message
      dismiss.setTextColor(ContextCompat.getColor(it, R.color.theq_sdk_color_accent))
      cashoutAllowedGroup.visibility = View.GONE
    }
  }

  private fun showDismissingMessageView(message: String) {
    context?.let {
      cashoutMessage.text = message
      dismiss.setTextColor(ContextCompat.getColor(it, R.color.theq_sdk_color_accent))
      cashoutAllowedGroup.visibility = View.GONE

      val subscription = Observable
          .timer(3, SECONDS)
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe {
            dismiss()
          }
      disposable.add(subscription)
    }
  }

  companion object {

    @JvmStatic fun newInstance() = CashoutDialogFragment()
  }
}
