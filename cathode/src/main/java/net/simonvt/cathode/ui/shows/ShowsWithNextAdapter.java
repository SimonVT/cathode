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
package net.simonvt.cathode.ui.shows;

import android.database.Cursor;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.Injector;
import net.simonvt.cathode.common.ui.adapter.RecyclerCursorAdapter;
import net.simonvt.cathode.common.widget.OverflowView;
import net.simonvt.cathode.common.widget.RemoteImageView;
import net.simonvt.cathode.common.widget.TimeStamp;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.LastModifiedColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.util.DataHelper;
import net.simonvt.cathode.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.dialog.CheckInDialog;
import net.simonvt.cathode.ui.dialog.CheckInDialog.Type;
import net.simonvt.cathode.ui.history.AddToHistoryDialog;
import net.simonvt.schematic.Cursors;

/**
 * A show adapter that displays the next episode as well.
 */
public class ShowsWithNextAdapter extends RecyclerCursorAdapter<ShowsWithNextAdapter.ViewHolder> {

  private static final String COLUMN_EPISODE_ID = "episodeId";
  private static final String COLUMN_EPISODE_LAST_UPDATED = "episodeLastUpdated";

  public static final String[] PROJECTION = new String[] {
      Tables.SHOWS + "." + ShowColumns.ID,
      Tables.SHOWS + "." + ShowColumns.TITLE,
      Tables.SHOWS + "." + ShowColumns.OVERVIEW,
      ShowColumns.AIRED_COUNT,
      Tables.SHOWS + "." + ShowColumns.WATCHED_COUNT,
      Tables.SHOWS + "." + ShowColumns.IN_COLLECTION_COUNT,
      Tables.SHOWS + "." + ShowColumns.STATUS,
      ShowColumns.WATCHING, Tables.SHOWS + "." + ShowColumns.LAST_MODIFIED,
      Tables.EPISODES + "." + EpisodeColumns.ID + " AS " + COLUMN_EPISODE_ID,
      Tables.EPISODES + "." + EpisodeColumns.TITLE,
      Tables.EPISODES + "." + EpisodeColumns.WATCHED,
      Tables.EPISODES + "." + EpisodeColumns.FIRST_AIRED,
      Tables.EPISODES + "." + EpisodeColumns.SEASON,
      Tables.EPISODES + "." + EpisodeColumns.EPISODE,
      Tables.EPISODES + "." + EpisodeColumns.LAST_MODIFIED + " AS " + COLUMN_EPISODE_LAST_UPDATED,
  };

