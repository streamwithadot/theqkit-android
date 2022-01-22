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
import androidx.core.content.ContextCompat
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
    val gameUrl = "${basePlayerUrl}partner/${partnerName}?game=${game.id
    }&qToken=${URLEncoder.encode(authToken, "utf-8")}&useMobile=1"

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
        setBackgroundColor(ContextCompat.getColor(this@WebViewGameActivity, R.color.theqkit_web_game_loading_background))
        addJavascriptInterface(GameWebAppInterface(gson, this@WebViewGameActivity), GameWebAppInterface.NAME)
      }
      loadUrl(gameUrl)
    }
  }

  override fun onBackPressed() {
    super.onBackPressed()
    setResult(RESULT_CODE, getResultIntent(game, winner = false, gameEnded = false, winnerCount = 0, reward = 0))
  }

  override fun onGameEnded(winner: Boolean, winnerCount: Int, reward: Int) {
    setResult(RESULT_CODE, getResultIntent(game, winner = winner, gameEnded = true, winnerCount = winnerCount, reward = reward))
    finish()
  }

  private fun getResultIntent(game: GameResponse, winner: Boolean, gameEnded: Boolean, winnerCount: Int, reward: Int): Intent {
    return Intent().apply {
      putExtra(KEY_GAME_WINNER, winner)
      putExtra(KEY_GAME_ENDED, gameEnded)
      putExtra(KEY_GAME_WINNER_COUNT, winnerCount)
      putExtra(KEY_GAME_REWARD, reward)
    }
  }

  companion object {
    const val KEY_GAME = "KEY_GAME"
    const val REQUEST_CODE = 58
    const val RESULT_CODE = 72
    const val KEY_WINNER = "WINNER"
    const val KEY_GAME_ENDED = "GAME_ENDED"
    const val KEY_GAME_WINNER = "GAME_WINNER"
    const val KEY_GAME_WINNER_COUNT = "GAME_WINNER_COUNT"
    const val KEY_GAME_REWARD = "GAME_REWARD"
  }
}

interface WebGameListener {
  fun onGameEnded(winner: Boolean, winnerCount: Int, reward: Int)
}
