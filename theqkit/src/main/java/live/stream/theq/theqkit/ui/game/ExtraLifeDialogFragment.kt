package live.stream.theq.theqkit.ui.game

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import kotlinx.android.synthetic.main.theqkit_fragment_extra_life_dialog.accept
import kotlinx.android.synthetic.main.theqkit_fragment_extra_life_dialog.deny
import live.stream.theq.theqkit.R

@Keep
internal class ExtraLifeDialogFragment : DialogFragment() {

  private var extraLifeListener: ExtraLifeListener? = null

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.theqkit_fragment_extra_life_dialog, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    isCancelable = false

    accept.setOnClickListener {
      extraLifeListener?.onUseHeartClicked()
      dismiss()
    }

    deny.setOnClickListener {
      extraLifeListener?.onDenyUseHeart()
      dismiss()
    }
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)

    if (context is ExtraLifeListener) {
      extraLifeListener = context
    } else {
      throw RuntimeException(context.toString() + " must implement ExtraLifeListener")
    }
  }

  override fun onDetach() {
    super.onDetach()
    extraLifeListener = null
  }

  interface ExtraLifeListener {
    fun onUseHeartClicked()
    fun onDenyUseHeart()
  }
}
