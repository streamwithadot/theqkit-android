package live.stream.theq.theqkit.util

import android.content.Context
import android.graphics.Point
import android.view.WindowManager

internal object NavigationUtil {

  fun getNavigationBarSize(context: Context): Point {

    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    val usableSize =
      getAppUsableScreenSize(windowManager)
    val realSize = getRealScreenSize(windowManager)

    return when {
      // navigation bar on the side
      usableSize.x < realSize.x -> Point(realSize.x - usableSize.x, usableSize.y)
      // navigation bar at the bottom
      usableSize.y < realSize.y -> Point(usableSize.x, realSize.y - usableSize.y)
      // navigation bar is not present
      else -> Point()
    }
  }

  private fun getAppUsableScreenSize(windowManager: WindowManager) = Point().also(windowManager.defaultDisplay::getSize)

  private fun getRealScreenSize(windowManager: WindowManager) = Point().also(windowManager.defaultDisplay::getRealSize)

}
