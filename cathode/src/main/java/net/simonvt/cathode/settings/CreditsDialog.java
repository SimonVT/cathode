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
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import java.util.ArrayList;
import java.util.List;
import net.simonvt.cathode.R;

public class CreditsDialog extends DialogFragment {

  @Override public Dialog onCreateDialog(Bundle inState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()) //
        .setTitle(R.string.credits).setAdapter(new CreditsAdapter(), null);

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

  private final class CreditsAdapter extends BaseAdapter {

    List<License> credits = new ArrayList<>();

    private CreditsAdapter() {
      credits.add(new License(R.string.credits_trakt, R.string.credits_trakt_text));
      credits.add(new License(R.string.credits_tmdb, R.string.credits_tmdb_text));
    }

    @Override public int getCount() {
      return credits.size();
    }

    @Override public Object getItem(int position) {
      return credits.get(position);
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
      License item = credits.get(position);

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
