package live.stream.theq.theqkit.exception

import java.lang.IllegalStateException

/**
 * Signals that a method on TheQKit has been invoked before initialization.
 *
 * TheQKit must be initialized by calling [live.stream.theq.theqkit.TheQKit.init] before other
 * methods on TheQKit may be invoked. This Exception indicates that this requirement was violated.
 */
class QKitInitializationException internal constructor(
  message: String = "TheQKit.init(config) must be called before invoking other TheQKit methods."
) : IllegalStateException(message)
