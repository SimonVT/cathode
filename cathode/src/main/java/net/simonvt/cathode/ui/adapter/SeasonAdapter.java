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
import android.content.res.Resources;
import android.database.Cursor;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.widget.RemoteImageView;
import net.simonvt.cathode.widget.TimeStamp;

public class SeasonAdapter extends CursorAdapter {

  @Inject ShowTaskScheduler showScheduler;
  @Inject EpisodeTaskScheduler episodeScheduler;

  private FragmentActivity activity;
  private Resources resources;

  private LibraryType type;

  public SeasonAdapter(FragmentActivity activity, LibraryType type) {
    super(activity, null, 0);
    CathodeApp.inject(activity, this);
    this.activity = activity;
    this.type = type;
    resources = activity.getResources();
  }

  @Override public void changeCursor(Cursor cursor) {
    super.changeCursor(cursor);
  }

  @Override public View newView(Context context, Cursor cursor, ViewGroup parent) {
    View v = LayoutInflater.from(context).inflate(R.layout.list_row_episode, parent, false);

    ViewHolder vh = new ViewHolder(v);
    v.setTag(vh);

    return v;
  }

  @Override public void bindView(View view, Context context, Cursor cursor) {
    final long id = cursor.getLong(cursor.getColumnIndexOrThrow(CathodeContract.Episodes._ID));
    final String title =
        cursor.getString(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.TITLE));
    final int season = cursor.getInt(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.SEASON));
    final int episode =
        cursor.getInt(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.EPISODE));
    final boolean watched =
        cursor.getInt(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.WATCHED)) == 1;
    final boolean inCollection =
        cursor.getInt(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.IN_COLLECTION)) == 1;
    final boolean inWatchlist =
        cursor.getInt(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.IN_WATCHLIST)) == 1;
    final boolean watching =
        cursor.getInt(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.WATCHING)) == 1;
    final boolean checkedIn =
        cursor.getInt(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.CHECKED_IN)) == 1;
    final long firstAired =
        cursor.getLong(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.FIRST_AIRED));
    final String screen =
        cursor.getString(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.SCREEN));

    final ViewHolder vh = (ViewHolder) view.getTag();

    vh.screen.setImage(screen);

    vh.title.setText(title);

    vh.firstAired.setTimeInMillis(firstAired);
    vh.firstAired.setExtended(true);

    vh.number.setText(String.valueOf(episode));
    if (type == LibraryType.COLLECTION) {
      vh.number.setTextColor(resources.getColorStateList(R.color.episode_number_collected));
      vh.number.setActivated(inCollection);
    } else {
      vh.number.setTextColor(resources.getColorStateList(R.color.episode_number_watched));
      vh.number.setActivated(watched);
    }
    vh.number.setOnClickListener(new OnClickListener() {
      @Override public void onClick(View v) {
        final boolean activated = vh.number.isActivated();
        vh.number.setActivated(!activated);
        if (type == LibraryType.COLLECTION) {
          episodeScheduler.setIsInCollection(id, !activated);
        } else {
          episodeScheduler.setWatched(id, !activated);
        }
      }
    });
  }

  static class ViewHolder {

    @InjectView(R.id.screen) RemoteImageView screen;

    @InjectView(R.id.title) TextView title;
    @InjectView(R.id.firstAired) TimeStamp firstAired;
    @InjectView(R.id.number) TextView number;

    ViewHolder(View v) {
      ButterKnife.inject(this, v);
    }
  }
}
