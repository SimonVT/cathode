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

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.R;
import net.simonvt.cathode.util.Intents;

public class AboutDialog extends DialogFragment {

  private static final String DIALOG_LICENSES =
      "net.simonvt.cathode.ui.dialog.AboutDialog.licenses";

  private Unbinder unbinder;

  @BindView(R.id.version) TextView version;

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setStyle(STYLE_NO_TITLE, getTheme());
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.dialog_about, container, false);
  }

  @Override public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    unbinder = ButterKnife.bind(this, view);
    version.setText(BuildConfig.VERSION_NAME);
  }

  @Override public void onDestroyView() {
    unbinder.unbind();
    unbinder = null;
    super.onDestroyView();
  }

  @OnClick(R.id.licenses) public void showLicenses() {
    new LicensesDialog().show(getFragmentManager(), DIALOG_LICENSES);
  }

  @OnClick({
      R.id.version, R.id.gplus, R.id.github, R.id.source
  }) public void openUrl(View v) {
    switch (v.getId()) {
      case R.id.version:
        Intents.openUrl(getActivity(), getString(R.string.play_store_url));
        break;
      case R.id.gplus:
        Intents.openUrl(getActivity(), getString(R.string.dev_gplus));
        break;
      case R.id.github:
        Intents.openUrl(getActivity(), getString(R.string.dev_github));
        break;
      case R.id.source:
        Intents.openUrl(getActivity(), getString(R.string.source_url));
        break;
    }
  }
}
