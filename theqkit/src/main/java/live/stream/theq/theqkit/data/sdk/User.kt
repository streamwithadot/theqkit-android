package live.stream.theq.theqkit.data.sdk

import java.math.BigDecimal

/** @suppress **/
data class User(
    val id: String,
    val username: String,
    val profilePicUrl: String?,
    val email: String?,
    val contactEmail: String?,
    val marketingOptIn: Boolean?,
    val balance: BigDecimal,
    val referralCode: String,
    val heartPieceCount: Int,
    val admin: Boolean?,
    val tester: Boolean?,
    val activeSubscription: Boolean?
)

/** @suppress **/
data class UserResponse(val user: User)




