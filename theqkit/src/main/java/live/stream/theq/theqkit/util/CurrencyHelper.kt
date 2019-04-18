package live.stream.theq.theqkit.util

import android.content.Context
import androidx.annotation.Keep
import live.stream.theq.theqkit.R
import java.text.DecimalFormat

/**
 * Provides helper methods for formatting numeric values as currency.
 */
@Keep
object CurrencyHelper {

  /**
   * Gets a currency-formatted string with fractional monetary unit precision.
   *
   * This method returns a formatted string according to the pattern specified by
   * `R.string.theqkit_full_currency_format`. This pattern may be overridden by your own
   * string resources to support different currencies or formatting preferences.
   *
   * @param context a reference to the current context.
   * @param number the number to format.
   * @return the formatted currency value.
   */
  fun getExactCurrency(context: Context, number: Any): String {
    val format = context.resources.getString(R.string.theqkit_full_currency_format)
    return DecimalFormat(format).format(number)
  }

  /**
   * Gets a currency-formatted string rounded to the nearest basic monetary unit value.
   *
   * This method returns a formatted string according to the pattern specified by
   * `R.string.theqkit_rounded_currency_format`. This pattern may be overridden by your own
   * string resources to support different currencies or formatting preferences.
   *
   * @param context a reference to the current context.
   * @param number the number to format.
   * @return the formatted currency value.
   */
  fun getRoundedCurrency(context: Context, number: Any): String {
    val format = context.resources.getString(R.string.theqkit_rounded_currency_format)
    return DecimalFormat(format).format(number)
  }

}
