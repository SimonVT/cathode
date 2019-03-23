package net.simonvt.cathode.actions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import timber.log.Timber

object ActionManager {

  private val job = SupervisorJob()
  private val scope = CoroutineScope(Dispatchers.IO + job)

  private val inFlight = mutableMapOf<String, Deferred<*>>()

  suspend fun <P> invokeSync(key: String, action: Action<P>, params: P) {
    val deferred = invokeAsync(key, action, params)
    deferred.await()
  }

  suspend fun <P> invokeAsync(key: String, action: Action<P>, params: P): Deferred<*> {
    var deferred: Deferred<Unit>?
    synchronized(inFlight) {
      Timber.d("Invoking action: $key")
      if (inFlight.containsKey(key)) {
        Timber.d("Existing action found: $key")
        return inFlight[key]!!
      }

      Timber.d("Creating action: $key")
      deferred = scope.async(Dispatchers.IO) { action(params) }
      inFlight[key] = deferred!!
    }

    scope.launch(Dispatchers.IO) {
      try {
        Timber.d("Awaiting action: $key")
        deferred!!.await()
      } catch (t: Throwable) {
        Timber.d(t, "Action failed: $key")
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
