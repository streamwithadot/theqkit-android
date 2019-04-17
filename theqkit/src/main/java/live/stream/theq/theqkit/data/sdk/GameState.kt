package live.stream.theq.theqkit.data.sdk

import live.stream.theq.theqkit.data.sdk.GameState.Companion.GameEvent.GAME_ENDED
import live.stream.theq.theqkit.data.sdk.GameState.Companion.GameEvent.GAME_WINNERS
import live.stream.theq.theqkit.data.sdk.GameState.Companion.GameEvent.QUESTION_ENDED
import live.stream.theq.theqkit.data.sdk.GameState.Companion.GameEvent.QUESTION_RESULT
import live.stream.theq.theqkit.data.sdk.GameState.Companion.GameEvent.QUESTION_STARTED
import live.stream.theq.theqkit.data.sdk.GameState.Companion.GameEvent.STATUS
import java.util.UUID

/** @suppress **/
data class GameState(
  val id: Long,
  val isUserActive: Boolean,
  val currentQuestion: QuestionState?,
  val currentQuestionNumber: Int?,
  val totalQuestionsCount: Int?,
  val isHeartEligible: Boolean,
  val lastQuestionHeartEligible: Int?,
    // indicates that hearts are enabled for this game
  val heartsEnabled: Boolean?,
  val winners: GameWinnersState? = null,
  val isEnded: Boolean = false,
  val scheduled: Long,
  val theme: Theme?,
  val lastEvent: GameEvent
) {

  internal constructor(gameStatus: GameStatus, game: GameResponse) : this(
      id = gameStatus.id,
      isUserActive = gameStatus.active,
      currentQuestion = gameStatus.question?.let(::QuestionStartState),
      currentQuestionNumber = gameStatus.question?.number,
      totalQuestionsCount = gameStatus.question?.total?.takeIf { it > 0 },
      isHeartEligible = gameStatus.heartEligible,
      lastQuestionHeartEligible = game.lastQuestionHeartEligible,
      heartsEnabled = game.heartsEnabled,
      scheduled = game.scheduled,
      theme = game.theme,
      lastEvent = STATUS
  )

  // note: special case for handling a question payload on the GameStatus event.
  // we use this to rapidly fire off the QuestionStart event in this case.
  internal fun getAsQuestionStart() = this.copy(lastEvent = QUESTION_STARTED)

  internal fun getUpdated(questionStart: QuestionStart) = this.copy(
      currentQuestion = QuestionStartState(questionStart),
      currentQuestionNumber = questionStart.number,
      totalQuestionsCount = questionStart.total.takeIf { it > 0 },
      isHeartEligible = this.isHeartEligible &&
          this.lastQuestionHeartEligible?.let { it >= questionStart.number } ?: true,
      lastEvent = QUESTION_STARTED
  )

  internal fun getUpdated(questionEnd: QuestionEnd) = this.copy(
      currentQuestion = this.currentQuestion
          ?.takeIf { it.id == questionEnd.questionId }
          ?.let { it as QuestionStartState? }
          ?.let { QuestionEndState(questionEnd, it) },
      lastEvent = QUESTION_ENDED
  )

  internal fun getUpdated(questionResult: QuestionResult) = this.copy(
      currentQuestion = QuestionResultState(
          questionResult,
          this.currentQuestion?.takeIf { it.id == questionResult.questionId },
          this.isUserActive // <-- note: we specifically need the previous value here
      ),
      isUserActive = questionResult.active,
      lastEvent = QUESTION_RESULT
  )

  internal fun getUpdated(gameWinners: GameWinners) = this.copy(
      currentQuestion = null,
      winners = GameWinnersState(gameWinners),
      lastEvent = GAME_WINNERS
  )

  internal fun getUpdated(gameEnded: GameEnded) = this.copy(
      currentQuestion = null,
      isEnded = true,
      lastEvent = GAME_ENDED
  )

  companion object {
    enum class GameEvent {
      STATUS,
      QUESTION_STARTED,
      QUESTION_ENDED,
      QUESTION_RESULT,
      GAME_WINNERS,
      GAME_ENDED
    }
  }

}

/** @suppress **/
interface QuestionState {
  val id: UUID
  val categoryId: UUID?
  val questionNumber: Int?
  val questionText: String?
  val choices: List<ChoiceState>?
  val questionType: String

  val isPopularChoice
    get() = questionType == Question.TYPE_POPULAR

  val isTrivia
    get() = questionType == Question.TYPE_TRIVIA
}

