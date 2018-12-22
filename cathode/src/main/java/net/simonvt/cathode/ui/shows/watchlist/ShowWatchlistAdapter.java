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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.entity.Episode;
import net.simonvt.cathode.common.entity.Show;
import net.simonvt.cathode.common.ui.adapter.HeaderAdapter;
import net.simonvt.cathode.common.widget.CircularProgressIndicator;
import net.simonvt.cathode.common.widget.OverflowView;
import net.simonvt.cathode.common.widget.RemoteImageView;
import net.simonvt.cathode.common.widget.TimeStamp;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.util.DataHelper;
import net.simonvt.cathode.ui.history.AddToHistoryDialog;
import net.simonvt.cathode.widget.IndicatorView;

public class ShowWatchlistAdapter extends HeaderAdapter<Object, RecyclerView.ViewHolder> {

  public interface RemoveListener {

    void onRemoveItem(Object object);
  }

  public interface ItemCallbacks {

    void onShowClicked(long showId, String title, String overview);

    void onRemoveShowFromWatchlist(long showId);

    void onEpisodeClicked(long episodeId, String showTitle);

    void onRemoveEpisodeFromWatchlist(long episodeId);
  }

  static final String[] PROJECTION_SHOW = new String[] {
      Tables.SHOWS + "." + ShowColumns.ID, Tables.SHOWS + "." + ShowColumns.TITLE,
      Tables.SHOWS + "." + ShowColumns.OVERVIEW, Tables.SHOWS + "." + ShowColumns.TVDB_ID,
      Tables.SHOWS + "." + ShowColumns.WATCHED_COUNT,
      Tables.SHOWS + "." + ShowColumns.IN_COLLECTION_COUNT,
      Tables.SHOWS + "." + ShowColumns.IN_WATCHLIST, Tables.SHOWS + "." + ShowColumns.RATING,
  };

  static final String[] PROJECTION_EPISODE = new String[] {
      Tables.EPISODES + "." + EpisodeColumns.ID, Tables.EPISODES + "." + EpisodeColumns.TITLE,
      Tables.EPISODES + "." + EpisodeColumns.WATCHED,
      Tables.EPISODES + "." + EpisodeColumns.FIRST_AIRED,
      Tables.EPISODES + "." + EpisodeColumns.SEASON, Tables.EPISODES + "." + EpisodeColumns.EPISODE,
      Tables.EPISODES + "." + EpisodeColumns.TVDB_ID,
      Tables.SHOWS + "." + ShowColumns.TITLE,
  };

  private static final int TYPE_SHOW = 0;

  private static final int TYPE_EPISODE = 1;

  private FragmentActivity activity;

  private RemoveListener onRemoveListener;

  private ItemCallbacks itemCallbacks;

  public ShowWatchlistAdapter(FragmentActivity activity, ItemCallbacks itemCallbacks,
      RemoveListener onRemoveListener) {
    super(activity);
    this.activity = activity;
    this.itemCallbacks = itemCallbacks;
    this.onRemoveListener = onRemoveListener;
    setHasStableIds(true);
  }

  @Override protected int getItemViewType(int headerRes, Object item) {
    if (headerRes == R.string.header_shows) {
      return TYPE_SHOW;
    }

    return TYPE_EPISODE;
  }

  @Override protected long getItemId(Object item) {
    if (item instanceof Episode) {
      return ((Episode) item).getTvdbId();
    }

    return ((Show) item).getTvdbId();
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
            Show show = (Show) getItem(position);
            itemCallbacks.onShowClicked(show.getId(), show.getTitle(), show.getOverview());
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
            Episode episode = (Episode) getItem(position);
            itemCallbacks.onEpisodeClicked(episode.getId(), episode.getShowTitle());
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

  @Override
  protected void onBindViewHolder(RecyclerView.ViewHolder holder, Object object, int position) {
    if (holder.getItemViewType() == TYPE_SHOW) {
      final ShowViewHolder vh = (ShowViewHolder) holder;
      Show show = (Show) object;

      final long id = show.getId();
      final String poster = ImageUri.create(ImageUri.ITEM_SHOW, ImageType.POSTER, id);

      vh.indicator.setWatched(show.getWatchedCount() > 0);
      vh.indicator.setCollected(show.getInCollectionCount() > 1);
      vh.indicator.setInWatchlist(show.getInWatchlist());

      vh.poster.setImage(poster);
      vh.title.setText(show.getTitle());
      vh.overview.setText(show.getOverview());

      vh.rating.setValue(show.getRating());

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
                onRemoveListener.onRemoveItem(object);
            }
          }
        }
      });
    } else {
      EpisodeViewHolder vh = (EpisodeViewHolder) holder;
      Episode episode = (Episode) object;

      final long id = episode.getId();
      final long firstAired = episode.getFirstAired();
      final int season = episode.getSeason();
      final int number = episode.getEpisode();
      final boolean watched = episode.getWatched();
      final String title =
          DataHelper.getEpisodeTitle(activity, episode.getTitle(), season, number, watched);

      final String screenshotUri = ImageUri.create(ImageUri.ITEM_EPISODE, ImageType.STILL, id);

      vh.screen.setImage(screenshotUri);
      vh.title.setText(title);
      vh.firstAired.setTimeInMillis(firstAired);
      final String episodeNumber = activity.getString(R.string.season_x_episode_y, season, number);
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
                onRemoveListener.onRemoveItem(object);
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
