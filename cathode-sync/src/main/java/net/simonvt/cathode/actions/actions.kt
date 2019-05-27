package net.simonvt.cathode.actions

import net.simonvt.cathode.common.http.requireBody
import net.simonvt.cathode.common.tmdb.TmdbRateLimiter
import retrofit2.Call
import retrofit2.Response
import timber.log.Timber
import java.io.IOException

interface Action<in Params> {
  fun key(params: Params): String
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

class PagedResponse<Params, T> internal constructor(
  private val action: PagedAction<Params, T>,
  val params: Params,
  val page: Int,
  val pageCount: Int,
  val response: List<T>
) {

  fun nextPage(): PagedResponse<Params, T>? {
    return action.nextPage(params, page, pageCount)
  }
}

abstract class PagedAction<Params, T> : ErrorHandlerAction<Params>() {

  internal fun nextPage(params: Params, page: Int, pageCount: Int): PagedResponse<Params, T>? {
    if (stopped || page >= pageCount) {
      return null
    } else {
      try {
        val newPage = page + 1
        val call = getCall(params, newPage)
        val response = call.execute()
        return createPage(params, response, newPage, pageCount)
      } catch (e: IOException) {
        throw ActionFailedException(e)
      }
    }
  }

  private fun createPage(
    params: Params,
    response: Response<List<T>>,
    page: Int,
    pageCount: Int
  ): PagedResponse<Params, T>? {
    if (!response.isSuccessful) {
      if (!isError(response)) {
        return null
      } else {
        throw ActionFailedException()
      }
    }
    val result = response.requireBody()
    return PagedResponse(this, params, page, pageCount, result)
  }

  final override suspend fun invoke(params: Params) {
    Timber.d("Invoking action: %s", javaClass.name)
    try {
      val page = 1
      val call = getCall(params, 1)
      val response = call.execute()

      val headers = response.headers()
      val pageCount = headers.get(HEADER_PAGE_COUNT)?.toIntOrNull() ?: 0

      val pagedResult = createPage(params, response, page, pageCount)
      if (pagedResult != null) {
        handleResponse(params, pagedResult)
      }

      if (!stopped) {
        onDone()
      }
    } catch (e: IOException) {

      throw ActionFailedException(e)
    }
  }

  internal abstract fun getCall(params: Params, page: Int): Call<List<T>>

  internal abstract suspend fun handleResponse(
    params: Params,
    pagedResponse: PagedResponse<Params, T>
  )

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
