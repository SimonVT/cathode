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

package net.simonvt.cathode.ui.adapter;

import android.database.Cursor;
import android.provider.BaseColumns;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;
import net.simonvt.cathode.provider.DatabaseContract.LastModifiedColumns;
import timber.log.Timber;

public abstract class HeaderCursorAdapter<T extends RecyclerView.ViewHolder>
    extends BaseAdapter<T> {

  static class Header {

    final int header;

    long headerId;

    Cursor cursor;

    int size;

    private Header(int header, long headerId) {
      this.header = header;
      this.headerId = headerId;
    }

    void updateSize() {
      final int cursorSize = cursor != null ? cursor.getCount() : 0;
      if (cursorSize > 0) {
        size = cursorSize + 1;
      } else {
        size = 0;
      }
    }

    void setCursor(Cursor cursor) {
      this.cursor = cursor;
      updateSize();
    }
  }

  public static class SpanLookup extends GridLayoutManager.SpanSizeLookup {

    @Override public int getSpanSize(int i) {
      return 0;
    }
  }

  List<Header> headers = new ArrayList<Header>();
  long headerIdOffset;

  int itemCount;

  final List<Integer> headerPositions = new ArrayList<Integer>();

  private SparseArray<Long> itemIds = new SparseArray<>();

  private AdapterNotifier notifier;

  public HeaderCursorAdapter() {
    setHasStableIds(true);
    notifier = new AdapterNotifier(this);
  }

  public void addHeader(int header) {
    addHeader(header, null);
  }

  public void addHeader(int headerRes, Cursor cursor) {
    final long headerId = Long.MAX_VALUE - headerIdOffset--;
    Header header = new Header(headerRes, headerId);
    header.setCursor(cursor);
    headers.add(header);

    notifyChanged();
  }

  public void notifyChanged() {
    itemIds.clear();
    itemCount = 0;

    headerPositions.clear();

    for (Header header : headers) {
      header.updateSize();

      if (header.size > 0) {
        headerPositions.add(itemCount);
        itemCount += header.size;
      }
    }

    notifier.notifyChanged();
  }

  public void updateCursorForHeader(int headerRes, Cursor cursor) {
    for (Header header : headers) {
      if (headerRes == header.header) {
        header.setCursor(cursor);
        notifyChanged();
        return;
      }
    }

    throw new IllegalArgumentException("No header for resource id " + headerRes + " found");
  }

  private long getId(int position) {
    int offset = 0;
    for (int i = 0; i < headers.size(); i++) {
      Header header = headers.get(i);

      if (position < offset + header.size) {
        final int offsetPosition = position - offset;
        if (offsetPosition == 0) {
          return header.headerId;
        }

        header.cursor.moveToPosition(offsetPosition - 1);
        return header.cursor.getLong(header.cursor.getColumnIndex(BaseColumns._ID));
      }

      offset += header.size;
    }

    throw new IllegalStateException("No id found for position " + position);
  }

  @Override public long getItemId(int position) {
    Long id = itemIds.get(position);
    if (id == null) {
      id = getId(position);
      itemIds.put(position, id);
    }
    return id;
  }

  @Override public long getLastModified(int position) {
    if (headerPositions.contains(position)) {
      return 0;
    }

    Cursor cursor = getCursor(position);
    return cursor.getLong(cursor.getColumnIndexOrThrow(LastModifiedColumns.LAST_MODIFIED));
  }

  public Header getHeader(int position) {
    int offset = 0;

    for (Header header : headers) {
      if (position < offset + header.size) {
        return header;
      }

      offset += header.size;
    }

    Timber.i("Count: " + getItemCount());
    Timber.i("Header count: " + headers.size());

    for (Header header : headers) {
      Timber.i("Header id: " + header.headerId);
      Timber.i("Header size: " + header.size);
    }

    throw new RuntimeException(
        "[" + this.getClass().getName() + "] No header found for position " + position);
  }

  public int getCursorPosition(int position) {
    int offset = 0;

    for (Header header : headers) {
      if (position < offset + header.size) {
        return position - offset - 1;
      }

      offset += header.size;
    }

    throw new RuntimeException("No cursor position found for position " + position);
  }

  public boolean isHeader(int position) {
    return headerPositions.contains(position);
  }

  public List<Cursor> getCursors() {
    List<Cursor> cursors = new ArrayList<>();

    for (Header header : headers) {
      cursors.add(header.cursor);
    }

    return cursors;
  }

  public Cursor getCursor(int position) {
    if (position >= itemCount) {
      throw new IndexOutOfBoundsException("Invalid index " + position + ", size is " + itemCount);
    }

    if (isHeader(position)) {
      Timber.i("Count: " + getItemCount());
      Timber.i("Header count: " + headers.size());

      for (Header header : headers) {
        Timber.i("Header id: " + header.headerId);
        Timber.i("Header size: " + header.size);
      }

      throw new RuntimeException("Trying to get cursor for a header position " + position);
    }

    int offset = 0;

    for (Header header : headers) {
      if (position < offset + header.size) {
        header.cursor.moveToPosition(position - offset - 1);
        return header.cursor;
      }

      offset += header.size;
    }

    throw new RuntimeException("No cursor found");
  }

  @Override public int getItemCount() {
    return itemCount;
  }

  @Override public final int getItemViewType(int position) {
    if (isHeader(position)) {
      return HeaderSpanLookup.TYPE_HEADER;
    }

    return getItemViewType(getHeader(position).header, getCursor(position));
  }

  protected abstract int getItemViewType(int headerRes, Cursor cursor);

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
      onBindHeader(holder, getHeader(position).header);
    } else {
      onBindViewHolder(holder, getCursor(position), position);
    }
  }

  protected abstract void onBindHeader(T holder, int headerRes);

  protected abstract void onBindViewHolder(T holder, Cursor cursor, int position);
}
