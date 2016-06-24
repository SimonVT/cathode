/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.DatabaseContract.LastModifiedColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.listener.MovieClickListener;
import net.simonvt.cathode.widget.OverflowView;

public class MoviesAdapter extends BaseMoviesAdapter<BaseMoviesAdapter.ViewHolder> {

  public static final String[] PROJECTION = new String[] {
      MovieColumns.ID, MovieColumns.TITLE, MovieColumns.WATCHED, MovieColumns.IN_COLLECTION,
      MovieColumns.IN_WATCHLIST, MovieColumns.WATCHING, MovieColumns.CHECKED_IN,
      MovieColumns.POSTER, MovieColumns.OVERVIEW, MovieColumns.RATING,
      LastModifiedColumns.LAST_MODIFIED
  };

  private int rowLayout;

  public MoviesAdapter(FragmentActivity activity, MovieClickListener listener, Cursor c,
      LibraryType libraryType) {
    this(activity, listener, c, R.layout.list_row_movie, libraryType);
  }

  public MoviesAdapter(FragmentActivity activity, MovieClickListener listener, Cursor c,
      int rowLayout, LibraryType libraryType) {
    super(activity, listener, c, libraryType);
    this.rowLayout = rowLayout;
  }

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(getContext()).inflate(rowLayout, parent, false);
    final ViewHolder holder = new ViewHolder(v);

    v.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        final int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
          listener.onMovieClicked(holder.itemView, position, holder.getItemId());
        }
      }
    });

    holder.overflow.setListener(new OverflowView.OverflowActionListener() {
      @Override public void onPopupShown() {
      }

      @Override public void onPopupDismissed() {
      }

      @Override public void onActionSelected(int action) {
        onOverflowActionSelected(holder.itemView, holder.getItemId(), action,
            holder.getAdapterPosition(), holder.title.getText().toString());
      }
    });

    return holder;
  }
}
