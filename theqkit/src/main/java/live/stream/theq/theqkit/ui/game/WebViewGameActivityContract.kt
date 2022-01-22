package live.stream.theq.theqkit.ui.game

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import live.stream.theq.theqkit.data.sdk.GameResponse
import live.stream.theq.theqkit.data.sdk.GameResult

class WebViewGameActivityContract : ActivityResultContract<GameResponse, GameResult>() {

    override fun createIntent(context: Context, game: GameResponse?): Intent {
        return Intent(context, WebViewGameActivity::class.java)
            .putExtra(SDKGameActivity.KEY_GAME, game)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): GameResult? {
        if (resultCode == WebViewGameActivity.RESULT_CODE) {
            val ended = intent?.getBooleanExtra(WebViewGameActivity.KEY_GAME_ENDED, false) ?: false
            val won = intent?.getBooleanExtra(WebViewGameActivity.KEY_GAME_WINNER, false) ?: false
            val winnerCount = intent?.getIntExtra(WebViewGameActivity.KEY_GAME_WINNER_COUNT, 0) ?: 0
            val reward = intent?.getDoubleExtra(WebViewGameActivity.KEY_GAME_REWARD, 0.0) ?: 0.0
            return GameResult(ended, won, winnerCount, reward)
        } else {
            return null
        }
    }
}
