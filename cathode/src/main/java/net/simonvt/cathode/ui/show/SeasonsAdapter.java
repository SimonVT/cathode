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
import android.database.Cursor;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.Injector;
import net.simonvt.cathode.common.ui.adapter.RecyclerCursorAdapter;
import net.simonvt.cathode.common.widget.RemoteImageView;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.sync.scheduler.SeasonTaskScheduler;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.listener.SeasonClickListener;
import net.simonvt.schematic.Cursors;

public class SeasonsAdapter extends RecyclerCursorAdapter<SeasonsAdapter.ViewHolder> {

  public static final String[] PROJECTION = new String[] {
      SeasonColumns.ID, SeasonColumns.SHOW_ID, SeasonColumns.SEASON, SeasonColumns.UNAIRED_COUNT,
      SeasonColumns.WATCHED_COUNT, SeasonColumns.IN_COLLECTION_COUNT, SeasonColumns.AIRED_COUNT,
      SeasonColumns.WATCHED_AIRED_COUNT, SeasonColumns.COLLECTED_AIRED_COUNT,
      SeasonColumns.LAST_MODIFIED, "(SELECT "
      + ShowColumns.TITLE
      + " FROM "
      + Tables.SHOWS
      + " WHERE "
      + Tables.SHOWS
      + "."
      + ShowColumns.ID
      + "="
      + Tables.SEASONS
      + "."
      + SeasonColumns.SHOW_ID
      + ") AS seasonShowTitle",
  };

  @Inject SeasonTaskScheduler seasonScheduler;

  private FragmentActivity activity;

  private Resources resources;

  private SeasonClickListener clickListener;

  private LibraryType type;

  private ColorStateList primaryColor;
  private ColorStateList secondaryColor;

  public SeasonsAdapter(FragmentActivity activity, SeasonClickListener clickListener,
      LibraryType type) {
    super(activity, null);
    Injector.inject(this);
    this.activity = activity;
    resources = activity.getResources();
    this.clickListener = clickListener;
    this.type = type;

    TypedArray a = activity.obtainStyledAttributes(new int[] {
        android.R.attr.textColorPrimary, android.R.attr.textColorSecondary,
    });
    primaryColor = a.getColorStateList(0);
    //noinspection ResourceType
    secondaryColor = a.getColorStateList(1);
    a.recycle();
  }

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewGroup) {
    View v = LayoutInflater.from(getContext()).inflate(R.layout.list_row_season, parent, false);

    final ViewHolder holder = new ViewHolder(v);

    v.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        final int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
          Cursor cursor = getCursor(position);
          final long showId = Cursors.getLong(cursor, SeasonColumns.SHOW_ID);
          final int seasonNumber = Cursors.getInt(cursor, SeasonColumns.SEASON);
          final String showTitle = Cursors.getString(cursor, "seasonShowTitle");
          clickListener.onSeasonClick(showId, holder.getItemId(), showTitle, seasonNumber);
        }
      }
    });

    return holder;
  }

  @Override protected void onBindViewHolder(final ViewHolder holder, Cursor cursor, int position) {
    final int seasonId = Cursors.getInt(cursor, SeasonColumns.ID);
    final int seasonNumber = Cursors.getInt(cursor, SeasonColumns.SEASON);

    switch (type) {
      case WATCHLIST:
      case WATCHED:
        bindWatched(getContext(), holder, cursor);
        break;

      case COLLECTION:
        bindCollection(getContext(), holder, cursor);
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

  private void bindWatched(Context context, ViewHolder holder, Cursor cursor) {
    final int unairedCount = Cursors.getInt(cursor, SeasonColumns.UNAIRED_COUNT);
    final int airedCount = Cursors.getInt(cursor, SeasonColumns.AIRED_COUNT);
    final int watchedAiredCount = Cursors.getInt(cursor, SeasonColumns.WATCHED_AIRED_COUNT);
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

  private void bindCollection(Context context, ViewHolder holder, Cursor cursor) {
    final int unairedCount = Cursors.getInt(cursor, SeasonColumns.UNAIRED_COUNT);

    final int airedCount = Cursors.getInt(cursor, SeasonColumns.AIRED_COUNT);
    final int collectedAiredCount = Cursors.getInt(cursor, SeasonColumns.COLLECTED_AIRED_COUNT);
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
