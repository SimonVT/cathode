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
package net.simonvt.cathode.ui.dashboard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.ui.adapter.BaseAdapter;
import net.simonvt.cathode.common.widget.RemoteImageView;
import net.simonvt.cathode.entity.ShowWithEpisode;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.provider.util.DataHelper;

public class DashboardUpcomingShowsAdapter
    extends BaseAdapter<ShowWithEpisode, DashboardUpcomingShowsAdapter.ViewHolder> {

  private DashboardFragment.OverviewCallback callback;

  public DashboardUpcomingShowsAdapter(Context context,
      DashboardFragment.OverviewCallback callback) {
    super(context);
    this.callback = callback;
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
    final View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.list_row_dashboard_show_upcoming, parent, false);
    final ViewHolder holder = new ViewHolder(view);

    view.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        final int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
          ShowWithEpisode showWithEpisode = getList().get(position);
          callback.onDisplayEpisode(showWithEpisode.getEpisode().getId(),
              showWithEpisode.getShow().getTitle());
        }
      }
    });

    return holder;
  }

  @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    ShowWithEpisode showWithEpisode = getList().get(position);

    final int season = showWithEpisode.getEpisode().getSeason();
    final int episode = showWithEpisode.getEpisode().getEpisode();
    final boolean watched = showWithEpisode.getEpisode().getWatched();
    final String episodeTitle =
        DataHelper.getEpisodeTitle(getContext(), showWithEpisode.getEpisode().getTitle(), season,
            episode, watched, true);

    final String poster =
        ImageUri.create(ImageUri.ITEM_SHOW, ImageType.POSTER, showWithEpisode.getShow().getId());
    holder.poster.setImage(poster);
    holder.title.setText(showWithEpisode.getShow().getTitle());

    if (showWithEpisode.getShow().getWatching()) {
      holder.nextEpisode.setText(R.string.show_watching);
    } else {
      holder.nextEpisode.setText(episodeTitle);
    }
  }

  static class ViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.poster) RemoteImageView poster;
    @BindView(R.id.title) TextView title;
    @BindView(R.id.nextEpisode) TextView nextEpisode;

    ViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }
}
