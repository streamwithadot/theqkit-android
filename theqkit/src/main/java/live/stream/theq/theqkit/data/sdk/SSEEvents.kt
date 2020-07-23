package live.stream.theq.theqkit.data.sdk

import java.math.BigDecimal
import java.util.UUID

internal data class GameStatus(
  val id: Long,
  val active: Boolean,
  val heartEligible: Boolean,
  val question: QuestionPayload?,
  val score: Int?
)

internal data class GameWon(
  val id: Long,
  val userId: UUID,
  val amount: BigDecimal
)

internal data class QuestionPayload(
  val id: UUID,
  val questionType: String,
  val categoryId: UUID?,
  val question: String,
  val choices: List<Choice>?,
  val secondsToRespond: Long,
  val number: Int,
  val total: Int,
  val pointValue: Int?
)

//internal data class GameWarn(
//  val id: Long,
//  val gameId: UUID,
//  val duration: Long
//)

internal data class GameEnded(
  val id: Long,
  val gameId: UUID
)

internal data class GameWinners(
  val id: Long,
  val gameId: UUID,
  val winnerCount: Int,
  val winners: List<GameWinner>
)

internal data class GameWinner(
  val user: String,
  val pic: String?
)

internal data class QuestionStart(
  val id: Long,
  val questionType: String,
  val gameId: UUID,
  val questionId: UUID,
  val categoryId: UUID?,
  val question: String,
  val choices: List<Choice>?,
  val secondsToRespond: Long,
  val number: Int,
  val total: Int,
  val pointValue: Int?
)

internal data class QuestionEnd(
  val id: Long,
  val gameId: UUID,
  val questionId: UUID,
  val selection: String?
)

internal data class QuestionResult(
  val id: Long,
  val questionType: String,
  val gameId: UUID,
  val questionId: UUID,
  val categoryId: UUID?,
  val answerId: String?,
  val correctResponse: String?,
  private val choices: List<ResponseResult>?,
  private val results: List<ResponseResult>?,
  val active: Boolean,
  val selection: String?,
  val canRedeemHeart: Boolean,
  val canUseSubscription: Boolean = false,
  val pointValue: Int?,
  val score: Int?
) {
  fun getResultList(): List<ResponseResult> {
    return when(questionType) {
      Question.TYPE_TRIVIA -> choices
      Question.TYPE_POPULAR -> results
      Question.TYPE_TEXT_SURVEY -> results
      Question.TYPE_CHOICE_SURVEY -> choices
      else -> emptyList()
    } ?: emptyList()
  }
}

internal data class ViewCountUpdate(
  val id: Long,
  val gameId: UUID,
  val viewCnt: Int,
  val uniqueCnt: Int
)

/** @suppress **/
class Question {
  companion object {
    const val TYPE_TRIVIA = "TRIVIA"
    const val TYPE_POPULAR = "POPULAR"
    const val TYPE_TEXT_SURVEY = "TEXT_SURVEY"
    const val TYPE_CHOICE_SURVEY = "CHOICE_SURVEY"
  }
}
