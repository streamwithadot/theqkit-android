package live.stream.theq.theqkit

import android.content.Context
import android.content.Intent
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.Observable
import live.stream.theq.theqkit.data.sdk.AccountKitLogin
import live.stream.theq.theqkit.data.sdk.FirebaseLogin
import live.stream.theq.theqkit.data.sdk.GameResponse
import live.stream.theq.theqkit.di.repositoryModule
import live.stream.theq.theqkit.di.theQKitModule
import live.stream.theq.theqkit.di.viewModelModule
import live.stream.theq.theqkit.events.Event
import live.stream.theq.theqkit.events.Events
import live.stream.theq.theqkit.exception.QKitInitializationException
import live.stream.theq.theqkit.http.RestClient
import live.stream.theq.theqkit.listener.GameResponseListener
import live.stream.theq.theqkit.listener.LoginResponseListener
import live.stream.theq.theqkit.repository.GameRepository
import live.stream.theq.theqkit.repository.UserRepository
import live.stream.theq.theqkit.ui.cashout.CashoutDialogFragment
import live.stream.theq.theqkit.ui.game.SDKGameActivity
import live.stream.theq.theqkit.ui.login.LoginDialogFragment
import live.stream.theq.theqkit.util.PrefsHelper
import org.koin.standalone.StandAloneContext.loadKoinModules

class TheQKit {
  private var initialized = false
  internal lateinit var config: TheQConfig
  internal lateinit var prefsHelper: PrefsHelper
  internal lateinit var restClient: RestClient
  private lateinit var gameRepository: GameRepository
  internal lateinit var userRepository: UserRepository

  @Keep
  fun init(config: TheQConfig) {
    if (initialized) {
      throw QKitInitializationException(
          "TheQKit.internalInit(config) can only be called once.")
    }
    this.config = config
    prefsHelper =
      PrefsHelper(config.appContext, config.sharedPreferences)
    restClient = RestClient.getInstance(config.baseUrl, prefsHelper, config.debug)
    gameRepository =
      GameRepository(restClient, config, prefsHelper)
    userRepository = UserRepository(restClient, prefsHelper)
    loadKoinModules(repositoryModule,
        theQKitModule, viewModelModule)
    initialized = true
  }

  /**
   * Login with AccountKit
   *
   * This method handles both new and current users.
   *
   * If the AccountKit user already exists on TheQ, we will log the user in.
   *
   * If the AccountKit user did not already exist, we will attempt to create one with the
   * [suggestedUsername] if passed. If no [suggestedUsername] was passed, we will open a dialog
   * asking the user to select a username.
   *
   * @param accountKitId AccountKit id
   * @param accountKitAccessToken AccountKit access token
   * @param suggestedUsername Username for the new user. If a user with this username already
   * exists, we will auto-increment a number at the end of the suggested username until a unique
   * username is found. Once the user confirms a username, the account will be created and the user
   * logged in.
   * @param listener to handle response
   */
  @Keep
  @JvmOverloads
  fun loginWithAccountKit(
    activity: AppCompatActivity,
    accountKitId: String,
    accountKitAccessToken: String,
    suggestedUsername: String? = null,
    listener: LoginResponseListener
  ) {
    throwIfNotInitialized()
    if (isAuthenticated()) {
      listener.onSuccess()
      return
    }
    val accountKitLogin = AccountKitLogin(accountKitId, accountKitAccessToken)
    LoginDialogFragment.newInstance(accountKitLogin = accountKitLogin,
        suggestedUsername = suggestedUsername, listener = listener)
        .show(activity.supportFragmentManager, LoginDialogFragment::class.java.name)
  }

