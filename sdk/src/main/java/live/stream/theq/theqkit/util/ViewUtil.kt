package live.stream.theq.theqkit.util

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

internal object ViewUtil {
  fun hideKeyboard(activity: Activity?) {
    activity?.let {
      val view = it.findViewById<View>(android.R.id.content)
      val imm = it.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
      imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
  }
}
