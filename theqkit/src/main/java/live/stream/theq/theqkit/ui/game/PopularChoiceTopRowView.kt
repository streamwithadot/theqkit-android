package live.stream.theq.theqkit.ui.game

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.theqkit_row_popular_choice_top.view.choiceText
import kotlinx.android.synthetic.main.theqkit_row_popular_choice_top.view.percentage
import kotlinx.android.synthetic.main.theqkit_row_popular_choice_top.view.rank
import live.stream.theq.theqkit.R
import live.stream.theq.theqkit.data.sdk.ChoiceResultState
import java.text.DecimalFormat

@Keep
internal class PopularChoiceTopRowView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyle: Int = 0,
  defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyle, defStyleRes) {

  val decimalFormat = DecimalFormat("##%")

  init {
    LayoutInflater.from(context)
        .inflate(R.layout.theqkit_row_popular_choice_top, this, true)

    orientation = HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
  }

  fun setRow(rank: Int, choiceResultState: ChoiceResultState?, userChoiceText: String?, wasUserCorrect: Boolean) {
    if (choiceResultState == null) {
      visibility = View.GONE
      clearRow()
      return
    }

    if (userChoiceText != null && userChoiceText == choiceResultState.id) {
      setBackgroundColor(ContextCompat.getColor(context, R.color.theqkit_white_40p_transparent))
    } else {
      background = null
    }

    this.rank.text = rank.toString()
    if (wasUserCorrect) {
      this.rank.setTextColor(ContextCompat.getColor(context, R.color.theqkit_game_overlay_correct))
    } else {
      this.rank.setTextColor(ContextCompat.getColor(context, R.color.theqkit_game_overlay_incorrect))
    }

    this.choiceText.text = choiceResultState.id

    val formattedPercent = decimalFormat.format(choiceResultState.userResponseRatio / 100)
    percentage.text = formattedPercent

    visibility = View.VISIBLE
  }

  private fun clearRow() {
    background = null
    rank.text = ""
    choiceText.text = ""
    percentage.text = ""
  }
}