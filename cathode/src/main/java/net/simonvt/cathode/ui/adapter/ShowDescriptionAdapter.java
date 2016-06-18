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
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.widget.CircularProgressIndicator;
import net.simonvt.cathode.widget.IndicatorView;
import net.simonvt.cathode.widget.OverflowView;
import net.simonvt.cathode.widget.RemoteImageView;
import net.simonvt.schematic.Cursors;

public class ShowDescriptionAdapter
    extends RecyclerCursorAdapter<ShowDescriptionAdapter.ViewHolder> {

  public static final String[] PROJECTION = new String[] {
      Tables.SHOWS + "." + ShowColumns.ID, Tables.SHOWS + "." + ShowColumns.TITLE,
      Tables.SHOWS + "." + ShowColumns.OVERVIEW, Tables.SHOWS + "." + ShowColumns.POSTER,
      Tables.SHOWS + "." + ShowColumns.TVDB_ID, Tables.SHOWS + "." + ShowColumns.WATCHED_COUNT,
      Tables.SHOWS + "." + ShowColumns.IN_COLLECTION_COUNT,
      Tables.SHOWS + "." + ShowColumns.IN_WATCHLIST, Tables.SHOWS + "." + ShowColumns.RATING,
      Tables.SHOWS + "." + ShowColumns.LAST_MODIFIED,
  };

  @Inject ShowTaskScheduler showScheduler;

  private ShowClickListener listener;

  private boolean displayRating;

  public ShowDescriptionAdapter(Context context, ShowClickListener listener, Cursor cursor) {
    this(context, listener, cursor, true);
    CathodeApp.inject(context, this);
    this.listener = listener;
  }

  public ShowDescriptionAdapter(Context context, ShowClickListener listener, Cursor cursor,
      boolean displayRating) {
    super(context, cursor);
    CathodeApp.inject(context, this);
    this.listener = listener;
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
          listener.onShowClick(v, position, holder.getItemId());
        }
      }
    });

    holder.overflow.setListener(new OverflowView.OverflowActionListener() {

      @Override public void onPopupShown() {
        holder.setIsRecyclable(false);
      }

      @Override public void onPopupDismissed() {
        holder.setIsRecyclable(true);
      }

      @Override public void onActionSelected(int action) {
        holder.setIsRecyclable(true);
        onOverflowActionSelected(holder.itemView, holder.getItemId(), action,
            holder.getAdapterPosition());
      }
    });

    return holder;
  }

  @Override public void onViewRecycled(ViewHolder holder) {
    holder.overflow.dismiss();
  }

  @Override protected void onBindViewHolder(final ViewHolder holder, Cursor cursor, int position) {
    final boolean watched = Cursors.getInt(cursor, ShowColumns.WATCHED_COUNT) > 0;
    final boolean inCollection = Cursors.getInt(cursor, ShowColumns.IN_COLLECTION_COUNT) > 1;
    final boolean inWatchlist = Cursors.getInt(cursor, ShowColumns.IN_WATCHLIST) == 1;

    holder.indicator.setWatched(watched);
    holder.indicator.setCollected(inCollection);
    holder.indicator.setInWatchlist(inWatchlist);

    holder.poster.setImage(Cursors.getString(cursor, ShowColumns.POSTER));
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
        showScheduler.setIsInWatchlist(id, true);
        break;

      case R.id.action_watchlist_remove:
        onWatchlistRemove(view, position, id);
        break;
    }
  }

  protected void onWatchlistRemove(View view, int position, long id) {
    showScheduler.setIsInWatchlist(id, false);
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
