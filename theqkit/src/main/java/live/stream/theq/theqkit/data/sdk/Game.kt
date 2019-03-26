package live.stream.theq.theqkit.data.sdk

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.UUID


data class GameListResponse(val games: List<GameResponse>)

@Parcelize data class GameResponse(
    val id: UUID,
    val streamUrl: String,
    val host: String?,
    val sseHost: String?,
    val reward: Double,
    val locked: Boolean,
    val active: Boolean,
    val scheduled: Long,
    val heartsEnabled: Boolean?,
    val lastQuestionHeartEligible: Int?,
    val theme: Theme?,
    val customRewardText: String?,
    val eligible: Boolean?,
    val notEligibleMessage: String?,
    val subscriberOnly: Boolean?,
    val adCode: String?,
    val gameType: String) : Parcelable

@Parcelize data class Theme(
    val id: UUID,
    val displayName: String,
    val backgroundImageUrl: String?,
    val scheduleBackgroundImageUrl: String?,
    val networkBadgeUrl: String?,
    val textColorCode: String,
    val altTextColorCode: String,
    val defaultColorCode: String) : Parcelable

data class Choice(val id: String, val questionId: UUID, val choice: String)

data class ResponseResult(
    val questionId: UUID,
    val correct: Boolean,
    val response: String,
    val responses: Int,
    val userResponseRatio: Double,
    val choice: String?) // TRIVIA ONLY