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
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.LastModifiedColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
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
public class ShowsWithNextAdapter extends RecyclerCursorAdapter<ShowsWithNextAdapter.ViewHolder> {

  private static final String COLUMN_EPISODE_ID = "episodeId";
  private static final String COLUMN_EPISODE_LAST_UPDATED = "episodeLastUpdated";

  public static final String[] PROJECTION = new String[] {
      Tables.SHOWS + "." + ShowColumns.ID, Tables.SHOWS + "." + ShowColumns.TITLE,
      Tables.SHOWS + "." + ShowColumns.OVERVIEW, Tables.SHOWS + "." + ShowColumns.POSTER,
      ShowColumns.AIRED_COUNT, Tables.SHOWS + "." + ShowColumns.WATCHED_COUNT,
      Tables.SHOWS + "." + ShowColumns.IN_COLLECTION_COUNT, Tables.SHOWS + "." + ShowColumns.STATUS,
      ShowColumns.WATCHING, Tables.SHOWS + "." + ShowColumns.LAST_MODIFIED,
      Tables.EPISODES + "." + EpisodeColumns.ID + " AS " + COLUMN_EPISODE_ID,
      Tables.EPISODES + "." + EpisodeColumns.TITLE,
      Tables.EPISODES + "." + EpisodeColumns.FIRST_AIRED,
      Tables.EPISODES + "." + EpisodeColumns.SEASON, Tables.EPISODES + "." + EpisodeColumns.EPISODE,
      Tables.EPISODES + "." + EpisodeColumns.LAST_MODIFIED + " AS " + COLUMN_EPISODE_LAST_UPDATED,
  };

  @Inject ShowTaskScheduler showScheduler;

  private FragmentActivity activity;

  private final LibraryType libraryType;

  protected ShowClickListener clickListener;

  public ShowsWithNextAdapter(FragmentActivity activity, ShowClickListener clickListener,
      LibraryType libraryType) {
    this(activity, clickListener, null, libraryType);
  }

  public ShowsWithNextAdapter(FragmentActivity activity, ShowClickListener clickListener,
      Cursor cursor, LibraryType libraryType) {
    super(activity, cursor);
    CathodeApp.inject(activity, this);
    this.activity = activity;
    this.clickListener = clickListener;
    this.libraryType = libraryType;
  }

