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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.Bind;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseSchematic;
import net.simonvt.cathode.widget.IndicatorView;
import net.simonvt.cathode.widget.OverflowView;

public class MovieSearchAdapter extends BaseMoviesAdapter<MovieSearchAdapter.ViewHolder> {

  public static final String[] PROJECTION = new String[] {
      DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.ID,
      DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.TITLE,
      DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.OVERVIEW,
      DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.POSTER,
      DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.TMDB_ID,
      DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.WATCHED,
      DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.IN_COLLECTION,
      DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.IN_WATCHLIST,
      DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.RATING,
      DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.WATCHING,
      DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.CHECKED_IN,
  };

  public MovieSearchAdapter(FragmentActivity activity, MovieClickListener listener, Cursor cursor) {
    super(activity, listener, cursor);
    CathodeApp.inject(activity, this);
  }

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View v =
        LayoutInflater.from(getContext()).inflate(R.layout.list_row_search_movie, parent, false);
    final ViewHolder holder = new ViewHolder(v);

    v.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        listener.onMovieClicked(holder.itemView, holder.getPosition(), holder.getItemId());
      }
    });

    holder.overflow.setListener(new OverflowView.OverflowActionListener() {

      @Override public void onPopupShown() {
        holder.setIsRecyclable(false);
      }

      @Override public void onPopupDismissed() {
        holder.setIsRecyclable(false);
      }

      @Override public void onActionSelected(int action) {
        holder.setIsRecyclable(true);
        onOverflowActionSelected(holder.itemView, holder.getItemId(), action,
            holder.getAdapterPosition(), holder.title.getText().toString());
      }
    });

    return holder;
  }

  @Override protected void onBindViewHolder(ViewHolder holder, Cursor cursor, int position) {
    super.onBindViewHolder(holder, cursor, position);

    final boolean watched = cursor.getInt(cursor.getColumnIndex(MovieColumns.WATCHED)) == 1;
    final boolean inCollection =
        cursor.getInt(cursor.getColumnIndex(MovieColumns.IN_COLLECTION)) == 1;
    final boolean inWatchlist =
        cursor.getInt(cursor.getColumnIndex(MovieColumns.IN_WATCHLIST)) == 1;

    holder.indicator.setWatched(watched);
    holder.indicator.setCollected(inCollection);
    holder.indicator.setInWatchlist(inWatchlist);
  }

  public static class ViewHolder extends BaseMoviesAdapter.ViewHolder {

    @Bind(R.id.indicator) IndicatorView indicator;

    ViewHolder(View v) {
      super(v);
    }
  }
}
