package live.stream.theq.theqkit.data.app

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize data class Device(
  val id: String,
  val type: String = "ANDROID",
  val token: String,
  val firebaseRegistrationToken: String,
  val gpsAdid: String?
) : Parcelable
