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
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import net.simonvt.cathode.R;

public class UpcomingTimeDialog extends AppCompatDialogFragment {

  public interface UpcomingTimeSelectedListener {

    void onUpcomingTimeSelected(UpcomingTime value);
  }

  public static final String TAG = "net.simonvt.cathode.settings.UpcomingTimeDialog";

  private static final String ARG_SELECTED =
      "net.simonvt.cathode.settings.UpcomingTimeDialog.selected";

  private UpcomingTime selected;

  private UpcomingTimeSelectedListener listener;

  public static UpcomingTimeDialog newInstance(UpcomingTime selected) {
    UpcomingTimeDialog dialog = new UpcomingTimeDialog();

    Bundle args = new Bundle();
    args.putSerializable(ARG_SELECTED, selected);
    dialog.setArguments(args);

    return dialog;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getTargetFragment() != null) {
      listener = (UpcomingTimeSelectedListener) getTargetFragment();
    } else {
      listener = (UpcomingTimeSelectedListener) getActivity();
    }

    Bundle args = getArguments();
    selected = (UpcomingTime) args.getSerializable(ARG_SELECTED);
  }

  @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
    final Resources resources = getResources();
    final UpcomingTime[] values = UpcomingTime.values();
    final int length = values.length;
    final CharSequence[] cs = new CharSequence[length];
    int selected = 0;
    for (int i = 0; i < length; i++) {
      UpcomingTime value = values[i];
      cs[i] = resources.getString(value.getStringRes());
      if (this.selected == value) {
        selected = i;
      }
    }

    return new AlertDialog.Builder(getActivity()).setTitle(R.string.preference_upcoming_time)
        .setSingleChoiceItems(cs, selected, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int which) {
            listener.onUpcomingTimeSelected(values[which]);

            dialog.dismiss();
          }
        })
        .create();
  }
}
