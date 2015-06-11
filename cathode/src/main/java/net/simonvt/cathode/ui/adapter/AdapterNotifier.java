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

package net.simonvt.cathode.ui.adapter;

import java.util.ArrayList;
import java.util.List;

public class AdapterNotifier {

  private class Item {

    long id;

    long lastModified;

    public Item(long id, long lastModified) {
      this.id = id;
      this.lastModified = lastModified;
    }
  }

  private BaseAdapter adapter;

  private List<Item> items;

  public AdapterNotifier(BaseAdapter adapter) {
    this.adapter = adapter;
  }

  private int indexOf(List<Item> items, long id) {
    for (int i = 0, count = items.size(); i < count; i++) {
      Item item = items.get(i);
      if (id == item.id) {
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
      newItems.add(new Item(adapter.getItemId(position), adapter.getLastModified(position)));
    }

    if (oldItems == null) {
      notifyItemRangeInserted(0, itemCount);
    } else {
      final int oldItemCount = oldItems.size();
      final int newItemCount = newItems.size();

      for (int i = oldItemCount - 1; i >= 0; i--) {
        Item item = oldItems.get(i);
        final int newPos = indexOf(oldItems, item.id);
        if (newPos == -1) {
          notifyItemRemoved(i);
          oldItems.remove(i);
        }
      }

      for (int newPos = 0; newPos < newItemCount; newPos++) {
        Item newItem = newItems.get(newPos);
        final int oldPos = indexOf(oldItems, newItem.id);
        if (oldPos == -1) {
          notifyItemInserted(newPos);
          oldItems.add(newPos, new Item(Long.MIN_VALUE, Long.MIN_VALUE));
        } else if (newPos == oldPos) {
          Item oldItem = oldItems.get(oldPos);
          if (newItem.lastModified != oldItem.lastModified) {
            notifyItemChanged(newPos);
          }
        } else if (newPos != oldPos) {
          notifyItemMoved(oldPos, newPos);
          oldItems.remove(oldPos);
          oldItems.add(newPos, new Item(Long.MIN_VALUE, Long.MIN_VALUE));
        }
      }

      if (oldItemCount > itemCount) {
        final int removeCount = oldItemCount - itemCount;
        notifyItemRangeRemoved(itemCount, removeCount);
      }
    }

    items = newItems;
  }

  public final void notifyItemChanged(int position) {
    adapter.notifyItemRangeChanged(position, 1);
  }

  public final void notifyItemRangeChanged(int positionStart, int itemCount) {
    adapter.notifyItemRangeChanged(positionStart, itemCount);
  }

  public final void notifyItemInserted(int position) {
    adapter.notifyItemRangeInserted(position, 1);
  }

  public final void notifyItemMoved(int fromPosition, int toPosition) {
    adapter.notifyItemMoved(fromPosition, toPosition);
  }

  public final void notifyItemRangeInserted(int positionStart, int itemCount) {
    adapter.notifyItemRangeInserted(positionStart, itemCount);
  }

  public final void notifyItemRemoved(int position) {
    adapter.notifyItemRangeRemoved(position, 1);
  }

  public final void notifyItemRangeRemoved(int positionStart, int itemCount) {
    adapter.notifyItemRangeRemoved(positionStart, itemCount);
  }
}
