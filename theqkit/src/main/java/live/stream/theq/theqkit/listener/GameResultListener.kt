package live.stream.theq.theqkit.listener

import androidx.annotation.Keep
import live.stream.theq.theqkit.data.sdk.GameResult

/**
 * A listener interface for receiving the result of a Web Game.
 *
 * On success, a  [GameResult](GameResult) objectswill be provided to the [onSuccess]
 * callback.
 */
@Keep
interface GameResultListener {
    fun onSuccess(result: GameResult)
}
