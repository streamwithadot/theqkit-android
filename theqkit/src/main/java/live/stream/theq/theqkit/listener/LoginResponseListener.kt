package live.stream.theq.theqkit.listener

import androidx.annotation.Keep
import live.stream.theq.theqkit.data.sdk.ApiError

/**
 * A listener interface for receiving the result of a login flow.
 *
 * On success, the [onSuccess] callback will be invoked. If an error is encountered, the [onFailure]
 * callback will be provided with an [ApiError](ApiError).
 */
@Keep
interface LoginResponseListener {
  fun onSuccess()
  fun onFailure(apiError: ApiError)
}
