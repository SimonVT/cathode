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
import android.widget.CursorAdapter;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.dialog.CheckInDialog;
import net.simonvt.cathode.ui.dialog.CheckInDialog.Type;
import net.simonvt.cathode.widget.CheckMark;
import net.simonvt.cathode.widget.OverflowView;
import net.simonvt.cathode.widget.RemoteImageView;
import net.simonvt.cathode.widget.TimeStamp;

public class SeasonAdapter extends CursorAdapter {

  @Inject ShowTaskScheduler showScheduler;
  @Inject EpisodeTaskScheduler episodeScheduler;

  private FragmentActivity activity;

  private LibraryType type;

  public SeasonAdapter(FragmentActivity activity, LibraryType type) {
    super(activity, null, 0);
    CathodeApp.inject(activity, this);
    this.activity = activity;
    this.type = type;
  }

  @Override public void changeCursor(Cursor cursor) {
    super.changeCursor(cursor);
  }

  @Override public View newView(Context context, Cursor cursor, ViewGroup parent) {
    View v = LayoutInflater.from(context).inflate(R.layout.list_row_episode, parent, false);

    ViewHolder vh = new ViewHolder(v);
    vh.checkbox.setType(
        type == LibraryType.COLLECTION ? LibraryType.COLLECTION : LibraryType.WATCHED);
    v.setTag(vh);

    return v;
  }

  @Override public void bindView(View view, Context context, Cursor cursor) {
    final long id = cursor.getLong(cursor.getColumnIndexOrThrow(CathodeContract.Episodes._ID));
    final String title =
        cursor.getString(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.TITLE));
    final int season = cursor.getInt(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.SEASON));
    final int episode =
        cursor.getInt(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.EPISODE));
    final boolean watched =
        cursor.getInt(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.WATCHED)) == 1;
    final boolean inCollection =
        cursor.getInt(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.IN_COLLECTION)) == 1;
    final boolean inWatchlist =
        cursor.getInt(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.IN_WATCHLIST)) == 1;
    final boolean watching =
        cursor.getInt(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.WATCHING)) == 1;
    final boolean checkedIn =
        cursor.getInt(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.CHECKED_IN)) == 1;
    final long firstAired =
        cursor.getLong(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.FIRST_AIRED));
    final String screen =
        cursor.getString(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.SCREEN));

    final ViewHolder vh = (ViewHolder) view.getTag();

    vh.title.setText(title);

    vh.firstAired.setTimeInMillis(firstAired);
    vh.firstAired.setExtended(true);
    vh.number.setText(String.valueOf(episode));

    vh.screen.setImage(screen);

    if (type == LibraryType.COLLECTION) {
      vh.checkbox.setVisibility(inCollection ? View.VISIBLE : View.INVISIBLE);
    } else {
      vh.checkbox.setVisibility(watched ? View.VISIBLE : View.INVISIBLE);
    }

    updateOverflowMenu(vh.overflow, watched, inCollection, inWatchlist, watching, checkedIn);

    vh.overflow.setListener(new OverflowView.OverflowActionListener() {
      @Override public void onPopupShown() {
      }

      @Override public void onPopupDismissed() {
      }

      @Override public void onActionSelected(int action) {
        switch (action) {
          case R.id.action_watched:
            updateOverflowMenu(vh.overflow, true, inCollection, inWatchlist, watching, checkedIn);
            episodeScheduler.setWatched(id, true);
            if (type == LibraryType.WATCHED) vh.checkbox.setVisibility(View.VISIBLE);
            break;

          case R.id.action_unwatched:
            updateOverflowMenu(vh.overflow, false, inCollection, inWatchlist, watching, checkedIn);
            episodeScheduler.setWatched(id, false);
            if (type == LibraryType.WATCHED) vh.checkbox.setVisibility(View.INVISIBLE);
            break;

          case R.id.action_checkin:
            CheckInDialog.showDialogIfNecessary(activity, Type.SHOW, title, id);
            break;

          case R.id.action_checkin_cancel:
            showScheduler.cancelCheckin();
            break;

          case R.id.action_collection_add:
            updateOverflowMenu(vh.overflow, watched, true, inWatchlist, watching, checkedIn);
            episodeScheduler.setIsInCollection(id, true);
            if (type == LibraryType.COLLECTION) vh.checkbox.setVisibility(View.VISIBLE);
            break;

          case R.id.action_collection_remove:
            updateOverflowMenu(vh.overflow, watched, false, inWatchlist, watching, checkedIn);
            episodeScheduler.setIsInCollection(id, false);
            if (type == LibraryType.COLLECTION) vh.checkbox.setVisibility(View.INVISIBLE);
            break;

          case R.id.action_watchlist_add:
            updateOverflowMenu(vh.overflow, watched, inCollection, true, watching, checkedIn);
            episodeScheduler.setIsInWatchlist(id, true);
            break;

          case R.id.action_watchlist_remove:
            updateOverflowMenu(vh.overflow, watched, inCollection, false, watching, checkedIn);
            episodeScheduler.setIsInWatchlist(id, false);
            break;
        }
      }
    });
  }

  private void updateOverflowMenu(OverflowView overflow, boolean watched, boolean inCollection,
      boolean inWatchlist, boolean watching, boolean checkedIn) {
    overflow.removeItems();
    if (checkedIn) {
      overflow.addItem(R.id.action_checkin_cancel, R.string.action_checkin_cancel);
    } else if (!watching) {
      overflow.addItem(R.id.action_checkin, R.string.action_checkin);
    }
    if (watched) {
      overflow.addItem(R.id.action_unwatched, R.string.action_unwatched);
    } else {
      overflow.addItem(R.id.action_watched, R.string.action_watched);
    }

    if (inCollection) {
      overflow.addItem(R.id.action_collection_remove, R.string.action_collection_remove);
    } else {
      overflow.addItem(R.id.action_collection_add, R.string.action_collection_add);
    }

    if (inWatchlist) {
      overflow.addItem(R.id.action_watchlist_remove, R.string.action_watchlist_remove);
    } else if (!watched) {
      overflow.addItem(R.id.action_watchlist_add, R.string.action_watchlist_add);
    }
  }

  static class ViewHolder {

    @InjectView(R.id.screen) RemoteImageView screen;

    @InjectView(R.id.title) TextView title;
    @InjectView(R.id.firstAired) TimeStamp firstAired;
    @InjectView(R.id.episode) TextView number;
    @InjectView(R.id.overflow) OverflowView overflow;
    @InjectView(R.id.checkbox) CheckMark checkbox;

    ViewHolder(View v) {
      ButterKnife.inject(this, v);
    }
  }
}
