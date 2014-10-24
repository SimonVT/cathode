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
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
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

public abstract class BaseMoviesAdapter<T extends BaseMoviesAdapter.ViewHolder>
    extends RecyclerCursorAdapter<T> {

  public interface MovieClickListener {

    void onMovieClicked(View v, int position, long id);
  }

  @Inject MovieTaskScheduler movieScheduler;

  protected FragmentActivity activity;

  protected MovieClickListener listener;

  public BaseMoviesAdapter(FragmentActivity activity, MovieClickListener listener, Cursor c) {
    super(activity, c);
    CathodeApp.inject(activity, this);
    this.activity = activity;
    this.listener = listener;
  }

  @Override protected void onBindViewHolder(T holder, Cursor cursor, int position) {
    final String title = cursor.getString(cursor.getColumnIndex(MovieColumns.TITLE));
    final boolean watched = cursor.getInt(cursor.getColumnIndex(MovieColumns.WATCHED)) == 1;
    final boolean collected = cursor.getInt(cursor.getColumnIndex(MovieColumns.IN_COLLECTION)) == 1;
    final boolean inWatchlist =
        cursor.getInt(cursor.getColumnIndex(MovieColumns.IN_WATCHLIST)) == 1;
    final boolean watching = cursor.getInt(cursor.getColumnIndex(MovieColumns.WATCHING)) == 1;
    final boolean checkedIn = cursor.getInt(cursor.getColumnIndex(MovieColumns.CHECKED_IN)) == 1;

    holder.poster.setImage(cursor.getString(cursor.getColumnIndex(MovieColumns.POSTER)));
    holder.title.setText(title);
    holder.overview.setText(cursor.getString(cursor.getColumnIndex(MovieColumns.OVERVIEW)));

    if (holder.rating != null) {
      final int rating = cursor.getInt(cursor.getColumnIndex(MovieColumns.RATING));
      holder.rating.setValue(rating);
    }

    holder.overflow.removeItems();
    setupOverflowItems(holder.overflow, watched, collected, inWatchlist, watching, checkedIn);
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

  public static class ViewHolder extends RecyclerView.ViewHolder {

    @InjectView(R.id.poster) RemoteImageView poster;
    @InjectView(R.id.title) TextView title;
    @InjectView(R.id.overview) TextView overview;
    @InjectView(R.id.overflow) OverflowView overflow;
    @InjectView(R.id.rating) @Optional CircularProgressIndicator rating;

    ViewHolder(View v) {
      super(v);
      ButterKnife.inject(this, v);
    }
  }
}
