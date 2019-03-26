package live.stream.theq.theqkit.listener

import androidx.annotation.Keep
import live.stream.theq.theqkit.data.sdk.ApiError
import live.stream.theq.theqkit.data.sdk.GameResponse

@Keep
interface GameResponseListener {
  fun onSuccess(games: List<GameResponse>)
  fun onFailure(apiError: ApiError)
}