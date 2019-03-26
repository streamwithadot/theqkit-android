package live.stream.theq.theqkit.ui.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.Keep
import androidx.fragment.app.DialogFragment
import live.stream.theq.theqkit.R
import live.stream.theq.theqkit.util.CurrencyHelper
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.lang.IllegalStateException
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import kotlinx.android.synthetic.main.theq_sdk_winners_layout.prizeText
import kotlinx.android.synthetic.main.theq_sdk_winners_layout.winnerRecyler
import kotlinx.android.synthetic.main.theq_sdk_winners_layout.winnerText

@Keep
internal class GameWinnersDialogFragment : DialogFragment() {

  private val gameViewModel: GameViewModel by sharedViewModel()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setStyle(DialogFragment.STYLE_NORMAL, R.style.TheQSdkWinner)
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.theq_sdk_winners_layout, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val winners = gameViewModel.game.value?.winners
        ?: throw IllegalStateException("winners cannot be null")
    val potSize = gameViewModel.gameResponse.reward

    winnerText.text =
      resources.getQuantityString(R.plurals.theq_sdk_n_winners, winners.count, winners.count)

    prizeText.text = CurrencyHelper.getExactCurrency(view.context, potSize)

    winnerRecyler.adapter = WinnersRecyclerAdapter(winners, potSize, view.context)

  }

  override fun onStart() {
    super.onStart()
    dialog.window?.setLayout(MATCH_PARENT, MATCH_PARENT)
  }

  companion object {
    fun newInstance() = GameWinnersDialogFragment()
  }

}
