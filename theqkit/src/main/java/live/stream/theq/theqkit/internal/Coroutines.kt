package live.stream.theq.theqkit.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal fun launchAsync(block: suspend CoroutineScope.() -> Unit): Job {
  return GlobalScope.launch(context = Dispatchers.Main, block = block)
}
