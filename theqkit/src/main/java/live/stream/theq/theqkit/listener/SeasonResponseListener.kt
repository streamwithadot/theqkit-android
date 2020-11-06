package live.stream.theq.theqkit.listener

import androidx.annotation.Keep
import live.stream.theq.theqkit.data.app.SeasonResponse
import live.stream.theq.theqkit.data.sdk.ApiError
import live.stream.theq.theqkit.data.sdk.GameResponse

/**
 * A listener interface for receiving the result of a [live.stream.theq.theqkit.TheQKit.fetchSeason] request.
 *
 * On success, a [SeasonResponse] objects will be provided to the [onSuccess]
 * callback. If an error is encountered, the [onFailure] callback will be provided with an
 * [ApiError](ApiError).
 */
@Keep
interface SeasonResponseListener {
  fun onSuccess(seasonResponse: SeasonResponse)
  fun onFailure(apiError: ApiError)
}
