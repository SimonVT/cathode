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

public class ShowOffsetDialog extends AppCompatDialogFragment {

  public interface ShowOffsetSelectedListener {

    void onShowOffsetSelected(int offset);
  }

  public static final String TAG = "net.simonvt.cathode.settings.ShowOffsetDialog";

  private static final String ARG_SELECTED =
      "net.simonvt.cathode.settings.ShowOffsetDialog.selected";

  private int selected;

  private ShowOffsetSelectedListener listener;

  public static ShowOffsetDialog newInstance(int selected) {
    ShowOffsetDialog dialog = new ShowOffsetDialog();

    Bundle args = new Bundle();
    args.putInt(ARG_SELECTED, selected);
    dialog.setArguments(args);

    return dialog;
  }

  @Override public void onCreate(@Nullable Bundle inState) {
    super.onCreate(inState);
    if (getTargetFragment() != null) {
      listener = (ShowOffsetSelectedListener) getTargetFragment();
    } else {
      listener = (ShowOffsetSelectedListener) getActivity();
    }

    Bundle args = getArguments();
    selected = args.getInt(ARG_SELECTED);
  }

  @Override public Dialog onCreateDialog(@Nullable Bundle inState) {
    final Resources resources = getResources();
    final String[] values = resources.getStringArray(R.array.setting_shows_offset);
    final int length = values.length;

    int selectedPosition = 0;

    for (int i = 0; i < length; i++) {
      final String value = values[i].replace("+", "");
      final int intValue = Integer.valueOf(value);
      if (intValue == selected) {
        selectedPosition = i;
        break;
      }
    }

    return new AlertDialog.Builder(getActivity()).setTitle(R.string.preference_shows_offset)
        .setSingleChoiceItems(values, selectedPosition, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int which) {
            String val = values[which];
            val = val.replace("+", "");
            listener.onShowOffsetSelected(Integer.valueOf(val));

            dialog.dismiss();
          }
        })
        .create();
  }
}
