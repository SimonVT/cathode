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
package net.simonvt.cathode.ui.shows.watchlist;

import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.ui.adapter.HeaderCursorAdapter;
import net.simonvt.cathode.common.widget.CircularProgressIndicator;
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
import net.simonvt.cathode.ui.history.AddToHistoryDialog;
import net.simonvt.cathode.widget.IndicatorView;
import net.simonvt.schematic.Cursors;

public class ShowWatchlistAdapter extends HeaderCursorAdapter<RecyclerView.ViewHolder> {

  public interface RemoveListener {

    void onRemoveItem(int position, long itemId);
  }

  public interface ItemCallbacks {

    void onShowClicked(long showId, String title, String overview);

    void onRemoveShowFromWatchlist(long showId);

    void onEpisodeClicked(long episodeId, String showTitle);

    void onRemoveEpisodeFromWatchlist(long episodeId);
  }

  static final String[] PROJECTION_SHOW = new String[] {
      Tables.SHOWS + "." + ShowColumns.ID,
      Tables.SHOWS + "." + ShowColumns.TITLE,
      Tables.SHOWS + "." + ShowColumns.OVERVIEW,
      Tables.SHOWS + "." + ShowColumns.TVDB_ID,
      Tables.SHOWS + "." + ShowColumns.WATCHED_COUNT,
      Tables.SHOWS + "." + ShowColumns.IN_COLLECTION_COUNT,
      Tables.SHOWS + "." + ShowColumns.IN_WATCHLIST,
      Tables.SHOWS + "." + ShowColumns.RATING,
      Tables.SHOWS + "." + LastModifiedColumns.LAST_MODIFIED,
  };

  static final String[] PROJECTION_EPISODE = new String[] {
      Tables.EPISODES + "." + EpisodeColumns.ID,
      Tables.EPISODES + "." + EpisodeColumns.TITLE,
      Tables.EPISODES + "." + EpisodeColumns.WATCHED,
      Tables.EPISODES + "." + EpisodeColumns.FIRST_AIRED,
      Tables.EPISODES + "." + EpisodeColumns.SEASON,
      Tables.EPISODES + "." + EpisodeColumns.EPISODE,
      Tables.EPISODES + "." + LastModifiedColumns.LAST_MODIFIED,
      Tables.SHOWS + "." + ShowColumns.TITLE,
  };

  private static final int TYPE_SHOW = 0;

  private static final int TYPE_EPISODE = 1;

  private FragmentActivity activity;

  private RemoveListener onRemoveListener;

  private ItemCallbacks itemCallbacks;

  public ShowWatchlistAdapter(FragmentActivity activity, ItemCallbacks itemCallbacks,
      RemoveListener onRemoveListener) {
    this.activity = activity;
    this.itemCallbacks = itemCallbacks;
    this.onRemoveListener = onRemoveListener;
    setHasStableIds(false);
  }

