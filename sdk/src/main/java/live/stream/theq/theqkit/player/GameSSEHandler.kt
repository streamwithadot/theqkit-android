package live.stream.theq.theqkit.player

import android.util.Log
import com.tylerjroach.eventsource.EventSource
import com.tylerjroach.eventsource.EventSourceHandler
import com.tylerjroach.eventsource.MessageEvent
import live.stream.theq.theqkit.data.sdk.GameEnded
import live.stream.theq.theqkit.data.sdk.GameStatus
import live.stream.theq.theqkit.data.sdk.GameWinners
import live.stream.theq.theqkit.data.sdk.GameWon
import live.stream.theq.theqkit.data.sdk.QuestionEnd
import live.stream.theq.theqkit.data.sdk.QuestionResult
import live.stream.theq.theqkit.data.sdk.QuestionStart
import live.stream.theq.theqkit.data.sdk.ViewCountUpdate
import live.stream.theq.theqkit.http.RestClient
import live.stream.theq.theqkit.util.PrefsHelper
import java.util.UUID

internal class GameSSEHandler(private val restClient: RestClient, private val prefsHelper: PrefsHelper): EventSourceHandler {

  private val gson = restClient.gson
  private var eventSource: EventSource? = null
  private val parsedEventListeners = mutableListOf<(Any)->Unit>()

  fun connect(host: String, gameId: UUID) {
    if (eventSource != null) return

    val url = restClient.createEventSourceUrl(host, gameId)

    eventSource = EventSource.Builder(url)
        .eventHandler(this)
        .headers(mutableMapOf("Authorization" to "Bearer ${prefsHelper.bearerToken}"))
        .build()

    eventSource?.connect()

    Log.d(TAG, "Event Source Connect: $url")
  }

  fun addParsedEventListener(listener: (Any)->Unit) {
    parsedEventListeners.add(listener)
  }

  override fun onMessage(event: String, message: MessageEvent) {
    Log.d(TAG, "$event: ${message.data}")

    val parsedEvent: Any? = when (event) {
      "GameStatus" -> gson.fromJson(message.data, GameStatus::class.java)
      "GameWon" -> gson.fromJson(message.data, GameWon::class.java)
      "GameEnded" -> gson.fromJson(message.data, GameEnded::class.java)
      "GameWinners" -> gson.fromJson(message.data, GameWinners::class.java)
      "QuestionStart" -> gson.fromJson(message.data, QuestionStart::class.java)
      "QuestionEnd" -> gson.fromJson(message.data, QuestionEnd::class.java)
      "QuestionResult" -> gson.fromJson(message.data, QuestionResult::class.java)
      "ViewCountUpdate" -> gson.fromJson(message.data, ViewCountUpdate::class.java)
      else -> null
    }

    parsedEvent?.let { evt ->
      parsedEventListeners.forEach { it.invoke(evt) }
    }
  }

  override fun onConnect() { Log.d(TAG, "onConnect") }

  override fun onComment(comment: String?) {}

  override fun onError(t: Throwable?) { Log.w(
      TAG, "onError - Message: ${t?.message}") }

  override fun onClosed(willReconnect: Boolean) {
    Log.d(TAG, "onClosed - Reconnect: $willReconnect")

    if (!willReconnect) {
      destroy()
    }
  }

  fun destroy() {
    eventSource?.close()
    eventSource = null
    parsedEventListeners.clear()
  }

  companion object {
    const val TAG = "GameSSEHandler"
  }
}
