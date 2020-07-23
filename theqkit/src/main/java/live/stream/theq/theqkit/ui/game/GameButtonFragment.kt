package live.stream.theq.theqkit.ui.game

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.amulyakhare.textdrawable.TextDrawable
import kotlinx.android.synthetic.main.theqkit_fragment_game_button.choiceImage
import kotlinx.android.synthetic.main.theqkit_fragment_game_button.choiceProgress
import kotlinx.android.synthetic.main.theqkit_fragment_game_button.choiceText
import kotlinx.android.synthetic.main.theqkit_fragment_game_button.plusOne
import kotlinx.android.synthetic.main.theqkit_fragment_game_button.responsesText
import kotlinx.android.synthetic.main.theqkit_fragment_game_button.submitProgress
import live.stream.theq.theqkit.R
import live.stream.theq.theqkit.data.sdk.ChoiceResultState
import live.stream.theq.theqkit.data.sdk.ChoiceState
import live.stream.theq.theqkit.data.sdk.GameState.Companion.GameEvent
import live.stream.theq.theqkit.data.sdk.GameState.Companion.GameEvent.QUESTION_ENDED
import live.stream.theq.theqkit.data.sdk.GameState.Companion.GameEvent.QUESTION_RESULT
import live.stream.theq.theqkit.data.sdk.GameState.Companion.GameEvent.QUESTION_STARTED
import live.stream.theq.theqkit.data.sdk.Question
import live.stream.theq.theqkit.data.sdk.QuestionStartState
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import kotlin.math.ceil

@Keep
internal class GameButtonFragment : Fragment() {

  private val gameViewModel: GameViewModel by sharedViewModel()

  private val questionEndHandler = Handler()

  private var choiceIdx: Int = -1

  private val layout: ConstraintLayout?
    get() = view as? ConstraintLayout?

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    gameViewModel.game.observe(this, Observer { game ->
      if (game?.currentQuestion?.questionType != Question.TYPE_TRIVIA
              && game?.currentQuestion?.questionType != Question.TYPE_CHOICE_SURVEY) {
        hide()
        return@Observer
      }

      when (game.lastEvent) {
        QUESTION_STARTED -> (game.currentQuestion as QuestionStartState?)?.let(::onQuestionStarted)
        QUESTION_ENDED -> onQuestionEnded()
        QUESTION_RESULT -> (getChoice() as ChoiceResultState?)?.let(::onQuestionResult)
        else -> {}
      }

      game.lastEvent.let {
        updateButtonClickability(it, gameViewModel.selectedChoice.value)
        updateButtonSelectedState(gameViewModel.selectedChoice.value)
      }
    })

    gameViewModel.selectedChoice.observe(this, Observer { selectedChoice ->
      gameViewModel.game.value?.lastEvent?.let {
        updateButtonClickability(it, selectedChoice)
        updateButtonSelectedState(selectedChoice)
      }
    })

