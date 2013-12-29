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
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.R;

public class AboutDialog extends DialogFragment {

  private static final String DIALOG_LICENSES =
      "net.simonvt.cathode.ui.dialog.AboutDialog.licenses";

  @InjectView(R.id.version) TextView version;
  @InjectView(R.id.licenses) View licenses;
  @InjectView(R.id.sourceCode) View sourceCode;

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.dialog_about, container, false);
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    ButterKnife.inject(this, view);
    getDialog().setTitle(R.string.app_name);

    version.setText(getString(R.string.version_x, BuildConfig.VERSION_NAME));

    licenses.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        new LicensesDialog().show(getFragmentManager(), DIALOG_LICENSES);
      }
    });
    sourceCode.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        Uri uri = Uri.parse(getString(R.string.sourceUrl));
        Intent i = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(i);
      }
    });
  }

  @Override public void onDestroyView() {
    ButterKnife.reset(this);
    super.onDestroyView();
  }
}
