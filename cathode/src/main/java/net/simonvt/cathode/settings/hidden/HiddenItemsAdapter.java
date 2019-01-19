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
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.collection.LongSparseArray;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.ui.adapter.HeaderCursorAdapter;
import net.simonvt.cathode.common.widget.OverflowView;
import net.simonvt.cathode.common.widget.RemoteImageView;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.provider.DatabaseContract.LastModifiedColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.util.SqlColumn;
import net.simonvt.schematic.Cursors;

public class HiddenItemsAdapter extends HeaderCursorAdapter<RecyclerView.ViewHolder> {

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

  private long nextId;

  public HiddenItemsAdapter(Context context, ItemCallbacks itemCallbacks) {
    this.context = context;
    this.itemCallbacks = itemCallbacks;
  }

  @Override public long getLastModified(int position) {
    if (!isHeader(position)) {
      Cursor cursor = getCursor(position);
      return Cursors.getLong(cursor, LastModifiedColumns.LAST_MODIFIED);
    }

    return super.getLastModified(position);
  }

  private LongSparseArray<LongSparseArray<Long>> idMaps = new LongSparseArray<>();
  private LongSparseArray<LongSparseArray<Long>> reverseIdMaps = new LongSparseArray<>();

  @Override protected long getItemId(int position, Cursor cursor) {
    final long itemId = super.getItemId(position, cursor);
    final long headerId = getHeader(position).headerId;

    LongSparseArray<Long> ids = idMaps.get(headerId);
    LongSparseArray<Long> reverseIds = reverseIdMaps.get(headerId);
    if (ids == null) {
      ids = new LongSparseArray<>();
      idMaps.put(headerId, ids);
    }
    if (reverseIds == null) {
      reverseIds = new LongSparseArray<>();
      reverseIdMaps.put(headerId, reverseIds);
    }

    Long id = ids.get(itemId);
    if (id == null) {
      id = nextId++;
      ids.put(itemId, id);
      reverseIds.put(id, itemId);
    }

    return id;
  }

  private long reverseIdLookup(long headerId, long id) {
    LongSparseArray<Long> reverseIds = reverseIdMaps.get(headerId);
    return reverseIds.get(id);
  }

  @Override protected int getItemViewType(int headerRes, Cursor cursor) {
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
            final long id = showHolder.getItemId();
            final long itemId = reverseIdLookup(getHeader(position).headerId, id);

            switch (action) {
              case R.id.action_unhide:
                Header header = getHeader(position);
                if (header.header == R.string.header_hidden_calendar_shows) {
                  itemCallbacks.displayShowInCalendar(itemId);
                } else if (header.header == R.string.header_hidden_watched_shows) {
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
            final long id = showHolder.getItemId();
            final long itemId = reverseIdLookup(getHeader(position).headerId, id);

            Cursor cursor = getCursor(position);
            final String title = Cursors.getString(cursor, ShowColumns.TITLE);
            final String overview = Cursors.getString(cursor, ShowColumns.OVERVIEW);

            itemCallbacks.onShowClicked(itemId, title, overview);
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
            final long id = movieHolder.getItemId();
            final long itemId = reverseIdLookup(getHeader(position).headerId, id);

            switch (action) {
              case R.id.action_unhide:
                Header header = getHeader(position);
                if (header.header == R.string.header_hidden_calendar_movies) {
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
            final long id = movieHolder.getItemId();
            final long itemId = reverseIdLookup(getHeader(position).headerId, id);

            Cursor cursor = getCursor(position);
            final String title = Cursors.getString(cursor, MovieColumns.TITLE);
            final String overview = Cursors.getString(cursor, MovieColumns.OVERVIEW);

            itemCallbacks.onMovieClicked(itemId, title, overview);
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

  @Override protected void onBindViewHolder(final RecyclerView.ViewHolder holder, Cursor cursor,
      int position) {
    if (holder.getItemViewType() == TYPE_SHOW) {
      final ShowViewHolder vh = (ShowViewHolder) holder;

      final long id = Cursors.getLong(cursor, ShowColumns.ID);
      final String poster = ImageUri.create(ImageUri.ITEM_SHOW, ImageType.POSTER, id);

      vh.poster.setImage(poster);
      vh.title.setText(Cursors.getString(cursor, ShowColumns.TITLE));
      vh.overview.setText(Cursors.getString(cursor, ShowColumns.OVERVIEW));
    } else {
      final MovieViewHolder vh = (MovieViewHolder) holder;

      final long id = Cursors.getLong(cursor, MovieColumns.ID);
      final String poster = ImageUri.create(ImageUri.ITEM_MOVIE, ImageType.POSTER, id);

      vh.poster.setImage(poster);
      vh.title.setText(Cursors.getString(cursor, MovieColumns.TITLE));
      vh.overview.setText(Cursors.getString(cursor, MovieColumns.OVERVIEW));
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
