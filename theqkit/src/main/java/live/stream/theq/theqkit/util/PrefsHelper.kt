package live.stream.theq.theqkit.util

import android.content.Context
import android.content.SharedPreferences
import live.stream.theq.theqkit.data.sdk.User
import java.math.BigDecimal
import java.util.UUID

/** @suppress **/
class PrefsHelper internal constructor(context: Context, customSharedPreferences: SharedPreferences? = null) {

  private val sharedPreferences =
    customSharedPreferences ?: context.getSharedPreferences(
        DEFAULT_PREFERENCE_FILENAME,
        Context.MODE_PRIVATE)
  private val editor: SharedPreferences.Editor = sharedPreferences.edit()

  fun clear() {
    editor.clear().commit()
  }

  var firstGame: Boolean?
    get() = sharedPreferences.getBoolean(
        FIRST_LEADERBOARD, true)
    set(firstGame) {
      editor.putBoolean(FIRST_LEADERBOARD, firstGame!!)
      editor.commit()
    }

  var bearerToken: String?
    get() = sharedPreferences.getString(
        BEARER_TOKEN, null)
    set(bearerToken) {
      editor.putString(BEARER_TOKEN, bearerToken)
      editor.commit()
    }

  var refreshToken: String?
    get() = sharedPreferences.getString(
        REFRESH_TOKEN, null)
    set(refreshToken) {
      editor.putString(REFRESH_TOKEN, refreshToken)
      editor.commit()
    }

  var fcmToken: String?
    get() = sharedPreferences.getString(
        FCM_TOKEN, null)
    set(fcmToken) {
      editor.putString(FCM_TOKEN, fcmToken)
      editor.commit()
    }

  var userId: String?
    get() = sharedPreferences.getString(USER_ID, null)
    set(userId) {
      editor.putString(USER_ID, userId)
      editor.commit()
    }

  var username: String?
    get() = sharedPreferences.getString(
        USERNAME, null)
    set(username) {
      editor.putString(USERNAME, username)
      editor.commit()
    }

  var email: String?
    get() = sharedPreferences.getString(EMAIL, null)
    set(email) {
      editor.putString(EMAIL, email)
      editor.commit()
    }

  var contactEmail: String?
    get() = sharedPreferences.getString(
        CONTACT_EMAIL, null)
    set(contactEmail) {
      editor.putString(CONTACT_EMAIL, contactEmail)
      editor.commit()
    }

  var marketingOptIn: Boolean
    get() = sharedPreferences.getBoolean(
        MARKETING_OPT_IN, false)
    set(marketingOptIn) {
      editor.putBoolean(MARKETING_OPT_IN, marketingOptIn)
      editor.commit()
    }

  var profilePicUrl: String?
    get() = sharedPreferences.getString(
        PROFILE_PIC_URL, null)
    set(profilePicUrl) {
      editor.putString(PROFILE_PIC_URL, profilePicUrl)
      editor.commit()
    }

  var balance: BigDecimal
    get() = BigDecimal(sharedPreferences.getString(
        BALANCE, "0"))
    set(balance) {
      editor.putString(BALANCE, balance.toPlainString())
      editor.commit()
    }

  var totalGameJoins: Int
    get () = sharedPreferences.getInt(
        TOTAL_GAME_JOINS, 0)
    set (totalGameJoins) {
      editor.putInt(TOTAL_GAME_JOINS, totalGameJoins)
      editor.commit()
    }

  var referralCode: String?
    get() = sharedPreferences.getString(
        REFERRAL_CODE, null)
    set(referralCode) {
      editor.putString(REFERRAL_CODE, referralCode)
      editor.commit()
    }

  var heartPieceCount: Int
    get() = sharedPreferences.getInt(
        HEART_PIECE_COUNT, 0)
    set(heartPieceCount) {
      editor.putInt(HEART_PIECE_COUNT, heartPieceCount)
      editor.commit()
    }

  var lastGame: UUID?
    get() = sharedPreferences.getString(
        LAST_GAME, null)?.let(UUID::fromString)
    set(lastGame) {
      editor.putString(LAST_GAME, lastGame?.toString())
      editor.commit()
    }

  var banned: Boolean
    get() = sharedPreferences.getBoolean(
        USER_BAN, false)
    set(banned) {
      editor.putBoolean(USER_BAN, banned)
      editor.commit()
    }

  var admin: Boolean
    get() = sharedPreferences.getBoolean(ADMIN, false)
    set(admin) {
      editor.putBoolean(ADMIN, admin)
      editor.commit()
    }

  var tester: Boolean
    get() = sharedPreferences.getBoolean(TESTER, false)
    set(tester) {
      editor.putBoolean(TESTER, tester)
      editor.commit()
    }

