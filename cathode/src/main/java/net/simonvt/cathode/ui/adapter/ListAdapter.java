/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.LastModifiedColumns;
import net.simonvt.cathode.provider.DatabaseContract.ListItemColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.ui.listener.EpisodeClickListener;
import net.simonvt.cathode.ui.listener.MovieClickListener;
import net.simonvt.cathode.ui.listener.SeasonClickListener;
import net.simonvt.cathode.widget.OverflowView;
import net.simonvt.cathode.widget.RemoteImageView;
import net.simonvt.schematic.Cursors;

public class ListAdapter extends RecyclerCursorAdapter<ListAdapter.ListViewHolder> {

  public interface OnRemoveItemListener {

    void onRemoveItem(int position, long id);
  }

  private ShowClickListener showListener;

  private SeasonClickListener seasonListener;

  private EpisodeClickListener episodeListener;

  private MovieClickListener movieListener;

  private OnRemoveItemListener removeListener;

  public ListAdapter(Context context, ShowClickListener showListener,
      SeasonClickListener seasonListener, EpisodeClickListener episodeListener,
      MovieClickListener movieListener, OnRemoveItemListener removeListener) {
    this(context, showListener, seasonListener, episodeListener, movieListener, removeListener,
        null);
  }

  public ListAdapter(Context context, ShowClickListener showListener,
      SeasonClickListener seasonListener, EpisodeClickListener episodeListener,
      MovieClickListener movieListener, OnRemoveItemListener removeListener, Cursor cursor) {
    super(context, cursor);
    this.showListener = showListener;
    this.seasonListener = seasonListener;
    this.episodeListener = episodeListener;
    this.movieListener = movieListener;
    this.removeListener = removeListener;
  }

  @Override public int getItemViewType(int position) {
    Cursor cursor = getCursor(position);
    return Cursors.getInt(cursor, ListItemColumns.ITEM_TYPE);
  }

  @Override public long getLastModified(int position) {
    Cursor cursor = getCursor(position);
    return Cursors.getLong(cursor, LastModifiedColumns.LAST_MODIFIED);
  }

  @Override public ListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    ListViewHolder holder;

