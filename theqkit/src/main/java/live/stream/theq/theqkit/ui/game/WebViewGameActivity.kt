package live.stream.theq.theqkit.ui.game

import android.graphics.Bitmap
import android.os.*
import android.view.Window
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import live.stream.theq.theqkit.R
import live.stream.theq.theqkit.TheQKit
import live.stream.theq.theqkit.data.sdk.GameResponse
import java.net.URLEncoder

@Keep
open class WebViewGameActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    window.setFlags(
        WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN
    )
    setContentView(R.layout.theqkit_activity_webview_game)

    val partnerName = TheQKit.getInstance().config.partnerName
    val basePlayerUrl = TheQKit.getInstance().config.webPlayerUrl
    val gameId = intent?.extras?.getParcelable<GameResponse>(KEY_GAME)?.id
    val authToken = TheQKit.getInstance().prefsHelper.bearerToken

    if (partnerName == null || basePlayerUrl == null || gameId == null || authToken == null) {
        finish()
        return
    }

    val gameUrl = "${basePlayerUrl}partner/${partnerName}?qToken=${URLEncoder.encode(authToken, "utf-8")}"

    findViewById<WebView>(R.id.theqkit_game_webview).apply {
      settings.apply {
        javaScriptEnabled = true
        builtInZoomControls = false
        displayZoomControls = false
        mediaPlaybackRequiresUserGesture = false
        webChromeClient = object : WebChromeClient() {
          override fun getDefaultVideoPoster(): Bitmap? {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
          }
        }
      }
      loadUrl(gameUrl)
    }
  }

  companion object {
    const val KEY_GAME = "KEY_GAME"
  }
}
