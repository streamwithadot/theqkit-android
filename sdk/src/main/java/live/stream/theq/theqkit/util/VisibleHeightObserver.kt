package live.stream.theq.theqkit.util

import android.graphics.Rect
import android.util.Log
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent

internal typealias VisibleHeightUpdateHandler = (visibleHeight: Int, isOpen: Boolean) -> Unit
internal typealias KeyboardStateUpdateHandler = (isOpen: Boolean) -> Unit

internal class VisibleHeightObserver private constructor(private val handlerState: HandlerState) {

  private var prevVisibleHeight: Int? = null
  private var wasKeyboardOpen: Boolean? = null
  private var rect = Rect()
  private val activity = handlerState.activity
  private val activityRoot = activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
  private val screenHeight: Int
    get() = activityRoot.rootView.height

  init {
    if (handlerState.owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
      checkForUpdates()
    }

    activityRoot.viewTreeObserver.addOnGlobalLayoutListener(::checkForUpdates)

    handlerState.owner.lifecycle.addObserver(object : LifecycleObserver {
      @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
      fun onResume() = checkForUpdates()

      @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
      fun onDestroy() =
        activityRoot.viewTreeObserver.removeOnGlobalLayoutListener(::checkForUpdates)
    })
  }

  private fun checkForUpdates() {
    val visibleHeight = getVisibleHeight()

    if (visibleHeight == prevVisibleHeight) return

    val isKeyboardOpen = checkKeyboardVisibility(visibleHeight)

    when (handlerState) {
      is VisibleHeightHandlerState -> {
        handlerState.handler(visibleHeight, isKeyboardOpen)
      }
      is KeyboardStateHandlerState -> {
        if (wasKeyboardOpen != isKeyboardOpen) {
          wasKeyboardOpen = isKeyboardOpen
          handlerState.handler(isKeyboardOpen)
        }
      }
    }
  }

  private fun checkKeyboardVisibility(visibleHeight: Int): Boolean {
    return screenHeight - visibleHeight > SOFT_KEYBOARD_SIZE_THRESHOLD
  }

  private fun getVisibleHeight(): Int {
    activityRoot.getWindowVisibleDisplayFrame(rect)
    return rect.bottom - rect.top
  }

  private interface HandlerState {
    val activity: FragmentActivity
    val owner: LifecycleOwner
  }

  private class VisibleHeightHandlerState(
    override val activity: FragmentActivity,
    override val owner: LifecycleOwner,
    val handler: VisibleHeightUpdateHandler
  ) : HandlerState

  private class KeyboardStateHandlerState(
    override val activity: FragmentActivity,
    override val owner: LifecycleOwner,
    val handler: KeyboardStateUpdateHandler
  ) : HandlerState

  internal companion object {
    private const val SOFT_KEYBOARD_SIZE_THRESHOLD = 200
    private const val TAG = "VisibleHeightObserver"

    fun observe(activity: FragmentActivity, handler: VisibleHeightUpdateHandler) {
      VisibleHeightObserver(
          VisibleHeightHandlerState(activity,
              activity, handler))
    }

    fun observe(fragment: Fragment, handler: VisibleHeightUpdateHandler) {
      fragment.activity?.let {
        VisibleHeightObserver(
            VisibleHeightHandlerState(it,
                fragment, handler))
      } ?: Log.w(TAG, "Unable to register Fragment observer with null Activity")
    }

    fun observeKeyboardState(activity: FragmentActivity, handler: KeyboardStateUpdateHandler) {
      VisibleHeightObserver(
          KeyboardStateHandlerState(activity,
              activity, handler))
    }

    fun observeKeyboardState(fragment: Fragment, handler: KeyboardStateUpdateHandler) {
      fragment.activity?.let {
        VisibleHeightObserver(
            KeyboardStateHandlerState(it,
                fragment, handler))
      } ?: Log.w(TAG, "Unable to register Fragment observer with null Activity")
    }
  }

}
