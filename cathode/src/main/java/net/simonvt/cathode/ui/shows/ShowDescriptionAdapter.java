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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.entity.Show;
import net.simonvt.cathode.common.ui.adapter.BaseAdapter;
import net.simonvt.cathode.common.widget.CircularProgressIndicator;
import net.simonvt.cathode.common.widget.OverflowView;
import net.simonvt.cathode.common.widget.RemoteImageView;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.widget.IndicatorView;

public class ShowDescriptionAdapter extends BaseAdapter<Show, ShowDescriptionAdapter.ViewHolder> {

  public interface ShowCallbacks {

    void onShowClick(long showId, String title, String overview);

    void setIsInWatchlist(long showId, boolean inWatchlist);
  }

  public static final String[] PROJECTION = new String[] {
      Tables.SHOWS + "." + ShowColumns.ID, Tables.SHOWS + "." + ShowColumns.TITLE,
      Tables.SHOWS + "." + ShowColumns.OVERVIEW, Tables.SHOWS + "." + ShowColumns.TVDB_ID,
      Tables.SHOWS + "." + ShowColumns.WATCHED_COUNT,
      Tables.SHOWS + "." + ShowColumns.IN_COLLECTION_COUNT,
      Tables.SHOWS + "." + ShowColumns.IN_WATCHLIST, Tables.SHOWS + "." + ShowColumns.RATING,
      Tables.SHOWS + "." + ShowColumns.LAST_MODIFIED,
  };

  private ShowCallbacks callbacks;

  private boolean displayRating;

  public ShowDescriptionAdapter(Context context, ShowCallbacks callbacks) {
    this(context, callbacks, true);
    this.callbacks = callbacks;
  }

  public ShowDescriptionAdapter(Context context, ShowCallbacks callbacks, boolean displayRating) {
    super(context);
    this.callbacks = callbacks;
    this.displayRating = displayRating;
    setHasStableIds(true);
  }

  @Override public long getItemId(int position) {
    return getList().get(position).getId();
  }

  @Override protected boolean areItemsTheSame(@NonNull Show oldItem, @NonNull Show newItem) {
    return oldItem.getId() == newItem.getId();
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
          Show show = getList().get(position);
          callbacks.onShowClick(show.getId(), show.getTitle(), show.getOverview());
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

  @Override public void onBindViewHolder(final ViewHolder holder, int position) {
    Show show = getList().get(position);

    final boolean watched = show.getWatchedCount() > 0;
    final boolean inCollection = show.getInCollectionCount() > 1;
    final boolean inWatchlist = show.getInWatchlist();

    final String poster = ImageUri.create(ImageUri.ITEM_SHOW, ImageType.POSTER, show.getId());

    holder.indicator.setWatched(watched);
    holder.indicator.setCollected(inCollection);
    holder.indicator.setInWatchlist(inWatchlist);

    holder.poster.setImage(poster);
    holder.title.setText(show.getTitle());
    holder.overview.setText(show.getOverview());

    if (displayRating) {
      holder.rating.setValue(show.getRating());
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