  @Inject ShowTaskScheduler showScheduler;
  @Inject EpisodeTaskScheduler episodeScheduler;

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
    Injector.inject(this);
    this.activity = activity;
    this.clickListener = clickListener;
    this.libraryType = libraryType;
  }

  @Override public long getLastModified(int position) {
    Cursor cursor = getCursor(position);

    final long showLastModified = Cursors.getLong(cursor, LastModifiedColumns.LAST_MODIFIED);
    final long episodeLastModified = Cursors.getLong(cursor, COLUMN_EPISODE_LAST_UPDATED);

    return showLastModified + episodeLastModified;
  }

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(getContext()).inflate(R.layout.list_row_show, parent, false);

    final ViewHolder holder = new ViewHolder(v);

    v.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        final int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
          Cursor cursor = getCursor(position);
          final String title = Cursors.getString(cursor, ShowColumns.TITLE);
          final String overview = Cursors.getString(cursor, ShowColumns.OVERVIEW);
          clickListener.onShowClick(holder.getItemId(), title, overview);
        }
      }
    });

    holder.overflow.setListener(new OverflowView.OverflowActionListener() {

      @Override public void onPopupShown() {
      }

      @Override public void onPopupDismissed() {
      }

      @Override public void onActionSelected(int action) {
        final int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
          switch (action) {
            case R.id.action_watchlist_remove:
              showScheduler.setIsInWatchlist(holder.getItemId(), false);
              break;

            case R.id.action_history_add:
              AddToHistoryDialog.newInstance(AddToHistoryDialog.Type.EPISODE, holder.episodeId,
                  holder.episodeTitle)
                  .show(activity.getSupportFragmentManager(), AddToHistoryDialog.TAG);
              break;

            case R.id.action_checkin:
              CheckInDialog.showDialogIfNecessary(activity, Type.SHOW, holder.episodeTitle,
                  holder.episodeId);
              break;

            case R.id.action_checkin_cancel:
              episodeScheduler.cancelCheckin();
              break;

            case R.id.action_collection_add:
              showScheduler.collectedNext(holder.getItemId());
              break;

            case R.id.action_watched_hide:
              showScheduler.hideFromWatched(holder.getItemId(), true);
              break;

            case R.id.action_collection_hide:
              showScheduler.hideFromCollected(holder.getItemId(), true);
              break;
          }
        }
      }
    });

    return holder;
  }

  @Override public void onViewRecycled(ViewHolder holder) {
    holder.overflow.dismiss();
  }

  @Override protected void onBindViewHolder(ViewHolder holder, Cursor cursor, int position) {
    final long id = Cursors.getLong(cursor, ShowColumns.ID);
    final String showTitle = Cursors.getString(cursor, ShowColumns.TITLE);
    final String showStatus = Cursors.getString(cursor, ShowColumns.STATUS);
    final boolean watching = Cursors.getBoolean(cursor, ShowColumns.WATCHING);

    final String showPosterUri = ImageUri.create(ImageUri.ITEM_SHOW, ImageType.POSTER, id);

    final int showAiredCount = Cursors.getInt(cursor, ShowColumns.AIRED_COUNT);
    int count = 0;
    switch (libraryType) {
      case WATCHED:
      case WATCHLIST:
        count = Cursors.getInt(cursor, ShowColumns.WATCHED_COUNT);
        break;

      case COLLECTION:
        count = Cursors.getInt(cursor, ShowColumns.IN_COLLECTION_COUNT);
        break;
    }
    final int showTypeCount = count;

    final long episodeId = Cursors.getLong(cursor, COLUMN_EPISODE_ID);
    final long episodeFirstAired = DataHelper.getFirstAired(cursor);
    final int episodeSeasonNumber = Cursors.getInt(cursor, EpisodeColumns.SEASON);
    final int episodeNumber = Cursors.getInt(cursor, EpisodeColumns.EPISODE);
    final boolean watched = Cursors.getBoolean(cursor, EpisodeColumns.WATCHED);

    String episodeTitle = null;
    if (episodeSeasonNumber > 0) {
      episodeTitle =
          DataHelper.getEpisodeTitle(getContext(), cursor, episodeSeasonNumber, episodeNumber,
              watched, true);
    }

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
        episodeText = getContext().getString(R.string.episode_next, episodeTitle);
      }
      holder.firstAired.setVisibility(View.VISIBLE);
      holder.firstAired.setTimeInMillis(episodeFirstAired);
    }
    holder.nextEpisode.setText(episodeText);

    holder.overflow.setVisibility(showAiredCount > 0 ? View.VISIBLE : View.INVISIBLE);

    holder.overflow.removeItems();
    setupOverflowItems(holder.overflow, showTypeCount, showAiredCount, episodeTitle != null,
        watching);

    holder.poster.setImage(showPosterUri);

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
            overflow.addItem(R.id.action_history_add, R.string.action_history_add);
          } else if (watching) {
            overflow.addItem(R.id.action_checkin_cancel, R.string.action_checkin_cancel);
          }
        }

        overflow.addItem(R.id.action_watched_hide, R.string.action_watched_hide);
        break;

      case COLLECTION:
        if (airedCount - typeCount > 0) {
          overflow.addItem(R.id.action_collection_add, R.string.action_collect_next);
        }

        overflow.addItem(R.id.action_collection_hide, R.string.action_collection_hide);
        break;
    }
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.title) TextView title;
    @BindView(R.id.watched) TextView watched;
    @BindView(R.id.progress) ProgressBar progressBar;
    @BindView(R.id.nextEpisode) TextView nextEpisode;
    @BindView(R.id.firstAired) TimeStamp firstAired;
    @BindView(R.id.overflow) OverflowView overflow;
    @BindView(R.id.poster) RemoteImageView poster;

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
