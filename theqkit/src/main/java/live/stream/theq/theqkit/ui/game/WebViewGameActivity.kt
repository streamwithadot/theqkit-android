package live.stream.theq.theqkit.ui.game

import android.content.Intent
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
open class WebViewGameActivity : AppCompatActivity(), WebGameListener {

  private val gson = TheQKit.getInstance().getRestClient().gson
  lateinit var game: GameResponse

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    window.setFlags(
        WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN
    )
    setContentView(R.layout.theqkit_activity_webview_game)

    val game = intent?.extras?.getParcelable<GameResponse>(KEY_GAME)
    val partnerName = TheQKit.getInstance().config.partnerName
    val basePlayerUrl = TheQKit.getInstance().config.webPlayerUrl
    val authToken = TheQKit.getInstance().prefsHelper.bearerToken

    if (partnerName == null || basePlayerUrl == null || game == null || authToken == null) {
        finish()
        return
    }

    this.game = game
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
        addJavascriptInterface(GameWebAppInterface(gson, this@WebViewGameActivity), GameWebAppInterface.NAME)
      }
      loadUrl(gameUrl)
    }
  }

  override fun onBackPressed() {
    super.onBackPressed()
    setResult(RESULT_CODE, getResultIntent(game, winner = false, gameEnded = false))
  }

  override fun onGameEnded(winner: Boolean) {
    setResult(RESULT_CODE, getResultIntent(game, winner = winner, gameEnded = true))
  }

  private fun getResultIntent(game: GameResponse, winner: Boolean, gameEnded: Boolean): Intent {
    return Intent().apply {
      putExtra(KEY_WINNER, winner)
      putExtra(KEY_GAME_ENDED, gameEnded)
      putExtra(KEY_GAME, game)
    }
  }

  companion object {
    const val KEY_GAME = "KEY_GAME"
    const val REQUEST_CODE = 58
    const val RESULT_CODE = 72
    const val KEY_WINNER = "WINNER"
    const val KEY_GAME_ENDED = "GAME_ENDED"
  }
}

interface WebGameListener {
  fun onGameEnded(winner: Boolean)
}