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

package net.simonvt.cathode.settings.login;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.Authorization;
import net.simonvt.cathode.ui.BaseActivity;
import net.simonvt.cathode.util.Intents;

public class LoginActivity extends BaseActivity {

  static final String HOST_OAUTH = "oauth";
  static final String PATH_AUTHORIZE = "authorize";
  static final String QUERY_CODE = "code";

  private static final int REQUEST_OAUTH = 1;

  @BindView(R.id.login_in_app) View loginInApp;

  private Intent browserIntent;

  private boolean browserAvailable = true;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    CathodeApp.inject(this);

    setContentView(R.layout.activity_login);
    ButterKnife.bind(this);

    browserIntent = new Intent(Intent.ACTION_VIEW);
    browserIntent.setData(Uri.parse(
        Authorization.getOAuthUri(BuildConfig.TRAKT_CLIENT_ID, BuildConfig.TRAKT_REDIRECT_URL)));

    if (!Intents.isAvailable(this, browserIntent)) {
      browserAvailable = false;

      loginInApp.setVisibility(View.GONE);
    }

    onNewIntent(getIntent());
  }

  @Override protected void onNewIntent(Intent intent) {
    Uri uri = intent.getData();
    if (uri != null) {
      String host = uri.getHost();
      if (HOST_OAUTH.equals(host)) {
        String path = uri.getPathSegments().get(0);
        if (PATH_AUTHORIZE.equals(path)) {
          String code = uri.getQueryParameter(QUERY_CODE);
          if (!TextUtils.isEmpty(code)) {
            onCodeReceived(code);
          }
        }
      }
    }
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_OAUTH && resultCode == RESULT_OK) {
      final String code = data.getStringExtra(QUERY_CODE);
      onCodeReceived(code);
    }
  }

  private void onCodeReceived(String code) {
    Intent result = new Intent(this, TokenActivity.class);
    result.putExtra(TokenActivity.EXTRA_CODE, code);
    startActivity(result);
    finish();
  }

  @OnClick(R.id.login_in_app) void onInAppClicked() {
    Intent authorize = new Intent(LoginActivity.this, OauthWebViewActivity.class);
    startActivityForResult(authorize, REQUEST_OAUTH);
  }

  @OnClick(R.id.login) void onLoginClicked() {
    if (browserAvailable) {
      startActivity(browserIntent);
    } else {
      onInAppClicked();
    }
  }
}
