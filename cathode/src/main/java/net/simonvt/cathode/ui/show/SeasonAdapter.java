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
package net.simonvt.cathode.ui.show;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.ui.FragmentsUtils;
import net.simonvt.cathode.common.ui.adapter.BaseAdapter;
import net.simonvt.cathode.common.widget.RemoteImageView;
import net.simonvt.cathode.common.widget.TimeStamp;
import net.simonvt.cathode.entity.Episode;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.provider.util.DataHelper;
import net.simonvt.cathode.settings.TraktLinkSettings;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.history.AddToHistoryDialog;
import net.simonvt.cathode.ui.history.RemoveFromHistoryDialog;

public class SeasonAdapter extends BaseAdapter<Episode, SeasonAdapter.ViewHolder> {

  public interface EpisodeCallbacks {

    void onEpisodeClick(long episodeId);

    void setEpisodeCollected(long episodeId, boolean collected);
  }

  private FragmentActivity activity;
  private Resources resources;

  private LibraryType type;

  private EpisodeCallbacks callbacks;

  public SeasonAdapter(FragmentActivity activity, EpisodeCallbacks callbacks, LibraryType type) {
    super(activity);
    this.activity = activity;
    this.callbacks = callbacks;
    this.type = type;
    resources = activity.getResources();
    setHasStableIds(true);
  }

  @Override public long getItemId(int position) {
    return getList().get(position).getId();
  }

  @Override protected boolean areItemsTheSame(@NonNull Episode oldItem, @NonNull Episode newItem) {
    return oldItem.getId() == newItem.getId();
  }

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(getContext()).inflate(R.layout.list_row_episode, parent, false);

    final ViewHolder holder = new ViewHolder(v);

    v.setOnClickListener(new OnClickListener() {
      @Override public void onClick(View v) {
        final int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
          callbacks.onEpisodeClick(holder.getItemId());
        }
      }
    });

    holder.number.setOnClickListener(new OnClickListener() {
      @Override public void onClick(View v) {
        if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
          final boolean activated = holder.number.isActivated();
          if (type == LibraryType.COLLECTION) {
            holder.number.setActivated(!activated);
            callbacks.setEpisodeCollected(holder.getItemId(), !activated);
          } else {
            if (activated) {
              if (TraktLinkSettings.isLinked(getContext())) {
                FragmentsUtils.instantiate(activity.getSupportFragmentManager(),
                    RemoveFromHistoryDialog.class,
                    RemoveFromHistoryDialog.getArgs(RemoveFromHistoryDialog.Type.EPISODE,
                        holder.getItemId(), holder.episodeTitle, holder.showTitle))
                    .show(activity.getSupportFragmentManager(), RemoveFromHistoryDialog.TAG);
              }
            } else {
              FragmentsUtils.instantiate(activity.getSupportFragmentManager(),
                  AddToHistoryDialog.class,
                  AddToHistoryDialog.getArgs(AddToHistoryDialog.Type.EPISODE, holder.getItemId(),
                      holder.episodeTitle))
                  .show(activity.getSupportFragmentManager(), AddToHistoryDialog.TAG);
            }
          }
        }
      }
    });

    return holder;
  }

  @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    Episode episode = getList().get(position);
    final long id = episode.getId();
    final String title =
        DataHelper.getEpisodeTitle(getContext(), episode.getTitle(), episode.getSeason(),
            episode.getEpisode(), episode.getWatched());
    final String showTitle = episode.getShowTitle();
    final String screenshotUri = ImageUri.create(ImageUri.ITEM_EPISODE, ImageType.STILL, id);

    holder.episodeTitle = title;
    holder.showTitle = showTitle;

    holder.screen.setImage(screenshotUri);

    holder.title.setText(title);

    holder.firstAired.setTimeInMillis(episode.getFirstAired());
    holder.firstAired.setExtended(true);

    holder.number.setText(String.valueOf(episode.getEpisode()));
    if (type == LibraryType.COLLECTION) {
      holder.number.setTextColor(resources.getColorStateList(R.color.episode_number_collected));
      holder.number.setActivated(episode.getInCollection());
    } else {
      holder.number.setTextColor(resources.getColorStateList(R.color.episode_number_watched));
      holder.number.setActivated(episode.getWatched());
    }
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.screen) RemoteImageView screen;

    @BindView(R.id.title) TextView title;
    @BindView(R.id.firstAired) TimeStamp firstAired;
    @BindView(R.id.number) TextView number;

    String episodeTitle;
    String showTitle;

    ViewHolder(View v) {
      super(v);
      ButterKnife.bind(this, v);
    }
  }
}
