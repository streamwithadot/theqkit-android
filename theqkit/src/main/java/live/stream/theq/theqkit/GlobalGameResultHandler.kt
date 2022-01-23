package live.stream.theq.theqkit

import live.stream.theq.theqkit.data.sdk.GameResult
import live.stream.theq.theqkit.listener.GameResultListener

class GlobalGameResultHandler {
    companion object {
        var listener: GameResultListener? = null

        fun addListener(listener: GameResultListener) {
            this.listener = listener;
        }

        fun handleResult(result: GameResult) {
            listener?.onSuccess(result)
            listener = null
        }
    }
}
