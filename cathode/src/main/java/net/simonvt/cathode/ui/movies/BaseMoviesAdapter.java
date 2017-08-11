/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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
package net.simonvt.cathode.ui.movies;

import android.database.Cursor;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import javax.inject.Inject;
import net.simonvt.cathode.Injector;
import net.simonvt.cathode.R;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.ui.adapter.RecyclerCursorAdapter;
import net.simonvt.cathode.ui.dialog.CheckInDialog;
import net.simonvt.cathode.ui.dialog.CheckInDialog.Type;
import net.simonvt.cathode.ui.history.AddToHistoryDialog;
import net.simonvt.cathode.ui.history.RemoveFromHistoryDialog;
import net.simonvt.cathode.ui.listener.MovieClickListener;
import net.simonvt.cathode.widget.CircularProgressIndicator;
import net.simonvt.cathode.widget.OverflowView;
import net.simonvt.cathode.widget.RemoteImageView;
import net.simonvt.schematic.Cursors;

public abstract class BaseMoviesAdapter<T extends BaseMoviesAdapter.ViewHolder>
    extends RecyclerCursorAdapter<T> {

  @Inject protected MovieTaskScheduler movieScheduler;

  protected FragmentActivity activity;

  protected MovieClickListener listener;

  public BaseMoviesAdapter(FragmentActivity activity, MovieClickListener listener, Cursor c) {
    super(activity, c);
    Injector.inject(this);
    this.activity = activity;
    this.listener = listener;
  }

  @Override public void onViewRecycled(ViewHolder holder) {
    holder.overflow.dismiss();
  }

  @Override protected void onBindViewHolder(T holder, Cursor cursor, int position) {
    final long id = Cursors.getLong(cursor, MovieColumns.ID);
    final String title = Cursors.getString(cursor, MovieColumns.TITLE);
    final boolean watched = Cursors.getBoolean(cursor, MovieColumns.WATCHED);
    final boolean collected = Cursors.getBoolean(cursor, MovieColumns.IN_COLLECTION);
    final boolean inWatchlist = Cursors.getBoolean(cursor, MovieColumns.IN_WATCHLIST);
    final boolean watching = Cursors.getBoolean(cursor, MovieColumns.WATCHING);
    final boolean checkedIn = Cursors.getBoolean(cursor, MovieColumns.CHECKED_IN);

    final String poster = ImageUri.create(ImageUri.ITEM_MOVIE, ImageType.POSTER, id);

    holder.poster.setImage(poster);
    holder.title.setText(title);
    holder.overview.setText(Cursors.getString(cursor, MovieColumns.OVERVIEW));

    if (holder.rating != null) {
      final float rating = Cursors.getFloat(cursor, MovieColumns.RATING);
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
      overflow.addItem(R.id.action_history_remove, R.string.action_history_remove);
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
      case R.id.action_history_add:
        AddToHistoryDialog.newInstance(AddToHistoryDialog.Type.MOVIE, id, title)
            .show(activity.getSupportFragmentManager(), AddToHistoryDialog.TAG);
        break;

      case R.id.action_history_remove:
        RemoveFromHistoryDialog.newInstance(RemoveFromHistoryDialog.Type.MOVIE, id, title)
            .show(activity.getSupportFragmentManager(), RemoveFromHistoryDialog.TAG);
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

    @BindView(R.id.poster) public RemoteImageView poster;
    @BindView(R.id.title) public TextView title;
    @BindView(R.id.overview) public TextView overview;
    @BindView(R.id.overflow) public OverflowView overflow;
    @BindView(R.id.rating) @Nullable public CircularProgressIndicator rating;

    public ViewHolder(View v) {
      super(v);
      ButterKnife.bind(this, v);
    }
  }
}
