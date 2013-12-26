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
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.provider.CathodeDatabase;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.widget.OverflowView;
import net.simonvt.cathode.widget.RemoteImageView;
import net.simonvt.cathode.widget.TimeStamp;

/**
 * A show adapter that displays the next episode as well.
 */
public class ShowsWithNextAdapter extends CursorAdapter {

  private static final String TAG = "ShowsAdapter";

  public static final String[] PROJECTION = new String[] {
      CathodeDatabase.Tables.SHOWS + "." + BaseColumns._ID,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.TITLE,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.POSTER,
      CathodeContract.Shows.AIRED_COUNT,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.WATCHED_COUNT,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.IN_COLLECTION_COUNT,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.STATUS,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.HIDDEN,
      CathodeDatabase.Tables.EPISODES + "." + CathodeContract.Episodes.TITLE,
      CathodeDatabase.Tables.EPISODES + "." + CathodeContract.Episodes.FIRST_AIRED,
      CathodeDatabase.Tables.EPISODES + "." + CathodeContract.Episodes.SEASON,
      CathodeDatabase.Tables.EPISODES + "." + CathodeContract.Episodes.EPISODE,
  };

  @Inject ShowTaskScheduler showScheduler;

  private final LibraryType libraryType;

  public ShowsWithNextAdapter(Context context, LibraryType libraryType) {
    this(context, null, libraryType);
  }

  public ShowsWithNextAdapter(Context context, Cursor cursor, LibraryType libraryType) {
    super(context, cursor, 0);
    CathodeApp.inject(context, this);
    this.libraryType = libraryType;
  }

  @Override public View newView(Context context, Cursor cursor, ViewGroup parent) {
    View v = LayoutInflater.from(context).inflate(R.layout.list_row_show, parent, false);

    ViewHolder vh = new ViewHolder(v);
    v.setTag(vh);

    return v;
  }

  @Override public void bindView(View view, Context context, Cursor cursor) {
    final long id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));

    final String showPosterUrl =
        cursor.getString(cursor.getColumnIndex(CathodeContract.Shows.POSTER));
    final String showTitle = cursor.getString(cursor.getColumnIndex(CathodeContract.Shows.TITLE));
    final String showStatus = cursor.getString(cursor.getColumnIndex(CathodeContract.Shows.STATUS));
    final boolean isHidden =
        cursor.getInt(cursor.getColumnIndex(CathodeContract.Shows.HIDDEN)) == 1;

    final int showAiredCount =
        cursor.getInt(cursor.getColumnIndex(CathodeContract.Shows.AIRED_COUNT));
    int showTypeCount = 0;
    switch (libraryType) {
      case WATCHED:
      case WATCHLIST:
        showTypeCount = cursor.getInt(cursor.getColumnIndex(CathodeContract.Shows.WATCHED_COUNT));
        break;

      case COLLECTION:
        showTypeCount =
            cursor.getInt(cursor.getColumnIndex(CathodeContract.Shows.IN_COLLECTION_COUNT));
        break;
    }

    final String episodeTitle =
        cursor.getString(cursor.getColumnIndex(CathodeContract.Episodes.TITLE));
    final long episodeFirstAired =
        cursor.getLong(cursor.getColumnIndex(CathodeContract.Episodes.FIRST_AIRED));
    final int episodeSeasonNumber =
        cursor.getInt(cursor.getColumnIndex(CathodeContract.Episodes.SEASON));
    final int episodeNumber =
        cursor.getInt(cursor.getColumnIndex(CathodeContract.Episodes.EPISODE));

    ViewHolder vh = (ViewHolder) view.getTag();

    vh.title.setText(showTitle);

    vh.progressBar.setMax(showAiredCount);
    vh.progressBar.setProgress(showTypeCount);

    vh.watched.setText(showTypeCount + "/" + showAiredCount);

    String episodeText;
    if (episodeTitle == null) {
      episodeText = showStatus;
      vh.firstAired.setVisibility(View.GONE);
    } else {
      episodeText = "Next: " + episodeSeasonNumber + "x" + episodeNumber + " " + episodeTitle;
      vh.firstAired.setVisibility(View.VISIBLE);
      vh.firstAired.setTimeInMillis(episodeFirstAired);
    }
    vh.nextEpisode.setText(episodeText);
    vh.nextEpisode.setEnabled(episodeTitle != null);

    vh.overflow.setVisibility(showAiredCount > 0 ? View.VISIBLE : View.INVISIBLE);
    vh.overflow.setListener(new OverflowView.OverflowActionListener() {
      @Override public void onPopupShown() {
      }

      @Override public void onPopupDismissed() {
      }

      @Override public void onActionSelected(int action) {
        switch (action) {
          case R.id.action_watchlist_remove:
            showScheduler.setIsInWatchlist(id, false);
            break;

          case R.id.action_watched:
            showScheduler.watchedNext(id);
            break;

          case R.id.action_watched_all:
            showScheduler.setWatched(id, true);
            break;

          case R.id.action_unwatch_all:
            showScheduler.setWatched(id, false);
            break;

          case R.id.action_collection_add:
            showScheduler.collectedNext(id);
            break;

          case R.id.action_collection_add_all:
            showScheduler.setIsInCollection(id, true);
            break;

          case R.id.action_collection_remove_all:
            showScheduler.setIsInCollection(id, false);
            break;

          case R.id.action_hide:
            showScheduler.setIsHidden(id, true);
            break;

          case R.id.action_unhide:
            showScheduler.setIsHidden(id, false);
            break;
        }
      }
    });

    vh.overflow.removeItems();
    setupOverflowItems(vh.overflow, showTypeCount, showAiredCount, episodeTitle != null, isHidden);

    vh.poster.setImage(showPosterUrl);
  }

  protected void setupOverflowItems(OverflowView overflow, int typeCount, int airedCount,
      boolean hasNext, boolean isHidden) {
    switch (libraryType) {
      case WATCHLIST:
        overflow.addItem(R.id.action_watchlist_remove, R.string.action_watchlist_remove);

      case WATCHED:
        if (airedCount - typeCount > 0) {
          if (hasNext) {
            overflow.addItem(R.id.action_watched, R.string.action_watched_next);
          }
          if (typeCount < airedCount) {
            overflow.addItem(R.id.action_watched_all, R.string.action_watched_all);
          }
        }
        if (typeCount > 0) {
          overflow.addItem(R.id.action_unwatch_all, R.string.action_unwatch_all);
        }
        break;

      case COLLECTION:
        if (airedCount - typeCount > 0) {
          overflow.addItem(R.id.action_collection_add, R.string.action_collect_next);
          if (typeCount < airedCount) {
            overflow.addItem(R.id.action_collection_add_all, R.string.action_collection_add_all);
          }
        }
        if (typeCount > 0) {
          overflow.addItem(R.id.action_collection_remove_all,
              R.string.action_collection_remove_all);
        }
        break;
    }
  }

  public static class ViewHolder {

    @InjectView(R.id.infoParent) View infoParent;
    @InjectView(R.id.title) TextView title;
    @InjectView(R.id.watched) TextView watched;
    @InjectView(R.id.progress) ProgressBar progressBar;
    @InjectView(R.id.nextEpisode) TextView nextEpisode;
    @InjectView(R.id.firstAired) TimeStamp firstAired;
    @InjectView(R.id.overflow) OverflowView overflow;
    @InjectView(R.id.poster) RemoteImageView poster;

    ViewHolder(View v) {
      ButterKnife.inject(this, v);
    }
  }
}
