/*
 * Copyright (C) 2015 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.simonvt.cathode.api

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class AuthInterceptor(val settings: TraktSettings) : Interceptor {

  @Throws(IOException::class)
  override fun intercept(chain: Interceptor.Chain): Response {
    if (settings.isLinked) {
      val request = chain.request()
        .newBuilder()
        .addHeader(HEADER_AUTHORIZATION, "Bearer " + settings.accessToken)
        .build()
      return chain.proceed(request)
    } else {
      return chain.proceed(chain.request())
    }
  }

  companion object {
    const val HEADER_AUTHORIZATION = "Authorization"
  }
}
