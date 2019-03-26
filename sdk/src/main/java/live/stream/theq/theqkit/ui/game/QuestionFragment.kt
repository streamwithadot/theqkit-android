package live.stream.theq.theqkit.ui.game

import android.content.Context
import androidx.lifecycle.Observer
import android.os.Bundle
import android.os.Handler
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import kotlinx.android.synthetic.main.theq_sdk_fragment_question.animationView
import kotlinx.android.synthetic.main.theq_sdk_fragment_question.navSpacer
import kotlinx.android.synthetic.main.theq_sdk_fragment_question.questionStatus
import kotlinx.android.synthetic.main.theq_sdk_fragment_question.questionText
import live.stream.theq.theqkit.R
import live.stream.theq.theqkit.data.sdk.GameState.Companion.GameEvent.QUESTION_ENDED
import live.stream.theq.theqkit.data.sdk.GameState.Companion.GameEvent.QUESTION_RESULT
import live.stream.theq.theqkit.data.sdk.GameState.Companion.GameEvent.QUESTION_STARTED
import live.stream.theq.theqkit.data.sdk.Question
import live.stream.theq.theqkit.data.sdk.QuestionEndState
import live.stream.theq.theqkit.data.sdk.QuestionResultState
import live.stream.theq.theqkit.data.sdk.QuestionStartState
import live.stream.theq.theqkit.events.CorrectSubmissionEvent
import live.stream.theq.theqkit.events.Events
import live.stream.theq.theqkit.events.IncorrectSubmissionEvent
import live.stream.theq.theqkit.events.NoSubmissionEvent
import live.stream.theq.theqkit.util.VisibleHeightObserver
import live.stream.theq.theqkit.util.Calculations
import live.stream.theq.theqkit.util.NavigationUtil
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

@Keep
internal class QuestionFragment : Fragment() {

  private val gameViewModel: GameViewModel by sharedViewModel()

  private val questionStatusHandler = Handler()
  private val questionEndHandler = Handler()
  private val hideQuestionHandler = Handler()

