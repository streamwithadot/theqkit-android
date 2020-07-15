package live.stream.theq.theqkit.ui.game

import android.animation.Animator
import android.content.Intent
import androidx.lifecycle.Observer
import android.net.Uri
import android.os.*
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.theqkit_activity_game.*
import live.stream.theq.theqkit.R
import live.stream.theq.theqkit.data.sdk.GameState.Companion.GameEvent.GAME_ENDED
import live.stream.theq.theqkit.data.sdk.GameState.Companion.GameEvent.GAME_WINNERS
import live.stream.theq.theqkit.data.sdk.GameState.Companion.GameEvent.QUESTION_ENDED
import live.stream.theq.theqkit.data.sdk.GameState.Companion.GameEvent.QUESTION_RESULT
import live.stream.theq.theqkit.data.sdk.GameState.Companion.GameEvent.QUESTION_STARTED
import live.stream.theq.theqkit.data.sdk.GameState.Companion.GameEvent.STATUS
import live.stream.theq.theqkit.util.AndroidBug5497Workaround
import live.stream.theq.theqkit.util.DeviceUtil
import live.stream.theq.theqkit.data.sdk.GameResponse
import live.stream.theq.theqkit.data.sdk.GameState
import live.stream.theq.theqkit.data.sdk.QuestionEndState
import live.stream.theq.theqkit.data.sdk.QuestionResultState
import live.stream.theq.theqkit.data.sdk.QuestionStartState
import live.stream.theq.theqkit.events.Events
import live.stream.theq.theqkit.events.ExitGameEvent
import live.stream.theq.theqkit.events.HeartNotUsedEvent
import live.stream.theq.theqkit.events.HeartUsedEvent
import live.stream.theq.theqkit.ui.game.ExtraLifeDialogFragment.ExtraLifeListener
import live.stream.theq.theqkit.ui.player.PlayerFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

@Keep
open class SDKGameActivity : AppCompatActivity(), ExtraLifeListener {

  open val billingSupported = false

  protected val gameViewModel: GameViewModel by viewModel()
  lateinit var gameResponse: GameResponse

  private var winnersDialog: AlertDialog? = null

  private val playerFragment by lazy {
    supportFragmentManager.findFragmentById(R.id.playerFragment) as PlayerFragment
  }

  private lateinit var linearLayoutManager: LinearLayoutManager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    window.setFlags(
        WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN
    )
    setContentView(R.layout.theqkit_activity_game)
    AndroidBug5497Workaround.assistActivity(this)
    loading.visibility = View.VISIBLE

    linearLayoutManager =
        LinearLayoutManager(this, RecyclerView.VERTICAL, false)

    gameResponse = intent.extras?.getParcelable(
        KEY_GAME
    ) ?:
        throw Error("Cannot start SDKGameActivity without a GameResponse in intent bundle")

    gameViewModel.game.observe(this, Observer { game ->
      when (game?.lastEvent) {
        STATUS -> onGameStatus(game)
        QUESTION_STARTED -> onQuestionStarted(game)
        QUESTION_ENDED -> onQuestionEnded(game)
        QUESTION_RESULT -> onQuestionResult(game)
        GAME_WINNERS -> onGameWinners(game)
        GAME_ENDED -> goBack()
      }
    })

    gameViewModel.viewerCount.observe(this, Observer { it?.let(this::updateViewCount) })

    gameViewModel.connectToGame(gameResponse)

