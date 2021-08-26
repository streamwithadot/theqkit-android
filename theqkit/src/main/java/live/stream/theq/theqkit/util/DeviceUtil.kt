package live.stream.theq.theqkit.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings.Secure
import live.stream.theq.theqkit.BuildConfig
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import java.io.UnsupportedEncodingException
import java.util.UUID

/** @suppress **/
object DeviceUtil : KoinComponent {

  private val prefsHelper: PrefsHelper by inject()
  private val context: Context by inject()

  fun isSupportedDevice(): Boolean {
    return (!isEmulator() && !isX86()) || prefsHelper.admin || BuildConfig.DEBUG
  }

  private fun isEmulator(): Boolean {
    return (Build.FINGERPRINT.startsWith("generic")
        || Build.FINGERPRINT.startsWith("unknown")
        || Build.MODEL.contains("google_sdk")
        || Build.MODEL.contains("Emulator")
        || Build.MODEL.contains("Android SDK built for x86")
        || Build.MANUFACTURER.contains("Genymotion")
        || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
        || "google_sdk" == Build.PRODUCT)
  }

  private fun isX86(): Boolean {
    return Build.SUPPORTED_ABIS[0].contains("x86")
  }

  fun getUniqueDeviceIdentifier(): String {
    val androidId = Secure.getString(context.contentResolver, Secure.ANDROID_ID)
    // Use the Android ID unless it's broken
    // then fallback on a random
    // number which we store to a prefs file
    return try {
      if (androidId != null && "9774d56d682e549c" != androidId && androidId != "") {
        UUID.nameUUIDFromBytes(androidId.toByteArray(charset("utf8"))).toString()
      } else {
        ""
      }
    } catch (e: UnsupportedEncodingException) {
      ""
    }
  }

  fun vibratePhone(context: Context) {
    if (Build.VERSION.SDK_INT >= 26) {
      (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
          .vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
      (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(150)
    }
  }
}
