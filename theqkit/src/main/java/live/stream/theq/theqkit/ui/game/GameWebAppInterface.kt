package live.stream.theq.theqkit.ui.game

import android.util.Log
import android.webkit.JavascriptInterface
import com.google.gson.Gson
import live.stream.theq.theqkit.data.sdk.WebGameEndedEvent
import java.lang.Exception

class GameWebAppInterface(private val gson: Gson, private val listener: WebGameListener) {

  @JavascriptInterface
  fun postMessage(message: String) {

    Log.i("POST_MESSAGE", "MESSAGE: ${message}") 
    val gameEndedEvent: WebGameEndedEvent? = try {
      gson.fromJson(message, WebGameEndedEvent::class.java)
    } catch (e : Exception) {
      null
    }

    if (gameEndedEvent != null) {
      Log.i("GAME_ENDED_EVENT", "Ended: ${gameEndedEvent.isGameEndedEvent()}, Winner: ${gameEndedEvent.isWinner()}, WinnerCount: ${gameEndedEvent.winnerCount()}, Reward ${gameEndedEvent.reward()}")
      listener.onGameEnded(gameEndedEvent.isGameEndedEvent() && gameEndedEvent.isWinner(), gameEndedEvent.winnerCount(), gameEndedEvent.reward())
    }
  }

  companion object {
    const val NAME = "appInterface"
  }
}