  /**
   * Login with Firebase
   *
   * This method handles both new and current users.
   *
   * If the Firebase user already exists on TheQ, we will log the user in.
   *
   * If the Firebase user did not already exist, we will attempt to create one with the
   * [suggestedUsername] if passed. If no [suggestedUsername] was passed, we will open a dialog
   * asking the user to select a username.
   *
   * @param firebaseId Firebase id
   * @param firebaseAccessToken Firebase access token
   * @param suggestedUsername Username for the new user. If a user with this username already
   * exists, we will auto-increment a number at the end of the suggested username until a unique
   * username is found. Once the user confirms a username, the account will be created and the user
   * logged in.
   * @param listener to handle response
   */
  @Keep
  @JvmOverloads
  fun loginWithFirebase(
    activity: AppCompatActivity,
    firebaseId: String,
    firebaseAccessToken: String,
    suggestedUsername: String? = null,
    listener: LoginResponseListener
  ) {
    throwIfNotInitialized()
    if (isAuthenticated()) {
      listener.onSuccess()
      return
    }
    val firebaseLogin = FirebaseLogin(firebaseId, firebaseAccessToken)
    LoginDialogFragment.newInstance(firebaseLogin = firebaseLogin,
        suggestedUsername = suggestedUsername, listener = listener)
        .show(activity.supportFragmentManager, LoginDialogFragment::class.java.name)
  }

  /**
   * Logout of TheQ
   *
   * This attempts to delete user token from TheQ servers and clears TheQ shared preferences stored on device.
   * Note: You must manage AccountKit logout separately.
   */
  @Keep
  fun logout() {
    throwIfNotInitialized()
    userRepository.logout()
  }

  /**
   *  Fetch list of games
   *
   *  @param listener to handle response.
   */
  @Keep
  fun fetchGames(listener: GameResponseListener) {
    throwIfNotInitialized()
    gameRepository.fetchGames(listener)
  }

  /**
   * Launch game screen
   *
   *
   *
   * @param context to launch the game intent (typically activity context here).
   * @param game to play.
   */
  @Keep
  fun launchGameActivity(context: Context, game: GameResponse) {
    throwIfNotInitialized()
    val intent = Intent(context, SDKGameActivity::class.java)
    intent.putExtra(SDKGameActivity.KEY_GAME, game)
    context.startActivity(intent)
  }

  /**
   * Show cash out dialog
   *
   * The dialog is launched as a DialogFragment.
   * As a result, we need an instance of the current [AppCompatActivity] in order to show the dialog on top.
   *
   * @param activity the current [AppCompatActivity] needed to launch the cash out dialog.
   */
  @Keep
  fun launchCashoutDialog(activity: AppCompatActivity) {
    throwIfNotInitialized()
    CashoutDialogFragment.newInstance()
        .show(activity.supportFragmentManager, CashoutDialogFragment::class.java.name)
    // TODO Implement
  }

  /**
   * Authentication check
   *
   * @return true if logged in
   */
  @Keep
  fun isAuthenticated(): Boolean {
    return prefsHelper.isUserAuthenticated()
  }

  /**
   * Get an Observable stream of [Event] objects.
   *
   * This allows partner implementations to react to various key events that may occur during
   * at various points during, or surrounding, a live game.
   */
  @Keep
  fun getEventStream(): Observable<Event> {
    throwIfNotInitialized()
    return Events.getEventStream(isPartnerImplementation = !config.partnerCode.isNullOrEmpty())
  }


  private fun throwIfNotInitialized() {
    if (!initialized) {
      throw QKitInitializationException()
    }
  }

  /**
   * Internal only
   *
   * Do not keep. stripped out by r8
   *
   * @suppress
   */
  fun getRestClient(): RestClient {
    throwIfNotInitialized()
    return restClient
  }

  /**
   * Internal only
   *
   * Do not keep. stripped out by r8
   *
   * @suppress
   */
  fun getPrefsHelper(): PrefsHelper {
    throwIfNotInitialized()
    return prefsHelper
  }

  companion object {

    internal const val TAG = "TheQKit"

    @Volatile private var INSTANCE: TheQKit? = null

    @Keep
    @JvmStatic fun getInstance(): TheQKit =
      INSTANCE ?: synchronized(this) {
        INSTANCE
            ?: TheQKit().also { INSTANCE = it }
      }
  }
}