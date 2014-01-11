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
import net.simonvt.cathode.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.widget.OverflowView;
import net.simonvt.cathode.widget.RemoteImageView;

public class MoviesAdapter extends CursorAdapter {

  private static final String TAG = "MoviesAdapter";

  @Inject MovieTaskScheduler movieScheduler;

  public MoviesAdapter(Context context, Cursor c) {
    super(context, c, 0);
    CathodeApp.inject(context, this);
  }

  @Override public View newView(Context context, Cursor cursor, ViewGroup parent) {
    View v = LayoutInflater.from(mContext).inflate(R.layout.list_row_movie, parent, false);
    v.setTag(new ViewHolder(v));
    return v;
  }

  @Override public void bindView(final View view, final Context context, final Cursor cursor) {
    ViewHolder vh = (ViewHolder) view.getTag();

    final long id = cursor.getLong(cursor.getColumnIndex(CathodeContract.Movies._ID));
    final boolean watched =
        cursor.getInt(cursor.getColumnIndex(CathodeContract.Movies.WATCHED)) == 1;
    final boolean collected =
        cursor.getInt(cursor.getColumnIndex(CathodeContract.Movies.IN_COLLECTION)) == 1;
    final boolean inWatchlist =
        cursor.getInt(cursor.getColumnIndex(CathodeContract.Movies.IN_WATCHLIST)) == 1;
    final boolean watching =
        cursor.getInt(cursor.getColumnIndex(CathodeContract.Movies.WATCHING)) == 1;

    vh.poster.setImage(cursor.getString(cursor.getColumnIndex(CathodeContract.Movies.POSTER)));
    vh.title.setText(cursor.getString(cursor.getColumnIndex(CathodeContract.Movies.TITLE)));
    vh.overview.setText(cursor.getString(cursor.getColumnIndex(CathodeContract.Movies.OVERVIEW)));

    vh.overflow.removeItems();
    setupOverflowItems(vh.overflow, watched, collected, inWatchlist, watching);

    vh.overflow.setListener(new OverflowView.OverflowActionListener() {
      @Override public void onPopupShown() {
      }

      @Override public void onPopupDismissed() {
      }

      @Override public void onActionSelected(int action) {
        onOverflowActionSelected(view, id, action, cursor.getPosition());
      }
    });
  }

  protected void setupOverflowItems(OverflowView overflow, boolean watched, boolean collected,
      boolean inWatchlist, boolean watching) {
    if (watching) {
      overflow.addItem(R.id.action_checkin_cancel, R.string.action_checkin_cancel);
    } else if (watched) {
      overflow.addItem(R.id.action_unwatched, R.string.action_unwatched);
    } else if (inWatchlist) {
      overflow.addItem(R.id.action_checkin, R.string.action_checkin);
      overflow.addItem(R.id.action_watchlist_remove, R.string.action_watchlist_remove);
    } else {
      overflow.addItem(R.id.action_checkin, R.string.action_checkin);
      overflow.addItem(R.id.action_watchlist_add, R.string.action_watchlist_add);
    }

    if (collected) {
      overflow.addItem(R.id.action_collection_remove, R.string.action_collection_remove);
    } else {
      overflow.addItem(R.id.action_collection_add, R.string.action_collection_add);
    }
  }

  protected void onOverflowActionSelected(View view, long id, int action, int position) {
    switch (action) {
      case R.id.action_watched:
        movieScheduler.setWatched(id, true);
        break;

      case R.id.action_unwatched:
        movieScheduler.setWatched(id, false);
        break;

      case R.id.action_checkin:
        movieScheduler.checkin(id);
        break;

      case R.id.action_checkin_cancel:
        movieScheduler.cancelCheckin();
        break;

      case R.id.action_watchlist_add:
        movieScheduler.setIsInWatchlist(id, true);
        break;

      case R.id.action_watchlist_remove:
        movieScheduler.setIsInWatchlist(id, false);
        break;

      case R.id.action_collection_add:
        movieScheduler.setIsInCollection(id, true);
        break;

      case R.id.action_collection_remove:
        movieScheduler.setIsInCollection(id, false);
        break;
    }
  }

  static class ViewHolder {

    @InjectView(R.id.poster) RemoteImageView poster;
    @InjectView(R.id.title) TextView title;
    @InjectView(R.id.overview) TextView overview;
    @InjectView(R.id.overflow) OverflowView overflow;

    ViewHolder(View v) {
      ButterKnife.inject(this, v);
    }
  }
}
