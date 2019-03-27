package live.stream.theq.theqkit.util

import android.content.Context
import androidx.annotation.Keep
import live.stream.theq.theqkit.R
import java.text.DecimalFormat

@Keep
object CurrencyHelper {

  fun getExactCurrency(context: Context, obj: Any): String {
    val format = context.resources.getString(R.string.theqkit_full_currency_format)
    return DecimalFormat(format).format(obj)
  }

  fun getRoundedCurrency(context: Context, obj: Any): String {
    val format = context.resources.getString(R.string.theqkit_rounded_currency_format)
    return DecimalFormat(format).format(obj)
  }
}