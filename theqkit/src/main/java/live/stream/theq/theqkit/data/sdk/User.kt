package live.stream.theq.theqkit.data.sdk

import java.math.BigDecimal

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
    val activeSubscription: Boolean?
)

data class UserResponse(val user: User)




