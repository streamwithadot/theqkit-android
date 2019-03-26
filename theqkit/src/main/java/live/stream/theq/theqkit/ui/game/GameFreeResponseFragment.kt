package live.stream.theq.theqkit.ui.game

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import live.stream.theq.theqkit.data.sdk.GameState.Companion.GameEvent.QUESTION_ENDED
import live.stream.theq.theqkit.data.sdk.GameState.Companion.GameEvent.QUESTION_RESULT
import live.stream.theq.theqkit.data.sdk.GameState.Companion.GameEvent.QUESTION_STARTED
import live.stream.theq.theqkit.data.sdk.QuestionStartState
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import android.content.Context
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.annotation.Keep
import kotlinx.android.synthetic.main.theq_sdk_fragment_game_free_response.submissionText
import kotlinx.android.synthetic.main.theq_sdk_fragment_game_free_response.submitButton
import kotlinx.android.synthetic.main.theq_sdk_fragment_game_free_response.responseGroup
import kotlinx.android.synthetic.main.theq_sdk_fragment_game_free_response.responseText
import kotlinx.android.synthetic.main.theq_sdk_fragment_game_free_response.submitGroup
import kotlinx.android.synthetic.main.theq_sdk_fragment_game_free_response.topChoice1
import kotlinx.android.synthetic.main.theq_sdk_fragment_game_free_response.topChoice2
import kotlinx.android.synthetic.main.theq_sdk_fragment_game_free_response.topChoice3
import kotlinx.android.synthetic.main.theq_sdk_fragment_game_free_response.topChoicesLayout
import live.stream.theq.theqkit.R
import live.stream.theq.theqkit.data.sdk.Question
import live.stream.theq.theqkit.data.sdk.QuestionResultState
import live.stream.theq.theqkit.extensions.nullIfNullOrEmpty
import live.stream.theq.theqkit.util.ViewUtil

@Keep
internal class GameFreeResponseFragment : Fragment() {

  private val gameViewModel: GameViewModel by sharedViewModel()

  private val questionEndHandler = Handler()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    gameViewModel.game.observe(this, Observer { game ->
      if (game?.currentQuestion?.questionType != Question.TYPE_POPULAR) {
        hide()
        return@Observer
      }

      when (game.lastEvent) {
        QUESTION_STARTED -> (game.currentQuestion as QuestionStartState?)?.let(::onQuestionStarted)
        QUESTION_ENDED -> onQuestionEnded()
        QUESTION_RESULT -> (game.currentQuestion as QuestionResultState?)?.let(::onQuestionResult)
        else -> {}
      }
    })

    gameViewModel.selectedChoice.observe(this, Observer { selectedChoice ->
      if (selectedChoice?.id != null) {
        showResponseGroup()
        hideSubmitGroup()
      } else if (gameViewModel.game.value?.lastEvent == QUESTION_STARTED){
        hideResponseGroup()
      }
    })

    gameViewModel.choiceSubmission.observe(this, Observer {
      if (it?.isLoading() == true) {
        //TODO show submit progress
        submitButton.isEnabled = false
      } else {
        //TODO hide submit progress
        submitButton.isEnabled = true
      }
    })
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.theq_sdk_fragment_game_free_response, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    hide()

    submitButton.setOnClickListener {
      submitChoice()
    }

    submissionText.setOnEditorActionListener { _, actionId, _ ->
      val handled = if (actionId == EditorInfo.IME_ACTION_SEND) {
        submitChoice()
        true
      } else {
        false
      }
      handled
    }
  }

  private fun onQuestionStarted(question: QuestionStartState) {
    hideResponseGroup()
    hideTopChoices()
    showSubmitGroup()
    view?.visibility = View.VISIBLE

    questionEndHandler.removeCallbacksAndMessages(null)
    questionEndHandler.postDelayed({
      hideSubmitGroup()
    }, Math.max(question.responseExpiry - System.currentTimeMillis() , 0))
  }

  private fun onQuestionEnded() {
    hideSubmitGroup()
    hideTopChoices()
    showResponseGroup()
  }

  private fun onQuestionResult(question: QuestionResultState) {
    hideSubmitGroup()
    showResponseGroup()
    showTopChoices(question)
  }

  private fun submitChoice() {
    val submission = submissionText.text?.toString()?.trim().nullIfNullOrEmpty() ?: return
    gameViewModel.game.value?.lastEvent
        ?.takeIf { it == QUESTION_STARTED }
        ?.takeIf { gameViewModel.selectedChoice.value == null }
        ?.let {
          gameViewModel.submitChoice(submission)
          ViewUtil.hideKeyboard(activity)
        }
  }

  private fun hide() {
    ViewUtil.hideKeyboard(activity)
    view?.visibility = View.GONE
  }

  private fun showKeyboard() {
    submissionText.requestFocus()
    val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
    imm?.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
  }

  override fun onDestroy() {
    super.onDestroy()
    questionEndHandler.removeCallbacksAndMessages(null)
  }

  private fun hideSubmitGroup() {
    submissionText.setText("")
    submitGroup.visibility = View.GONE
    ViewUtil.hideKeyboard(activity)
  }

  private fun showSubmitGroup() {
    submissionText.setText("")
    submitGroup.visibility = View.VISIBLE
    showKeyboard()
  }

  private fun showResponseGroup() {
    val text = gameViewModel.selectedChoice.value?.id
    if (text == null) {
      responseText.alpha = 0.7f
      responseText.text = "(none)"
    } else {
      responseText.alpha = 1f
      responseText.text = text
    }
    responseGroup.visibility = View.VISIBLE
  }

  private fun hideResponseGroup() {
    responseGroup.visibility = View.GONE
  }

  private fun showTopChoices(question: QuestionResultState) {
    val choices = question.choices.take(3)
    val choice1 = choices.getOrNull(0)
    val choice2 = choices.getOrNull(1)
    val choice3 = choices.getOrNull(2)

    topChoice1.setRow(1, choice1, question.serverReceivedSelection, question.wasUserCorrect)
    topChoice2.setRow(2, choice2, question.serverReceivedSelection, question.wasUserCorrect)
    topChoice3.setRow(3, choice3, question.serverReceivedSelection, question.wasUserCorrect)

    topChoicesLayout.visibility = View.VISIBLE
  }

  private fun hideTopChoices() {
    topChoicesLayout.visibility = View.GONE
  }

  companion object {

    fun newInstance() = GameFreeResponseFragment()
  }

}