  @Override public long getLastModified(int position) {
    if (!isHeader(position)) {
      Cursor cursor = getCursor(position);
      return Cursors.getLong(cursor, LastModifiedColumns.LAST_MODIFIED);
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
      View v = LayoutInflater.from(parent.getContext())
          .inflate(R.layout.list_row_show_description_rating, parent, false);
      final ShowViewHolder holder = new ShowViewHolder(v);
      holder.itemView.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View view) {
          final int position = holder.getAdapterPosition();
          if (position != RecyclerView.NO_POSITION) {
            Cursor cursor = getCursor(position);
            final long id = getItemId(position);
            final String title = Cursors.getString(cursor, ShowColumns.TITLE);
            final String overview = Cursors.getString(cursor, ShowColumns.OVERVIEW);
            itemCallbacks.onShowClicked(id, title, overview);
          }
        }
      });
      return holder;
    } else {
      View v = LayoutInflater.from(parent.getContext())
          .inflate(R.layout.list_row_watchlist_episode, parent, false);
      final EpisodeViewHolder holder = new EpisodeViewHolder(v);
      holder.itemView.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View view) {
          final int position = holder.getAdapterPosition();
          if (position != RecyclerView.NO_POSITION) {
            Cursor cursor = getCursor(position);
            final long id = getItemId(position);
            final String showTitle = Cursors.getString(cursor, ShowColumns.TITLE);
            itemCallbacks.onEpisodeClicked(id, showTitle);
          }
        }
      });
      return holder;
    }
  }

  @Override protected RecyclerView.ViewHolder onCreateHeaderHolder(ViewGroup parent) {
    View v = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.list_row_upcoming_header, parent, false);
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
      final ShowViewHolder vh = (ShowViewHolder) holder;

      final long id = Cursors.getLong(cursor, ShowColumns.ID);
      final boolean watched = Cursors.getInt(cursor, ShowColumns.WATCHED_COUNT) > 0;
      final boolean inCollection = Cursors.getInt(cursor, ShowColumns.IN_COLLECTION_COUNT) > 1;
      final boolean inWatchlist = Cursors.getBoolean(cursor, ShowColumns.IN_WATCHLIST);
      final float rating = Cursors.getFloat(cursor, ShowColumns.RATING);

      final String poster = ImageUri.create(ImageUri.ITEM_SHOW, ImageType.POSTER, id);

      vh.indicator.setWatched(watched);
      vh.indicator.setCollected(inCollection);
      vh.indicator.setInWatchlist(inWatchlist);

      vh.poster.setImage(poster);
      vh.title.setText(Cursors.getString(cursor, ShowColumns.TITLE));
      vh.overview.setText(Cursors.getString(cursor, ShowColumns.OVERVIEW));

      vh.rating.setValue(rating);

      vh.overflow.setListener(new OverflowView.OverflowActionListener() {

        @Override public void onPopupShown() {
        }

        @Override public void onPopupDismissed() {
        }

        @Override public void onActionSelected(int action) {
          final int position = holder.getAdapterPosition();
          if (position != RecyclerView.NO_POSITION) {
            switch (action) {
              case R.id.action_watchlist_remove:
                itemCallbacks.onRemoveShowFromWatchlist(id);
                onRemoveListener.onRemoveItem(position, id);
            }
          }
        }
      });
    } else {
      EpisodeViewHolder vh = (EpisodeViewHolder) holder;

      final long id = Cursors.getLong(cursor, EpisodeColumns.ID);
      final long firstAired = DataHelper.getFirstAired(cursor);
      final int season = Cursors.getInt(cursor, EpisodeColumns.SEASON);
      final int episode = Cursors.getInt(cursor, EpisodeColumns.EPISODE);
      final boolean watched = Cursors.getBoolean(cursor, EpisodeColumns.WATCHED);
      final String title = DataHelper.getEpisodeTitle(activity, cursor, season, episode, watched);

      final String screenshotUri = ImageUri.create(ImageUri.ITEM_EPISODE, ImageType.STILL, id);

      vh.screen.setImage(screenshotUri);
      vh.title.setText(title);
      vh.firstAired.setTimeInMillis(firstAired);
      final String episodeNumber = activity.getString(R.string.season_x_episode_y, season, episode);
      vh.episode.setText(episodeNumber);
      vh.overflow.setListener(new OverflowView.OverflowActionListener() {

        @Override public void onPopupShown() {
        }

        @Override public void onPopupDismissed() {
        }

        @Override public void onActionSelected(int action) {
          final int position = holder.getAdapterPosition();
          if (position != RecyclerView.NO_POSITION) {
            switch (action) {
              case R.id.action_history_add:
                AddToHistoryDialog.newInstance(AddToHistoryDialog.Type.EPISODE, id, title)
                    .show(activity.getSupportFragmentManager(), AddToHistoryDialog.TAG);
                break;

              case R.id.action_watchlist_remove:
                itemCallbacks.onRemoveEpisodeFromWatchlist(id);
                onRemoveListener.onRemoveItem(position, id);
                break;
            }
          }
        }
      });
    }
  }

  static class HeaderViewHolder extends RecyclerView.ViewHolder {

    TextView header;

    HeaderViewHolder(TextView header) {
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

    EpisodeViewHolder(View v) {
      super(v);
      ButterKnife.bind(this, v);
    }
  }
}
