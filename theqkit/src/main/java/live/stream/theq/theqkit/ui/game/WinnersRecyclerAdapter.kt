package live.stream.theq.theqkit.ui.game

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import live.stream.theq.theqkit.R
import live.stream.theq.theqkit.data.sdk.GameWinnerState
import live.stream.theq.theqkit.data.sdk.GameWinnersState
import live.stream.theq.theqkit.ui.game.WinnersRecyclerAdapter.ViewHolder
import live.stream.theq.theqkit.util.CurrencyHelper
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import java.math.BigDecimal

internal class WinnersRecyclerAdapter(
    private val winners: GameWinnersState,
    private val potSize: Double,
    val context: Context
) : RecyclerView.Adapter<ViewHolder>(), KoinComponent {

  private val profilePhotoLoader: GameWinnerProfilePhotoLoader? by inject()

  override fun getItemCount() = winners.profiles.size

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
      LayoutInflater.from(parent.context).inflate(R.layout.theqkit_winner_item, parent, false)
  )

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val winner = winners.profiles[position]
    holder.userName.text = winner.username
    val splitPot = BigDecimal(potSize/winners.count).setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()
    holder.balance.text = CurrencyHelper.getExactCurrency(context, splitPot)

    val defaultProfileDrawable = getDefaultProfileDrawable(winner)

    profilePhotoLoader
        ?.load(context, winner, holder.profilePic, defaultProfileDrawable)
        ?: holder.profilePic.setImageDrawable(defaultProfileDrawable)
  }

  private fun getDefaultProfileDrawable(winner: GameWinnerState) = TextDrawable.builder().buildRound(
      winner.username[0].toString().toUpperCase(),
      ColorGenerator.MATERIAL.getColor(winner.username)
  )

  class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val userName = itemView.findViewById<View>(R.id.username) as TextView
    val balance = itemView.findViewById<View>(R.id.money) as TextView
    val profilePic = itemView.findViewById<View>(R.id.profilePhoto) as ImageView
  }

}

interface GameWinnerProfilePhotoLoader {
  fun load(
    context: Context,
    winner: GameWinnerState,
    imageView: ImageView,
    defaultDrawable: Drawable
  )
}
