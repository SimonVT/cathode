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
import android.provider.BaseColumns;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.provider.CathodeDatabase;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.widget.CircularProgressIndicator;
import net.simonvt.cathode.widget.IndicatorView;
import net.simonvt.cathode.widget.OverflowView;
import net.simonvt.cathode.widget.RemoteImageView;

public class ShowDescriptionAdapter extends CursorAdapter {

  private static final String TAG = "ShowDescriptionAdapter";

  public static final String[] PROJECTION = new String[] {
      CathodeDatabase.Tables.SHOWS + "." + BaseColumns._ID,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.TITLE,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.OVERVIEW,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.POSTER,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.TVDB_ID,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.WATCHED_COUNT,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.IN_COLLECTION_COUNT,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.IN_WATCHLIST,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.RATING_PERCENTAGE,
  };

  @Inject ShowTaskScheduler showScheduler;

  public ShowDescriptionAdapter(Context context, Cursor cursor) {
    super(context, cursor, 0);
    CathodeApp.inject(context, this);
  }

  @Override public View newView(Context context, Cursor cursor, ViewGroup parent) {
    View v = LayoutInflater.from(context).inflate(R.layout.list_row_show_description, parent, false);
    v.setTag(new ViewHolder(v));
    return v;
  }

  @Override public void bindView(final View view, Context context, final Cursor cursor) {
    ViewHolder vh = (ViewHolder) view.getTag();
    final int position = cursor.getPosition();

    final long id = cursor.getLong(cursor.getColumnIndex(CathodeContract.Shows._ID));
    final boolean watched =
        cursor.getInt(cursor.getColumnIndex(CathodeContract.Shows.WATCHED_COUNT)) > 0;
    final boolean inCollection =
        cursor.getInt(cursor.getColumnIndex(CathodeContract.Shows.IN_COLLECTION_COUNT)) > 1;
    final boolean inWatchlist =
        cursor.getInt(cursor.getColumnIndex(CathodeContract.Shows.IN_WATCHLIST)) == 1;
    final int rating =
        cursor.getInt(cursor.getColumnIndex(CathodeContract.Shows.RATING_PERCENTAGE));

    vh.indicator.setWatched(watched);
    vh.indicator.setCollected(inCollection);
    vh.indicator.setInWatchlist(inWatchlist);

    vh.poster.setImage(cursor.getString(cursor.getColumnIndex(CathodeContract.Shows.POSTER)));
    vh.title.setText(cursor.getString(cursor.getColumnIndex(CathodeContract.Shows.TITLE)));
    vh.overview.setText(cursor.getString(cursor.getColumnIndex(CathodeContract.Shows.OVERVIEW)));

    vh.rating.setValue(rating);

    vh.overflow.removeItems();
    setupOverflowItems(vh.overflow, inWatchlist);

    vh.overflow.setListener(new OverflowView.OverflowActionListener() {
      @Override public void onPopupShown() {
      }

      @Override public void onPopupDismissed() {
      }

      @Override public void onActionSelected(int action) {
        onOverflowActionSelected(view, id, action, position);
      }
    });
  }

  protected void setupOverflowItems(OverflowView overflow, boolean inWatchlist) {
    if (inWatchlist) {
      overflow.addItem(R.id.action_watchlist_remove, R.string.action_watchlist_remove);
    } else {
      overflow.addItem(R.id.action_watchlist_add, R.string.action_watchlist_add);
    }
  }

  protected void onOverflowActionSelected(View view, long id, int action, int position) {
    switch (action) {
      case R.id.action_watchlist_add:
        showScheduler.setIsInWatchlist(id, true);
        break;

      case R.id.action_watchlist_remove:
        onWatchlistRemove(view, position, id);
        break;
    }
  }

  protected void onWatchlistRemove(View view, int position, long id) {
    showScheduler.setIsInWatchlist(id, false);
  }

  static class ViewHolder {

    @InjectView(R.id.poster) RemoteImageView poster;
    @InjectView(R.id.indicator) IndicatorView indicator;
    @InjectView(R.id.title) TextView title;
    @InjectView(R.id.overview) TextView overview;
    @InjectView(R.id.overflow) OverflowView overflow;
    @InjectView(R.id.rating) CircularProgressIndicator rating;

    ViewHolder(View v) {
      ButterKnife.inject(this, v);
    }
  }
}
