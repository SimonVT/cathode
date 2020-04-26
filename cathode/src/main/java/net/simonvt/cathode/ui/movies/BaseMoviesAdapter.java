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
package net.simonvt.cathode.ui.movies;

import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.ui.FragmentsUtils;
import net.simonvt.cathode.common.ui.adapter.BaseAdapter;
import net.simonvt.cathode.common.widget.CircularProgressIndicator;
import net.simonvt.cathode.common.widget.OverflowView;
import net.simonvt.cathode.common.widget.RemoteImageView;
import net.simonvt.cathode.entity.Movie;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.settings.TraktLinkSettings;
import net.simonvt.cathode.ui.dialog.CheckInDialog;
import net.simonvt.cathode.ui.dialog.CheckInDialog.Type;
import net.simonvt.cathode.ui.history.AddToHistoryDialog;
import net.simonvt.cathode.ui.history.RemoveFromHistoryDialog;

public abstract class BaseMoviesAdapter<T extends BaseMoviesAdapter.ViewHolder>
    extends BaseAdapter<Movie, T> {

  public interface Callbacks {

    void onMovieClicked(long movieId, String title, String overview);

    void onCheckin(long movieId);

    void onCancelCheckin();

    void onWatchlistAdd(long movieId);

    void onWatchlistRemove(long movieId);

    void onCollectionAdd(long movieId);

    void onCollectionRemove(long movieId);
  }

  protected FragmentActivity activity;

  protected Callbacks callbacks;

  public BaseMoviesAdapter(FragmentActivity activity, Callbacks callbacks) {
    super(activity);
    this.activity = activity;
    this.callbacks = callbacks;
    setHasStableIds(true);
  }

  @Override public long getItemId(int position) {
    return getList().get(position).getId();
  }

  @Override protected boolean areItemsTheSame(@NonNull Movie oldItem, @NonNull Movie newItem) {
    return oldItem.getId() == newItem.getId();
  }

  @Override public void onViewRecycled(ViewHolder holder) {
    holder.overflow.dismiss();
  }

  @Override public void onBindViewHolder(T holder, int position) {
    Movie movie = getList().get(position);

    final String poster = ImageUri.create(ImageUri.ITEM_MOVIE, ImageType.POSTER, movie.getId());
    holder.poster.setImage(poster);
    holder.title.setText(movie.getTitle());
    holder.overview.setText(movie.getOverview());

    if (holder.rating != null) {
      holder.rating.setValue(movie.getRating());
    }

    holder.overflow.removeItems();
    setupOverflowItems(holder.overflow, movie.getWatched(), movie.getInCollection(),
        movie.getInWatchlist(), movie.getWatching(), movie.getCheckedIn());
  }

  protected void setupOverflowItems(OverflowView overflow, boolean watched, boolean collected,
      boolean inWatchlist, boolean watching, boolean checkedIn) {
    if (checkedIn) {
      overflow.addItem(R.id.action_checkin_cancel, R.string.action_checkin_cancel);
    } else if (watched) {
      overflow.addItem(R.id.action_history_remove, R.string.action_history_remove);
    } else if (inWatchlist) {
      overflow.addItem(R.id.action_checkin, R.string.action_checkin);
      overflow.addItem(R.id.action_watchlist_remove, R.string.action_watchlist_remove);
    } else {
      if (!watching) overflow.addItem(R.id.action_checkin, R.string.action_checkin);
      overflow.addItem(R.id.action_watchlist_add, R.string.action_watchlist_add);
    }

    if (collected) {
      overflow.addItem(R.id.action_collection_remove, R.string.action_collection_remove);
    } else {
      overflow.addItem(R.id.action_collection_add, R.string.action_collection_add);
    }
  }

  protected void onOverflowActionSelected(View view, long id, int action, int position,
      String title) {
    switch (action) {
      case R.id.action_history_add:
        FragmentsUtils.instantiate(activity.getSupportFragmentManager(), AddToHistoryDialog.class,
            AddToHistoryDialog.getArgs(AddToHistoryDialog.Type.MOVIE, id, title))
            .show(activity.getSupportFragmentManager(), AddToHistoryDialog.TAG);
        break;

      case R.id.action_history_remove:
        if (TraktLinkSettings.isLinked(getContext())) {
          FragmentsUtils.instantiate(activity.getSupportFragmentManager(),
              RemoveFromHistoryDialog.class,
              RemoveFromHistoryDialog.getArgs(RemoveFromHistoryDialog.Type.MOVIE, id, title, null))
              .show(activity.getSupportFragmentManager(), RemoveFromHistoryDialog.TAG);
        }
        break;

      case R.id.action_checkin:
        if (!CheckInDialog.showDialogIfNecessary(activity, Type.MOVIE, title, id)) {
          callbacks.onCheckin(id);
        }
        break;

      case R.id.action_checkin_cancel:
        callbacks.onCancelCheckin();
        break;

      case R.id.action_watchlist_add:
        callbacks.onWatchlistAdd(id);
        break;

      case R.id.action_watchlist_remove:
        callbacks.onWatchlistRemove(id);
        break;

      case R.id.action_collection_add:
        callbacks.onCollectionAdd(id);
        break;

      case R.id.action_collection_remove:
        callbacks.onCollectionRemove(id);
        break;
    }
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    public RemoteImageView poster;
    public TextView title;
    public TextView overview;
    public OverflowView overflow;
    @Nullable public CircularProgressIndicator rating;

    public ViewHolder(View v) {
      super(v);
      poster = v.findViewById(R.id.poster);
      title = v.findViewById(R.id.title);
      overview = v.findViewById(R.id.overview);
      overflow = v.findViewById(R.id.overflow);
      rating = v.findViewById(R.id.rating);
    }
  }
}
