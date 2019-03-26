package live.stream.theq.theqkit.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import live.stream.theq.theqkit.TheQConfig
import live.stream.theq.theqkit.data.sdk.ApiError
import live.stream.theq.theqkit.data.sdk.GameResponse
import live.stream.theq.theqkit.http.RestClient
import live.stream.theq.theqkit.listener.GameResponseListener
import live.stream.theq.theqkit.util.PrefsHelper
import java.lang.Exception
import java.util.concurrent.atomic.AtomicBoolean

internal class GameRepository(
  restClient: RestClient,
  private val config: TheQConfig,
  private val prefsHelper: PrefsHelper
) : CoroutineScope {
  private val apiService = restClient.apiService

  private val job = Job()
  override val coroutineContext = job + Dispatchers.Main

  private val requestInProgress = AtomicBoolean()
  private var lastSuccessfulResponse: LastSuccessfulResponse? = null

  internal fun fetchGames(listener: GameResponseListener) {
    if (requestInProgress.get()) {
      launch {
        listener.onFailure(
            ApiError("REQUEST_IN_PROGRESS",
                "There is already an existing request in progress."))
      }
      return
    }

    requestInProgress.set(true)

    launch {
      val cachedResponse = lastSuccessfulResponse
      if (cachedResponse != null && cachedResponse.requestedTime > System.currentTimeMillis() - MAX_CACHE_LIMIT_MILLISECONDS) {
        listener.onSuccess(cachedResponse.games)
        requestInProgress.set(false)
        return@launch
      }

      lastSuccessfulResponse = null
      val requestTimestamp = System.currentTimeMillis()
      try {
        val gameListResponse =
          apiService.scheduledGamesAsync(
              partnerCode = config.partnerCode, userId = prefsHelper.userId
          ).await()
        if (gameListResponse.isSuccessful) {
          val games = gameListResponse.body()?.games ?: emptyList()
          lastSuccessfulResponse =
            LastSuccessfulResponse(
                requestTimestamp, games)
          listener.onSuccess(games)
        } else {
          listener.onFailure(ApiError())
        }
      } catch (e: Exception) {
        listener.onFailure(ApiError())
      }
      requestInProgress.set(false)
    }
  }

  private data class LastSuccessfulResponse(
    val requestedTime: Long,
    val games: List<GameResponse>
  )

  companion object {
    private const val MAX_CACHE_LIMIT_MILLISECONDS = 15000
  }
}