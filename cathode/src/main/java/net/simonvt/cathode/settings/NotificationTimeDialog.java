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
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import net.simonvt.cathode.R;

public class NotificationTimeDialog extends DialogFragment {

  public interface NotificationTimeSelectedListener {

    void onNotificationTimeSelected(NotificationTime value);
  }

  public static final String TAG = "net.simonvt.cathode.settings.NotificationTimeDialog";

  private static final String ARG_SELECTED =
      "net.simonvt.cathode.settings.NotificationTimeDialog.selected";

  private NotificationTime selected;

  private NotificationTimeSelectedListener listener;

  public static NotificationTimeDialog newInstance(NotificationTime selected) {
    NotificationTimeDialog dialog = new NotificationTimeDialog();

    Bundle args = new Bundle();
    args.putSerializable(ARG_SELECTED, selected);
    dialog.setArguments(args);

    return dialog;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getTargetFragment() != null) {
      listener = (NotificationTimeSelectedListener) getTargetFragment();
    } else {
      listener = (NotificationTimeSelectedListener) getActivity();
    }

    Bundle args = getArguments();
    selected = (NotificationTime) args.getSerializable(ARG_SELECTED);
  }

  @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
    final Resources resources = getResources();
    final NotificationTime[] values = NotificationTime.values();
    final int length = values.length;
    final CharSequence[] cs = new CharSequence[length];
    int selected = 0;
    for (int i = 0; i < length; i++) {
      NotificationTime value = values[i];
      cs[i] = resources.getString(value.getStringRes());
      if (this.selected == value) {
        selected = i;
      }
    }

    return new AlertDialog.Builder(getActivity()).setTitle(R.string.preference_notification_time)
        .setSingleChoiceItems(cs, selected, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int which) {
            listener.onNotificationTimeSelected(values[which]);

            dialog.dismiss();
          }
        })
        .create();
  }
}
