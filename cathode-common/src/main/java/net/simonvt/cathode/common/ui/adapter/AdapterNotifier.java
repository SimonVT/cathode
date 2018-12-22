/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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

package net.simonvt.cathode.common.ui.adapter;

import java.util.ArrayList;
import java.util.List;
import timber.log.Timber;

public class AdapterNotifier {

  private class Item {

    long id;

    int viewType;

    long lastModified;

    Item(long id, int viewType, long lastModified) {
      this.id = id;
      this.viewType = viewType;
      this.lastModified = lastModified;
    }
  }

  private BaseCursorAdapter adapter;

  private List<Item> items;

  public AdapterNotifier(BaseCursorAdapter adapter) {
    this.adapter = adapter;
  }

  private int indexOf(List<Item> items, long id, int viewType) {
    for (int i = 0, count = items.size(); i < count; i++) {
      Item item = items.get(i);
      if (id == item.id && viewType == item.viewType) {
        return i;
      }
    }

    return -1;
  }

  public void notifyChanged() {
    List<Item> oldItems = items;
    final int itemCount = adapter.getItemCount();
    List<Item> newItems = new ArrayList<>();
    for (int position = 0; position < itemCount; position++) {
      newItems.add(new Item(adapter.getItemId(position), adapter.getItemViewType(position),
          adapter.getLastModified(position)));
    }

    if (oldItems == null) {
      notifyItemRangeInserted(0, itemCount);
    } else {
      final int oldItemCount = oldItems.size();
      final int newItemCount = newItems.size();

      for (int i = oldItemCount - 1; i >= 0; i--) {
        Item item = oldItems.get(i);
        final int newPos = indexOf(newItems, item.id, item.viewType);
        if (newPos == -1) {
          notifyItemRemoved(i);
          oldItems.remove(i);
        }
      }

      // Fill oldItems, in case one is moved to the end of the list later.
      if (oldItems.size() < newItemCount) {
        for (int i = oldItems.size(); i < newItemCount; i++) {
          oldItems.add(new Item(Long.MIN_VALUE, Integer.MIN_VALUE, Long.MIN_VALUE));
        }
      }

      for (int newPos = 0; newPos < newItemCount; newPos++) {
        Item newItem = newItems.get(newPos);
        final int oldPos = indexOf(oldItems, newItem.id, newItem.viewType);
        if (oldPos == -1) {
          notifyItemInserted(newPos);
          oldItems.add(newPos, new Item(Long.MIN_VALUE, Integer.MIN_VALUE, Long.MIN_VALUE));
        } else if (newPos != oldPos) {
          notifyItemMoved(oldPos, newPos);
          oldItems.remove(oldPos);
          oldItems.add(newPos, new Item(Long.MIN_VALUE, Integer.MIN_VALUE, Long.MIN_VALUE));
        }

        if (oldPos != -1) {
          Item oldItem = oldItems.get(oldPos);
          if (newItem.lastModified != oldItem.lastModified) {
            notifyItemChanged(newPos);
          }
        }
      }
    }

    items = newItems;
  }

  public final void notifyItemChanged(int position) {
    Timber.d("notifyItemChanged: %d", position);
    adapter.notifyItemChanged(position);
  }

  public final void notifyItemRangeChanged(int positionStart, int itemCount) {
    adapter.notifyItemRangeChanged(positionStart, itemCount);
  }

  public final void notifyItemInserted(int position) {
    Timber.d("notifyItemInserted: %d", position);
    adapter.notifyItemRangeInserted(position, 1);
  }

  public final void notifyItemMoved(int fromPosition, int toPosition) {
    Timber.d("notifyItemMoved: %d - %d", fromPosition, toPosition);
    adapter.notifyItemMoved(fromPosition, toPosition);
  }

  public final void notifyItemRangeInserted(int positionStart, int itemCount) {
    Timber.d("notifyItemRangeInserted: %d", positionStart);
    adapter.notifyItemRangeInserted(positionStart, itemCount);
  }

  public final void notifyItemRemoved(int position) {
    Timber.d("notifyItemRemoved: %d", position);
    adapter.notifyItemRangeRemoved(position, 1);
  }

  public final void notifyItemRangeRemoved(int positionStart, int itemCount) {
    Timber.d("notifyItemRangeRemoved: %d", positionStart);
    adapter.notifyItemRangeRemoved(positionStart, itemCount);
  }
}
