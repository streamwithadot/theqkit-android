package live.stream.theq.theqkit

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.Keep
import live.stream.theq.theqkit.exception.QKitInitializationException

/**
 * Configuration for [TheQKit]
 *
 * Use [TheQConfig.Builder] to create your configuration.
 * Pass [TheQConfig] to [TheQKit.init] once created.
 */
class TheQConfig private constructor(
  internal val appContext: Context,
  internal val baseUrl: String,
  internal val partnerCode: String?,
  internal val sharedPreferences: SharedPreferences? = null,
  internal val debug: Boolean
) {

  /**
   * Builder for [TheQConfig]
   *
   * Use this builder to create your [TheQConfig] configuration.
   */
  class Builder @Keep constructor(private val appContext: Application) {
    private var baseUrl: String? = null
    private var partnerCode: String? = null
    private var debug: Boolean = false
    private var sharedPreferences: SharedPreferences? = null
    private var requirePartnerCode: Boolean = true

    /**
     * Provide The Q API endpoint for TheQKit
     *
     * @param baseUrl of The Q API
     * @return instance of [Builder]
     */
    @Keep
    fun baseUrl(baseUrl: String): Builder {
      return apply { this.baseUrl = baseUrl }
    }

    /**
     * Provide partner code to TheQKit
     *
     * @param partnerCode for application
     * @return instance of [Builder]
     */
    @Keep
    fun partnerCode(partnerCode: String): Builder {
      return apply { this.partnerCode = partnerCode }
    }

    /**
     * Debugging toggle
     *
     * Allows debugging the SDK. Only enable in development mode!
     *
     * @param debug mode enabled if true
     * @return instance of [Builder]
     */
    @Keep
    fun debuggable(debug: Boolean): Builder {
      return apply { this.debug = debug }
    }

    /**
     * Internal only
     *
     * Do not keep. stripped out by r8
     *
     * @suppress
     */
    @Suppress
    fun sharedPreferences(sharedPreferences: SharedPreferences): Builder {
      return apply { this.sharedPreferences = sharedPreferences }
    }

    /**
     * Internal only
     *
     * Do not keep. stripped out by r8
     *
     * @suppress
     */
    fun requirePartnerCode(require: Boolean): Builder {
      return apply { this.requirePartnerCode = require }
    }

    /**
     * Build TheQConfig
     *
     * Call this method after providing all required values to [TheQConfig.Builder],
     * then pass this returned value to [TheQKit.init]
     *
     * @return instance of [TheQConfig]
     */
    @Keep
    fun build(): TheQConfig {
      val baseUrl = this.baseUrl
      val partnerCode = this.partnerCode

      if (baseUrl.isNullOrBlank()) {
        throw QKitInitializationException("API base url must be set before building TheQConfig.")
      }

      if (requirePartnerCode && partnerCode.isNullOrBlank()) {
        throw QKitInitializationException("Partner Code must be set before building TheQConfig.")
      }

      return TheQConfig(
          appContext = appContext,
          baseUrl = baseUrl,
          partnerCode = partnerCode,
          sharedPreferences = sharedPreferences,
          debug = debug)
    }
  }
}