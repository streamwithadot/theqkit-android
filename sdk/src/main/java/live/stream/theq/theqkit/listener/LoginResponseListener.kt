package live.stream.theq.theqkit.listener

import androidx.annotation.Keep
import live.stream.theq.theqkit.data.sdk.ApiError

@Keep
interface LoginResponseListener {
  fun onSuccess()
  fun onFailure(apiError: ApiError)
}