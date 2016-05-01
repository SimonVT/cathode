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
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.LastModifiedColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.widget.CircularProgressIndicator;
import net.simonvt.cathode.widget.IndicatorView;
import net.simonvt.cathode.widget.OverflowView;
import net.simonvt.cathode.widget.RemoteImageView;
import net.simonvt.cathode.widget.TimeStamp;

public class ShowWatchlistAdapter extends HeaderCursorAdapter<RecyclerView.ViewHolder> {

  public interface RemoveListener {

    void onRemoveItem(View view, int position, long id);
  }

  public interface OnItemClickListener {

    void onShowClicked(int position, long id);

    void onEpisodeClicked(int position, long id);
  }

  public static final String[] PROJECTION_SHOW = new String[] {
      Tables.SHOWS + "." + ShowColumns.ID, Tables.SHOWS + "." + ShowColumns.TITLE,
      Tables.SHOWS + "." + ShowColumns.OVERVIEW, Tables.SHOWS + "." + ShowColumns.POSTER,
      Tables.SHOWS + "." + ShowColumns.TVDB_ID, Tables.SHOWS + "." + ShowColumns.WATCHED_COUNT,
      Tables.SHOWS + "." + ShowColumns.IN_COLLECTION_COUNT,
      Tables.SHOWS + "." + ShowColumns.IN_WATCHLIST, Tables.SHOWS + "." + ShowColumns.RATING,
      Tables.SHOWS + "." + LastModifiedColumns.LAST_MODIFIED,
  };

  public static final String[] PROJECTION_EPISODE = new String[] {
      Tables.EPISODES + "." + EpisodeColumns.ID, Tables.EPISODES + "." + EpisodeColumns.SCREENSHOT,
      Tables.EPISODES + "." + EpisodeColumns.TITLE,
      Tables.EPISODES + "." + EpisodeColumns.FIRST_AIRED,
      Tables.EPISODES + "." + EpisodeColumns.SEASON, Tables.EPISODES + "." + EpisodeColumns.EPISODE,
      Tables.EPISODES + "." + LastModifiedColumns.LAST_MODIFIED,
      Tables.SHOWS + "." + ShowColumns.TITLE,
  };

  private static final int TYPE_SHOW = 0;

  private static final int TYPE_EPISODE = 1;

  @Inject ShowTaskScheduler showScheduler;

  @Inject EpisodeTaskScheduler episodeScheduler;

  private Context context;

  private RemoveListener onRemoveListener;

  private OnItemClickListener onItemClickListener;

  public ShowWatchlistAdapter(Context context, OnItemClickListener onItemClickListener,
      RemoveListener onRemoveListener) {
    super();
    this.context = context;
    this.onItemClickListener = onItemClickListener;
    this.onRemoveListener = onRemoveListener;
    CathodeApp.inject(context, this);
  }

  @Override public long getLastModified(int position) {
    if (!isHeader(position)) {
      Cursor cursor = getCursor(position);
      return cursor.getLong(cursor.getColumnIndexOrThrow(LastModifiedColumns.LAST_MODIFIED));
    }

    return super.getLastModified(position);
  }

  @Override protected int getItemViewType(int headerRes, Cursor cursor) {
    if (headerRes == R.string.header_shows) {
      return TYPE_SHOW;
    }

    return TYPE_EPISODE;
  }