    if (viewType == DatabaseContract.ItemType.SHOW) {
      View v = LayoutInflater.from(getContext()).inflate(R.layout.row_list_show, parent, false);
      final ShowViewHolder showHolder = new ShowViewHolder(v);
      holder = showHolder;

      v.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          final int position = showHolder.getAdapterPosition();
          if (position != RecyclerView.NO_POSITION) {
            Cursor cursor = getCursor(position);
            final long itemId = Cursors.getLong(cursor, ListItemColumns.ITEM_ID);
            showListener.onShowClick(v, position, itemId);
          }
        }
      });
    } else if (viewType == DatabaseContract.ItemType.SEASON) {
      View v = LayoutInflater.from(getContext()).inflate(R.layout.row_list_season, parent, false);
      final SeasonViewHolder seasonHolder = new SeasonViewHolder(v);
      holder = seasonHolder;

      v.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          final int position = seasonHolder.getAdapterPosition();
          if (position != RecyclerView.NO_POSITION) {
            Cursor cursor = getCursor(position);

            final long showId = Cursors.getLong(cursor, SeasonColumns.SHOW_ID);
            final String showTitle = Cursors.getString(cursor, "seasonShowTitle");
            final int seasonNumber = Cursors.getInt(cursor, SeasonColumns.SEASON);
            final long seasonId = Cursors.getLong(cursor, ListItemColumns.ITEM_ID);
            seasonListener.onSeasonClick(showId, seasonId, showTitle, seasonNumber);
          }
        }
      });
    } else if (viewType == DatabaseContract.ItemType.EPISODE) {
      View v = LayoutInflater.from(getContext()).inflate(R.layout.row_list_episode, parent, false);

      final EpisodeViewHolder episodeHolder = new EpisodeViewHolder(v);
      holder = episodeHolder;

      v.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          final int position = episodeHolder.getAdapterPosition();
          if (position != RecyclerView.NO_POSITION) {
            Cursor cursor = getCursor(position);
            final long itemId = Cursors.getLong(cursor, ListItemColumns.ITEM_ID);
            episodeListener.onEpisodeClick(episodeHolder.itemView, position, itemId);
          }
        }
      });
    } else if (viewType == DatabaseContract.ItemType.MOVIE) {
      View v = LayoutInflater.from(getContext()).inflate(R.layout.row_list_movie, parent, false);
      final MovieViewHolder movieHolder = new MovieViewHolder(v);
      holder = movieHolder;

      v.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          final int position = movieHolder.getAdapterPosition();
          if (position != RecyclerView.NO_POSITION) {
            Cursor cursor = getCursor(position);
            final long itemId = Cursors.getLong(cursor, ListItemColumns.ITEM_ID);
            movieListener.onMovieClicked(movieHolder.itemView, position, itemId);
          }
        }
      });
    } else {
      View view =
          LayoutInflater.from(getContext()).inflate(R.layout.row_list_person, parent, false);
      holder = new PersonViewHolder(view);
    }

    final ListViewHolder finalHolder = holder;
    holder.overflow.addItem(R.id.action_list_remove, R.string.action_list_remove);
    holder.overflow.setListener(new OverflowView.OverflowActionListener() {
      @Override public void onPopupShown() {
      }

      @Override public void onPopupDismissed() {
      }

      @Override public void onActionSelected(int action) {
        final int position = finalHolder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
          switch (action) {
            case R.id.action_list_remove:
              removeListener.onRemoveItem(position, finalHolder.getItemId());
              break;
          }
        }
      }
    });

    return holder;
  }

  @Override public void onViewRecycled(ListViewHolder holder) {
    holder.overflow.dismiss();
  }

  @Override
  protected void onBindViewHolder(final ListViewHolder holder, Cursor cursor, int position) {
    if (holder.getItemViewType() == DatabaseContract.ItemType.SHOW) {
      final ShowViewHolder showHolder = (ShowViewHolder) holder;

      showHolder.poster.setImage(Cursors.getString(cursor, ShowColumns.POSTER));
      showHolder.title.setText(Cursors.getString(cursor, ShowColumns.TITLE));
      showHolder.overview.setText(Cursors.getString(cursor, ShowColumns.OVERVIEW));
    } else if (holder.getItemViewType() == DatabaseContract.ItemType.SEASON) {
      final String showPoster = Cursors.getString(cursor, "seasonShowPoster");
      final String showTitle = Cursors.getString(cursor, "seasonShowTitle");
      final int season = Cursors.getInt(cursor, SeasonColumns.SEASON);

      SeasonViewHolder seasonHolder = (SeasonViewHolder) holder;
      seasonHolder.poster.setImage(showPoster);
      seasonHolder.season.setText(getContext().getResources().getString(R.string.season_x, season));
      seasonHolder.show.setText(showTitle);
    } else if (holder.getItemViewType() == DatabaseContract.ItemType.EPISODE) {
      final EpisodeViewHolder episodeHolder = (EpisodeViewHolder) holder;
      String title = Cursors.getString(cursor, EpisodeColumns.TITLE);
      final String screen = Cursors.getString(cursor, EpisodeColumns.SCREENSHOT);
      final String showTitle = Cursors.getString(cursor, "episodeShowTitle");
      final int season = Cursors.getInt(cursor, EpisodeColumns.SEASON);
      final int episode = Cursors.getInt(cursor, EpisodeColumns.EPISODE);

      if (TextUtils.isEmpty(title)) {
        if (season == 0) {
          title = getContext().getString(R.string.special_x, episode);
        } else {
          title = getContext().getString(R.string.episode_x, episode);
        }
      }

      episodeHolder.screen.setImage(screen);

      episodeHolder.title.setText(title);

      episodeHolder.showTitle.setText(showTitle);
    } else if (holder.getItemViewType() == DatabaseContract.ItemType.MOVIE) {
      MovieViewHolder movieHolder = (MovieViewHolder) holder;

      movieHolder.poster.setImage(Cursors.getString(cursor, MovieColumns.POSTER));
      movieHolder.title.setText(Cursors.getString(cursor, MovieColumns.TITLE));
      movieHolder.overview.setText(Cursors.getString(cursor, MovieColumns.OVERVIEW));
    } else {
      PersonViewHolder personHolder = (PersonViewHolder) holder;
      personHolder.headshot.setImage(Cursors.getString(cursor, PersonColumns.HEADSHOT));
      personHolder.name.setText(Cursors.getString(cursor, PersonColumns.NAME));
    }
  }

  public static class ListViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.overflow) OverflowView overflow;

    public ListViewHolder(View v) {
      super(v);
      ButterKnife.bind(this, v);
    }
  }

  public static class ShowViewHolder extends ListViewHolder {

    @BindView(R.id.poster) RemoteImageView poster;
    @BindView(R.id.title) TextView title;
    @BindView(R.id.overview) TextView overview;

    public ShowViewHolder(View v) {
      super(v);
    }
  }

  public static class SeasonViewHolder extends ListViewHolder {

    @BindView(R.id.poster) RemoteImageView poster;
    @BindView(R.id.season) TextView season;
    @BindView(R.id.show) TextView show;

    public SeasonViewHolder(View v) {
      super(v);
    }
  }

  public static class EpisodeViewHolder extends ListViewHolder {

    @BindView(R.id.screen) RemoteImageView screen;
    @BindView(R.id.title) TextView title;
    @BindView(R.id.showTitle) TextView showTitle;

    EpisodeViewHolder(View v) {
      super(v);
    }
  }

  public static class MovieViewHolder extends ListViewHolder {

    @BindView(R.id.poster) public RemoteImageView poster;
    @BindView(R.id.title) public TextView title;
    @BindView(R.id.overview) public TextView overview;

    public MovieViewHolder(View v) {
      super(v);
    }
  }

  public static class PersonViewHolder extends ListViewHolder {

    @BindView(R.id.headshot) RemoteImageView headshot;
    @BindView(R.id.person_name) TextView name;

    public PersonViewHolder(View v) {
      super(v);
    }
  }
}
