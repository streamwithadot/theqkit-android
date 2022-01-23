package live.stream.theq.theqkit.data.sdk

data class WebGameEndedEvent(val success: Boolean, val type: String, val data: WebGameEndedData) {

  fun isGameEndedEvent() = success && type == "GAME_ENDED"
  fun isWinner() = data.won
  fun winnersCount() = data.winnersCount
  fun reward() = data.reward
}
data class WebGameEndedData(
    val won: Boolean,
    val winnersCount: Int,
    val reward: Double)
