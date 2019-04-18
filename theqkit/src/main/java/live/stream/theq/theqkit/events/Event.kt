package live.stream.theq.theqkit.events

import live.stream.theq.theqkit.data.sdk.ChoiceState
import live.stream.theq.theqkit.data.sdk.GameState
import live.stream.theq.theqkit.data.sdk.QuestionResultState
import java.math.BigDecimal
import java.util.UUID

/**
 * Base class for TheQKit events.
 */
sealed class Event(internal val sdkVisible: Boolean = false)

/**
 * Signals that the currently authenticated user has been banned.
 *
 * This event fires in response to network calls when the API returns an error indicating that
 * the user is in a banned state. Consuming applications should discontinue from sending
 * further requests upon encountering this event.
 */
data class UserBannedEvent internal constructor(
  /**
   * If true, indicates that the current device was previously aware of the user's banned state.
   */
  val wasPreviouslyBanned: Boolean
) : Event(sdkVisible = true)

/**
 * Signals that the user has selected a response, and it is about to be submitted to the API server.
 */
data class ChoiceSelectedEvent internal constructor(
  val game: GameState,
  val choice: ChoiceState,
  val usedHeart: Boolean
) : Event(sdkVisible = true)

/**
 * Signals that the user's selected response was correct, and that it was successfully received
 * by the server.
 */
data class CorrectSubmissionEvent internal constructor(
  val game: GameState,
  val question: QuestionResultState
) : Event(sdkVisible = true)

/**
 * Signals that there was an error submitting the user's selected response to the server.
 */
data class ErrorSubmissionEvent internal constructor(
  val game: GameState,
  val choice: ChoiceState,
  val errorCode: String
) : Event(sdkVisible = true)

/**
 * Signals that the server successfully received the user's selected response, but that it was
 * incorrect.
 */
data class IncorrectSubmissionEvent internal constructor(
  val game: GameState,
  val question: QuestionResultState
) : Event(sdkVisible = true)

/** @suppress **/
data class LagRestartEvent internal constructor(
  val gameId: UUID,
  val isFastConnection: Boolean
) : Event()

/**
 * Signals that the current user won the game.
 */
data class GameWonEvent internal constructor(
  val amount: BigDecimal
) : Event(sdkVisible = true)

/** @suppress **/
data class ExitGameEvent internal constructor(
  val gameId: UUID
) : Event()

/** @suppress **/
data class HeartUsedEvent internal constructor(
  val gameId: UUID
) : Event()

/** @suppress **/
data class HeartNotUsedEvent internal constructor(
  val gameId: UUID
) : Event()

/** @suppress **/
data class NoSubmissionEvent internal constructor(val game: GameState) : Event()
