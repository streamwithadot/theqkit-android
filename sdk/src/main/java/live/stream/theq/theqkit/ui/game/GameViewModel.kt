package live.stream.theq.theqkit.ui.game

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import live.stream.theq.theqkit.data.sdk.ChoiceState
import live.stream.theq.theqkit.data.sdk.ChoiceSubmissionState
import live.stream.theq.theqkit.data.sdk.FreeResponseChoiceState
import live.stream.theq.theqkit.data.sdk.GameResponse
import live.stream.theq.theqkit.data.sdk.GameState
import live.stream.theq.theqkit.data.sdk.GameState.Companion.GameEvent.QUESTION_ENDED
import live.stream.theq.theqkit.data.sdk.GameState.Companion.GameEvent.QUESTION_RESULT
import live.stream.theq.theqkit.data.sdk.GameState.Companion.GameEvent.QUESTION_STARTED
import live.stream.theq.theqkit.data.sdk.QuestionEndState
import live.stream.theq.theqkit.data.sdk.QuestionResultState
import live.stream.theq.theqkit.data.sdk.Resource
import live.stream.theq.theqkit.events.ChoiceSelectedEvent
import live.stream.theq.theqkit.events.Events
import live.stream.theq.theqkit.repository.LiveGame
import live.stream.theq.theqkit.repository.LiveGameRepository

class GameViewModel internal constructor(private val liveGameRepository: LiveGameRepository) : ViewModel() {

  private var liveGame: LiveGame? = null
  lateinit var gameResponse: GameResponse

  val game = MediatorLiveData<GameState?>()
  val viewerCount = MediatorLiveData<Long?>()
  val choiceSubmission = MediatorLiveData<Resource<ChoiceSubmissionState>?>()
  val selectedChoice = MutableLiveData<ChoiceState?>()

  private var userJustPurchasedSubscription: Boolean = false
  private var subscriptionWindowOpen: Boolean = false
  private var shouldRedeemHeart: Boolean = false

  fun connectToGame(gameResponse: GameResponse) {
    this.gameResponse = gameResponse
    if (liveGame?.gameResponse?.id == gameResponse.id) return

    liveGame?.let {
      game.removeSource(it.game)
      viewerCount.removeSource(it.viewerCount)
      choiceSubmission.removeSource(it.choiceSubmission)
      it.destroy()
    }

    liveGame = liveGameRepository.getLiveGame(gameResponse).also {
      game.addSource(it.game) { value ->
        when (value?.lastEvent) {
          QUESTION_STARTED -> {
            selectedChoice.value = null
            // note: we don't want to flip this back to false until _after_
            // we've completed handing the QUESTION_RESULT following the purchasing
            if (userJustPurchasedSubscription && !subscriptionWindowOpen) {
              userJustPurchasedSubscription = false
            }
          }
          QUESTION_ENDED -> {
            subscriptionWindowOpen = false
            shouldRedeemHeart = false
            selectedChoice.value = (value.currentQuestion as QuestionEndState?)?.let { qEndState ->
              qEndState.serverReceivedSelection?.let {
                qEndState.choices?.firstOrNull { choice -> choice.id == it }
              }
            }
          }
          QUESTION_RESULT -> {
            selectedChoice.value =
              (value.currentQuestion as QuestionResultState?)?.let { qResultState ->
                qResultState.serverReceivedSelection?.let {
                  qResultState.choices.firstOrNull { choice -> choice.id == it }
                }
              }
          }
        }

        game.value = value
      }
      viewerCount.addSource(it.viewerCount) { value -> viewerCount.value = value }
      choiceSubmission.addSource(it.choiceSubmission) { value ->
        value
            ?.takeIf { it.isError() }
            ?.takeIf { it.data?.choice?.questionId == game.value?.currentQuestion?.id }
            ?.let { selectedChoice.value = null }

        choiceSubmission.value = value
      }
    }
  }

  fun submitChoice(response: String) {
    if (response.isNotBlank()) {
      game.value?.currentQuestion?.id?.let { questionId ->
        submitChoice(
            FreeResponseChoiceState(questionId = questionId,
                id = response)
        )
      }
    }
  }

  fun submitChoice(choiceState: ChoiceState) {
    game.value?.let { Events.publish(
        ChoiceSelectedEvent(it, choiceState, shouldRedeemHeart)) }
    selectedChoice.value = choiceState
    liveGame?.submitChoice(choiceState, shouldRedeemHeart)
  }

  fun onSubscriptionWindowStart() {
    subscriptionWindowOpen = true
  }

  fun isSubscriptionWindowStillOpen(): Boolean {
    return subscriptionWindowOpen
  }

  fun onSubscriptionPurchased() {
    userJustPurchasedSubscription = true
  }

  fun didUserSkipQuestionAfterPurchase(): Boolean {
    return userJustPurchasedSubscription && selectedChoice.value == null
  }

  fun useHeartOnNextSubmission() {
    shouldRedeemHeart = true
  }

  override fun onCleared() {
    super.onCleared()
    liveGame?.destroy()
  }
}
