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

import android.app.Dialog;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dagger.android.support.AndroidSupportInjection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.DatabaseContract.ListItemColumns;
import net.simonvt.cathode.provider.DatabaseContract.ListsColumns;
import net.simonvt.cathode.sync.scheduler.ListsTaskScheduler;
import net.simonvt.schematic.Cursors;

public class ListsDialog extends DialogFragment {

  private static class Item {

    long listId;

    String listName;

    boolean checked;

    Item(long listId, String listName) {
      this.listId = listId;
      this.listName = listName;
    }
  }

  private static final String ARG_TYPE = "net.simonvt.cathode.ui.lists.ListsDialog.itemType";
  private static final String ARG_ID = "net.simonvt.cathode.ui.lists.ListsDialog.itemId";

  static final String[] LISTS_PROJECTION = {
      ListsColumns.ID, ListsColumns.TRAKT_ID, ListsColumns.NAME,
  };

  static final String[] LIST_ITEM_PROJECTION = {
      ListItemColumns.ID, ListItemColumns.LIST_ID,
  };

  @Inject ListsTaskScheduler listScheduler;

  private int itemType;

  private long itemId;

  private ListsDialogViewModel viewModel;

  private Cursor listsCursor;
  private Cursor listItemCursor;

  private List<Item> lists = new ArrayList<>();

  private Unbinder unbinder;

  @BindView(R.id.container) LinearLayout container;

  private View loading;
  private View empty;

  public static ListsDialog newInstance(int itemType, long itemId) {
    ListsDialog dialog = new ListsDialog();

    Bundle args = new Bundle();
    args.putInt(ARG_TYPE, itemType);
    args.putLong(ARG_ID, itemId);
    dialog.setArguments(args);

    return dialog;
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    AndroidSupportInjection.inject(this);

    Bundle args = getArguments();
    itemType = args.getInt(ARG_TYPE);
    itemId = args.getLong(ARG_ID);

    viewModel = ViewModelProviders.of(this).get(ListsDialogViewModel.class);
    viewModel.setItemTypeAndId(itemType, itemId);
    viewModel.getList().observe(this, new Observer<Cursor>() {
      @Override public void onChanged(Cursor cursor) {
        setListsCursor(cursor);
      }
    });
    viewModel.getListItems().observe(this, new Observer<Cursor>() {
      @Override public void onChanged(Cursor cursor) {
        setListItemCursor(cursor);
      }
    });
  }

  @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
    Dialog dialog = super.onCreateDialog(savedInstanceState);
    dialog.setTitle(R.string.action_list_add);
    return dialog;
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    View view = inflater.inflate(R.layout.dialog_lists, container, false);
    LinearLayout listsContainer = view.findViewById(R.id.container);
    loading = inflater.inflate(R.layout.dialog_lists_loading, listsContainer, false);
    empty = inflater.inflate(R.layout.dialog_lists_empty, listsContainer, false);
    return view;
  }

  @Override public void onViewCreated(View view, @Nullable Bundle inState) {
    super.onViewCreated(view, inState);
    unbinder = ButterKnife.bind(this, view);
    updateList();
  }

  @Override public void onDestroyView() {
    unbinder.unbind();
    unbinder = null;
    loading = null;
    empty = null;
    super.onDestroyView();
  }

  private void setListsCursor(Cursor listsCursor) {
    this.listsCursor = listsCursor;
    updateList();
  }

  private void setListItemCursor(Cursor listItemCursor) {
    this.listItemCursor = listItemCursor;
    updateList();
  }

  private void updateList() {
    if (getView() == null) {
      return;
    }

    container.removeAllViews();

    if (listsCursor == null || listItemCursor == null) {
      container.addView(loading);
      return;
    }

    if (listsCursor.getCount() == 0) {
      container.addView(empty);
      return;
    }

    listsCursor.moveToPosition(-1);
    while (listsCursor.moveToNext()) {
      final long id = Cursors.getLong(listsCursor, ListsColumns.ID);
      final String name = Cursors.getString(listsCursor, ListsColumns.NAME);
      Item item = getItem(id);
      if (item == null) {
        item = new Item(id, name);
        lists.add(item);
      } else {
        item.listName = name;
      }
      item.checked = false;
    }

    Collections.sort(lists, comparator);

    listItemCursor.moveToPosition(-1);
    while (listItemCursor.moveToNext()) {
      final long listId = Cursors.getLong(listItemCursor, ListItemColumns.LIST_ID);
      getItem(listId).checked = true;
    }

    for (final Item item : lists) {
      View v = createView(item.listId, item.listName, item.checked);
      container.addView(v);
    }
  }

  Comparator<Item> comparator = new Comparator<Item>() {
    @Override public int compare(Item lhs, Item rhs) {
      return lhs.listName.compareToIgnoreCase(rhs.listName);
    }
  };

  private Item getItem(long id) {
    for (Item item : lists) {
      if (id == item.listId) {
        return item;
      }
    }

    return null;
  }

  private View createView(final long listId, String listName, boolean checked) {
    View v =
        LayoutInflater.from(getActivity()).inflate(R.layout.row_dialog_lists, container, false);

    final TextView name = v.findViewById(R.id.name);
    final CheckBox checkBox = v.findViewById(R.id.checkBox);

    name.setText(listName);
    checkBox.setChecked(checked);

    v.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        if (checkBox.isChecked()) {
          checkBox.setChecked(false);
          listScheduler.removeItem(listId, itemType, itemId);
        } else {
          checkBox.setChecked(true);
          listScheduler.addItem(listId, itemType, itemId);
        }
      }
    });

    return v;
  }
}
