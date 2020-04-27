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

import dagger.Lazy
import net.simonvt.cathode.api.body.TokenRequest
import net.simonvt.cathode.api.enumeration.GrantType
import net.simonvt.cathode.api.service.AuthorizationService
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber
import java.io.IOException

class TraktAuthenticator(
  private val settings: TraktSettings,
  private val authService: Lazy<AuthorizationService>
) : Authenticator {

  @Volatile
  private var refreshingToken: Boolean = false
    get() = synchronized(this) {
      return field
    }
    set(value) = synchronized(this) {
      field = value
    }

  @Throws(IOException::class)
  override fun authenticate(route: Route?, response: Response): Request? {
    synchronized(TraktAuthenticator::class.java) {
      if (!settings.isLinked) {
        return null
      }

      if (responseCount(response) >= 2) {
        Timber.d("Failed 2 times, giving up")
        return null
      }

      val credential = "Bearer " + settings.accessToken!!
      if (credential == response.request.header("Authorization")) {
        val refreshToken = settings.refreshToken
        if (refreshToken == null) {
          Timber.d("Refresh token is null, giving up")
          return null
        }

        val newToken = refreshToken() ?: return null

        return response.request
          .newBuilder()
          .header("Authorization", "Bearer $newToken")
          .build()
      }

      return null
    }
  }

  private fun refreshToken(): String? {
    synchronized(this) {
      if (!refreshingToken) {
        if (!settings.isTokenExpired) {
          Timber.d("Token still valid")
          return settings.accessToken
        }

        val refreshToken = settings.refreshToken ?: return null

        try {
          refreshingToken = true

          val tokenRequest = TokenRequest(
            null, refreshToken, settings.clientId, settings.secret,
            settings.redirectUrl, GrantType.REFRESH_TOKEN
          )

          Timber.d("Getting new tokens, with refresh token: %s", refreshToken)
          val call = authService.get().getToken(tokenRequest)
          val response = call.execute()
          if (response.isSuccessful) {
            val token = response.body()
            settings.updateTokens(token!!)
            return token.access_token
          } else {
            if (response.code() == 401) {
              settings.clearRefreshToken()
              return null
            }

            val message = "Code: " + response.code()
            Timber.e(TokenRefreshFailedException(message), "Unable to get token")
          }
        } catch (e: IOException) {
          Timber.d(e, "Unable to get new tokens")
        } finally {
          refreshingToken = false
        }
      }
    }

    return null
  }

  private fun responseCount(response: Response): Int {
    var result = 1
    var priorResponse = response.priorResponse
    while (priorResponse != null) {
      result++
      priorResponse = priorResponse.priorResponse
    }
    return result
  }

  class TokenRefreshFailedException(message: String) : Exception(message)
}
