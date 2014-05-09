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

import android.content.Context;
import android.database.Cursor;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.InjectView;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseSchematic;
import net.simonvt.cathode.widget.IndicatorView;

public class MovieSearchAdapter extends MoviesAdapter {

  public static final String[] PROJECTION = new String[] {
      DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.ID,
      DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.TITLE,
      DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.OVERVIEW,
      DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.POSTER,
      DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.TMDB_ID,
      DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.WATCHED,
      DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.IN_COLLECTION,
      DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.IN_WATCHLIST,
      DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.RATING_PERCENTAGE,
      DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.WATCHING,
      DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.CHECKED_IN,
  };

  public MovieSearchAdapter(FragmentActivity activity, Cursor cursor) {
    super(activity, cursor);
    CathodeApp.inject(activity, this);
  }

  @Override public View newView(Context context, Cursor cursor, ViewGroup parent) {
    View v = LayoutInflater.from(context).inflate(R.layout.list_row_search_movie, parent, false);
    v.setTag(new ViewHolder(v));
    return v;
  }

  @Override public void bindView(View view, Context context, Cursor cursor) {
    super.bindView(view, context, cursor);
    ViewHolder vh = (ViewHolder) view.getTag();

    final boolean watched = cursor.getInt(cursor.getColumnIndex(MovieColumns.WATCHED)) == 1;
    final boolean inCollection =
        cursor.getInt(cursor.getColumnIndex(MovieColumns.IN_COLLECTION)) == 1;
    final boolean inWatchlist =
        cursor.getInt(cursor.getColumnIndex(MovieColumns.IN_WATCHLIST)) == 1;

    vh.indicator.setWatched(watched);
    vh.indicator.setCollected(inCollection);
    vh.indicator.setInWatchlist(inWatchlist);
  }

  static class ViewHolder extends MoviesAdapter.ViewHolder {

    @InjectView(R.id.indicator) IndicatorView indicator;

    ViewHolder(View v) {
      super(v);
    }
  }
}
