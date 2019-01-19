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
package net.simonvt.cathode.ui.lists;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.ui.adapter.RecyclerCursorAdapter;
import net.simonvt.cathode.common.widget.OverflowView;
import net.simonvt.cathode.common.widget.RemoteImageView;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.LastModifiedColumns;
import net.simonvt.cathode.provider.DatabaseContract.ListItemColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.util.DataHelper;
import net.simonvt.schematic.Cursors;

public class ListAdapter extends RecyclerCursorAdapter<ListAdapter.ListViewHolder> {

  interface ListListener {

    void onShowClick(long showId, String title, String overview);

    void onSeasonClick(long showId, long seasonId, String showTitle, int seasonNumber);

    void onEpisodeClick(long id);

    void onMovieClicked(long movieId, String title, String overview);

    void onPersonClick(long personId);

    void onRemoveItem(int position, long id);
  }

  ListListener listener;

  public ListAdapter(Context context, ListListener listener) {
    super(context);
    this.listener = listener;
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
            final String title = Cursors.getString(cursor, ShowColumns.TITLE);
            final String overview = Cursors.getString(cursor, ShowColumns.OVERVIEW);
            listener.onShowClick(itemId, title, overview);
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
            listener.onSeasonClick(showId, seasonId, showTitle, seasonNumber);
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
            listener.onEpisodeClick(itemId);
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
            final String title = Cursors.getString(cursor, MovieColumns.TITLE);
            final String overview = Cursors.getString(cursor, MovieColumns.OVERVIEW);
            listener.onMovieClicked(itemId, title, overview);
          }
        }
      });
    } else {
      View v = LayoutInflater.from(getContext()).inflate(R.layout.row_list_person, parent, false);
      final PersonViewHolder personHolder = new PersonViewHolder(v);
      holder = personHolder;

      v.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          final int position = personHolder.getAdapterPosition();
          if (position != RecyclerView.NO_POSITION) {
            Cursor cursor = getCursor(position);
            final long itemId = Cursors.getLong(cursor, ListItemColumns.ITEM_ID);
            listener.onPersonClick(itemId);
          }
        }
      });
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
              listener.onRemoveItem(position, finalHolder.getItemId());
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
    final long itemId = Cursors.getLong(cursor, ListItemColumns.ITEM_ID);

    if (holder.getItemViewType() == DatabaseContract.ItemType.SHOW) {
      final ShowViewHolder showHolder = (ShowViewHolder) holder;

      final String poster = ImageUri.create(ImageUri.ITEM_SHOW, ImageType.POSTER, itemId);

      showHolder.poster.setImage(poster);
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
      final String showTitle = Cursors.getString(cursor, "episodeShowTitle");
      final int season = Cursors.getInt(cursor, EpisodeColumns.SEASON);
      final int episode = Cursors.getInt(cursor, EpisodeColumns.EPISODE);
      final boolean watched = Cursors.getBoolean(cursor, EpisodeColumns.WATCHED);
      final String title =
          DataHelper.getEpisodeTitle(getContext(), cursor, season, episode, watched);

      final String screenshotUri = ImageUri.create(ImageUri.ITEM_EPISODE, ImageType.STILL, itemId);

      episodeHolder.screen.setImage(screenshotUri);
      episodeHolder.title.setText(title);
      episodeHolder.showTitle.setText(showTitle);
    } else if (holder.getItemViewType() == DatabaseContract.ItemType.MOVIE) {
      MovieViewHolder movieHolder = (MovieViewHolder) holder;

      final String poster = ImageUri.create(ImageUri.ITEM_MOVIE, ImageType.POSTER, itemId);

      movieHolder.poster.setImage(poster);
      movieHolder.title.setText(Cursors.getString(cursor, MovieColumns.TITLE));
      movieHolder.overview.setText(Cursors.getString(cursor, MovieColumns.OVERVIEW));
    } else {
      PersonViewHolder personHolder = (PersonViewHolder) holder;

      final String headshot = ImageUri.create(ImageUri.ITEM_PERSON, ImageType.PROFILE, itemId);

      personHolder.headshot.setImage(headshot);
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