  @Override protected RecyclerView.ViewHolder onCreateItemHolder(ViewGroup parent, int viewType) {
    if (viewType == TYPE_SHOW) {
      View v =
          LayoutInflater.from(context).inflate(R.layout.list_row_show_description, parent, false);
      final ShowViewHolder holder = new ShowViewHolder(v);
      holder.itemView.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View view) {
          final int position = holder.getAdapterPosition();
          if (position != RecyclerView.NO_POSITION) {
            onItemClickListener.onShowClicked(position, holder.getItemId());
          }
        }
      });
      return holder;
    } else {
      View v =
          LayoutInflater.from(context).inflate(R.layout.list_row_watchlist_episode, parent, false);
      final EpisodeViewHolder holder = new EpisodeViewHolder(v);
      holder.itemView.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View view) {
          final int position = holder.getAdapterPosition();
          if (position != RecyclerView.NO_POSITION) {
            onItemClickListener.onEpisodeClicked(position, holder.getItemId());
          }
        }
      });
      return holder;
    }
  }

  @Override protected RecyclerView.ViewHolder onCreateHeaderHolder(ViewGroup parent) {
    View v = LayoutInflater.from(context).inflate(R.layout.list_row_upcoming_header, parent, false);
    return new HeaderViewHolder((TextView) v);
  }

  @Override public void onViewRecycled(RecyclerView.ViewHolder holder) {
    if (holder instanceof ShowViewHolder) {
      ((ShowViewHolder) holder).overflow.dismiss();
    } else if (holder instanceof EpisodeViewHolder) {
      ((EpisodeViewHolder) holder).overflow.dismiss();
    }
  }

  @Override protected void onBindHeader(RecyclerView.ViewHolder holder, int headerRes) {
    ((HeaderViewHolder) holder).header.setText(headerRes);
  }

  @Override protected void onBindViewHolder(final RecyclerView.ViewHolder holder, Cursor cursor,
      final int position) {
    if (holder.getItemViewType() == TYPE_SHOW) {
      ShowViewHolder vh = (ShowViewHolder) holder;

      final long id = cursor.getLong(cursor.getColumnIndex(ShowColumns.ID));
      final boolean watched = cursor.getInt(cursor.getColumnIndex(ShowColumns.WATCHED_COUNT)) > 0;
      final boolean inCollection =
          cursor.getInt(cursor.getColumnIndex(ShowColumns.IN_COLLECTION_COUNT)) > 1;
      final boolean inWatchlist =
          cursor.getInt(cursor.getColumnIndex(ShowColumns.IN_WATCHLIST)) == 1;
      final float rating = cursor.getFloat(cursor.getColumnIndex(ShowColumns.RATING));

      vh.indicator.setWatched(watched);
      vh.indicator.setCollected(inCollection);
      vh.indicator.setInWatchlist(inWatchlist);

      vh.poster.setImage(cursor.getString(cursor.getColumnIndex(ShowColumns.POSTER)));
      vh.title.setText(cursor.getString(cursor.getColumnIndex(ShowColumns.TITLE)));
      vh.overview.setText(cursor.getString(cursor.getColumnIndex(ShowColumns.OVERVIEW)));

      vh.rating.setValue(rating);

      final View view = holder.itemView;
      vh.overflow.setListener(new OverflowView.OverflowActionListener() {

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
              onRemoveListener.onRemoveItem(view, position, id);
              showScheduler.setIsInWatchlist(id, false);
          }
        }
      });
    } else {
      EpisodeViewHolder vh = (EpisodeViewHolder) holder;

      final long id = cursor.getLong(cursor.getColumnIndex(EpisodeColumns.ID));
      final String screenshotUrl =
          cursor.getString(cursor.getColumnIndexOrThrow(EpisodeColumns.SCREENSHOT));
      String title = cursor.getString(cursor.getColumnIndexOrThrow(EpisodeColumns.TITLE));
      final long firstAired =
          cursor.getLong(cursor.getColumnIndexOrThrow(EpisodeColumns.FIRST_AIRED));
      final int season = cursor.getInt(cursor.getColumnIndexOrThrow(EpisodeColumns.SEASON));
      final int episode = cursor.getInt(cursor.getColumnIndexOrThrow(EpisodeColumns.EPISODE));

      if (TextUtils.isEmpty(title)) {
        if (season == 0) {
          title = context.getString(R.string.special_x, episode);
        } else {
          title = context.getString(R.string.episode_x, episode);
        }
      }

      vh.screen.setImage(screenshotUrl);
      vh.title.setText(title);
      vh.firstAired.setTimeInMillis(firstAired);
      final String episodeNumber = context.getString(R.string.season_x_episode_y, season, episode);
      vh.episode.setText(episodeNumber);
      final View view = holder.itemView;
      vh.overflow.setListener(new OverflowView.OverflowActionListener() {

        @Override public void onPopupShown() {
          holder.setIsRecyclable(false);
        }

        @Override public void onPopupDismissed() {
          holder.setIsRecyclable(true);
        }

        @Override public void onActionSelected(int action) {
          holder.setIsRecyclable(true);

          switch (action) {
            case R.id.action_watched:
              episodeScheduler.setWatched(id, true);
              break;

            case R.id.action_watchlist_remove:
              onRemoveListener.onRemoveItem(view, position, id);
              episodeScheduler.setIsInWatchlist(id, false);
              break;
          }
        }
      });
    }
  }

  static class HeaderViewHolder extends RecyclerView.ViewHolder {

    TextView header;

    public HeaderViewHolder(TextView header) {
      super(header);
      this.header = header;
    }
  }

  static class ShowViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.poster) RemoteImageView poster;
    @BindView(R.id.indicator) IndicatorView indicator;
    @BindView(R.id.title) TextView title;
    @BindView(R.id.overview) TextView overview;
    @BindView(R.id.overflow) OverflowView overflow;
    @BindView(R.id.rating) CircularProgressIndicator rating;

    ShowViewHolder(View v) {
      super(v);
      ButterKnife.bind(this, v);
    }
  }

  static class EpisodeViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.screen) RemoteImageView screen;
    @BindView(R.id.title) TextView title;
    @BindView(R.id.firstAired) TimeStamp firstAired;
    @BindView(R.id.episode) TextView episode;
    @BindView(R.id.overflow) OverflowView overflow;

    public EpisodeViewHolder(View v) {
      super(v);
      ButterKnife.bind(this, v);
    }
  }
}
