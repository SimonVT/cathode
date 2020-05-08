/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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

import android.content.Context;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public abstract class HeaderAdapter<Type, T extends RecyclerView.ViewHolder>
    extends RecyclerView.Adapter<T> {

  public static class Header<Type> {

    final int headerRes;

    final long headerId;

    public List<Type> items;
    List<Type> pendingItems;

    int size;

    private Header(int headerRes, long headerId) {
      this.headerRes = headerRes;
      this.headerId = headerId;
    }

    void updateSize() {
      final int size = items != null ? items.size() : 0;
      if (size > 0) {
        this.size = size + 1;
      } else {
        this.size = 0;
      }
    }

    void setItems(List<Type> items) {
      this.items = items;
      updateSize();
    }
  }

  public static class Item<Type> {

    boolean isHeader;

    long headerId;

    int headerRes;

    Type item;

    public Item(long headerId, int headerRes) {
      this.headerId = headerId;
      this.headerRes = headerRes;
      isHeader = true;
    }

    public Item(Type item) {
      this.item = item;
      isHeader = false;
    }
  }

  private DiffUtil.ItemCallback<Item<Type>> diffCallback = new DiffUtil.ItemCallback<Item<Type>>() {
    @Override
    public boolean areItemsTheSame(@NonNull Item<Type> oldItem, @NonNull Item<Type> newItem) {
      return HeaderAdapter.this.areItemsTheSame(oldItem, newItem);
    }

    @Override
    public boolean areContentsTheSame(@NonNull Item<Type> oldItem, @NonNull Item<Type> newItem) {
      return HeaderAdapter.this.areContentsTheSame(oldItem, newItem);
    }
  };

  private Context context;

  private AsyncListDiffer<Item<Type>> asyncDiffer = new AsyncListDiffer<>(this, diffCallback);

  private List<Header<Type>> headers = new ArrayList<>();
  private long headerIdOffset;

  public HeaderAdapter(Context context) {
    this.context = context;
  }

  public Context getContext() {
    return context;
  }

  @Override public int getItemCount() {
    return asyncDiffer.getCurrentList().size();
  }

  private List<Item<Type>> getList() {
    return asyncDiffer.getCurrentList();
  }

  public void removeItem(Type item) {
    final int position = getPosition(item);
    if (position > 0) {
      Header<Type> header = getHeader(getHeaderRes(position));
      List<Type> newList = new ArrayList<>(header.items);
      newList.remove(item);
      updateHeaderItems(header.headerRes, newList);
    }
  }

  private int getPosition(Type item) {
    List<Item<Type>> items = getList();
    for (int i = 0; i < items.size(); i++) {
      Item<Type> listItem = items.get(i);
      if (!listItem.isHeader && item == listItem.item) {
        return i;
      }
    }

    return -1;
  }

  public Type getItem(int position) {
    Item<Type> item = getList().get(position);
    if (item.isHeader) {
      throw new IllegalArgumentException("getItem can not be used to get a Header");
    }

    return item.item;
  }

  public void addHeader(int header) {
    addHeader(header, null);
  }

  public void addHeader(int headerRes, List<Type> items) {
    final long headerId = Long.MAX_VALUE - headerIdOffset--;
    Header<Type> header = new Header<>(headerRes, headerId);
    header.setItems(items);
    headers.add(header);

    submitNewList();
  }

  private class PendingChange {

    final Header<Type> header;
    final List<Type> pendingItems;

    public PendingChange(Header<Type> header, List<Type> pendingItems) {
      this.header = header;
      this.pendingItems = pendingItems;
    }
  }

  private void submitNewList() {
    final List<PendingChange> changes = new ArrayList<>();

    List<Item<Type>> items = new ArrayList<>();
    for (Header<Type> header : headers) {
      if (header.pendingItems != null) {
        if (header.pendingItems.size() > 0) {
          items.add(new Item<Type>(header.headerId, header.headerRes));

          for (Type item : header.pendingItems) {
            items.add(new Item<Type>(item));
          }
        }

        changes.add(new PendingChange(header, header.pendingItems));
      } else if (header.size > 0) {
        items.add(new Item<Type>(header.headerId, header.headerRes));

        for (Type item : header.items) {
          items.add(new Item<Type>(item));
        }
      }
    }

    asyncDiffer.submitList(items, new Runnable() {
      @Override public void run() {
        for (PendingChange change : changes) {
          change.header.setItems(change.pendingItems);
          if (change.header.pendingItems == change.pendingItems) {
            change.header.pendingItems = null;
          }
        }
      }
    });
  }

  public void updateHeaderItems(int headerRes, List<Type> items) {
    for (Header<Type> header : headers) {
      if (headerRes == header.headerRes) {
        header.pendingItems = items;
        submitNewList();
        return;
      }
    }

    throw new IllegalArgumentException("No header for resource id " + headerRes + " found");
  }

  @Override public final long getItemId(int position) {
    Item<Type> item = getList().get(position);
    if (item.isHeader) {
      return item.headerId;
    }

    return getItemId(item.item);
  }

  protected long getItemId(Type item) {
    return RecyclerView.NO_ID;
  }

  private boolean areItemsTheSame(@NonNull Item<Type> oldItem, @NonNull Item<Type> newItem) {
    if (oldItem.isHeader && newItem.isHeader) {
      return oldItem.headerId == newItem.headerId;
    } else if (!oldItem.isHeader && !newItem.isHeader) {
      return areItemsTheSame(oldItem.item, newItem.item);
    }

    return false;
  }

  protected boolean areItemsTheSame(@NonNull Type oldItem, @NonNull Type newItem) {
    return getItemId(oldItem) == getItemId(newItem);
  }

  private boolean areContentsTheSame(@NonNull Item<Type> oldItem, @NonNull Item<Type> newItem) {
    if (oldItem.isHeader || newItem.isHeader) {
      return oldItem.headerRes == newItem.headerRes;
    }

    return oldItem.item.equals(newItem.item);
  }

  public int getHeaderRes(int position) {
    return getHeaderItem(position).headerRes;
  }

  private Item<Type> getHeaderItem(int position) {
    for (int i = position; i >= 0; i--) {
      Item<Type> item = getList().get(i);
      if (item.isHeader) {
        return item;
      }
    }

    throw new RuntimeException(
        "[" + this.getClass().getName() + "] No header item found for position " + position);
  }

  private Header<Type> getHeader(int headerRes) {
    for (Header<Type> header : headers) {
      if (headerRes == header.headerRes) {
        return header;
      }
    }

    throw new RuntimeException(
        "[" + this.getClass().getName() + "] No Header found for headerRes " + headerRes);
  }

  @Override public final int getItemViewType(int position) {
    Item<Type> item = getList().get(position);
    if (item.isHeader) {
      return HeaderSpanLookup.TYPE_HEADER;
    }

    return getItemViewType(getHeaderRes(position), item.item);
  }

  protected abstract int getItemViewType(int headerRes, Type item);

  @Override public final T onCreateViewHolder(ViewGroup parent, int viewType) {
    if (viewType == HeaderSpanLookup.TYPE_HEADER) {
      return onCreateHeaderHolder(parent);
    } else {
      return onCreateItemHolder(parent, viewType);
    }
  }

  protected abstract T onCreateItemHolder(ViewGroup parent, int viewType);

  protected abstract T onCreateHeaderHolder(ViewGroup parent);

  @Override public final void onBindViewHolder(T holder, int position) {
    if (holder.getItemViewType() == HeaderSpanLookup.TYPE_HEADER) {
      onBindHeader(holder, getHeaderRes(position));
    } else {
      onBindViewHolder(holder, getList().get(position).item, position);
    }
  }

  protected abstract void onBindHeader(T holder, int headerRes);

  protected abstract void onBindViewHolder(T holder, Type item, int position);
}
