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

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.ui.adapter.RecyclerCursorAdapter;
import net.simonvt.cathode.common.widget.CircularProgressIndicator;
import net.simonvt.cathode.common.widget.OverflowView;
import net.simonvt.cathode.common.widget.RemoteImageView;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.widget.IndicatorView;
import net.simonvt.schematic.Cursors;

public class ShowDescriptionAdapter
    extends RecyclerCursorAdapter<ShowDescriptionAdapter.ViewHolder> {

  public interface ShowCallbacks {

    void onShowClick(long showId, String title, String overview);

    void setIsInWatchlist(long showId, boolean inWatchlist);
  }

  public static final String[] PROJECTION = new String[] {
      Tables.SHOWS + "." + ShowColumns.ID,
      Tables.SHOWS + "." + ShowColumns.TITLE,
      Tables.SHOWS + "." + ShowColumns.OVERVIEW,
      Tables.SHOWS + "." + ShowColumns.TVDB_ID,
      Tables.SHOWS + "." + ShowColumns.WATCHED_COUNT,
      Tables.SHOWS + "." + ShowColumns.IN_COLLECTION_COUNT,
      Tables.SHOWS + "." + ShowColumns.IN_WATCHLIST,
      Tables.SHOWS + "." + ShowColumns.RATING,
      Tables.SHOWS + "." + ShowColumns.LAST_MODIFIED,
  };

  private ShowCallbacks callbacks;

  private boolean displayRating;

  public ShowDescriptionAdapter(Context context, ShowCallbacks callbacks, Cursor cursor) {
    this(context, callbacks, cursor, true);
    this.callbacks = callbacks;
  }

  public ShowDescriptionAdapter(Context context, ShowCallbacks callbacks, Cursor cursor,
      boolean displayRating) {
    super(context, cursor);
    this.callbacks = callbacks;
    this.displayRating = displayRating;
  }

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View v;
    if (displayRating) {
      v = LayoutInflater.from(getContext())
          .inflate(R.layout.list_row_show_description_rating, parent, false);
    } else {
      v = LayoutInflater.from(getContext())
          .inflate(R.layout.list_row_show_description, parent, false);
    }
    final ViewHolder holder = new ViewHolder(v);

    v.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        final int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
          Cursor cursor = getCursor(position);
          final String title = Cursors.getString(cursor, ShowColumns.TITLE);
          final String overview = Cursors.getString(cursor, ShowColumns.OVERVIEW);
          callbacks.onShowClick(holder.getItemId(), title, overview);
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
          onOverflowActionSelected(holder.itemView, holder.getItemId(), action, position);
        }
      }
    });

    return holder;
  }

  @Override public void onViewRecycled(ViewHolder holder) {
    holder.overflow.dismiss();
  }

  @Override protected void onBindViewHolder(final ViewHolder holder, Cursor cursor, int position) {
    final long id = Cursors.getLong(cursor, ShowColumns.ID);

    final boolean watched = Cursors.getInt(cursor, ShowColumns.WATCHED_COUNT) > 0;
    final boolean inCollection = Cursors.getInt(cursor, ShowColumns.IN_COLLECTION_COUNT) > 1;
    final boolean inWatchlist = Cursors.getBoolean(cursor, ShowColumns.IN_WATCHLIST);

    final String poster = ImageUri.create(ImageUri.ITEM_SHOW, ImageType.POSTER, id);

    holder.indicator.setWatched(watched);
    holder.indicator.setCollected(inCollection);
    holder.indicator.setInWatchlist(inWatchlist);

    holder.poster.setImage(poster);
    holder.title.setText(Cursors.getString(cursor, ShowColumns.TITLE));
    holder.overview.setText(Cursors.getString(cursor, ShowColumns.OVERVIEW));

    if (displayRating) {
      final float rating = Cursors.getFloat(cursor, ShowColumns.RATING);
      holder.rating.setValue(rating);
    }

    holder.overflow.removeItems();
    setupOverflowItems(holder.overflow, inWatchlist);
  }

  protected void setupOverflowItems(OverflowView overflow, boolean inWatchlist) {
    if (inWatchlist) {
      overflow.addItem(R.id.action_watchlist_remove, R.string.action_watchlist_remove);
    } else {
      overflow.addItem(R.id.action_watchlist_add, R.string.action_watchlist_add);
    }
  }

  protected void onOverflowActionSelected(View view, long id, int action, int position) {
    switch (action) {
      case R.id.action_watchlist_add:
        callbacks.setIsInWatchlist(id, true);
        break;

      case R.id.action_watchlist_remove:
        onWatchlistRemove(id);
        break;
    }
  }

  protected void onWatchlistRemove(long showId) {
    callbacks.setIsInWatchlist(showId, false);
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.poster) RemoteImageView poster;
    @BindView(R.id.indicator) IndicatorView indicator;
    @BindView(R.id.title) TextView title;
    @BindView(R.id.overview) TextView overview;
    @BindView(R.id.overflow) OverflowView overflow;
    @BindView(R.id.rating) @Nullable CircularProgressIndicator rating;

    ViewHolder(View v) {
      super(v);
      ButterKnife.bind(this, v);
    }
  }
}
