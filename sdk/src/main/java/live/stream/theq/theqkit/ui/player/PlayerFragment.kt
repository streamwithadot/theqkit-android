package live.stream.theq.theqkit.ui.player

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import androidx.fragment.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.EventListener
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource.MediaPeriodId
import com.google.android.exoplayer2.source.MediaSourceEventListener
import com.google.android.exoplayer2.source.MediaSourceEventListener.LoadEventInfo
import com.google.android.exoplayer2.source.MediaSourceEventListener.MediaLoadData
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.theq_sdk_fragment_game_player.loading
import kotlinx.android.synthetic.main.theq_sdk_fragment_game_player.playerView
import kotlinx.android.synthetic.main.theq_sdk_fragment_game_player.videoErrorMessage
import live.stream.theq.theqkit.R
import live.stream.theq.theqkit.events.Events
import live.stream.theq.theqkit.events.LagRestartEvent
import live.stream.theq.theqkit.player.LowLatencyLoadControl
import live.stream.theq.theqkit.util.Connectivity
import java.io.IOException
import java.util.UUID

@Keep
internal class PlayerFragment : Fragment(), EventListener, MediaSourceEventListener {

  private var gameId: UUID? = null
  private var rtmpUri: Uri? = null

  private var playbackStarted = false
  private var lagDelay = 0L
  private var lastBufferTime: Long? = null
  private var scheduledRestartTime: Long? = null

  private val mainHandler = Handler()
  private val restartPlayerHandler = Handler()

  var player: SimpleExoPlayer? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.theq_sdk_fragment_game_player, container, false)
  }

  override fun onResume() {
    super.onResume()
    Log.d(TAG, "onResume()")
    initializePlayer()
  }

  override fun onPause() {
    super.onPause()
    Log.d(TAG, "onPause()")
    releasePlayer()
  }

  override fun onStop() {
    super.onStop()
    Log.d(TAG, "onPause()")
    releasePlayer()
  }

  private fun initializePlayer() {
    if (player != null) return

    Log.d(TAG, "Initializing player")

    player = ExoPlayerFactory.newSimpleInstance(
        context,
        DefaultRenderersFactory(context).apply {
          setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        },
        DefaultTrackSelector(AdaptiveTrackSelection.Factory()),
        LowLatencyLoadControl()
    ).apply {
      addListener(this@PlayerFragment)
      playerView.player = this
      playWhenReady = true
    }

    play()
  }

  fun configure(rtmpUri: Uri, gameId: UUID) {
    this.rtmpUri = rtmpUri
    this.gameId = gameId
    play()
  }

  private fun play() {
    rtmpUri?.let {
      Log.d(TAG, "play()")
      scheduledRestartTime = null
      playbackStarted = false
      lagDelay = 0L
      lastBufferTime = null
      playerView.visibility = View.VISIBLE
      player?.repeatMode = Player.REPEAT_MODE_OFF

      val mediaSource = ExtractorMediaSource.Factory(
          DefaultDataSourceFactory(
              context,
              Util.getUserAgent(context, "TheQKit Live Player")
          )
      ).createMediaSource(rtmpUri)

      mediaSource.addEventListener(mainHandler, this)

      player?.prepare(mediaSource)
    } ?: run { playerView.visibility = View.GONE }
  }


  private fun cancelRestart() {
    Log.d(TAG, "Canceling player restart")
    scheduledRestartTime = null
    restartPlayerHandler.removeCallbacksAndMessages(null)
  }

  private fun scheduleRestart(delay: Long, reason: String) {
    val runAtTime = SystemClock.uptimeMillis() + delay

    scheduledRestartTime?.let {
      // bail out of we have a restart scheduled for earlier
      if (it > runAtTime) cancelRestart() else return
    }

    scheduledRestartTime = runAtTime

    Log.d(TAG, "Scheduling player restart; Reason: $reason; Delay: ${delay}ms")

    restartPlayerHandler.postAtTime({
      Log.d(TAG, "Player restarting now; Reason: $reason")
      play()
    }, runAtTime)
  }

  private fun releasePlayer() {
    Log.d(TAG, "Releasing player")
    cancelRestart()
    player?.release()
    player = null
  }


  override fun onPlayerError(error: ExoPlaybackException?) {
    loading.visibility = View.GONE
  }

  override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
    when (playbackState) {
      Player.STATE_BUFFERING -> {
        if (playbackStarted) {
          lastBufferTime = System.currentTimeMillis()
          scheduleRestart(MAX_LAG - lagDelay, "PLAYER BUFFERING")
        }
      }
      Player.STATE_READY -> {
        playbackStarted = true
        lastBufferTime?.let {
          lagDelay += System.currentTimeMillis() - it
          Log.d(TAG, "Player recovered after buffer; Lag: ${lagDelay}ms")
        }
        lastBufferTime = null
        videoErrorMessage.visibility = View.GONE
        loading.visibility = View.GONE
        cancelRestart()

        if (lagDelay > MAX_LAG) {
          context?.let { context -> gameId?.let { gameId ->
            Events.publish(
                LagRestartEvent(gameId,
                    Connectivity.isConnectedFast(context)))
          } }
          scheduleRestart(0L, "LAG EXCEEDED MAX DELAY")
        }
      }
      Player.STATE_ENDED -> {
        // unclear why, but we often hit this state during network issues,
        // without ever hitting STATE_BUFFERING.
        scheduleRestart(1000L, "PLAYER ENDED")
      }
    }
  }

  override fun onLoadingChanged(isLoading: Boolean) {
    loading.visibility = if (isLoading) View.VISIBLE else View.GONE
  }

  override fun onLoadError(
    windowIndex: Int,
    mediaPeriodId: MediaPeriodId?,
    loadEventInfo: LoadEventInfo?,
    mediaLoadData: MediaLoadData?,
    error: IOException?,
    wasCanceled: Boolean
  ) {
    loading.visibility = View.GONE
    videoErrorMessage.visibility = View.VISIBLE
    scheduleRestart(1000L, "LOAD ERROR")
  }

  override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {}
  override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {}
  override fun onDownstreamFormatChanged(windowIndex: Int, mediaPeriodId: MediaPeriodId?, mediaLoadData: MediaLoadData?) {}
  override fun onLoadCanceled(windowIndex: Int, mediaPeriodId: MediaPeriodId?, loadEventInfo: LoadEventInfo?, mediaLoadData: MediaLoadData?) {}
  override fun onLoadCompleted(windowIndex: Int, mediaPeriodId: MediaPeriodId?, loadEventInfo: LoadEventInfo?, mediaLoadData: MediaLoadData?) {}
  override fun onLoadStarted(windowIndex: Int, mediaPeriodId: MediaPeriodId?, loadEventInfo: LoadEventInfo?, mediaLoadData: MediaLoadData?) {}
  override fun onMediaPeriodCreated(windowIndex: Int, mediaPeriodId: MediaPeriodId?) {}
  override fun onMediaPeriodReleased(windowIndex: Int, mediaPeriodId: MediaPeriodId?) {}
  override fun onReadingStarted(windowIndex: Int, mediaPeriodId: MediaPeriodId?) {}
  override fun onUpstreamDiscarded(windowIndex: Int, mediaPeriodId: MediaPeriodId?, mediaLoadData: MediaLoadData?) {}

  companion object {

    private const val MAX_LAG = 2000L
    private const val TAG = "PlayerFragment"

  }

}
