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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.LastModifiedColumns;
import net.simonvt.cathode.provider.DatabaseContract.ListItemColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.ui.listener.EpisodeClickListener;
import net.simonvt.cathode.ui.listener.MovieClickListener;
import net.simonvt.cathode.ui.listener.SeasonClickListener;
import net.simonvt.cathode.util.Cursors;
import net.simonvt.cathode.widget.OverflowView;
import net.simonvt.cathode.widget.RemoteImageView;

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
    return cursor.getInt(cursor.getColumnIndex(ListItemColumns.ITEM_TYPE));
  }

  @Override public long getLastModified(int position) {
    Cursor cursor = getCursor(position);
    return cursor.getLong(cursor.getColumnIndex(LastModifiedColumns.LAST_MODIFIED));
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
            final long itemId = cursor.getLong(cursor.getColumnIndex(ListItemColumns.ITEM_ID));
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
            final long itemId = cursor.getLong(cursor.getColumnIndex(ListItemColumns.ITEM_ID));
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
            final long itemId = cursor.getLong(cursor.getColumnIndex(ListItemColumns.ITEM_ID));
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
        finalHolder.setIsRecyclable(false);
      }

      @Override public void onPopupDismissed() {
        finalHolder.setIsRecyclable(true);
      }

      @Override public void onActionSelected(int action) {
        finalHolder.setIsRecyclable(true);
        if (finalHolder.getAdapterPosition() == RecyclerView.NO_POSITION) {
          return;
        }

        switch (action) {
          case R.id.action_list_remove:
            removeListener.onRemoveItem(finalHolder.getAdapterPosition(), finalHolder.getItemId());
            break;
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

      showHolder.poster.setImage(cursor.getString(cursor.getColumnIndex(ShowColumns.POSTER)));
      showHolder.title.setText(cursor.getString(cursor.getColumnIndex(ShowColumns.TITLE)));
      showHolder.overview.setText(cursor.getString(cursor.getColumnIndex(ShowColumns.OVERVIEW)));
    } else if (holder.getItemViewType() == DatabaseContract.ItemType.SEASON) {
      final String showPoster = cursor.getString(cursor.getColumnIndex("seasonShowPoster"));
      final String showTitle = cursor.getString(cursor.getColumnIndex("seasonShowTitle"));
      final int season = cursor.getInt(cursor.getColumnIndex(SeasonColumns.SEASON));

      SeasonViewHolder seasonHolder = (SeasonViewHolder) holder;
      seasonHolder.poster.setImage(showPoster);
      seasonHolder.season.setText(getContext().getResources().getString(R.string.season_x, season));
      seasonHolder.show.setText(showTitle);
    } else if (holder.getItemViewType() == DatabaseContract.ItemType.EPISODE) {
      final EpisodeViewHolder episodeHolder = (EpisodeViewHolder) holder;
      final String title = cursor.getString(cursor.getColumnIndex(EpisodeColumns.TITLE));
      final String screen = cursor.getString(cursor.getColumnIndex(EpisodeColumns.SCREENSHOT));
      final String showTitle = cursor.getString(cursor.getColumnIndex("episodeShowTitle"));

      episodeHolder.screen.setImage(screen);

      episodeHolder.title.setText(title);

      episodeHolder.showTitle.setText(showTitle);
    } else if (holder.getItemViewType() == DatabaseContract.ItemType.MOVIE) {
      MovieViewHolder movieHolder = (MovieViewHolder) holder;

      movieHolder.poster.setImage(cursor.getString(cursor.getColumnIndex(MovieColumns.POSTER)));
      movieHolder.title.setText(cursor.getString(cursor.getColumnIndex(MovieColumns.TITLE)));
      movieHolder.overview.setText(cursor.getString(cursor.getColumnIndex(MovieColumns.OVERVIEW)));
    } else {
      PersonViewHolder personHolder = (PersonViewHolder) holder;
      personHolder.headshot.setImage(
          cursor.getString(cursor.getColumnIndex(DatabaseContract.PersonColumns.HEADSHOT)));
      personHolder.name.setText(
          cursor.getString(cursor.getColumnIndex(DatabaseContract.PersonColumns.NAME)));
    }
  }

  public static class ListViewHolder extends RecyclerView.ViewHolder {

    @Bind(R.id.overflow) OverflowView overflow;

    public ListViewHolder(View v) {
      super(v);
      ButterKnife.bind(this, v);
    }
  }

  public static class ShowViewHolder extends ListViewHolder {

    @Bind(R.id.poster) RemoteImageView poster;
    @Bind(R.id.title) TextView title;
    @Bind(R.id.overview) TextView overview;

    public ShowViewHolder(View v) {
      super(v);
    }
  }

  public static class SeasonViewHolder extends ListViewHolder {

    @Bind(R.id.poster) RemoteImageView poster;
    @Bind(R.id.season) TextView season;
    @Bind(R.id.show) TextView show;

    public SeasonViewHolder(View v) {
      super(v);
    }
  }

  public static class EpisodeViewHolder extends ListViewHolder {

    @Bind(R.id.screen) RemoteImageView screen;
    @Bind(R.id.title) TextView title;
    @Bind(R.id.showTitle) TextView showTitle;

    EpisodeViewHolder(View v) {
      super(v);
    }
  }

  public static class MovieViewHolder extends ListViewHolder {

    @Bind(R.id.poster) public RemoteImageView poster;
    @Bind(R.id.title) public TextView title;
    @Bind(R.id.overview) public TextView overview;

    public MovieViewHolder(View v) {
      super(v);
    }
  }

  public static class PersonViewHolder extends ListViewHolder {

    @Bind(R.id.headshot) RemoteImageView headshot;
    @Bind(R.id.person_name) TextView name;

    public PersonViewHolder(View v) {
      super(v);
    }
  }
}
