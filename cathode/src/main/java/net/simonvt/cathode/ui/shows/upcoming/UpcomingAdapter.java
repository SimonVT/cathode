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
package net.simonvt.cathode.ui.shows.upcoming;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.ui.FragmentsUtils;
import net.simonvt.cathode.common.ui.adapter.HeaderAdapter;
import net.simonvt.cathode.common.widget.OverflowView.OverflowActionListener;
import net.simonvt.cathode.common.widget.RemoteImageView;
import net.simonvt.cathode.common.widget.TimeStamp;
import net.simonvt.cathode.entity.ShowWithEpisode;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.provider.util.DataHelper;
import net.simonvt.cathode.ui.dialog.CheckInDialog;
import net.simonvt.cathode.ui.dialog.CheckInDialog.Type;
import net.simonvt.cathode.ui.history.AddToHistoryDialog;
import net.simonvt.cathode.widget.CheckInView;

public class UpcomingAdapter extends HeaderAdapter<ShowWithEpisode, RecyclerView.ViewHolder> {

  public interface Callbacks {

    void onEpisodeClicked(long episodeId, String showTitle);

    void onCheckin(long episodeId);

    void onCancelCheckin();
  }

  private static final int TYPE_ITEM = 0;

  private FragmentActivity activity;

  private Callbacks callbacks;

  public UpcomingAdapter(FragmentActivity activity, Callbacks callbacks) {
    super(activity);
    this.activity = activity;
    this.callbacks = callbacks;

    setHasStableIds(true);
  }

  @Override protected int getItemViewType(int headerRes, ShowWithEpisode item) {
    return TYPE_ITEM;
  }

  @Override protected long getItemId(ShowWithEpisode item) {
    return item.getShow().getId();
  }

  @Override protected RecyclerView.ViewHolder onCreateItemHolder(ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(activity).inflate(R.layout.list_row_upcoming, parent, false);
    final ItemViewHolder holder = new ItemViewHolder(v);
    holder.itemView.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        final int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
          ShowWithEpisode showWithEpisode = getItem(position);
          if (holder.watching) {
            callbacks.onEpisodeClicked(holder.watchingId, showWithEpisode.getShow().getTitle());
          } else {
            callbacks.onEpisodeClicked(showWithEpisode.getEpisode().getId(),
                showWithEpisode.getShow().getTitle());
          }
        }
      }
    });
    return holder;
  }

  @Override protected RecyclerView.ViewHolder onCreateHeaderHolder(ViewGroup parent) {
    View v =
        LayoutInflater.from(activity).inflate(R.layout.list_row_upcoming_header, parent, false);
    return new HeaderViewHolder((TextView) v);
  }

  @Override public void onViewRecycled(RecyclerView.ViewHolder holder) {
    if (holder instanceof ItemViewHolder) {
      ItemViewHolder itemHolder = (ItemViewHolder) holder;
      itemHolder.checkIn.dismiss();
      itemHolder.checkIn.reset();

      itemHolder.watching = false;
      itemHolder.watchingId = null;
    }
  }

  @Override protected void onBindHeader(RecyclerView.ViewHolder holder, int headerRes) {
    HeaderViewHolder vh = (HeaderViewHolder) holder;
    vh.header.setText(headerRes);
  }

  @Override protected void onBindViewHolder(final RecyclerView.ViewHolder holder,
      ShowWithEpisode showWithEpisode, int position) {
    final ItemViewHolder vh = (ItemViewHolder) holder;

    final long showId = showWithEpisode.getShow().getId();
    final boolean watching = showWithEpisode.getShow().getWatching();

    final int airedCount = showWithEpisode.getShow().getAiredCount();
    final int watchedCount = showWithEpisode.getShow().getWatchedCount();

    final long episodeId = showWithEpisode.getEpisode().getId();
    final long episodeFirstAired = showWithEpisode.getEpisode().getFirstAired();
    final int episodeSeasonNumber = showWithEpisode.getEpisode().getSeason();
    final int episodeNumber = showWithEpisode.getEpisode().getEpisode();
    final boolean watched = showWithEpisode.getEpisode().getWatched();
    final String episodeTitle =
        DataHelper.getEpisodeTitle(activity, showWithEpisode.getEpisode().getTitle(),
            episodeSeasonNumber, episodeNumber, watched, true);
    final Long watchingEpisodeId = showWithEpisode.getShow().getWatchingEpisodeId();

    final String showPosterUri = ImageUri.create(ImageUri.ITEM_SHOW, ImageType.POSTER, showId);

    vh.title.setText(showWithEpisode.getShow().getTitle());
    vh.poster.setImage(showPosterUri);

    vh.watching = watching;
    vh.watchingId = watchingEpisodeId;

    if (watching) {
      vh.nextEpisode.setText(R.string.show_watching);
    } else {
      vh.nextEpisode.setText(episodeTitle);
    }

    vh.firstAired.setVisibility(View.VISIBLE);
    vh.firstAired.setTimeInMillis(episodeFirstAired);

    vh.checkIn.setWatching(watching);
    vh.checkIn.setId(showId);
    vh.checkIn.setListener(new OverflowActionListener() {
      @Override public void onPopupShown() {
      }

      @Override public void onPopupDismissed() {
      }

      @Override public void onActionSelected(int action) {
        final int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
          switch (action) {
            case R.id.action_checkin_cancel:
              callbacks.onCancelCheckin();
              vh.checkIn.setWatching(false);
              break;

            case R.id.action_checkin:
              if (!CheckInDialog.showDialogIfNecessary(activity, Type.SHOW, episodeTitle,
                  episodeId)) {
                callbacks.onCheckin(episodeId);
                vh.checkIn.setWatching(true);
              }
              break;

            case R.id.action_history_add:
              FragmentsUtils.instantiate(activity.getSupportFragmentManager(),
                  AddToHistoryDialog.class,
                  AddToHistoryDialog.getArgs(AddToHistoryDialog.Type.EPISODE, episodeId,
                      episodeTitle))
                  .show(activity.getSupportFragmentManager(), AddToHistoryDialog.TAG);
              break;
          }
        }
      }
    });
  }

  public static class HeaderViewHolder extends RecyclerView.ViewHolder {

    TextView header;

    public HeaderViewHolder(TextView header) {
      super(header);
      this.header = header;
    }
  }

  public static class ItemViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.title) TextView title;
    @BindView(R.id.nextEpisode) TextView nextEpisode;
    @BindView(R.id.firstAired) TimeStamp firstAired;
    @BindView(R.id.check_in) CheckInView checkIn;
    @BindView(R.id.poster) RemoteImageView poster;

    boolean watching;
    Long watchingId;

    ItemViewHolder(View v) {
      super(v);
      ButterKnife.bind(this, v);
    }
  }
}
