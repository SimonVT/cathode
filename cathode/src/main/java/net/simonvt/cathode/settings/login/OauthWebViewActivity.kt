/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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
package net.simonvt.cathode.settings.login

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import net.simonvt.cathode.BuildConfig
import net.simonvt.cathode.R
import net.simonvt.cathode.api.Authorization
import net.simonvt.cathode.databinding.ActivityOauthBinding
import timber.log.Timber

class OauthWebViewActivity : AppCompatActivity() {

  private lateinit var binding: ActivityOauthBinding

  private val webViewClient = object : WebViewClient() {
    override fun onReceivedError(
      view: WebView,
      errorCode: Int,
      description: String,
      failingUrl: String
    ) {
    }

    override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {
      if (url != null && url.startsWith(BuildConfig.TRAKT_REDIRECT_URL)) {
        val uri = Uri.parse(url)

        val code = uri.getQueryParameter(LoginActivity.QUERY_CODE)
        Timber.d("We got a code! %s", code)

        val result = Intent()
        result.putExtra(LoginActivity.QUERY_CODE, code)
        setResult(Activity.RESULT_OK, result)
        finish()
        return true
      }

      return false
    }
  }

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    binding = ActivityOauthBinding.inflate(layoutInflater)
    setContentView(binding.root)

    binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24dp)
    binding.toolbar.setNavigationOnClickListener { finish() }
    binding.toolbar.setTitle(R.string.login_title)

    binding.webview.webViewClient = webViewClient
    val cookieManager = CookieManager.getInstance()
    cookieManager.removeAllCookie()
    binding.webview.clearCache(true)

    binding.webview.webChromeClient = object : WebChromeClient() {
      override fun onProgressChanged(view: WebView, newProgress: Int) {
        binding.progressTop.progress = newProgress
        if (newProgress == 100) {
          binding.progressTop.animate().alpha(0.0f).withEndAction { binding.progressTop.visibility = View.GONE }
        } else {
          if (binding.progressTop.visibility == View.GONE) {
            binding.progressTop.visibility = View.VISIBLE
            binding.progressTop.alpha = 0.0f
            binding.progressTop.animate().alpha(1.0f)
          }
        }
      }
    }

    binding.webview.loadUrl(
      Authorization.getOAuthUri(
        BuildConfig.TRAKT_CLIENT_ID,
        BuildConfig.TRAKT_REDIRECT_URL
      )
    )
  }
}
