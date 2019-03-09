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
package net.simonvt.cathode.settings.hidden;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.entity.Movie;
import net.simonvt.cathode.common.entity.Show;
import net.simonvt.cathode.common.ui.adapter.HeaderAdapter;
import net.simonvt.cathode.common.widget.OverflowView;
import net.simonvt.cathode.common.widget.RemoteImageView;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.provider.DatabaseContract.LastModifiedColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.util.SqlColumn;

public class HiddenItemsAdapter extends HeaderAdapter<Object, RecyclerView.ViewHolder> {

  public interface ItemCallbacks {

    void onShowClicked(long showId, String title, String overview);

    void displayShowInCalendar(long showId);

    void displayShowInWatched(long showId);

    void displayShowInCollection(long showId);

    void onMovieClicked(long movieId, String title, String overview);

    void displayMovieInCalendar(long movieId);
  }

  public static final String[] PROJECTION_SHOW = new String[] {
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.ID),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.TRAKT_ID),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.TITLE),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.OVERVIEW),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.WATCHED_COUNT),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.IN_COLLECTION_COUNT),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.IN_WATCHLIST),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.RATING),
      SqlColumn.table(Tables.SHOWS).column(LastModifiedColumns.LAST_MODIFIED),
  };

  public static final String[] PROJECTION_MOVIES = new String[] {
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.ID),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.TRAKT_ID),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.TITLE),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.OVERVIEW),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.WATCHED),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.IN_COLLECTION),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.IN_WATCHLIST),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.WATCHING),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.CHECKED_IN),
      SqlColumn.table(Tables.MOVIES).column(LastModifiedColumns.LAST_MODIFIED),
  };

  private static final int TYPE_SHOW = 0;

  private static final int TYPE_MOVIE = 1;

  private Context context;

  private ItemCallbacks itemCallbacks;

  public HiddenItemsAdapter(Context context, ItemCallbacks itemCallbacks) {
    super(context);
    this.context = context;
    this.itemCallbacks = itemCallbacks;
  }

  @Override protected long getItemId(Object item) {
    if (item instanceof Show) {
      return ((Show) item).getTraktId();
    }

    return ((Movie) item).getTraktId();
  }

  @Override protected int getItemViewType(int headerRes, Object item) {
    switch (headerRes) {
      case R.string.header_hidden_calendar_shows:
      case R.string.header_hidden_watched_shows:
      case R.string.header_hidden_collected_shows:
        return TYPE_SHOW;

      default:
        return TYPE_MOVIE;
    }
  }

  @Override protected RecyclerView.ViewHolder onCreateItemHolder(ViewGroup parent, int viewType) {
    ListViewHolder holder;
    if (viewType == TYPE_SHOW) {
      View v = LayoutInflater.from(context).inflate(R.layout.row_list_show, parent, false);
      final ShowViewHolder showHolder = new ShowViewHolder(v);
      holder = showHolder;

      showHolder.overflow.addItem(R.id.action_unhide, R.string.action_unhide);
      showHolder.overflow.setListener(new OverflowView.OverflowActionListener() {
        @Override public void onPopupShown() {
        }

        @Override public void onPopupDismissed() {
        }

        @Override public void onActionSelected(int action) {
          final int position = showHolder.getAdapterPosition();
          if (position != RecyclerView.NO_POSITION) {
            Show show = (Show) getItem(position);
            final long itemId = show.getId();

            switch (action) {
              case R.id.action_unhide:
                int headerRes = getHeaderRes(position);
                if (headerRes == R.string.header_hidden_calendar_shows) {
                  itemCallbacks.displayShowInCalendar(itemId);
                } else if (headerRes == R.string.header_hidden_watched_shows) {
                  itemCallbacks.displayShowInWatched(itemId);
                } else {
                  itemCallbacks.displayShowInCollection(itemId);
                }
                break;
            }
          }
        }
      });

      v.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          final int position = showHolder.getAdapterPosition();
          if (position != RecyclerView.NO_POSITION) {
            Show show = (Show) getItem(position);
            final long itemId = show.getId();
            itemCallbacks.onShowClicked(itemId, show.getTitle(), show.getOverview());
          }
        }
      });
    } else {
      View v = LayoutInflater.from(context).inflate(R.layout.row_list_movie, parent, false);
      final MovieViewHolder movieHolder = new MovieViewHolder(v);
      holder = movieHolder;

      movieHolder.overflow.addItem(R.id.action_unhide, R.string.action_unhide);
      movieHolder.overflow.setListener(new OverflowView.OverflowActionListener() {
        @Override public void onPopupShown() {
        }

        @Override public void onPopupDismissed() {
        }

        @Override public void onActionSelected(int action) {
          final int position = movieHolder.getAdapterPosition();
          if (position != RecyclerView.NO_ID) {
            Movie movie = (Movie) getItem(position);
            final long itemId = movie.getId();

            switch (action) {
              case R.id.action_unhide:
                int headerRes = getHeaderRes(position);
                if (headerRes == R.string.header_hidden_calendar_movies) {
                  itemCallbacks.displayMovieInCalendar(itemId);
                }
                break;
            }
          }
        }
      });

      v.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          final int position = movieHolder.getAdapterPosition();
          if (position != RecyclerView.NO_POSITION) {
            Movie movie = (Movie) getItem(position);
            final long itemId = movie.getId();
            itemCallbacks.onMovieClicked(itemId, movie.getTitle(), movie.getOverview());
          }
        }
      });
    }

    return holder;
  }

  @Override protected RecyclerView.ViewHolder onCreateHeaderHolder(ViewGroup parent) {
    View v = LayoutInflater.from(context).inflate(R.layout.list_row_upcoming_header, parent, false);
    return new HeaderViewHolder((TextView) v);
  }

  @Override public void onViewRecycled(RecyclerView.ViewHolder holder) {
    if (holder instanceof ListViewHolder) {
      ((ListViewHolder) holder).overflow.dismiss();
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

      final String poster = ImageUri.create(ImageUri.ITEM_SHOW, ImageType.POSTER, show.getId());
      vh.poster.setImage(poster);
      vh.title.setText(show.getTitle());
      vh.overview.setText(show.getOverview());
    } else {
      final MovieViewHolder vh = (MovieViewHolder) holder;
      Movie movie = (Movie) object;

      final String poster = ImageUri.create(ImageUri.ITEM_MOVIE, ImageType.POSTER, movie.getId());
      vh.poster.setImage(poster);
      vh.title.setText(movie.getTitle());
      vh.overview.setText(movie.getOverview());
    }
  }

  static class HeaderViewHolder extends RecyclerView.ViewHolder {

    TextView header;

    HeaderViewHolder(TextView header) {
      super(header);
      this.header = header;
    }
  }

  static class ListViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.overflow) OverflowView overflow;

    ListViewHolder(View v) {
      super(v);
      ButterKnife.bind(this, v);
    }
  }

  static class ShowViewHolder extends ListViewHolder {

    @BindView(R.id.poster) RemoteImageView poster;
    @BindView(R.id.title) TextView title;
    @BindView(R.id.overview) TextView overview;

    ShowViewHolder(View v) {
      super(v);
    }
  }

  static class MovieViewHolder extends ListViewHolder {

    @BindView(R.id.poster) RemoteImageView poster;
    @BindView(R.id.title) TextView title;
    @BindView(R.id.overview) TextView overview;

    MovieViewHolder(View v) {
      super(v);
    }
  }
}