    gameViewModel.choiceSubmission.observe(this, Observer {
      it?.data?.takeIf { isSameChoiceAsButton(it.choice) }?.let { _ ->
        if (it.isLoading()) {
          submitProgress.visibility = View.VISIBLE
        } else {
          submitProgress.visibility = View.GONE
        }
      } ?: run { submitProgress.visibility = View.GONE }
    })

  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.theqkit_fragment_game_button, container, false)
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)

    choiceIdx = arguments?.getInt(
        KEY_IDX, -1) ?: -1
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    layout?.setOnClickListener {
      gameViewModel.game.value?.lastEvent
          ?.takeIf { it == QUESTION_STARTED }
          ?.takeIf { gameViewModel.selectedChoice.value == null }
          ?.let { selectButton() }
    }

    getColor(R.color.theqkit_plus_one_background)?.let {
      plusOne.setImageDrawable(TextDrawable.builder().buildRound("+1", it))
    }
  }

  private fun onQuestionStarted(question: QuestionStartState) {
    val choice = getChoice() ?: return hide()

    resetButtonUI()
    choiceText.text = choice.choiceText
    layout?.visibility = View.VISIBLE

    questionEndHandler.removeCallbacksAndMessages(null)
    questionEndHandler.postDelayed({
      gameViewModel.game.value?.lastEvent?.let {
        val selectedChoice = gameViewModel.selectedChoice.value
        updateButtonClickability(it, selectedChoice)
        updateButtonSelectedState(selectedChoice)
      }
      onQuestionEnded()
    }, Math.max(question.responseExpiry - System.currentTimeMillis() , 0))
  }

  private fun onQuestionEnded() {
    questionEndHandler.removeCallbacksAndMessages(null)
    submitProgress.visibility = View.GONE
  }

  private fun onQuestionResult(choice: ChoiceResultState) {
    choiceImage.visibility = View.VISIBLE
    plusOne.visibility = View.INVISIBLE

    responsesText.text = choice.responseCount.toString()
    getColor(R.color.theqkit_white)?.let(responsesText::setTextColor)

    choiceText.text = choice.choiceText
    getColor(R.color.theqkit_white)?.let(choiceText::setTextColor)

    val isSelected = isSameChoiceAsButton(gameViewModel.selectedChoice.value)

    if (choice.isCorrect && isSelected) {
      getDrawable(R.drawable.theqkit_choice_background_selected)?.let { layout?.background = it }
      getDrawable(R.drawable.theqkit_correct_selected)?.let(choiceImage::setImageDrawable)
      getDrawable(R.drawable.theqkit_progress_bar_selected)?.let(choiceProgress::setProgressDrawable)
      getColor(R.color.theqkit_selected_green)?.let(choiceText::setTextColor)
      getColor(R.color.theqkit_selected_green)?.let(responsesText::setTextColor)
      if(gameViewModel.game.value?.currentQuestion?.questionType != Question.TYPE_CHOICE_SURVEY) {
        plusOne.visibility = View.VISIBLE
      }
    } else if (choice.isCorrect && !isSelected) {
      getDrawable(R.drawable.theqkit_correct_unselected)?.let(choiceImage::setImageDrawable)
      getDrawable(R.drawable.theqkit_progress_bar_not_selected)?.let(choiceProgress::setProgressDrawable)
      getDrawable(R.drawable.theqkit_choice_background_active)?.let { layout?.background = it }
    } else if (isSelected) {
      getDrawable(R.drawable.theqkit_choice_background_selected)?.let { layout?.background = it }
      getDrawable(R.drawable.theqkit_incorrect_selected)?.let(choiceImage::setImageDrawable)
      getDrawable(R.drawable.theqkit_progress_bar_selected)?.let(choiceProgress::setProgressDrawable)
      getColor(R.color.theqkit_color_accent)?.let(choiceText::setTextColor)
      getColor(R.color.theqkit_color_accent)?.let(responsesText::setTextColor)
    } else {
      getDrawable(R.drawable.theqkit_incorrect_unselected)?.let(choiceImage::setImageDrawable)
      getDrawable(R.drawable.theqkit_progress_bar_not_selected)?.let(choiceProgress::setProgressDrawable)
      getDrawable(R.drawable.theqkit_choice_background_unselected)?.let { layout?.background = it }
    }

    val choicePercentage = calculatePercentage(choice.responseCount, choice.totalResponseCount)
    val progressRatio = choiceProgress.height.toFloat() / choiceProgress.width.toFloat()
    choiceProgress.progress = Math.max(choicePercentage, ceil(progressRatio * 100).toInt())
  }

  private fun selectButton() = getChoice()?.let { choice ->
    gameViewModel.submitChoice(choice)
  }

  private fun updateButtonClickability(lastEvent: GameEvent, selectedChoice: ChoiceState?) {
    layout?.isClickable = selectedChoice == null && lastEvent == QUESTION_STARTED
  }

  private fun updateButtonSelectedState(selectedChoice: ChoiceState?) {
    isSameChoiceAsButton(selectedChoice).let { isSelected ->
      layout?.isSelected = isSelected
      choiceText.isSelected = isSelected
    }
  }

  private fun resetButtonUI() {
    submitProgress.visibility = View.GONE
    plusOne.visibility = View.INVISIBLE
    choiceProgress.progress = 0
    responsesText.text = ""
    getDrawable(R.drawable.theqkit_choice_background_active)?.let { layout?.background = it }
    choiceText.text = ""
    getColorStateList(R.color.theqkit_choice_text_active)?.let(choiceText::setTextColor)
    choiceImage.visibility = View.GONE
  }

  private fun getChoice(): ChoiceState? {
    return choiceIdx.takeIf { it > -1 }?.let {
      gameViewModel.game.value?.currentQuestion?.choices?.getOrNull(it)
    }
  }

  private fun isSameChoiceAsButton(choice: ChoiceState?): Boolean {
    return getChoice()?.let { it.id == choice?.id } ?: false
  }

  private fun hide() { layout?.visibility = View.GONE }

  private fun getColor(id: Int) = context?.let { ContextCompat.getColor(it, id) }

  private fun getColorStateList(id: Int) = context?.let { ContextCompat.getColorStateList(it, id) }

  private fun getDrawable(id: Int) = context?.let { ContextCompat.getDrawable(it, id) }

  private fun calculatePercentage(choiceCount: Int, totalCount: Int): Int {
    return if (totalCount == 0) 0 else (choiceCount.toDouble() / totalCount * 100).toInt()
  }

  override fun onDestroy() {
    super.onDestroy()
    questionEndHandler.removeCallbacksAndMessages(null)
  }

  companion object {

    fun newInstance(choiceIdx: Int) = GameButtonFragment().apply {
      arguments = Bundle().apply { putInt(
          KEY_IDX, choiceIdx) }
    }

    const val KEY_IDX = "CHOICE_IDX"

  }

}
