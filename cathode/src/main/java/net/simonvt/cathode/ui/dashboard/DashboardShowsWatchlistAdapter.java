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
import java.util.ArrayList;
import java.util.List;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.ui.adapter.BaseAdapter;
import net.simonvt.cathode.common.widget.RemoteImageView;
import net.simonvt.cathode.entity.Episode;
import net.simonvt.cathode.entity.Show;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.provider.util.DataHelper;

public class DashboardShowsWatchlistAdapter extends BaseAdapter<Object, RecyclerView.ViewHolder> {

  private static final int TYPE_SHOW = 0;
  private static final int TYPE_EPISODE = 1;

  private Context context;

  private DashboardFragment.OverviewCallback callback;

  private List<Show> shows;
  private List<Episode> episodes;

  public DashboardShowsWatchlistAdapter(Context context,
      DashboardFragment.OverviewCallback callback) {
    super(context);
    this.context = context;
    this.callback = callback;
  }

  @Override protected boolean areItemsTheSame(@NonNull Object oldItem, @NonNull Object newItem) {
    if (oldItem instanceof Show && newItem instanceof Show) {
      return ((Show) oldItem).getId() == ((Show) newItem).getId();
    } else if (oldItem instanceof Episode && newItem instanceof Episode) {
      return ((Episode) oldItem).getId() == ((Episode) newItem).getId();
    }

    return false;
  }

  public void changeShowList(List<Show> shows) {
    this.shows = shows;
    updateItems();
  }

  public void changeEpisodeList(List<Episode> episodes) {
    this.episodes = episodes;
    updateItems();
  }

  private void updateItems() {
    List<Object> items = new ArrayList<>();

    if (shows != null) {
      items.addAll(shows);
    }

    if (episodes != null) {
      items.addAll(episodes);
    }

    setList(items);
  }

  @Override public int getItemViewType(int position) {
    if (getList().get(position) instanceof Show) {
      return TYPE_SHOW;
    }

    return TYPE_EPISODE;
  }

  @Override public long getItemId(int position) {
    // TODO: Better way=
    if (getItemViewType(position) == TYPE_SHOW) {
      return ((Show) getList().get(position)).getTvdbId();
    }

    return ((Episode) getList().get(position)).getTvdbId();
  }

  @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    if (viewType == TYPE_SHOW) {
      final View view = LayoutInflater.from(parent.getContext())
          .inflate(R.layout.list_row_dashboard_show, parent, false);
      final ShowViewHolder holder = new ShowViewHolder(view);

      view.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          final int position = holder.getAdapterPosition();
          if (position != RecyclerView.NO_POSITION) {
            Show show = (Show) getList().get(position);
            callback.onDisplayShow(show.getId(), show.getTitle(), show.getOverview());
          }
        }
      });

      return holder;
    } else {
      final View view = LayoutInflater.from(parent.getContext())
          .inflate(R.layout.list_row_dashboard_episode, parent, false);
      final EpisodeViewHolder holder = new EpisodeViewHolder(view);

      view.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          final int position = holder.getAdapterPosition();
          if (position != RecyclerView.NO_POSITION) {
            Episode episode = (Episode) getList().get(position);
            callback.onDisplayEpisode(episode.getId(), episode.getShowTitle());
          }
        }
      });

      return holder;
    }
  }

  @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    if (holder.getItemViewType() == TYPE_SHOW) {
      ShowViewHolder showHolder = (ShowViewHolder) holder;
      Show show = (Show) getList().get(position);

      final String poster = ImageUri.create(ImageUri.ITEM_SHOW, ImageType.POSTER, show.getId());
      showHolder.poster.setImage(poster);
      showHolder.title.setText(show.getTitle());
    } else {
      EpisodeViewHolder episodeHolder = (EpisodeViewHolder) holder;
      Episode episode = (Episode) getList().get(position);

      final String screenshotUri =
          ImageUri.create(ImageUri.ITEM_EPISODE, ImageType.STILL, episode.getId());
      episodeHolder.screenshot.setImage(screenshotUri);

      final String title =
          DataHelper.getEpisodeTitle(context, episode.getTitle(), episode.getSeason(),
              episode.getEpisode(), episode.getWatched(), true);
      episodeHolder.title.setText(title);
    }
  }

  static class ShowViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.poster) RemoteImageView poster;
    @BindView(R.id.title) TextView title;

    ShowViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }

  static class EpisodeViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.screenshot) RemoteImageView screenshot;
    @BindView(R.id.title) TextView title;

    EpisodeViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }
}
