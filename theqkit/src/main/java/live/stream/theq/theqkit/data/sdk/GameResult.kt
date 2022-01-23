package live.stream.theq.theqkit.data.sdk

/** @suppress **/
data class GameResult(
    val ended: Boolean,
    val won: Boolean,
    val winnersCount: Int,
    val reward: Double)
