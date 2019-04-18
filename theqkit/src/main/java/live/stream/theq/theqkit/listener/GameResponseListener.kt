package live.stream.theq.theqkit.listener

import androidx.annotation.Keep
import live.stream.theq.theqkit.data.sdk.ApiError
import live.stream.theq.theqkit.data.sdk.GameResponse

/**
 * A listener interface for receiving the result of a [live.stream.theq.theqkit.TheQKit.fetchGames] request.
 *
 * On success, a [List] of [GameResponse](GameResponse) objects will be provided to the [onSuccess]
 * callback. If an error is encountered, the [onFailure] callback will be provided with an
 * [ApiError](ApiError).
 */
@Keep
interface GameResponseListener {
  fun onSuccess(games: List<GameResponse>)
  fun onFailure(apiError: ApiError)
}
