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

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.DatabaseContract.ListsColumns;
import net.simonvt.schematic.Cursors;

public class ListsAdapter extends RecyclerCursorAdapter<ListsAdapter.ViewHolder> {

  public interface OnListClickListener {

    void onListClicked(long listId, String listName);
  }

  public static final String[] PROJECTION = new String[] {
      ListsColumns.ID, ListsColumns.NAME, ListsColumns.DESCRIPTION, ListsColumns.LAST_MODIFIED,
      ListsColumns.TRAKT_ID,
  };

  private OnListClickListener listener;

  public ListsAdapter(OnListClickListener listener, Context context) {
    super(context);
    this.listener = listener;
  }

  public ListsAdapter(OnListClickListener listener, Context context, Cursor cursor) {
    super(context, cursor);
    this.listener = listener;
  }

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(getContext()).inflate(R.layout.list_row_list, parent, false);
    final ViewHolder vh = new ViewHolder(v);

    v.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        listener.onListClicked(vh.getItemId(), vh.name.getText().toString());
      }
    });

    return vh;
  }

  @Override protected void onBindViewHolder(ViewHolder holder, Cursor cursor, int position) {
    final String name = Cursors.getString(cursor, ListsColumns.NAME);
    final String description = Cursors.getString(cursor, ListsColumns.DESCRIPTION);
    final long traktId = Cursors.getLong(cursor, ListsColumns.TRAKT_ID);

    final boolean enabled = traktId >= 0L;
    holder.itemView.setEnabled(enabled);
    holder.name.setEnabled(enabled);
    holder.description.setEnabled(enabled);
    holder.name.setText(name);
    holder.description.setText(description);
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.name) TextView name;

    @BindView(R.id.description) TextView description;

    public ViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }
}
