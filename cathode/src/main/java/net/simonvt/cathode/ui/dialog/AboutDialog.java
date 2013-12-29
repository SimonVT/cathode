/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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
package net.simonvt.cathode.ui.dialog;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.R;

public class AboutDialog extends DialogFragment {

  private static final String DIALOG_LICENSES =
      "net.simonvt.cathode.ui.dialog.AboutDialog.licenses";

  @InjectView(R.id.version) TextView version;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setStyle(STYLE_NO_TITLE, getTheme());
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.dialog_about, container, false);
  }

  @Override public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    ButterKnife.inject(this, view);
    version.setText(BuildConfig.VERSION_NAME);
  }

  @OnClick(R.id.licenses)
  public void showLicenses() {
    new LicensesDialog().show(getFragmentManager(), DIALOG_LICENSES);
  }

  @OnClick({
      R.id.logo, R.id.gplus, R.id.github, R.id.source
  })
  public void openUrl(View v) {
    switch (v.getId()) {
      case R.id.logo:
        openUrl("https://play.google.com/store/apps/details?id=net.simonvt.cathode");
        break;
      case R.id.gplus:
        openUrl("https://plus.google.com/+SimonVigTherkildsen/posts");
        break;
      case R.id.github:
        openUrl("https://github.com/SimonVT");
        break;
      case R.id.source:
        openUrl("https://github.com/SimonVT/cathode");
        break;
    }
  }

  @Override public void onDestroyView() {
    ButterKnife.reset(this);
    super.onDestroyView();
  }

  private void openUrl(String url) {
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    startActivity(intent);
  }
}