  @Override public long getLastModified(int position) {
    Cursor cursor = getCursor(position);

    final long showLastModified =
        cursor.getLong(cursor.getColumnIndexOrThrow(LastModifiedColumns.LAST_MODIFIED));
    final long episodeLastModified =
        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_EPISODE_LAST_UPDATED));

    return showLastModified + episodeLastModified;
  }

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(getContext()).inflate(R.layout.list_row_show, parent, false);

    final ViewHolder holder = new ViewHolder(v);

    v.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        final int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
          clickListener.onShowClick(holder.itemView, position, holder.getItemId());
        }
      }
    });

    holder.overflow.setListener(new OverflowView.OverflowActionListener() {

      @Override public void onPopupShown() {
        holder.setIsRecyclable(false);
      }

      @Override public void onPopupDismissed() {
        holder.setIsRecyclable(true);
      }

      @Override public void onActionSelected(int action) {
        holder.setIsRecyclable(true);

        switch (action) {
          case R.id.action_watchlist_remove:
            showScheduler.setIsInWatchlist(holder.getItemId(), false);
            break;

          case R.id.action_watched:
            onWatchNext(holder.itemView, holder.getAdapterPosition(), holder.getItemId(),
                holder.showTypeCount, holder.showAiredCount);
            break;

          case R.id.action_watched_all:
            showScheduler.setWatched(holder.getItemId(), true);
            break;

          case R.id.action_unwatch_all:
            showScheduler.setWatched(holder.getItemId(), false);
            break;

          case R.id.action_checkin:
            CheckInDialog.showDialogIfNecessary(activity, Type.SHOW, holder.episodeTitle,
                holder.episodeId);
            break;

          case R.id.action_checkin_cancel:
            showScheduler.cancelCheckin();
            break;

          case R.id.action_collection_add:
            showScheduler.collectedNext(holder.getItemId());
            break;
        }
      }
    });

    return holder;
  }

  @Override public void onViewRecycled(ViewHolder holder) {
    holder.overflow.dismiss();
  }

  @Override protected void onBindViewHolder(ViewHolder holder, Cursor cursor, int position) {
    final String showPosterUrl = cursor.getString(cursor.getColumnIndex(ShowColumns.POSTER));
    final String showTitle = cursor.getString(cursor.getColumnIndex(ShowColumns.TITLE));
    final String showStatus = cursor.getString(cursor.getColumnIndex(ShowColumns.STATUS));
    final boolean watching = cursor.getInt(cursor.getColumnIndex(ShowColumns.WATCHING)) == 1;

    final int showAiredCount = cursor.getInt(cursor.getColumnIndex(ShowColumns.AIRED_COUNT));
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
    final String episodeTitle = cursor.getString(cursor.getColumnIndex(EpisodeColumns.TITLE));
    final long episodeFirstAired =
        cursor.getLong(cursor.getColumnIndex(EpisodeColumns.FIRST_AIRED));
    final int episodeSeasonNumber = cursor.getInt(cursor.getColumnIndex(EpisodeColumns.SEASON));
    final int episodeNumber = cursor.getInt(cursor.getColumnIndex(EpisodeColumns.EPISODE));

    holder.title.setText(showTitle);

    holder.progressBar.setMax(showAiredCount);
    holder.progressBar.setProgress(showTypeCount);
    final String typeCount = getContext().getString(R.string.x_of_y, showTypeCount, showAiredCount);
    holder.watched.setText(typeCount);

    String episodeText;
    if (episodeTitle == null) {
      episodeText = showStatus;
      holder.firstAired.setVisibility(View.GONE);
    } else {
      if (watching) {
        episodeText = getContext().getString(R.string.show_watching);
      } else {
        episodeText =
            getContext().getString(R.string.episode_next, episodeSeasonNumber, episodeNumber,
                episodeTitle);
      }
      holder.firstAired.setVisibility(View.VISIBLE);
      holder.firstAired.setTimeInMillis(episodeFirstAired);
    }
    holder.nextEpisode.setText(episodeText);

    holder.overflow.setVisibility(showAiredCount > 0 ? View.VISIBLE : View.INVISIBLE);

    holder.overflow.removeItems();
    setupOverflowItems(holder.overflow, showTypeCount, showAiredCount, episodeTitle != null,
        watching);

    holder.poster.setImage(showPosterUrl);

    holder.showTypeCount = showTypeCount;
    holder.showAiredCount = showAiredCount;
    holder.episodeTitle = episodeTitle;
    holder.episodeId = episodeId;
  }

  protected void onWatchNext(View view, int position, long showId, int watchedCount,
      int airedCount) {
    showScheduler.watchedNext(showId);
  }

  protected void setupOverflowItems(OverflowView overflow, int typeCount, int airedCount,
      boolean hasNext, boolean watching) {
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

  public static class ViewHolder extends RecyclerView.ViewHolder {

    @Bind(R.id.title) TextView title;
    @Bind(R.id.watched) TextView watched;
    @Bind(R.id.progress) ProgressBar progressBar;
    @Bind(R.id.nextEpisode) TextView nextEpisode;
    @Bind(R.id.firstAired) TimeStamp firstAired;
    @Bind(R.id.overflow) OverflowView overflow;
    @Bind(R.id.poster) RemoteImageView poster;

    public int showTypeCount;
    public int showAiredCount;
    public String episodeTitle;
    public long episodeId;

    ViewHolder(View v) {
      super(v);
      ButterKnife.bind(this, v);
    }
  }
}
