package live.stream.theq.theqkit.repository

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Job
import live.stream.theq.theqkit.data.sdk.ApiException
import live.stream.theq.theqkit.data.sdk.ChoiceState
import live.stream.theq.theqkit.data.sdk.ChoiceSubmissionState
import live.stream.theq.theqkit.data.sdk.GameEnded
import live.stream.theq.theqkit.data.sdk.GameResponse
import live.stream.theq.theqkit.data.sdk.GameState
import live.stream.theq.theqkit.data.sdk.GameStatus
import live.stream.theq.theqkit.data.sdk.GameWinners
import live.stream.theq.theqkit.data.sdk.GameWon
import live.stream.theq.theqkit.data.sdk.QuestionEnd
import live.stream.theq.theqkit.data.sdk.QuestionResult
import live.stream.theq.theqkit.data.sdk.QuestionStart
import live.stream.theq.theqkit.data.sdk.Resource
import live.stream.theq.theqkit.data.sdk.ViewCountUpdate
import live.stream.theq.theqkit.events.ErrorSubmissionEvent
import live.stream.theq.theqkit.events.Events
import live.stream.theq.theqkit.events.GameWonEvent
import live.stream.theq.theqkit.extensions.getOrThrow
import live.stream.theq.theqkit.player.GameSSEHandler
import live.stream.theq.theqkit.http.RestClient
import live.stream.theq.theqkit.util.PrefsHelper
import live.stream.theq.theqkit.internal.launchAsync
import java.lang.Exception

internal class LiveGameRepository internal constructor(
  private val restClient: RestClient,
  private val prefsHelper: PrefsHelper
) {

  private var currentLiveGame: LiveGame? = null

  fun getLiveGame(game: GameResponse): LiveGame {
    currentLiveGame?.let {
      if (!it.isDestroyed && it.gameResponse.id == game.id) {
        return it
      } else if (!it.isDestroyed) {
        it.destroy()
      }
    }
    return LiveGame(game, restClient, prefsHelper)
        .also { currentLiveGame = it }
  }
}

internal class LiveGame(
  val gameResponse: GameResponse,
  private val restClient: RestClient,
  private val prefsHelper: PrefsHelper
) {

  private val apiService = restClient.createHostApiService(gameResponse.host)
  private val sseHandler = GameSSEHandler(restClient, prefsHelper)
  private var choiceJob: Job? = null

  internal var isDestroyed = false

  val game = MutableLiveData<GameState?>()
  val viewerCount = MutableLiveData<Long?>()
  val choiceSubmission = MutableLiveData<Resource<ChoiceSubmissionState>?>()

  init {
    val host = gameResponse.host ?: throw Error(
        "Cannot initialize SSE connection for game with null sseHost: ${gameResponse.id}"
    )

    sseHandler.addParsedEventListener(this::onEvent)
    sseHandler.connect(host, gameResponse.id)
  }

  private fun onEvent(event: Any) {
    when (event) {
      is GameStatus -> GameState(
        event,
        gameResponse
      ).let { gameState ->
        game.postValue(gameState)
        gameState.currentQuestion?.let { game.postValue(gameState.getAsQuestionStart()) }
      }
      is GameEnded -> game.value?.let { game.postValue(it.getUpdated(event)) }
      is GameWinners -> game.value?.let { game.postValue(it.getUpdated(event)) }
      is GameWon -> Events.publish(GameWonEvent(event.amount))
      is QuestionStart -> game.value?.let {
        choiceJob?.cancel() // <-- just being paranoid. shouldn't need this...
        choiceSubmission.postValue(null)
        game.postValue(it.getUpdated(event))
      }
      is QuestionEnd -> game.value?.let {
        choiceJob?.cancel()
        if (choiceSubmission.value?.isLoading() == true) {
          choiceSubmission.postValue(
              Resource.error("Choice submission did not complete before QuestionEnd event")
          )
        }
        game.postValue(it.getUpdated(event))
      }
      is QuestionResult -> game.value?.let { game.postValue(it.getUpdated(event)) }
      is ViewCountUpdate -> viewerCount.postValue(event.viewCnt.toLong())
    }
  }

  fun submitChoice(choice: ChoiceState, useHeart: Boolean) {
    if (this.isDestroyed) return

    choiceSubmission.value = Resource.loading(
        ChoiceSubmissionState(choice, null))

    choiceJob?.cancel()
    choiceJob = launchAsync {
      try {
        apiService.submitAnswerAsync(
            this@LiveGame.gameResponse.id,
            choice.questionId,
            choice.id.toString(),
            useHeart
        )
            .await()
            .let {
              val result = it.getOrThrow(restClient)
              if (result?.usedHeart == true) prefsHelper.subtractFullHeart()
              choiceSubmission.value = if (result?.success == true) {
                Resource.success(
                    ChoiceSubmissionState(choice, result)
                )
              } else {
                trackSubmissionError(choice)
                Resource.error(
                    "success flag was false",
                    ChoiceSubmissionState(choice, result)
                )
              }
            }
      } catch (err: Exception) {
        trackSubmissionError(
            choice,
            (err as? ApiException)?.errorCode
        )
        choiceSubmission.value = Resource.error(
            err,
            ChoiceSubmissionState(choice, null)
        )
      }
    }
  }

  private fun trackSubmissionError(choice: ChoiceState, errorCode: String? = null) {
    game.value?.let {
      Events.publish(
          ErrorSubmissionEvent(it, choice,
              errorCode ?: "UNKNOWN_ERROR"))
    }
  }

  fun destroy() {
    sseHandler.destroy()
    choiceJob?.cancel()
    game.postValue(null)
    viewerCount.postValue(null)
    choiceSubmission.postValue(null)
    isDestroyed = true
  }
}