    playerFragment.configure(Uri.parse(gameResponse.streamUrl), gameResponse.id)
  }

  override fun onPause() {
    super.onPause()
    if (!isChangingConfigurations && isFinishing) {
      Events.publish(
          ExitGameEvent(gameResponse.id))
      winnersDialog?.dismiss()
    }
  }

  private fun onGameStatus(game: GameState) {
    loading.visibility = View.GONE

    if (!game.isUserActive) showJoinLate()

    updateHeartIcon(game.isHeartEligible)
    updateScore(game.score)
    updateAdditionalPoints(null)
  }

  private fun onQuestionStarted(game: GameState) {
    updateAdditionalPoints(null)
    val question = game.currentQuestion as QuestionStartState? ?: return

    DeviceUtil.vibratePhone(this)
    dismissSubscribePromptDialog()

    questionNumberText.visibility = View.VISIBLE
    questionNumberText.text = game.totalQuestionsCount?.let { total ->
      "${question.questionNumber}/$total"
    } ?: "Q${question.questionNumber}"

    if (!game.isHeartEligible) {
      updateHeartIcon(false)
    }
  }

  private fun onQuestionEnded(game: GameState) {
    dismissExtraLifeDialog()

    val question = game.currentQuestion as QuestionEndState? ?: return

    if (question.serverReceivedSelection == null && gameViewModel.selectedChoice.value != null) {
      showSubmissionFailedWarning()
    }

  }

  private fun onQuestionResult(game: GameState) {
    val question = game.currentQuestion as QuestionResultState? ?: return

    updateScore(question.score)
    updateAdditionalPoints(question.pointsValue)

    if (question.userWasEliminatedOnQuestion) {
      if (gameViewModel.didUserSkipQuestionAfterPurchase()) {
        showLateSubscriptionWarning()
        showEliminatedNotification()
      } else if (question.canRedeemHeart) {
        updateHeartIcon(false)
        showExtraLifeDialog()
      } else if (question.canUseSubscription && billingSupported) {
        updateHeartIcon(false)
        showSubscribePromptDialog(game.currentQuestionNumber, game.totalQuestionsCount)
      } else {
        dismissExtraLifeDialog()
        showEliminatedNotification()
      }
    } else if (!game.isUserActive) {
      showEliminatedNotification()
    }
  }

  private fun updateViewCount(views: Long) {
    viewCount.visibility = View.VISIBLE
    viewCount.text = "$views "
  }

  private fun showJoinLate () {
    if (isDestroyed) return

    showEliminatedNotification()

    val dialogView = layoutInflater.inflate(R.layout.theqkit_late_join_dialog, null)
    AlertDialog.Builder(this).apply {
      setView(dialogView)
      create().apply {
        show()
        dialogView.findViewById<Button>(R.id.earnPoints).setOnClickListener { dismiss() }
      }
    }
  }

  fun showEliminatedNotification() {
    eliminatedNotification.alpha = 1f
    eliminatedNotification.visibility = View.VISIBLE
  }

  override fun onUseHeartClicked() {
    usedHeartAnimationView.visibility = View.VISIBLE
    usedHeartAnimationView.addAnimatorListener(object : Animator.AnimatorListener {
      override fun onAnimationRepeat(p0: Animator?) {}
      override fun onAnimationEnd(p0: Animator?) { usedHeartAnimationView.visibility = View.GONE }
      override fun onAnimationCancel(p0: Animator?) {}
      override fun onAnimationStart(p0: Animator?) {}
    })
    usedHeartAnimationView.playAnimation()
    gameViewModel.useHeartOnNextSubmission()
    Events.publish(
        HeartUsedEvent(gameResponse.id))
  }

  override fun onDenyUseHeart() {
    showEliminatedNotification()
    Events.publish(
        HeartNotUsedEvent(gameResponse.id))
  }

  private fun goBack() {
    val active = gameViewModel.game.value?.isUserActive ?: false
    setResult(RESULT_CODE, getResultIntent(gameResponse, active, true))
    finish()
  }

  override fun onBackPressed() {
    val active = gameViewModel.game.value?.isUserActive ?: false
    val lastEvent = gameViewModel.game.value?.lastEvent
    val gameEnded = lastEvent == GAME_ENDED || lastEvent == GAME_WINNERS
    setResult(RESULT_CODE, getResultIntent(gameResponse, active, gameEnded))
    showBackPressedWarning()
  }

  private fun onGameWinners(game: GameState) {
    updateAdditionalPoints(null)
    game.winners?.let { showGameWinnersDialog() }
  }

  private fun updateHeartIcon(canRedeemHeart: Boolean) {
    heartEligible.visibility = View.VISIBLE
    heartEligible.setImageResource(
        if (canRedeemHeart) R.drawable.theqkit_heart_game else R.drawable.theqkit_heart_game_unavailable
    )
  }

  private fun updateScore(score: Int?) {
    if (score != null && score > 0) {
      currentPoints.text = resources.getQuantityString(R.plurals.theqkit_n_points, score, score)
      currentPoints.visibility = View.VISIBLE
    } else {
      currentPoints.visibility = View.INVISIBLE
    }
  }

  private fun updateAdditionalPoints(pointsValue: Int?) {
    if (pointsValue != null && pointsValue > 0) {
      additionalPoints.text = resources.getQuantityString(R.plurals.theqkit_n_additional_points, pointsValue, pointsValue)
      additionalPoints.visibility = View.VISIBLE
    } else {
      additionalPoints.visibility = View.GONE
    }
  }

  private fun showBackPressedWarning() {
    if (this.isDestroyed) return
    AlertDialog.Builder(this)
        .setTitle(resources.getString(R.string.theqkit_leave_game_title))
        .setMessage(resources.getString(R.string.theqkit_leave_game_message))
        .setPositiveButton(resources.getString(R.string.theqkit_leave)) { _, _ -> super.onBackPressed() }
        .setNegativeButton(resources.getString(R.string.theqkit_cancel), null)
        .create()
        .show()
  }

  private fun showSubmissionFailedWarning() {
    if (this.isDestroyed) return
    AlertDialog.Builder(this)
        .setTitle(getString(R.string.theqkit_submission_timed_out))
        .setMessage(getString(R.string.theqkit_submission_timed_out_description))
        .setPositiveButton(getString(R.string.theqkit_continue_playing), null)
        .create()
        .show()
  }

  open fun showLateSubscriptionWarning() {}

  private fun showExtraLifeDialog() {
    if (this.isDestroyed) return
    ExtraLifeDialogFragment()
        .show(supportFragmentManager, ExtraLifeDialogFragment::class.java.name)
  }

  private fun dismissExtraLifeDialog() {
    (supportFragmentManager.findFragmentByTag(
       ExtraLifeDialogFragment::class.java.name
    ) as DialogFragment?)?.dismiss()
  }

  open fun showSubscribePromptDialog(currentQuestion: Int?, totalQuestions: Int?) {}

  open fun dismissSubscribePromptDialog() {}

  private fun showGameWinnersDialog() {
    if (this.isDestroyed) return
    GameWinnersDialogFragment.newInstance().show(
        supportFragmentManager,
        GameWinnersDialogFragment::class.java.name
    )
  }

  private fun dismissGameWinnersDialog() {
    (supportFragmentManager.findFragmentByTag(
        GameWinnersDialogFragment::class.java.name
    ) as DialogFragment?)?.dismiss()
  }

  private fun getResultIntent(game: GameResponse, activeWhenLeft: Boolean, gameEnded: Boolean): Intent {
    return Intent().apply {
      putExtra(KEY_ACTIVE_WHEN_LEFT, activeWhenLeft)
      putExtra(KEY_GAME_ENDED, gameEnded)
      putExtra(KEY_GAME, game)
    }
  }

  override fun finish() {
    dismissGameWinnersDialog()
    super.finish()
  }

  companion object {
    const val REQUEST_CODE = 100
    const val RESULT_CODE = 5
    const val KEY_GAME = "KEY_GAME"
    const val KEY_ACTIVE_WHEN_LEFT = "KEY_ACTIVE_WHEN_LEFT"
    const val KEY_GAME_ENDED = "KEY_GAME_ENDED"
  }
}
