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
import android.database.Cursor;
import android.provider.BaseColumns;
import androidx.recyclerview.widget.RecyclerView;

public abstract class RecyclerCursorAdapter<VH extends RecyclerView.ViewHolder>
    extends BaseCursorAdapter<VH> {

  private Context context;

  private Cursor cursor;

  private int idIndex = -1;

  private int lastModifiedIndex = -1;

  public RecyclerCursorAdapter(Context context) {
    this(context, null);
  }

  public RecyclerCursorAdapter(Context context, Cursor cursor) {
    this.context = context;
    setHasStableIds(true);
    changeCursor(cursor);
  }

  protected Context getContext() {
    return context;
  }

  public Cursor getCursor(int position) {
    if (cursor != null) {
      cursor.moveToPosition(position);
      return cursor;
    }

    return null;
  }

  public void changeCursor(Cursor cursor) {
    if (cursor == this.cursor) {
      return;
    }

    if (this.cursor != null) {
      this.cursor.close();
    }

    this.cursor = cursor;

    if (cursor != null) {
      idIndex = cursor.getColumnIndexOrThrow(BaseColumns._ID);
      lastModifiedIndex = cursor.getColumnIndexOrThrow(LastModifiedColumn.LAST_MODIFIED);
    } else {
      idIndex = -1;
      lastModifiedIndex = -1;
    }

    notifyChanged();
  }

  @Override public long getItemId(int position) {
    return getCursor(position).getLong(idIndex);
  }

  @Override public long getLastModified(int position) {
    return getCursor(position).getLong(lastModifiedIndex);
  }

  @Override public final void onBindViewHolder(VH holder, int position) {
    onBindViewHolder(holder, getCursor(position), position);
  }

  protected abstract void onBindViewHolder(VH holder, Cursor cursor, int position);

  @Override public int getItemCount() {
    return cursor != null ? cursor.getCount() : 0;
  }
}
