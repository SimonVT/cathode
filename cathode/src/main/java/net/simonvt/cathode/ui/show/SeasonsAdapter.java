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

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
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
import net.simonvt.cathode.common.ui.adapter.BaseAdapter;
import net.simonvt.cathode.common.widget.RemoteImageView;
import net.simonvt.cathode.entity.Season;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.ui.LibraryType;

public class SeasonsAdapter extends BaseAdapter<Season, SeasonsAdapter.ViewHolder> {

  public interface SeasonClickListener {

    void onSeasonClick(long showId, long seasonId, String showTitle, int seasonNumber);
  }

  private FragmentActivity activity;

  private Resources resources;

  private SeasonClickListener clickListener;

  private LibraryType type;

  private ColorStateList primaryColor;
  private ColorStateList secondaryColor;

  public SeasonsAdapter(FragmentActivity activity, SeasonClickListener clickListener,
      LibraryType type) {
    super(activity);
    this.activity = activity;
    resources = activity.getResources();
    this.clickListener = clickListener;
    this.type = type;
    setHasStableIds(true);

    TypedArray a = activity.obtainStyledAttributes(new int[] {
        android.R.attr.textColorPrimary, android.R.attr.textColorSecondary,
    });
    primaryColor = a.getColorStateList(0);
    //noinspection ResourceType
    secondaryColor = a.getColorStateList(1);
    a.recycle();
  }

  @Override public long getItemId(int position) {
    return getList().get(position).getId();
  }

  @Override protected boolean areItemsTheSame(@NonNull Season oldItem, @NonNull Season newItem) {
    return oldItem.getId() == newItem.getId();
  }

  @Override protected boolean areContentsTheSame(@NonNull Season oldItem, @NonNull Season newItem) {
    return oldItem.equals(newItem);
  }

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewGroup) {
    View v = LayoutInflater.from(getContext()).inflate(R.layout.list_row_season, parent, false);

    final ViewHolder holder = new ViewHolder(v);

    v.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        final int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
          Season season = getList().get(position);
          clickListener.onSeasonClick(season.getShowId(), season.getId(), season.getShowTitle(),
              season.getSeason());
        }
      }
    });

    return holder;
  }

  @Override public void onBindViewHolder(final ViewHolder holder, int position) {
    Season season = getList().get(position);
    final Long seasonId = season.getId();
    final int seasonNumber = season.getSeason();

    switch (type) {
      case WATCHLIST:
      case WATCHED:
        bindWatched(getContext(), holder, season);
        break;

      case COLLECTION:
        bindCollection(getContext(), holder, season);
        break;
    }

    if (seasonNumber == 0) {
      holder.title.setText(R.string.season_special);
    } else {
      holder.title.setText(resources.getString(R.string.season_x, seasonNumber));
    }

    final String posterUri = ImageUri.create(ImageUri.ITEM_SEASON, ImageType.POSTER, seasonId);
    holder.poster.setImage(posterUri);
  }

  private void bindWatched(Context context, ViewHolder holder, Season season) {
    final int unairedCount = season.getUnairedCount();
    final int airedCount = season.getAiredCount();
    final int watchedAiredCount = season.getWatchedAiredCount();
    final int toWatch = airedCount - watchedAiredCount;

    holder.progress.setMax(airedCount);
    holder.progress.setProgress(watchedAiredCount);

    String summary;
    if (toWatch > 0 && unairedCount > 0) {
      summary = resources.getQuantityString(R.plurals.x_unwatched_x_unaired, toWatch, toWatch,
          unairedCount);
    } else if (toWatch > 0 && unairedCount == 0) {
      summary = resources.getQuantityString(R.plurals.x_unwatched, toWatch, toWatch);
    } else if (toWatch == 0 && unairedCount > 0) {
      summary = resources.getQuantityString(R.plurals.x_unaired, unairedCount, unairedCount);
    } else {
      summary = resources.getString(R.string.all_watched);
    }

    holder.summary.setText(summary);
  }

  private void bindCollection(Context context, ViewHolder holder, Season season) {
    final int unairedCount = season.getUnairedCount();
    final int airedCount = season.getAiredCount();
    final int collectedAiredCount = season.getCollectedAiredCount();
    final int toCollect = airedCount - collectedAiredCount;

    holder.progress.setMax(airedCount);
    holder.progress.setProgress(collectedAiredCount);

    String summary;
    if (toCollect > 0 && unairedCount > 0) {
      summary = resources.getQuantityString(R.plurals.x_uncollected_x_unaired, toCollect, toCollect,
          unairedCount);
    } else if (toCollect > 0 && unairedCount == 0) {
      summary = resources.getQuantityString(R.plurals.x_uncollected, toCollect, toCollect);
    } else if (toCollect == 0 && unairedCount > 0) {
      summary = resources.getQuantityString(R.plurals.x_unaired, unairedCount, unairedCount);
    } else {
      summary = resources.getString(R.string.all_collected);
    }

    holder.summary.setText(summary);
  }

  static class ViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.title) TextView title;
    @BindView(R.id.progress) ProgressBar progress;
    @BindView(R.id.summary) TextView summary;
    @BindView(R.id.poster) RemoteImageView poster;

    ViewHolder(View v) {
      super(v);
      ButterKnife.bind(this, v);
    }
  }
}
