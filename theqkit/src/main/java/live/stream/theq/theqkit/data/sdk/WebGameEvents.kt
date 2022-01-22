package live.stream.theq.theqkit.data.sdk

data class WebGameEndedEvent(val success: Boolean, val type: String, val data: WebGameEndedData) {

  fun isGameEndedEvent() = success && type == "GAME_ENDED"
  fun isWinner() = data.won
  fun winnerCount() = data.winnerCount
  fun reward() = data.reward
}

data class WebGameEndedData(
    val won: Boolean,
    val winnerCount: Int,
    val reward: Double
)
