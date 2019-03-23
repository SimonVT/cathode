package net.simonvt.cathode.actions

import net.simonvt.cathode.common.http.requireBody
import net.simonvt.cathode.common.tmdb.TmdbRateLimiter
import retrofit2.Call
import retrofit2.Response
import timber.log.Timber
import java.io.IOException

interface Action<in Params> {
  suspend operator fun invoke(params: Params)
}

abstract class ErrorHandlerAction<Params> : Action<Params> {

  var stopped = false

  internal open fun isError(response: Response<*>): Boolean {
    return ErrorHandler.isError(response)
  }
}

abstract class CallAction<Params, T> : ErrorHandlerAction<Params>() {

  override suspend fun invoke(params: Params) {
    Timber.d("Invoking action: %s", javaClass.name)
    try {
      val response = getCall(params).execute()

      if (stopped) {
        return
      }

      if (response.isSuccessful) {
        handleResponse(params, response.requireBody())
      } else {
        if (isError(response)) {
          throw ActionFailedException()
        }
      }
    } catch (e: IOException) {
      Timber.d(e, "Action failed")
      throw ActionFailedException(e)
    }
  }

  internal abstract fun getCall(params: Params): Call<T>

  abstract suspend fun handleResponse(params: Params, response: T)
}

abstract class OptionalBodyCallAction<Params, T> : ErrorHandlerAction<Params>() {

  override suspend fun invoke(params: Params) {
    Timber.d("Invoking action: %s", javaClass.name)
    try {
      val response = getCall(params).execute()

      if (stopped) {
        return
      }

      if (response.isSuccessful) {
        handleResponse(params, response.body())
      } else {
        if (isError(response)) {
          throw ActionFailedException()
        }
      }
    } catch (e: IOException) {
      Timber.d(e, "Action failed")
      throw ActionFailedException(e)
    }
  }

  internal abstract fun getCall(params: Params): Call<T>

  abstract suspend fun handleResponse(params: Params, response: T?)
}

abstract class TmdbCallAction<Params, T> : CallAction<Params, T>() {

  override suspend fun invoke(params: Params) {
    Timber.d("Invoking action: %s", javaClass.name)
    TmdbRateLimiter.acquire()
    super.invoke(params)
  }
}

abstract class PagedAction<Params, T> : ErrorHandlerAction<Params>() {

  final override suspend fun invoke(params: Params) {
    Timber.d("Invoking action: %s", javaClass.name)
    try {
      var page = 1
      var pageCount = 0
      do {
        val call = getCall(params, page)
        val response = call.execute()
        if (!response.isSuccessful) {
          if (!isError(response)) {
            return
          } else {
            throw ActionFailedException()
          }
        }

        val headers = response.headers()
        val pageCountStr = headers.get(HEADER_PAGE_COUNT)
        if (pageCountStr != null) {
          pageCount = Integer.valueOf(pageCountStr)
        }

        val result = response.body()!!
        handleResponse(params, page, result)

        page++
      } while (page <= pageCount && !stopped)

      if (!stopped) {
        onDone()
      }
    } catch (e: IOException) {
      Timber.d(e, "Action failed")
      throw ActionFailedException(e)
    }
  }

  internal abstract fun getCall(params: Params, page: Int): Call<List<T>>

  internal abstract suspend fun handleResponse(params: Params, page: Int, response: List<T>)

  internal open fun onDone() {}

  companion object {
    internal const val HEADER_PAGE_COUNT = "x-pagination-page-count"
  }
}

class ActionFailedException : Exception {

  constructor() : super()

  constructor(detailMessage: String) : super(detailMessage)

  constructor(detailMessage: String, throwable: Throwable) : super(detailMessage, throwable)

  constructor(throwable: Throwable) : super(throwable)
}
