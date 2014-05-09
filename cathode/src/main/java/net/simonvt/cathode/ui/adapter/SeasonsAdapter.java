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
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.scheduler.SeasonTaskScheduler;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.widget.OverflowView;

public class SeasonsAdapter extends CursorAdapter {

  public static final String[] PROJECTION = new String[] {
      SeasonColumns.ID, SeasonColumns.AIRDATE_COUNT, SeasonColumns.UNAIRED_COUNT,
      SeasonColumns.WATCHED_COUNT, SeasonColumns.IN_COLLECTION_COUNT, SeasonColumns.SEASON,
      SeasonColumns.WATCHED_COUNT,
  };

  @Inject SeasonTaskScheduler seasonScheduler;

  private Resources resources;

  private LibraryType type;

  public SeasonsAdapter(Context context, LibraryType type) {
    super(context, null, 0);
    CathodeApp.inject(context, this);
    resources = context.getResources();
    this.type = type;
  }

  @Override public View newView(Context context, Cursor cursor, ViewGroup parent) {
    View v = LayoutInflater.from(context).inflate(R.layout.list_row_season, parent, false);

    ViewHolder vh = new ViewHolder(v);
    v.setTag(vh);

    return v;
  }

  private void bindWatched(Context context, ViewHolder vh, Cursor cursor) {
    final int airdateCount =
        cursor.getInt(cursor.getColumnIndexOrThrow(SeasonColumns.AIRDATE_COUNT));
    final int unairedCount =
        cursor.getInt(cursor.getColumnIndexOrThrow(SeasonColumns.UNAIRED_COUNT));
    final int watchedCount =
        cursor.getInt(cursor.getColumnIndexOrThrow(SeasonColumns.WATCHED_COUNT));
    int toWatch = airdateCount - unairedCount - watchedCount;
    toWatch = Math.max(toWatch, 0); // TODO: Query watched, aired, episodes instead

    vh.progress.setMax(airdateCount);
    vh.progress.setProgress(watchedCount);

    TypedArray a = context.obtainStyledAttributes(new int[] {
        android.R.attr.textColorPrimary, android.R.attr.textColorSecondary,
    });
    ColorStateList primaryColor = a.getColorStateList(0);
    ColorStateList secondaryColor = a.getColorStateList(1);
    a.recycle();

    String unwatched;
    if (toWatch == 0) {
      unwatched = resources.getString(R.string.all_watched);
    } else {
      unwatched = resources.getString(R.string.x_unwatched, toWatch);
    }
    String unaired;
    if (unairedCount > 0) {
      unaired = resources.getString(R.string.x_unaired, unairedCount);
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

    vh.summary.setText(ssb.toString());
  }

  private void bindCollection(Context context, ViewHolder vh, Cursor cursor) {
    final int airdateCount =
        cursor.getInt(cursor.getColumnIndexOrThrow(SeasonColumns.AIRDATE_COUNT));
    final int unairedCount =
        cursor.getInt(cursor.getColumnIndexOrThrow(SeasonColumns.UNAIRED_COUNT));
    final int collectedCount =
        cursor.getInt(cursor.getColumnIndexOrThrow(SeasonColumns.IN_COLLECTION_COUNT));
    int toCollect = airdateCount - unairedCount - collectedCount;
    toCollect = Math.max(toCollect, 0); // TODO: Query collected, aired, episodes instead

    vh.progress.setMax(airdateCount);
    vh.progress.setProgress(collectedCount);

    String uncollected;
    if (toCollect == 0) {
      uncollected = resources.getString(R.string.all_collected);
    } else {
      uncollected = resources.getString(R.string.x_uncollected, toCollect);
    }
    vh.summary.setText(uncollected);
  }

  @Override public void bindView(View view, Context context, Cursor cursor) {
    ViewHolder vh = (ViewHolder) view.getTag();

    final int seasonId = cursor.getInt(cursor.getColumnIndexOrThrow(SeasonColumns.ID));
    final int seasonNumber = cursor.getInt(cursor.getColumnIndexOrThrow(SeasonColumns.SEASON));
    final int airdateCount =
        cursor.getInt(cursor.getColumnIndexOrThrow(SeasonColumns.AIRDATE_COUNT));
    final int unairedCount =
        cursor.getInt(cursor.getColumnIndexOrThrow(SeasonColumns.UNAIRED_COUNT));
    final int airedCount = airdateCount - unairedCount;
    final int collectedCount =
        cursor.getInt(cursor.getColumnIndexOrThrow(SeasonColumns.IN_COLLECTION_COUNT));
    final int watchedCount =
        cursor.getInt(cursor.getColumnIndexOrThrow(SeasonColumns.WATCHED_COUNT));

    switch (type) {
      case WATCHLIST:
      case WATCHED:
        bindWatched(context, vh, cursor);
        break;

      case COLLECTION:
        bindCollection(context, vh, cursor);
        break;
    }

    vh.overflow.removeItems();
    if (airedCount - watchedCount > 0) {
      vh.overflow.addItem(R.id.action_watched, R.string.action_watched);
    }
    if (watchedCount > 0) {
      vh.overflow.addItem(R.id.action_unwatched, R.string.action_unwatched);
    }
    if (airedCount - collectedCount > 0) {
      vh.overflow.addItem(R.id.action_collection_add, R.string.action_collection_add);
    }
    if (collectedCount > 0) {
      vh.overflow.addItem(R.id.action_collection_remove, R.string.action_collection_remove);
    }

    vh.title.setText(resources.getQuantityString(R.plurals.season_x, seasonNumber, seasonNumber));
    vh.overflow.setListener(new OverflowView.OverflowActionListener() {
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

  static class ViewHolder {

    @InjectView(R.id.title) TextView title;
    @InjectView(R.id.progress) ProgressBar progress;
    @InjectView(R.id.summary) TextView summary;
    @InjectView(R.id.overflow) OverflowView overflow;

    ViewHolder(View v) {
      ButterKnife.inject(this, v);
      overflow.addItem(R.id.action_watched, R.string.action_watched);
      overflow.addItem(R.id.action_unwatched, R.string.action_unwatched);
    }
  }
}
