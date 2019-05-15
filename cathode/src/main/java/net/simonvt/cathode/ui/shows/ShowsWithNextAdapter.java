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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.ui.FragmentsUtils;
import net.simonvt.cathode.common.ui.adapter.BaseAdapter;
import net.simonvt.cathode.common.widget.OverflowView;
import net.simonvt.cathode.common.widget.RemoteImageView;
import net.simonvt.cathode.common.widget.TimeStamp;
import net.simonvt.cathode.entity.NextEpisode;
import net.simonvt.cathode.entity.Show;
import net.simonvt.cathode.entity.ShowWithEpisode;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.provider.util.DataHelper;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.dialog.CheckInDialog;
import net.simonvt.cathode.ui.dialog.CheckInDialog.Type;
import net.simonvt.cathode.ui.history.AddToHistoryDialog;

/**
 * A show adapter that displays the next episode as well.
 */
public class ShowsWithNextAdapter
    extends BaseAdapter<ShowWithEpisode, ShowsWithNextAdapter.ViewHolder> {

  public interface Callbacks {

    void onShowClick(long showId, String title, String overview);

    void onRemoveFromWatchlist(long showId);

    void onCheckin(long episodeId);

    void onCancelCheckin();

    void onCollectNext(long showId);

    void onHideFromWatched(long showId);

    void onHideFromCollection(long showId);
  }

  private FragmentActivity activity;

  private final LibraryType libraryType;

  protected Callbacks callbacks;

  public ShowsWithNextAdapter(FragmentActivity activity, Callbacks callbacks,
      LibraryType libraryType) {
    super(activity);
    this.activity = activity;
    this.callbacks = callbacks;
    this.libraryType = libraryType;
    setHasStableIds(true);
  }

  @Override public long getItemId(int position) {
    return getList().get(position).getShow().getId();
  }

  @Override protected boolean areItemsTheSame(@NonNull ShowWithEpisode oldItem,
      @NonNull ShowWithEpisode newItem) {
    return oldItem.getShow().getId() == newItem.getShow().getId();
  }

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(getContext()).inflate(R.layout.list_row_show, parent, false);

    final ViewHolder holder = new ViewHolder(v);

    v.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        final int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
          ShowWithEpisode showWithEpisode = getList().get(position);
          callbacks.onShowClick(holder.getItemId(), showWithEpisode.getShow().getTitle(),
              showWithEpisode.getShow().getOverview());
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
          switch (action) {
            case R.id.action_watchlist_remove:
              callbacks.onRemoveFromWatchlist(holder.getItemId());
              break;

            case R.id.action_history_add:
              FragmentsUtils.instantiate(activity.getSupportFragmentManager(),
                  AddToHistoryDialog.class,
                  AddToHistoryDialog.getArgs(AddToHistoryDialog.Type.EPISODE, holder.episodeId,
                      holder.episodeTitle))
                  .show(activity.getSupportFragmentManager(), AddToHistoryDialog.TAG);
              break;

            case R.id.action_checkin:
              if (!CheckInDialog.showDialogIfNecessary(activity, Type.SHOW, holder.episodeTitle,
                  holder.episodeId)) {
                callbacks.onCheckin(holder.episodeId);
              }
              break;

            case R.id.action_checkin_cancel:
              callbacks.onCancelCheckin();
              break;

            case R.id.action_collection_add:
              callbacks.onCollectNext(holder.getItemId());
              break;

            case R.id.action_watched_hide:
              callbacks.onHideFromWatched(holder.getItemId());
              break;

            case R.id.action_collection_hide:
              callbacks.onHideFromCollection(holder.getItemId());
              break;
          }
        }
      }
    });

    return holder;
  }

  @Override public void onViewRecycled(ViewHolder holder) {
    holder.overflow.dismiss();
  }

  @Override public void onBindViewHolder(ViewHolder holder, int position) {
    ShowWithEpisode showWithEpisode = getList().get(position);
    Show show = showWithEpisode.getShow();
    NextEpisode episode = showWithEpisode.getEpisode();

    final String showPosterUri =
        ImageUri.create(ImageUri.ITEM_SHOW, ImageType.POSTER, show.getId());

    final int showAiredCount = show.getAiredCount();
    int count = 0;
    switch (libraryType) {
      case WATCHED:
      case WATCHLIST:
        count = show.getWatchedCount();
        break;

      case COLLECTION:
        count = show.getInCollectionCount();
        break;
    }
    final int showTypeCount = count;

    String episodeTitle = null;
    if (episode.getSeason() > 0) {
      episodeTitle =
          DataHelper.getEpisodeTitle(getContext(), episode.getTitle(), episode.getSeason(),
              episode.getEpisode(), episode.getWatched(), true);
    }

    holder.title.setText(show.getTitle());

    holder.progressBar.setMax(showAiredCount);
    holder.progressBar.setProgress(showTypeCount);
    final String typeCount = getContext().getString(R.string.x_of_y, showTypeCount, showAiredCount);
    holder.watched.setText(typeCount);

    String episodeText = null;
    if (episodeTitle == null) {
      if (show.getStatus() != null) {
        episodeText = show.getStatus().toString();
      }
      holder.firstAired.setVisibility(View.GONE);
    } else {
      if (show.getWatching()) {
        episodeText = getContext().getString(R.string.show_watching);
      } else {
        episodeText = getContext().getString(R.string.episode_next, episodeTitle);
      }

      holder.firstAired.setVisibility(View.VISIBLE);
      holder.firstAired.setTimeInMillis(episode.getFirstAired());
    }
    holder.nextEpisode.setText(episodeText);

    holder.overflow.setVisibility(showAiredCount > 0 ? View.VISIBLE : View.INVISIBLE);

    holder.overflow.removeItems();
    setupOverflowItems(holder.overflow, showTypeCount, showAiredCount, episodeTitle != null,
        show.getWatching());

    holder.poster.setImage(showPosterUri);

    holder.showTypeCount = showTypeCount;
    holder.showAiredCount = showAiredCount;
    holder.episodeTitle = episodeTitle;
    holder.episodeId = episode.getId();
  }

  protected void setupOverflowItems(OverflowView overflow, int typeCount, int airedCount,
      boolean hasNext, boolean watching) {
    switch (libraryType) {
      case WATCHLIST:
        overflow.addItem(R.id.action_watchlist_remove, R.string.action_watchlist_remove);

      case WATCHED:
        if (airedCount - typeCount > 0) {
          if (!watching && hasNext) {
            overflow.addItem(R.id.action_checkin, R.string.action_checkin);
            overflow.addItem(R.id.action_history_add, R.string.action_history_add);
          } else if (watching) {
            overflow.addItem(R.id.action_checkin_cancel, R.string.action_checkin_cancel);
          }
        }

        overflow.addItem(R.id.action_watched_hide, R.string.action_watched_hide);
        break;

      case COLLECTION:
        if (airedCount - typeCount > 0) {
          overflow.addItem(R.id.action_collection_add, R.string.action_collect_next);
        }

        overflow.addItem(R.id.action_collection_hide, R.string.action_collection_hide);
        break;
    }
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.title) TextView title;
    @BindView(R.id.watched) TextView watched;
    @BindView(R.id.progress) ProgressBar progressBar;
    @BindView(R.id.nextEpisode) TextView nextEpisode;
    @BindView(R.id.firstAired) TimeStamp firstAired;
    @BindView(R.id.overflow) OverflowView overflow;
    @BindView(R.id.poster) RemoteImageView poster;

    public int showTypeCount;
    public int showAiredCount;
    public String episodeTitle;
    public long episodeId;

    ViewHolder(View v) {
      super(v);
      ButterKnife.bind(this, v);
    }
  }
}
