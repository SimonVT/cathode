package net.simonvt.cathode.api

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class ApiInterceptor(val settings: TraktSettings) : Interceptor {

  @Throws(IOException::class)
  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
      .newBuilder()
      .addHeader(HEADER_API_KEY, settings.clientId)
      .addHeader(HEADER_API_VERSION, "2")
      .build()
    return chain.proceed(request)
  }

  companion object {
    const val HEADER_API_KEY = "trakt-api-key"
    const val HEADER_API_VERSION = "trakt-api-version"
  }
}