  private val layout: ConstraintLayout?
    get() = view as? ConstraintLayout?

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    gameViewModel.game.observe(this, Observer { game ->
      when (game?.lastEvent) {
        QUESTION_STARTED -> {
          (game.currentQuestion as QuestionStartState?)?.let(::onQuestionStarted)
        }
        QUESTION_ENDED -> {
          (game.currentQuestion as QuestionEndState?)?.let(::onQuestionEnded)
        }
        QUESTION_RESULT -> {
          (game.currentQuestion as QuestionResultState?)?.let(::onQuestionResult)
        }
        else -> {}
      }
    })
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.theq_sdk_fragment_question, container, false)
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    // prevent content from disappearing behind the nav bar...
    (navSpacer.layoutParams as ConstraintLayout.LayoutParams)
        .height = (dpToPx(requireContext(), 4f) + navHeight(requireContext())).toInt()

    // ...but only when the keyboard is closed
    VisibleHeightObserver.observeKeyboardState(this) { isVisible ->
      val visibility = if (isVisible) View.GONE else View.VISIBLE
      navSpacer.visibility = visibility
      questionStatus.visibility = visibility // note see comments in `showQuestionStatus()`
    }

    // don't re-run the below code if we're being restored an unretained activity
    if (savedInstanceState != null) return

    childFragmentManager.beginTransaction().apply {
      (0..2).forEach { idx -> add(R.id.choicesContainer,
          GameButtonFragment.newInstance(idx)) }
      add(R.id.choicesContainer,
          GameFreeResponseFragment.newInstance())
      commit()
    }

  }

  override fun onDestroy() {
    super.onDestroy()

    questionStatusHandler.removeCallbacksAndMessages(null)
    questionEndHandler.removeCallbacksAndMessages(null)
    hideQuestionHandler.removeCallbacksAndMessages(null)
  }

  private fun onQuestionStarted(question: QuestionStartState) {
    val isPopularChoice = question.questionType == Question.TYPE_POPULAR

    questionText.text = question.questionText

    val backgroundColorId: Int

    if (isPopularChoice) {
      hideQuestionStatus() // hide to make sure we control reappearing
      backgroundColorId = R.color.theq_sdk_game_overlay_popular_choice
    } else {
      val strQuestionNum = getString(R.string.theq_sdk_question_number, question.questionNumber)
      showQuestionStatus(strQuestionNum, R.color.theq_sdk_color_accent, null)
      backgroundColorId = R.color.theq_sdk_game_overlay_default
    }

    show(backgroundColorId)

    startQuestionTimer(question.responseExpiry, isPopularChoice)
  }

  private fun onQuestionEnded(question: QuestionEndState) {
    handleQuestionEnd()
  }

  private fun onQuestionResult(question: QuestionResultState) {
    if (question.wasUserCorrect) {
      playAnimation(ANIM_CORRECT)
      showQuestionStatus("Correct!", R.color.theq_sdk_selected_green)
      show(R.color.theq_sdk_game_overlay_correct)
      gameViewModel.game.value?.let { Events.publish(
          CorrectSubmissionEvent(it, question)) }
    } else {
      playAnimation(ANIM_INCORRECT)
      val label = if (question.serverReceivedSelection == null) "No Answer!" else "Wrong Answer!"
      showQuestionStatus(label, R.color.theq_sdk_color_accent)
      show(R.color.theq_sdk_game_overlay_incorrect)

      question.serverReceivedSelection?.let {
        gameViewModel.game.value?.let { Events.publish(
            IncorrectSubmissionEvent(it, question)) }
      } ?: gameViewModel.game.value?.let { Events.publish(
          NoSubmissionEvent(it)) }
    }

    scheduleHide(4750)
  }

  private fun startQuestionTimer(responseExpiry: Long, isPopularChoice: Boolean) {
    val millisLeft = Math.max(responseExpiry - System.currentTimeMillis() , 0)
    val secondsLeft = millisLeft / 1000f

    if (secondsLeft < 1f) return

    val timer = if (isPopularChoice) PopularChoiceTimer else DefaultTimer

    playAnimation(timer.lottieAsset, Math.max(Math.min(timer.startProgress(secondsLeft), 1f), 0f))

    questionEndHandler.removeCallbacksAndMessages(null)
    questionEndHandler.postDelayed(this::handleQuestionEnd, millisLeft)
  }

  private fun handleQuestionEnd() {
    questionEndHandler.removeCallbacksAndMessages(null)
    scheduleHide(1800)
    showQuestionStatus("Time's Up!", R.color.theq_sdk_color_accent)
  }

  private fun showQuestionStatus(text: String, colorId: Int, hideDelayMillis: Long? = 2000) {
    questionStatusHandler.removeCallbacksAndMessages(null)
    questionStatus.text = text
    getColor(colorId)?.let(questionStatus::setTextColor)
    // note: we toggle alpha for showing and hiding. this allows us to:
    //   a) avoid layout jank, and
    //   b) use View.GONE/VISIBLE when toggling with the keyboard state w/o extra state tracking
    questionStatus.alpha = 1f

    hideDelayMillis?.let {
      questionStatusHandler.postDelayed({
        hideQuestionStatus()
      }, it)
    }
  }

  private fun hideQuestionStatus() {
    questionStatus.alpha = 0f
  }

  private fun show(colorId: Int) {
    cancelHide()
    // note: synthetic reference to `layout` was always null. need to research futher...
    layout?.apply {
      getColor(colorId)?.let { setBackgroundColor(it) }
      visibility = View.VISIBLE
    }
  }

  private fun scheduleHide(delayMillis: Long) {
    cancelHide()
    hideQuestionHandler.postDelayed({
      hideQuestionStatus()
      layout?.apply { visibility = View.INVISIBLE }
    }, delayMillis)
  }

  private fun cancelHide() {
    hideQuestionHandler.removeCallbacksAndMessages(null)
  }

  private fun getColor(id: Int): Int? {
    return context?.let { ContextCompat.getColor(it, id) }
  }

  private fun playAnimation(animation: String, initialProgress: Float = 0f) {
    animationView.clearAnimation()
    animationView.setAnimation(animation)
    animationView.progress = initialProgress
    animationView.resumeAnimation()
  }

  private fun dpToPx(context: Context, dp: Float) = Calculations.convertDpToPixel(context, dp)

  private fun navHeight(context: Context) = NavigationUtil.getNavigationBarSize(context).y

  private class CountdownAnimation(val lottieAsset: String, val countdownSeconds: Float) {
    fun startProgress(secondsLeft: Float) = 1 - (secondsLeft / countdownSeconds)
  }

  companion object {

    private val DefaultTimer = CountdownAnimation(
        "theq_sdk_timer_10s.json", 10f)
    private val PopularChoiceTimer =
      CountdownAnimation(
          "theq_sdk_timer_13s.json", 13f)
    private const val ANIM_CORRECT = "theq_sdk_correct.json"
    private const val ANIM_INCORRECT = "theq_sdk_incorrect.json"

  }

}
