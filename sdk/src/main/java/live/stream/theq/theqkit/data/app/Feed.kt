package live.stream.theq.theqkit.data.app

import java.math.BigDecimal
import java.util.UUID

data class FeedItem(
  val id: UUID,
  val userId: UUID,
  val activityType: String,
  val activitySubtype: String? = null,
  val amount: BigDecimal? = null,
  val customField: String? = null,
  val totalAmount: BigDecimal? = null,
  val childCount: Int,
  val grouped: Boolean,
  val created: Long,
  val updated: Long) {

  fun isRecognized(): Boolean {
    return when (activityType) {
      CASHOUT_COMPLETE,
      CASHOUT_REQUEST,
      DEVICE_WELCOME,
      GAME_WON -> true
      HEART_PIECE_EARNED -> {
        when(activitySubtype) {
          AD_COLONY_AD_WATCHED,
          USER_REFERRAL -> true
          else -> false
        }
      }
      else -> false
    }
  }

  companion object {
    // activity types
    const val CASHOUT_COMPLETE = "CASHOUT_COMPLETE"
    const val CASHOUT_REQUEST = "CASHOUT_REQUEST"
    const val DEVICE_WELCOME = "DEVICE_WELCOME" // generated device-side
    const val GAME_WON = "GAME_WON"
    const val HEART_PIECE_EARNED = "HEART_PIECE_EARNED"
    // activity subtypes
    const val AD_COLONY_AD_WATCHED = "AD_COLONY_AD_WATCHED"
    const val USER_REFERRAL = "USER_REFERRAL"

    fun createWelcomeItem(userId: String): FeedItem {
      val now = System.currentTimeMillis()

      return FeedItem(
          id = UUID.randomUUID(),
          userId = UUID.fromString(userId),
          activityType = DEVICE_WELCOME,
          childCount = 0,
          grouped = false,
          created = now,
          updated = now
      )
    }
  }

}

data class FeedResponse(val items: List<FeedItem>)
