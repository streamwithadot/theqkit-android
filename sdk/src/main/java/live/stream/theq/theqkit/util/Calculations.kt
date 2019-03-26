package live.stream.theq.theqkit.util

import android.content.Context
import android.util.DisplayMetrics

internal object Calculations {

  fun convertDpToPixel(context: Context, dp: Float): Float {
    val metrics = context.resources.displayMetrics
    return dp * metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT
  }
}
