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
import android.text.TextUtils
import android.view.View
import net.simonvt.cathode.BuildConfig
import net.simonvt.cathode.R
import net.simonvt.cathode.api.Authorization
import net.simonvt.cathode.common.util.Intents
import net.simonvt.cathode.databinding.ActivityLoginBinding
import net.simonvt.cathode.ui.BaseActivity

class LoginActivity : BaseActivity() {

  private lateinit var binding: ActivityLoginBinding

  private var task: Int = 0

  private lateinit var browserIntent: Intent

  private var browserAvailable = true

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    val intent = intent
    task = intent.getIntExtra(EXTRA_TASK, TASK_LOGIN)

    binding = ActivityLoginBinding.inflate(layoutInflater)
    setContentView(binding.root)

    binding.toolbarInclude.toolbar.setTitle(R.string.login_title)
    binding.toolbarInclude.toolbar.setNavigationIcon(R.drawable.ic_action_cancel_24dp)
    binding.toolbarInclude.toolbar.setNavigationOnClickListener { finish() }

    binding.loginInApp.setOnClickListener { onInAppClicked() }
    binding.login.setOnClickListener(loginClickListener)

    browserIntent = Intent(Intent.ACTION_VIEW)
    browserIntent.data = Uri.parse(
      Authorization.getOAuthUri(BuildConfig.TRAKT_CLIENT_ID, BuildConfig.TRAKT_REDIRECT_URL)
    )

    if (!Intents.isAvailable(this, browserIntent)) {
      browserAvailable = false
      binding.loginInApp.visibility = View.GONE
    }

    onNewIntent(getIntent())
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    val uri = intent.data
    if (uri != null) {
      val host = uri.host
      if (HOST_OAUTH == host) {
        val path = uri.pathSegments[0]
        if (PATH_AUTHORIZE == path) {
          val code = uri.getQueryParameter(QUERY_CODE)
          if (!TextUtils.isEmpty(code)) {
            onCodeReceived(code)
          }
        }
      }
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == REQUEST_OAUTH && resultCode == Activity.RESULT_OK) {
      val code = data!!.getStringExtra(QUERY_CODE)
      onCodeReceived(code)
    }
  }

  private fun onCodeReceived(code: String?) {
    val result = Intent(this, TokenActivity::class.java)
    result.putExtra(EXTRA_TASK, task)
    result.putExtra(TokenActivity.EXTRA_CODE, code)
    startActivity(result)
    finish()
  }

  private fun onInAppClicked() {
    val authorize = Intent(this@LoginActivity, OauthWebViewActivity::class.java)
    startActivityForResult(authorize, REQUEST_OAUTH)
  }

  private val loginClickListener = View.OnClickListener {
    if (browserAvailable) {
      startActivity(browserIntent)
    } else {
      onInAppClicked()
    }
  }

  companion object {

    const val EXTRA_TASK = "net.simonvt.cathode.settings.login.LoginActivity.task"

    const val TASK_LOGIN = 0
    const val TASK_TOKEN_REFRESH = 1
    const val TASK_LINK = 2

    private const val HOST_OAUTH = "oauth"
    private const val PATH_AUTHORIZE = "authorize"
    const val QUERY_CODE = "code"

    private const val REQUEST_OAUTH = 1
  }
}
