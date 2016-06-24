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
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.scheduler.SeasonTaskScheduler;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.listener.SeasonClickListener;
import net.simonvt.cathode.widget.OverflowView;
import net.simonvt.schematic.Cursors;

public class SeasonsAdapter extends RecyclerCursorAdapter<SeasonsAdapter.ViewHolder> {

  public static final String[] PROJECTION = new String[] {
      SeasonColumns.ID,
      SeasonColumns.SHOW_ID,
      SeasonColumns.SEASON,
      SeasonColumns.UNAIRED_COUNT,
      SeasonColumns.WATCHED_COUNT,
      SeasonColumns.IN_COLLECTION_COUNT,
      SeasonColumns.AIRED_COUNT,
      SeasonColumns.WATCHED_AIRED_COUNT,
      SeasonColumns.COLLECTED_AIRED_COUNT,
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

  private Resources resources;

  private SeasonClickListener clickListener;

  private LibraryType type;

  private ColorStateList primaryColor;
  private ColorStateList secondaryColor;

  public SeasonsAdapter(Context context, SeasonClickListener clickListener, LibraryType type) {
    super(context, null);
    CathodeApp.inject(context, this);
    resources = context.getResources();
    this.clickListener = clickListener;
    this.type = type;

    TypedArray a = context.obtainStyledAttributes(new int[] {
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

  @Override public void onViewRecycled(ViewHolder holder) {
    holder.overflow.dismiss();
  }

  @Override protected void onBindViewHolder(final ViewHolder holder, Cursor cursor, int position) {
    final int seasonId = Cursors.getInt(cursor, SeasonColumns.ID);
    final int seasonNumber = Cursors.getInt(cursor, SeasonColumns.SEASON);
    final int airedCount = Cursors.getInt(cursor, SeasonColumns.AIRED_COUNT);
    final int watchedAiredCount = Cursors.getInt(cursor, SeasonColumns.WATCHED_AIRED_COUNT);
    final int watchedCount = Cursors.getInt(cursor, SeasonColumns.WATCHED_COUNT);
    final int collectedAiredCount = Cursors.getInt(cursor, SeasonColumns.COLLECTED_AIRED_COUNT);
    final int collectedCount = Cursors.getInt(cursor, SeasonColumns.IN_COLLECTION_COUNT);

    switch (type) {
      case WATCHLIST:
      case WATCHED:
        bindWatched(getContext(), holder, cursor);
        break;

      case COLLECTION:
        bindCollection(getContext(), holder, cursor);
        break;
    }

    holder.overflow.removeItems();
    if (airedCount - watchedAiredCount > 0) {
      holder.overflow.addItem(R.id.action_watched, R.string.action_watched);
    }
    if (watchedCount > 0) {
      holder.overflow.addItem(R.id.action_unwatched, R.string.action_unwatched);
    }
    if (airedCount - collectedAiredCount > 0) {
      holder.overflow.addItem(R.id.action_collection_add, R.string.action_collection_add);
    }
    if (collectedCount > 0) {
      holder.overflow.addItem(R.id.action_collection_remove, R.string.action_collection_remove);
    }

    if (seasonNumber == 0) {
      holder.title.setText(R.string.season_special);
    } else {
      holder.title.setText(resources.getString(R.string.season_x, seasonNumber));
    }

    holder.overflow.setListener(new OverflowView.OverflowActionListener() {

      @Override public void onPopupShown() {
      }

      @Override public void onPopupDismissed() {
      }

      @Override public void onActionSelected(int action) {
        switch (action) {
          case R.id.action_watched:
            seasonScheduler.setWatched(seasonId, true);
            break;

          case R.id.action_unwatched:
            seasonScheduler.setWatched(seasonId, false);
            break;

          case R.id.action_collection_add:
            seasonScheduler.setInCollection(seasonId, true);
            break;

          case R.id.action_collection_remove:
            seasonScheduler.setInCollection(seasonId, false);
            break;
        }
      }
    });
  }

  private void bindWatched(Context context, ViewHolder holder, Cursor cursor) {
    final int unairedCount = Cursors.getInt(cursor, SeasonColumns.UNAIRED_COUNT);
    final int airedCount = Cursors.getInt(cursor, SeasonColumns.AIRED_COUNT);
    final int watchedAiredCount = Cursors.getInt(cursor, SeasonColumns.WATCHED_AIRED_COUNT);
    final int toWatch = airedCount - watchedAiredCount;

    holder.progress.setMax(airedCount);
    holder.progress.setProgress(watchedAiredCount);

    String unwatched;
    if (toWatch == 0) {
      unwatched = resources.getString(R.string.all_watched);
    } else {
      unwatched = resources.getQuantityString(R.plurals.x_unwatched, toWatch, toWatch);
    }
    String unaired;
    if (unairedCount > 0) {
      unaired = resources.getQuantityString(R.plurals.x_unaired, unairedCount, unairedCount);
    } else {
      unaired = "";
    }

    SpannableStringBuilder ssb =
        new SpannableStringBuilder().append(unwatched).append(" ").append(unaired);
    ssb.setSpan(new TextAppearanceSpan(null, 0, 0, primaryColor, null), 0, unwatched.length() - 1,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    if (unairedCount > 0) {
      ssb.setSpan(new TextAppearanceSpan(null, 0, 0, secondaryColor, null), unwatched.length(),
          unwatched.length() + unaired.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    holder.summary.setText(ssb.toString());
  }

  private void bindCollection(Context context, ViewHolder holder, Cursor cursor) {
    final int unairedCount = Cursors.getInt(cursor, SeasonColumns.UNAIRED_COUNT);

    final int airedCount = Cursors.getInt(cursor, SeasonColumns.AIRED_COUNT);
    final int collectedAiredCount = Cursors.getInt(cursor, SeasonColumns.COLLECTED_AIRED_COUNT);
    final int toCollect = airedCount - collectedAiredCount;

    holder.progress.setMax(airedCount);
    holder.progress.setProgress(collectedAiredCount);

    String uncollected;
    if (toCollect == 0) {
      uncollected = resources.getString(R.string.all_collected);
    } else {
      uncollected = resources.getQuantityString(R.plurals.x_uncollected, toCollect, toCollect);
    }
    String unaired;
    if (unairedCount > 0) {
      unaired = resources.getQuantityString(R.plurals.x_unaired, unairedCount, unairedCount);
    } else {
      unaired = "";
    }

    SpannableStringBuilder ssb =
        new SpannableStringBuilder().append(uncollected).append(" ").append(unaired);
    ssb.setSpan(new TextAppearanceSpan(null, 0, 0, primaryColor, null), 0, uncollected.length() - 1,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    if (unairedCount > 0) {
      ssb.setSpan(new TextAppearanceSpan(null, 0, 0, secondaryColor, null), uncollected.length(),
          uncollected.length() + unaired.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    holder.summary.setText(ssb.toString());
  }

  static class ViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.title) TextView title;
    @BindView(R.id.progress) ProgressBar progress;
    @BindView(R.id.summary) TextView summary;
    @BindView(R.id.overflow) OverflowView overflow;

    ViewHolder(View v) {
      super(v);
      ButterKnife.bind(this, v);
      overflow.addItem(R.id.action_watched, R.string.action_watched);
      overflow.addItem(R.id.action_unwatched, R.string.action_unwatched);
    }
  }
}
