package live.stream.theq.theqkit.data.sdk

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import live.stream.theq.theqkit.data.app.Device

@Parcelize data class FacebookLogin(val id: String, val accessToken: String) : Parcelable
@Parcelize data class AccountKitLogin(val id: String, val accessToken: String) : Parcelable
@Parcelize data class FirebaseLogin(val id: String, val accessToken: String): Parcelable

@Parcelize data class LoginAuthData(val facebook: FacebookLogin? = null, val accountKit: AccountKitLogin? = null, val firebase: FirebaseLogin? = null, val device: Device? = null) : Parcelable
@Parcelize data class AuthData(val facebook: FacebookLogin? = null, val accountKit: AccountKitLogin? = null, val firebase: FirebaseLogin? = null) : Parcelable
data class SignupAuthData(
  val authData: AuthData,
  val username: String,
  val email: String? = null,
  val contactEmail: String? = null,
  val marketingOptIn: Boolean = false,
  val profilePicUrl: String? = null,
  val device: Device? = null)
data class AuthResponse(val user: User, val oauth: OauthResponse)
data class OauthResponse(val accessToken: String, val refreshToken: String)