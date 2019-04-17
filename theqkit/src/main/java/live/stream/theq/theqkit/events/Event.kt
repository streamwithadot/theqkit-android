package live.stream.theq.theqkit.events

import live.stream.theq.theqkit.data.sdk.ChoiceState
import live.stream.theq.theqkit.data.sdk.GameState
import live.stream.theq.theqkit.data.sdk.QuestionResultState
import java.math.BigDecimal
import java.util.UUID

sealed class Event(internal val sdkVisible: Boolean = false)

data class UserBannedEvent internal constructor(
  val wasPreviouslyBanned: Boolean
) : Event(sdkVisible = true)

data class ChoiceSelectedEvent internal constructor(
  val game: GameState,
  val choice: ChoiceState,
  val usedHeart: Boolean
) : Event(sdkVisible = true)

data class CorrectSubmissionEvent internal constructor(
  val game: GameState,
  val question: QuestionResultState
) : Event(sdkVisible = true)

data class ErrorSubmissionEvent internal constructor(
  val game: GameState,
  val choice: ChoiceState,
  val errorCode: String
) : Event(sdkVisible = true)

data class IncorrectSubmissionEvent internal constructor(
  val game: GameState,
  val question: QuestionResultState
) : Event(sdkVisible = true)

/** @suppress **/
data class LagRestartEvent internal constructor(
  val gameId: UUID,
  val isFastConnection: Boolean
) : Event()

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
