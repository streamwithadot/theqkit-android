package live.stream.theq.theqkit.data.sdk

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Category(
        val id: String,
        val name: String,
        val code: String,
        val description: String,
        val active: Boolean,
        val iconImageUrl:String,
        val backgroundImageUrl:String,
        val backgroundVideoUrl: String,
        val colorCode: String,
        val leaderboard: List<LeaderboardEntry>?,
        val reward: Double
) : Parcelable

@Parcelize
data class LeaderboardEntry(val username: String, val profilePicUrl: String?, val score: Int) : Parcelable

@Parcelize
data class Season(val id: String, val name: String, val startDate: Long, val endDate: Long,val active: Boolean) : Parcelable

data class SeasonResponse(val season: Season, val categories: ArrayList<Category>)
data class SeasonScoreResponse(val scores: List<Score>)
data class Score(val categoryId: String, val score: Int)