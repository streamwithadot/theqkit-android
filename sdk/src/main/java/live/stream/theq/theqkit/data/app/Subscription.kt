package live.stream.theq.theqkit.data.app

import java.util.UUID

data class Subscription(
  val id: UUID,
  val userId: UUID,
  val active: Boolean,
  val lastRenewalDate: Long?,
  val storeType: String,   // ios,android
  val productIdentifier: String,
  val heartPieceValue: Int,
  val created: Long?)

data class SubscriptionCreatePayload(
  val storeType: String = "android",
  val productIdentifier: String,
  val receiptIdentifier: String)


data class SubscriptionResponse(val success: Boolean, val active: Boolean, val heartPieceValue: Int)

data class CreateSubscriptionResponse(
  val success: Boolean,
  val subscription: Subscription?,
  val trialPeriod: Boolean,
  val canRedeemHeart: Boolean?
)
