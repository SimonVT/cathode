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
package net.simonvt.cathode.settings;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import java.util.ArrayList;
import java.util.List;
import net.simonvt.cathode.R;

public class LicensesDialog extends AppCompatDialogFragment {

  @Override public Dialog onCreateDialog(@Nullable Bundle inState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()) //
        .setTitle(R.string.licenses).setAdapter(new LicenseAdapter(), null);

    return builder.create();
  }

  private static final class License {
    int library;

    int license;

    private License(int library, int license) {
      this.library = library;
      this.license = license;
    }
  }

  private final class LicenseAdapter extends BaseAdapter {

    List<License> licenses = new ArrayList<>();

    private LicenseAdapter() {
      licenses.add(new License(R.string.license_android_support_library,
          R.string.license_android_support_library_text));
      licenses.add(new License(R.string.license_gson, R.string.license_gson_text));
      licenses.add(new License(R.string.license_dagger, R.string.license_dagger_text));
      licenses.add(new License(R.string.license_okhttp, R.string.license_okhttp_text));
      licenses.add(new License(R.string.license_picasso, R.string.license_picasso_text));
      licenses.add(new License(R.string.license_picasso_downloader,
          R.string.license_picasso_downloader_text));
      licenses.add(new License(R.string.license_retrofit, R.string.license_retrofit_text));
      licenses.add(new License(R.string.license_timber, R.string.license_timber_text));
      licenses.add(new License(R.string.license_tmdb_java, R.string.license_tmdb_java_text));
      licenses.add(new License(R.string.license_schematic, R.string.license_schematic_text));
    }

    @Override public int getCount() {
      return licenses.size();
    }

    @Override public Object getItem(int position) {
      return licenses.get(position);
    }

    @Override public long getItemId(int position) {
      return position;
    }

    @Override public boolean areAllItemsEnabled() {
      return false;
    }

    @Override public boolean isEnabled(int position) {
      return false;
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
      View v = convertView;
      License item = licenses.get(position);

      if (v == null) {
        v = LayoutInflater.from(getActivity()).inflate(R.layout.row_license, parent, false);
      }

      TextView library = v.findViewById(R.id.library);
      library.setText(item.library);

      TextView license = v.findViewById(R.id.license);
      license.setText(item.license);

      return v;
    }
  }
}
