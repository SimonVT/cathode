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
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.dialog.CheckInDialog;
import net.simonvt.cathode.ui.dialog.CheckInDialog.Type;
import net.simonvt.cathode.widget.OverflowView;
import net.simonvt.cathode.widget.RemoteImageView;
import net.simonvt.cathode.widget.TimeStamp;

/**
 * A show adapter that displays the next episode as well.
 */
public class ShowsWithNextAdapter extends CursorAdapter {

  private static final String COLUMN_EPISODE_ID = "episodeId";

  public static final String[] PROJECTION = new String[] {
      DatabaseSchematic.Tables.SHOWS + "." + ShowColumns.ID,
      DatabaseSchematic.Tables.SHOWS + "." + ShowColumns.TITLE,
      DatabaseSchematic.Tables.SHOWS + "." + ShowColumns.POSTER,
      ShowColumns.AIRED_COUNT,
      DatabaseSchematic.Tables.SHOWS + "." + ShowColumns.WATCHED_COUNT,
      DatabaseSchematic.Tables.SHOWS + "." + ShowColumns.IN_COLLECTION_COUNT,
      DatabaseSchematic.Tables.SHOWS + "." + ShowColumns.STATUS,
      DatabaseSchematic.Tables.SHOWS + "." + ShowColumns.HIDDEN,
      ShowColumns.WATCHING,
      DatabaseSchematic.Tables.EPISODES + "." + EpisodeColumns.ID
          + " AS " + COLUMN_EPISODE_ID,
      DatabaseSchematic.Tables.EPISODES + "." + EpisodeColumns.TITLE,
      DatabaseSchematic.Tables.EPISODES + "." + EpisodeColumns.FIRST_AIRED,
      DatabaseSchematic.Tables.EPISODES + "." + EpisodeColumns.SEASON,
      DatabaseSchematic.Tables.EPISODES + "." + EpisodeColumns.EPISODE,
  };

  @Inject ShowTaskScheduler showScheduler;

  private FragmentActivity activity;

  private final LibraryType libraryType;

  public ShowsWithNextAdapter(FragmentActivity activity, LibraryType libraryType) {
    this(activity, null, libraryType);
  }

  public ShowsWithNextAdapter(FragmentActivity activity, Cursor cursor, LibraryType libraryType) {
    super(activity, cursor, 0);
    CathodeApp.inject(activity, this);
    this.activity = activity;
    this.libraryType = libraryType;
  }

  @Override public View newView(Context context, Cursor cursor, ViewGroup parent) {
    View v = LayoutInflater.from(context).inflate(R.layout.list_row_show, parent, false);

    ViewHolder vh = new ViewHolder(v);
    v.setTag(vh);

    return v;
  }

  @Override public void bindView(final View view, Context context, Cursor cursor) {
    final long id = cursor.getLong(cursor.getColumnIndex(ShowColumns.ID));
    final int position = cursor.getPosition();

    final String showPosterUrl =
        cursor.getString(cursor.getColumnIndex(ShowColumns.POSTER));
    final String showTitle = cursor.getString(cursor.getColumnIndex(ShowColumns.TITLE));
    final String showStatus = cursor.getString(cursor.getColumnIndex(ShowColumns.STATUS));
    final boolean isHidden =
        cursor.getInt(cursor.getColumnIndex(ShowColumns.HIDDEN)) == 1;
    final boolean watching =
        cursor.getInt(cursor.getColumnIndex(ShowColumns.WATCHING)) == 1;

    final int showAiredCount =
        cursor.getInt(cursor.getColumnIndex(ShowColumns.AIRED_COUNT));
    int count = 0;
    switch (libraryType) {
      case WATCHED:
      case WATCHLIST:
        count = cursor.getInt(cursor.getColumnIndex(ShowColumns.WATCHED_COUNT));
        break;

      case COLLECTION:
        count = cursor.getInt(cursor.getColumnIndex(ShowColumns.IN_COLLECTION_COUNT));
        break;
    }
    final int showTypeCount = count;

    final long episodeId = cursor.getLong(cursor.getColumnIndex(COLUMN_EPISODE_ID));
    final String episodeTitle =
        cursor.getString(cursor.getColumnIndex(EpisodeColumns.TITLE));
    final long episodeFirstAired =
        cursor.getLong(cursor.getColumnIndex(EpisodeColumns.FIRST_AIRED));
    final int episodeSeasonNumber =
        cursor.getInt(cursor.getColumnIndex(EpisodeColumns.SEASON));
    final int episodeNumber =
        cursor.getInt(cursor.getColumnIndex(EpisodeColumns.EPISODE));

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
      if (watching) {
        episodeText = context.getString(R.string.show_watching);
      } else {
        episodeText = context.getString(R.string.episode_next, episodeSeasonNumber, episodeNumber,
            episodeTitle);
      }
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
            onWatchNext(view, position, id, showTypeCount, showAiredCount);
            break;

          case R.id.action_watched_all:
            showScheduler.setWatched(id, true);
            break;

          case R.id.action_unwatch_all:
            showScheduler.setWatched(id, false);
            break;

          case R.id.action_checkin:
            CheckInDialog.showDialogIfNecessary(activity, Type.SHOW, episodeTitle, episodeId);
            break;

          case R.id.action_checkin_cancel:
            showScheduler.cancelCheckin();
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
    setupOverflowItems(vh.overflow, showTypeCount, showAiredCount, episodeTitle != null, isHidden,
        watching);

    vh.poster.setImage(showPosterUrl);
  }

  protected void onWatchNext(View view, int position, long showId, int watchedCount,
      int airedCount) {
    showScheduler.watchedNext(showId);
  }

  protected void setupOverflowItems(OverflowView overflow, int typeCount, int airedCount,
      boolean hasNext, boolean isHidden, boolean watching) {
    switch (libraryType) {
      case WATCHLIST:
        overflow.addItem(R.id.action_watchlist_remove, R.string.action_watchlist_remove);

      case WATCHED:
        if (airedCount - typeCount > 0) {
          if (!watching && hasNext) {
            overflow.addItem(R.id.action_checkin, R.string.action_checkin);
            overflow.addItem(R.id.action_watched, R.string.action_watched);
          } else if (watching) {
            overflow.addItem(R.id.action_checkin_cancel, R.string.action_checkin_cancel);
          }
        }
        break;

      case COLLECTION:
        if (airedCount - typeCount > 0) {
          overflow.addItem(R.id.action_collection_add, R.string.action_collect_next);
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