  var mostRecentNotificationRead: Long
    get() = sharedPreferences.getLong(
        NOTIFICATIONS_LAST_CHECKED, 0)
    set(lastChecked) {
      editor.putLong(NOTIFICATIONS_LAST_CHECKED, lastChecked)
      editor.commit()
    }

  var sendOneTimeRegistration: Boolean
    get() = sharedPreferences.getBoolean(
        ONE_TIME_REGISTRATION, true)
    set(oneTimeRegistration) {
      editor.putBoolean(ONE_TIME_REGISTRATION, oneTimeRegistration)
      editor.commit()
    }

  var firstLoadAfterSignUp: Boolean
    get() = sharedPreferences.getBoolean(
        FIRST_LOAD_AFTER_SIGNUP, false)
    set(firstLoadAfterSignUp) {
      editor.putBoolean(FIRST_LOAD_AFTER_SIGNUP, firstLoadAfterSignUp)
      editor.commit()
    }

  var firstFeedNotification: Boolean
    get() = sharedPreferences.getBoolean(
        FIRST_FEED_NOTIFICATION, true)
    set(firstFeedNotification) {
      editor.putBoolean(FIRST_FEED_NOTIFICATION, firstFeedNotification)
      editor.commit()
    }

  var activeSubscription: Boolean
    get() = sharedPreferences.getBoolean(
        ACTIVE_SUBSCRIPTION, false)
    set(activeSubscription) {
      editor.putBoolean(ACTIVE_SUBSCRIPTION, activeSubscription)
      editor.commit()
    }

  fun subtractFullHeart() {
    heartPieceCount = Math.max(0, heartPieceCount - 4)
  }

  fun saveUser(user: User) {
    editor.putString(USER_ID, user.id)
    editor.putString(USERNAME, user.username)
    editor.putString(PROFILE_PIC_URL, user.profilePicUrl)
    editor.putString(EMAIL, user.email ?: user.contactEmail)
    editor.putString(EMAIL, user.contactEmail)
    editor.putBoolean(
        MARKETING_OPT_IN, user.marketingOptIn ?: false)
    editor.putString(BALANCE, user.balance.toPlainString())
    editor.putString(REFERRAL_CODE, user.referralCode)
    editor.putInt(HEART_PIECE_COUNT, user.heartPieceCount)
    editor.putBoolean(ADMIN, user.admin ?: false)
    editor.putBoolean(TESTER, user.tester ?: false)
    editor.putBoolean(ONE_TIME_REGISTRATION, false)
    editor.putBoolean(ACTIVE_SUBSCRIPTION, user.activeSubscription ?: false)
    editor.commit()
  }

  fun saveOauth(bearerToken: String, refreshToken: String) {
    editor.putString(BEARER_TOKEN, bearerToken)
    editor.putString(REFRESH_TOKEN, refreshToken)
    editor.commit()
  }

  // note: there's a corner case where some users had the bearer token set but _not_ the
  // userId. unclear how this happens, but checking against both properties for authentication
  // checks should help remedy that situation
  fun isUserAuthenticated() = !bearerToken.isNullOrEmpty() && !userId.isNullOrEmpty()

  companion object {
    private const val DEFAULT_PREFERENCE_FILENAME = "live.stream.theq.sdk"

    private const val BEARER_TOKEN = "bearerToken"
    private const val REFRESH_TOKEN = "refreshToken"
    private const val FCM_TOKEN = "fcmToken"
    private const val USER_ID = "userId"
    private const val USERNAME = "username"
    private const val EMAIL = "email"
    private const val CONTACT_EMAIL = "contactEmail"
    private const val MARKETING_OPT_IN = "marketingOptIn"
    private const val PROFILE_PIC_URL = "profilePicUrl"
    private const val BALANCE = "balance_string"
    private const val REFERRAL_CODE = "referral_code"
    private const val HEART_PIECE_COUNT = "heart_piece_count"
    private const val LAST_GAME = "lastGame"
    private const val FIRST_LEADERBOARD = "firstLeaderboard"
    private const val ADMIN = "admin"
    private const val TESTER = "tester"
    private const val USER_BAN = "userBanned"
    private const val NOTIFICATIONS_LAST_CHECKED = "notificationsLastChecked"
    private const val ONE_TIME_REGISTRATION = "oneTimeRegistration"
    private const val FIRST_LOAD_AFTER_SIGNUP = "firstLoadAfterSignUp"
    private const val FIRST_FEED_NOTIFICATION = "firstFeedNotification"
    private const val ACTIVE_SUBSCRIPTION = "activeSubscription"
    private const val TOTAL_GAME_JOINS = "totalGameJoins"
  }

}