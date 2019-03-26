package live.stream.theq.theqkit.events

import live.stream.theq.theqkit.data.sdk.ChoiceState
import live.stream.theq.theqkit.data.sdk.GameState
import live.stream.theq.theqkit.data.sdk.QuestionResultState
import java.util.UUID

sealed class Event

data class ChoiceSelectedEvent internal constructor(
  val game: GameState,
  val choice: ChoiceState,
  val usedHeart: Boolean
) : Event()

data class CorrectSubmissionEvent internal constructor(
  val game: GameState,
  val question: QuestionResultState
) : Event()

data class ErrorSubmissionEvent internal constructor(
  val game: GameState,
  val choice: ChoiceState,
  val errorCode: String
) : Event()

data class IncorrectSubmissionEvent internal constructor(
  val game: GameState,
  val question: QuestionResultState
) : Event()

data class LagRestartEvent internal constructor(
  val gameId: UUID,
  val isFastConnection: Boolean
) : Event()

data class ExitGameEvent constructor(
  val gameId: UUID
) : Event()

data class HeartUsedEvent constructor(
  val gameId: UUID
) : Event()

data class HeartNotUsedEvent constructor(
  val gameId: UUID
) : Event()

data class NoSubmissionEvent internal constructor(val game: GameState) : Event()
