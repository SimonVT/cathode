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
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.ui.dialog.CheckInDialog;
import net.simonvt.cathode.ui.dialog.CheckInDialog.Type;
import net.simonvt.cathode.widget.CircularProgressIndicator;
import net.simonvt.cathode.widget.OverflowView;
import net.simonvt.cathode.widget.RemoteImageView;

public class MoviesAdapter extends CursorAdapter {

  @Inject MovieTaskScheduler movieScheduler;

  private FragmentActivity activity;

  private int rowLayout;

  public MoviesAdapter(FragmentActivity activity, Cursor c) {
    this(activity, c, R.layout.list_row_movie);
    CathodeApp.inject(activity, this);
  }

  public MoviesAdapter(FragmentActivity activity, Cursor c, int rowLayout) {
    super(activity, c, 0);
    CathodeApp.inject(activity, this);
    this.activity = activity;
    this.rowLayout = rowLayout;
  }

  @Override public View newView(Context context, Cursor cursor, ViewGroup parent) {
    View v = LayoutInflater.from(mContext).inflate(rowLayout, parent, false);
    v.setTag(new ViewHolder(v));
    return v;
  }

  @Override public void bindView(final View view, final Context context, final Cursor cursor) {
    ViewHolder vh = (ViewHolder) view.getTag();
    final int position = cursor.getPosition();

    final long id = cursor.getLong(cursor.getColumnIndex(MovieColumns.ID));
    final String title = cursor.getString(cursor.getColumnIndex(MovieColumns.TITLE));
    final boolean watched = cursor.getInt(cursor.getColumnIndex(MovieColumns.WATCHED)) == 1;
    final boolean collected = cursor.getInt(cursor.getColumnIndex(MovieColumns.IN_COLLECTION)) == 1;
    final boolean inWatchlist =
        cursor.getInt(cursor.getColumnIndex(MovieColumns.IN_WATCHLIST)) == 1;
    final boolean watching = cursor.getInt(cursor.getColumnIndex(MovieColumns.WATCHING)) == 1;
    final boolean checkedIn = cursor.getInt(cursor.getColumnIndex(MovieColumns.CHECKED_IN)) == 1;

    vh.poster.setImage(cursor.getString(cursor.getColumnIndex(MovieColumns.POSTER)));
    vh.title.setText(title);
    vh.overview.setText(cursor.getString(cursor.getColumnIndex(MovieColumns.OVERVIEW)));

    if (vh.rating != null) {
      final int rating = cursor.getInt(cursor.getColumnIndex(MovieColumns.RATING));
      vh.rating.setValue(rating);
    }

    vh.overflow.removeItems();
    setupOverflowItems(vh.overflow, watched, collected, inWatchlist, watching, checkedIn);

    vh.overflow.setListener(new OverflowView.OverflowActionListener() {
      @Override public void onPopupShown() {
      }

      @Override public void onPopupDismissed() {
      }

      @Override public void onActionSelected(int action) {
        onOverflowActionSelected(view, id, action, position, title);
      }
    });
  }

  protected void setupOverflowItems(OverflowView overflow, boolean watched, boolean collected,
      boolean inWatchlist, boolean watching, boolean checkedIn) {
    if (checkedIn) {
      overflow.addItem(R.id.action_checkin_cancel, R.string.action_checkin_cancel);
    } else if (watched) {
      overflow.addItem(R.id.action_unwatched, R.string.action_unwatched);
    } else if (inWatchlist) {
      overflow.addItem(R.id.action_checkin, R.string.action_checkin);
      overflow.addItem(R.id.action_watchlist_remove, R.string.action_watchlist_remove);
    } else {
      if (!watching) overflow.addItem(R.id.action_checkin, R.string.action_checkin);
      overflow.addItem(R.id.action_watchlist_add, R.string.action_watchlist_add);
    }

    if (collected) {
      overflow.addItem(R.id.action_collection_remove, R.string.action_collection_remove);
    } else {
      overflow.addItem(R.id.action_collection_add, R.string.action_collection_add);
    }
  }

  protected void onOverflowActionSelected(View view, long id, int action, int position,
      String title) {
    switch (action) {
      case R.id.action_watched:
        movieScheduler.setWatched(id, true);
        break;

      case R.id.action_unwatched:
        movieScheduler.setWatched(id, false);
        break;

      case R.id.action_checkin:
        CheckInDialog.showDialogIfNecessary(activity, Type.MOVIE, title, id);
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
    @InjectView(R.id.rating) @Optional CircularProgressIndicator rating;

    ViewHolder(View v) {
      ButterKnife.inject(this, v);
    }
  }
}