internal data class QuestionStartState(
  override val id: UUID,
  override val categoryId: UUID?,
  override val questionNumber: Int,
  override val questionText: String,
  override val choices: List<InitialChoiceState>?,
  override val questionType: String,
  val responseExpiry: Long
) : QuestionState {

  constructor(questionPayload: QuestionPayload) : this(
      id = questionPayload.id,
      questionType = questionPayload.questionType,
      categoryId = questionPayload.categoryId,
      questionNumber = questionPayload.number,
      questionText = questionPayload.question,
      choices = questionPayload.choices?.map(::InitialChoiceState),
      responseExpiry = System.currentTimeMillis() + (questionPayload.secondsToRespond * 1000)
  )

  constructor(questionStart: QuestionStart) : this(
      id = questionStart.questionId,
      questionType = questionStart.questionType,
      categoryId = questionStart.categoryId,
      questionNumber = questionStart.number,
      questionText = questionStart.question,
      choices = questionStart.choices?.map(::InitialChoiceState),
      responseExpiry = System.currentTimeMillis() + (questionStart.secondsToRespond * 1000)
  )

}

internal data class QuestionEndState(
  override val id: UUID,
  override val categoryId: UUID?,
  override val questionNumber: Int,
  override val questionText: String,
  override val choices: List<InitialChoiceState>?,
  override val questionType: String,
  val serverReceivedSelection: String?
) : QuestionState {

  constructor(questionEnd: QuestionEnd, questionStartState: QuestionStartState) : this(
      id = questionEnd.questionId,
      questionType = questionStartState.questionType,
      categoryId = questionStartState.categoryId,
      questionNumber = questionStartState.questionNumber,
      questionText = questionStartState.questionText,
      choices = questionStartState.choices,
      serverReceivedSelection = questionEnd.selection
  )

}

/** @suppress **/
data class QuestionResultState(
  override val id: UUID,
  override val categoryId: UUID?,
  override val questionNumber: Int?,
  override val questionText: String?,
  override val choices: List<ChoiceResultState>,
  override val questionType: String,
  val answerId: String?,
    // indicates that the user can redeem a heart on the next question to get back
    // in the game
  val canRedeemHeart: Boolean,
    // indicates that the user can redeem a heart on the next question if they successfully
    // start a new subscription
  val canUseSubscription: Boolean,
  val serverReceivedSelection: String?,
  val wasUserCorrect: Boolean,
  val userWasEliminatedOnQuestion: Boolean
) : QuestionState {

  internal constructor(
    questionResult: QuestionResult,
    questionState: QuestionState?,
    userPreviouslyActive: Boolean
  ) : this(
      id = questionResult.questionId,
      questionType = questionResult.questionType,
      categoryId = questionResult.categoryId,
      questionNumber = questionState?.questionNumber,
      questionText = questionState?.questionText,
      choices = questionResult.getResultList().let {
        val totalResponseCount = it.sumBy { it.responses }
        it.map { ChoiceResultState(it, totalResponseCount) }
      },
      answerId = questionResult.answerId,
      canRedeemHeart = questionResult.canRedeemHeart,
      canUseSubscription = questionResult.canUseSubscription,
      serverReceivedSelection = questionResult.selection,
      wasUserCorrect = isUserCorrect(
          questionResult
      ),
      userWasEliminatedOnQuestion = userPreviouslyActive && !questionResult.active
  )

  companion object {
    private fun isUserCorrect(questionResult: QuestionResult): Boolean {
      return when(questionResult.questionType) {
        Question.TYPE_TRIVIA -> questionResult.answerId == questionResult.selection
        Question.TYPE_POPULAR -> questionResult.selection != null && questionResult.selection == questionResult.correctResponse
        else -> false
      }
    }
  }
}

/** @suppress **/
interface ChoiceState {
  val id: String?
  val questionId: UUID
  val choiceText: String?
}

/** @suppress **/
data class InitialChoiceState(
  override val id: String,
  override val questionId: UUID,
  override val choiceText: String
) : ChoiceState {

  internal constructor(choice: Choice) : this(
      id = choice.id,
      questionId = choice.questionId,
      choiceText = choice.choice
  )

}

/** @suppress **/
data class ChoiceResultState(
  override val id: String?,
  override val questionId: UUID,
  override val choiceText: String?,
  val isCorrect: Boolean,
  val responseCount: Int,
  val totalResponseCount: Int,
  val userResponseRatio: Double
) : ChoiceState {

  internal constructor(response: ResponseResult, totalResponseCount: Int) : this(
      id = response.response,
      questionId = response.questionId,
      choiceText = response.choice,
      isCorrect = response.correct,
      responseCount = response.responses,
      totalResponseCount = totalResponseCount,
      userResponseRatio = response.userResponseRatio
  )

}

internal data class FreeResponseChoiceState(
  override val id: String,
  override val questionId: UUID,
  override val choiceText: String? = null
) : ChoiceState

/** @suppress **/
data class ChoiceSubmissionState(
  val choice: ChoiceState,
  val response: SubmitAnswerResponse?
)

/** @suppress **/
data class GameWinnersState(val count: Int, val profiles: List<GameWinnerState>) {
  internal constructor(gameWinners: GameWinners) : this(
      count = gameWinners.winnerCount,
      profiles = gameWinners.winners.map(::GameWinnerState)
  )
}

/** @suppress **/
data class GameWinnerState(val username: String, val pic: String?) {
  internal constructor(gameWinner: GameWinner) : this(gameWinner.user, gameWinner.pic)
}
