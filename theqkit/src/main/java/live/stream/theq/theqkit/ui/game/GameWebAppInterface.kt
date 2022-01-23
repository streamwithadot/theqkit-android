package live.stream.theq.theqkit.ui.game

import android.webkit.JavascriptInterface
import com.google.gson.Gson
import live.stream.theq.theqkit.data.sdk.WebGameEndedEvent
import java.lang.Exception

class GameWebAppInterface(private val gson: Gson, private val listener: WebGameListener) {

  @JavascriptInterface
  fun postMessage(message: String) {

    val gameEndedEvent: WebGameEndedEvent? = try {
      gson.fromJson(message, WebGameEndedEvent::class.java)
    } catch (e : Exception) {
      null
    }

    if (gameEndedEvent != null) {
      listener.onGameEnded(
        gameEndedEvent.isGameEndedEvent() && gameEndedEvent.isWinner(),
        gameEndedEvent.winnersCount(),
        gameEndedEvent.reward())
    }
  }

  companion object {
    const val NAME = "appInterface"
  }
}
