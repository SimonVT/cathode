package net.simonvt.cathode.actions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import timber.log.Timber

suspend fun <T> Action<T>.invokeSync(params: T) {
  ActionManager.invokeSync(this, params)
}

suspend fun <T> Action<T>.invokeAsync(params: T): Deferred<*> =
  ActionManager.invokeAsync(this, params)

object ActionManager {

  private val job = SupervisorJob()
  private val scope = CoroutineScope(Dispatchers.IO + job)

  private val inFlight = mutableMapOf<String, Deferred<*>>()

  suspend fun <P> invokeSync(action: Action<P>, params: P) {
    val deferred = invokeAsync(action, params)
    deferred.await()
  }

  suspend fun <P> invokeAsync(action: Action<P>, params: P): Deferred<*> {
    val key = action.key(params)
    var deferred: Deferred<Unit>?
    synchronized(inFlight) {
      Timber.d("Invoking action: $key")
      if (inFlight.containsKey(key)) {
        Timber.d("Existing action found: $key")
        return inFlight[key]!!
      }

      Timber.d("Creating action: $key")
      deferred = scope.async(Dispatchers.IO) {
        try {
          action(params)
        } catch (e: ActionFailedException) {
          Timber.d(e, "Action failed: $key")
        } catch (t: Throwable) {
          Timber.e(t, "Action failed: $key")
        }
      }
      inFlight[key] = deferred!!
    }

    scope.launch(Dispatchers.IO) {
      try {
        Timber.d("Awaiting action: $key")
        deferred!!.await()
      } catch (t: Throwable) {
        // Handled above
      }

      synchronized(inFlight) {
        Timber.d("Removing action: $key")
        inFlight.remove(key)
      }
    }

    Timber.d("Returning deferred: $key")
    return deferred!!
  }
}
