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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import butterknife.BindView;
import butterknife.ButterKnife;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.Authorization;
import timber.log.Timber;

public class OauthWebViewActivity extends AppCompatActivity {

  @BindView(R.id.toolbar) Toolbar toolbar;

  @BindView(R.id.progress_top) ProgressBar progressBar;

  @BindView(R.id.webview) WebView webView;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_oauth);
    ButterKnife.bind(this);

    toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24dp);
    toolbar.setNavigationOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        finish();
      }
    });
    toolbar.setTitle(R.string.login_title);

    webView.setWebViewClient(webViewClient);
    CookieManager cookieManager = CookieManager.getInstance();
    cookieManager.removeAllCookie();
    webView.clearCache(true);

    webView.setWebChromeClient(new WebChromeClient() {
      @Override public void onProgressChanged(WebView view, int newProgress) {
        progressBar.setProgress(newProgress);
        if (newProgress == 100) {
          progressBar.animate().alpha(0.0f).withEndAction(new Runnable() {
            @Override public void run() {
              progressBar.setVisibility(View.GONE);
            }
          });
        } else {
          if (progressBar.getVisibility() == View.GONE) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setAlpha(0.0f);
            progressBar.animate().alpha(1.0f);
          }
        }
      }
    });

    webView.loadUrl(
        Authorization.getOAuthUri(BuildConfig.TRAKT_CLIENT_ID, BuildConfig.TRAKT_REDIRECT_URL));
  }

  private WebViewClient webViewClient = new WebViewClient() {
    @Override public void onReceivedError(WebView view, int errorCode, String description,
        String failingUrl) {

    }

    @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
      if (url != null && url.startsWith(BuildConfig.TRAKT_REDIRECT_URL)) {
        Uri uri = Uri.parse(url);

        final String code = uri.getQueryParameter(LoginActivity.QUERY_CODE);
        Timber.d("We got a code! %s", code);

        Intent result = new Intent();
        result.putExtra(LoginActivity.QUERY_CODE, code);
        setResult(RESULT_OK, result);
        finish();
        return true;
      }

      return false;
    }
  };
}
