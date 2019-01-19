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
package net.simonvt.cathode.ui.lists;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;

public class ListDialog extends DialogFragment {

  public interface Callback {
    void onItemSelected(int id);
  }

  public static class Item implements Parcelable {

    int id;

    int text;

    public Item(Parcel in) {
      id = in.readInt();
      text = in.readInt();
    }

    public Item(int id, int text) {
      this.id = id;
      this.text = text;
    }

    public int getId() {
      return id;
    }

    public int getText() {
      return text;
    }

    public int describeContents() {
      return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
      out.writeInt(id);
      out.writeInt(id);
    }

    public static final Parcelable.Creator<Item> CREATOR = new Parcelable.Creator<Item>() {
      public Item createFromParcel(Parcel in) {
        return new Item(in);
      }

      public Item[] newArray(int size) {
        return new Item[size];
      }
    };
  }

  private static final String ARG_TITLE = "net.simonvt.cathode.ui.lists.ListDialog.title";
  private static final String ARG_ITEMS = "net.simonvt.cathode.ui.lists.ListDialog.items";

  public static ListDialog newInstance(int title, ArrayList<Item> items, Fragment target) {
    ListDialog dialog = new ListDialog();
    if (target != null) {
      dialog.setTargetFragment(target, 0);
    }

    Bundle args = new Bundle();
    args.putInt(ARG_TITLE, title);
    args.putParcelableArrayList(ARG_ITEMS, items);
    dialog.setArguments(args);

    return dialog;
  }

  @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
    Callback c;
    Fragment target = getTargetFragment();
    if (target == null) {
      c = (Callback) requireActivity();
    } else {
      c = (Callback) target;
    }
    final Callback callback = c;
    final int title = getArguments().getInt(ARG_TITLE);
    final ArrayList<Item> listItems = getArguments().getParcelableArrayList(ARG_ITEMS);

    CharSequence[] items = new CharSequence[listItems.size()];
    for (int i = 0, size = listItems.size(); i < size; i++) {
      String text = getResources().getString(listItems.get(i).text);
      items[i] = text;
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext()).setItems(items,
        new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int which) {
            callback.onItemSelected(listItems.get(which).getId());
          }
        }).setTitle(title);

    return builder.create();
  }
}
