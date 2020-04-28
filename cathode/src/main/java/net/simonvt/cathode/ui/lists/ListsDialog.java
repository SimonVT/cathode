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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.databinding.DialogListsBinding;
import net.simonvt.cathode.databinding.DialogListsEmptyBinding;
import net.simonvt.cathode.databinding.DialogListsLoadingBinding;
import net.simonvt.cathode.entity.UserList;
import net.simonvt.cathode.sync.scheduler.ListsTaskScheduler;
import net.simonvt.cathode.ui.lists.DialogListItemListMapper.DialogListItem;
import timber.log.Timber;

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

  private ListsTaskScheduler listScheduler;

  private ItemType itemType;

  private long itemId;

  private ListsDialogViewModel viewModel;

  private List<UserList> userLists;
  private List<DialogListItem> listItems;

  private List<Item> lists = new ArrayList<>();

  private DialogListsBinding binding;
  private DialogListsLoadingBinding loadingBinding;
  private DialogListsEmptyBinding emptyBinding;

  public static Bundle getArgs(ItemType itemType, long itemId) {
    Bundle args = new Bundle();
    args.putSerializable(ARG_TYPE, itemType);
    args.putLong(ARG_ID, itemId);
    return args;
  }

  @Inject public ListsDialog(ListsTaskScheduler listScheduler) {
    this.listScheduler = listScheduler;
  }

  @Override public void onCreate(@Nullable Bundle inState) {
    super.onCreate(inState);
    Bundle args = getArguments();
    itemType = (ItemType) args.getSerializable(ARG_TYPE);
    itemId = args.getLong(ARG_ID);

    viewModel = new ViewModelProvider(this).get(ListsDialogViewModel.class);
    viewModel.setItemTypeAndId(itemType, itemId);
    viewModel.getLists().observe(this, new Observer<List<UserList>>() {
      @Override public void onChanged(List<UserList> userLists) {
        setUserLists(userLists);
      }
    });
    viewModel.getListItems().observe(this, new Observer<List<DialogListItem>>() {
      @Override public void onChanged(List<DialogListItem> listItems) {
        setListItems(listItems);
      }
    });
  }

  @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle inState) {
    Dialog dialog = super.onCreateDialog(inState);
    dialog.setTitle(R.string.action_list_add);
    return dialog;
  }

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle inState) {
    binding = DialogListsBinding.inflate(inflater, container, false);
    loadingBinding = DialogListsLoadingBinding.inflate(inflater, binding.container, false);
    emptyBinding = DialogListsEmptyBinding.inflate(inflater, binding.container, false);
    return binding.getRoot();
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle inState) {
    super.onViewCreated(view, inState);
    updateList();
  }

  @Override public void onDestroyView() {
    binding = null;
    loadingBinding = null;
    emptyBinding = null;
    super.onDestroyView();
  }

  private void setUserLists(List<UserList> userLists) {
    Timber.d("setUserLists");
    this.userLists = userLists;
    updateList();
  }

  private void setListItems(List<DialogListItem> listItems) {
    this.listItems = listItems;
    updateList();
  }

  private void updateList() {
    Timber.d("updateList");
    if (getView() == null) {
      return;
    }

    binding.container.removeAllViews();

    if (userLists == null || listItems == null) {
      Timber.d("Something is null");
      binding.container.addView(loadingBinding.getRoot());
      return;
    }

    if (userLists.isEmpty()) {
      binding.container.addView(emptyBinding.getRoot());
      return;
    }

    for (UserList userList : userLists) {
      long id = userList.getId();
      String name = userList.getName();
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

    for (DialogListItem listItem : listItems) {
      long listId = listItem.getListId();
      getItem(listId).checked = true;
    }

    for (final Item item : lists) {
      View v = createView(item.listId, item.listName, item.checked);
      binding.container.addView(v);
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
    View v = LayoutInflater.from(requireContext())
        .inflate(R.layout.row_dialog_lists, binding.container, false);

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
