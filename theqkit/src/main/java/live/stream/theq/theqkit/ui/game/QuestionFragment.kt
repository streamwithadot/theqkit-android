package live.stream.theq.theqkit.ui.game

import android.animation.Animator
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
import kotlinx.android.synthetic.main.theqkit_fragment_question.animationView
import kotlinx.android.synthetic.main.theqkit_fragment_question.navSpacer
import kotlinx.android.synthetic.main.theqkit_fragment_question.questionStatus
import kotlinx.android.synthetic.main.theqkit_fragment_question.questionText
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
import live.stream.theq.theqkit.internal.VisibleHeightObserver
import live.stream.theq.theqkit.util.Calculations
import live.stream.theq.theqkit.util.NavigationUtil
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.lang.IllegalArgumentException

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
    return inflater.inflate(R.layout.theqkit_fragment_question, container, false)
  }

  override fun onResume() {
    super.onResume()

    animationView.addAnimatorListener(object : Animator.AnimatorListener {
      override fun onAnimationEnd(p0: Animator?) {
        animationView.visibility = View.GONE
      }
      override fun onAnimationStart(p0: Animator?) {}
      override fun onAnimationRepeat(p0: Animator?) {}
      override fun onAnimationCancel(p0: Animator?) {}
    })

  }

  override fun onPause() {
    super.onPause()
    animationView.removeAllAnimatorListeners()
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
      backgroundColorId = R.color.theqkit_game_overlay_popular_choice
    } else {
      val strQuestionNum = getString(R.string.theqkit_question_number, question.questionNumber)
      showQuestionStatus(strQuestionNum, R.color.theqkit_color_accent, null)
      backgroundColorId = R.color.theqkit_game_overlay_default
    }

    show(backgroundColorId)

    startQuestionTimer(question.responseExpiry, isPopularChoice)
  }

  private fun onQuestionEnded(question: QuestionEndState) {
    handleQuestionEnd()
  }

  private fun onQuestionResult(question: QuestionResultState) {
    val resultAnimation: TimedAnimation
    if (question.wasUserCorrect) {
      resultAnimation = if (question.isPopularChoice) {
        PopularChoiceCorrectAnimation
      } else {
        DefaultCorrectAnimation
      }
      showQuestionStatus("Correct!", R.color.theqkit_selected_green)
      show(R.color.theqkit_game_overlay_correct)
      gameViewModel.game.value?.let { Events.publish(CorrectSubmissionEvent(it, question)) }
    } else {
      resultAnimation = if (question.isPopularChoice) {
        PopularChoiceIncorrectAnimation
      } else {
        DefaultIncorrectAnimation
      }
      val label = if (question.serverReceivedSelection == null) "No Answer!" else "Wrong Answer!"
      showQuestionStatus(label, R.color.theqkit_color_accent)
      show(R.color.theqkit_game_overlay_incorrect)

      question.serverReceivedSelection?.let {
        gameViewModel.game.value?.let { Events.publish(IncorrectSubmissionEvent(it, question)) }
      } ?: gameViewModel.game.value?.let { Events.publish(NoSubmissionEvent(it)) }
    }

    playAnimation(resultAnimation.lottieAsset)
    scheduleHide(resultAnimation.durationMillis)
  }

  private fun startQuestionTimer(responseExpiry: Long, isPopularChoice: Boolean) {
    val millisLeft = Math.max(responseExpiry - System.currentTimeMillis() , 0)
    val secondsLeft = millisLeft / 1000f

    if (secondsLeft < 1f) return

    val timer = getCountdownAnimation(resources.getInteger(
        if (isPopularChoice)
          R.integer.theqkit_response_time_pop_choice
        else
          R.integer.theqkit_response_time_trivia
    ).toFloat())

    playAnimation(timer.lottieAsset, Math.max(Math.min(timer.startProgress(secondsLeft), 1f), 0f))

    questionEndHandler.removeCallbacksAndMessages(null)
    questionEndHandler.postDelayed(this::handleQuestionEnd, millisLeft)
  }

  private fun handleQuestionEnd() {
    questionEndHandler.removeCallbacksAndMessages(null)
    scheduleHide(1800)
    showQuestionStatus("Time's Up!", R.color.theqkit_color_accent)
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
    animationView.visibility = View.VISIBLE
    animationView.resumeAnimation()
  }

  private fun dpToPx(context: Context, dp: Float) = Calculations.convertDpToPixel(context, dp)

  private fun navHeight(context: Context) = NavigationUtil.getNavigationBarSize(context).y

  private class CountdownAnimation(val lottieAsset: String, val countdownSeconds: Float) {
    fun startProgress(secondsLeft: Float) = 1 - (secondsLeft / countdownSeconds)
  }

  private class TimedAnimation(val lottieAsset: String, val durationMillis: Long)

  private fun getCountdownAnimation(countdownSeconds: Float) = when(countdownSeconds) {
    10f -> CountdownAnimation("theqkit_timer_10s.json", 10f)
    13f -> CountdownAnimation("theqkit_timer_13s.json", 13f)
    15f -> CountdownAnimation("theqkit_timer_15s.json", 15f)
    else -> throw IllegalArgumentException("No timer animations found for duration $countdownSeconds")
  }

  companion object {
    private val DefaultTimer = CountdownAnimation("theqkit_timer_10s.json", 10f)
    private val PopularChoiceTimer = CountdownAnimation("theqkit_timer_13s.json", 13f)
    private val DefaultCorrectAnimation = TimedAnimation("theqkit_correct.json", 5000)
    private val DefaultIncorrectAnimation = TimedAnimation("theqkit_incorrect.json", 5000)
    private val PopularChoiceCorrectAnimation = TimedAnimation("theqkit_correct-pc.json", 6000)
    private val PopularChoiceIncorrectAnimation = TimedAnimation("theqkit_incorrect-pc.json", 6000)
  }

}
