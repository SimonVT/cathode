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

import android.content.res.Resources;
import android.database.Cursor;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.widget.RemoteImageView;
import net.simonvt.cathode.widget.TimeStamp;

public class SeasonAdapter extends RecyclerCursorAdapter<SeasonAdapter.ViewHolder> {

  public interface EpisodeClickListener {

    void onEpisodeClick(View view, int position, long id);
  }

  @Inject ShowTaskScheduler showScheduler;
  @Inject EpisodeTaskScheduler episodeScheduler;

  private FragmentActivity activity;
  private Resources resources;

  private LibraryType type;

  private EpisodeClickListener clickListener;

  public SeasonAdapter(FragmentActivity activity, EpisodeClickListener clickListener, Cursor cursor,
      LibraryType type) {
    super(activity, cursor);
    CathodeApp.inject(activity, this);
    this.activity = activity;
    this.clickListener = clickListener;
    this.type = type;
    resources = activity.getResources();
  }

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(getContext()).inflate(R.layout.list_row_episode, parent, false);

    final ViewHolder holder = new ViewHolder(v);

    v.setOnClickListener(new OnClickListener() {
      @Override public void onClick(View v) {
        clickListener.onEpisodeClick(holder.itemView, holder.getPosition(), holder.getItemId());
      }
    });

    holder.number.setOnClickListener(new OnClickListener() {
      @Override public void onClick(View v) {
        final boolean activated = holder.number.isActivated();
        holder.number.setActivated(!activated);
        if (type == LibraryType.COLLECTION) {
          episodeScheduler.setIsInCollection(holder.getItemId(), !activated);
        } else {
          episodeScheduler.setWatched(holder.getItemId(), !activated);
        }
      }
    });

    return holder;
  }

  @Override protected void onBindViewHolder(ViewHolder holder, Cursor cursor, int position) {
    final long id = cursor.getLong(cursor.getColumnIndexOrThrow(EpisodeColumns.ID));
    final String title = cursor.getString(cursor.getColumnIndexOrThrow(EpisodeColumns.TITLE));
    final int season = cursor.getInt(cursor.getColumnIndexOrThrow(EpisodeColumns.SEASON));
    final int episode = cursor.getInt(cursor.getColumnIndexOrThrow(EpisodeColumns.EPISODE));
    final boolean watched =
        cursor.getInt(cursor.getColumnIndexOrThrow(EpisodeColumns.WATCHED)) == 1;
    final boolean inCollection =
        cursor.getInt(cursor.getColumnIndexOrThrow(EpisodeColumns.IN_COLLECTION)) == 1;
    final boolean inWatchlist =
        cursor.getInt(cursor.getColumnIndexOrThrow(EpisodeColumns.IN_WATCHLIST)) == 1;
    final boolean watching =
        cursor.getInt(cursor.getColumnIndexOrThrow(EpisodeColumns.WATCHING)) == 1;
    final boolean checkedIn =
        cursor.getInt(cursor.getColumnIndexOrThrow(EpisodeColumns.CHECKED_IN)) == 1;
    final long firstAired =
        cursor.getLong(cursor.getColumnIndexOrThrow(EpisodeColumns.FIRST_AIRED));
    final String screen = cursor.getString(cursor.getColumnIndexOrThrow(EpisodeColumns.SCREEN));

    holder.screen.setImage(screen);

    holder.title.setText(title);

    holder.firstAired.setTimeInMillis(firstAired);
    holder.firstAired.setExtended(true);

    holder.number.setText(String.valueOf(episode));
    if (type == LibraryType.COLLECTION) {
      holder.number.setTextColor(resources.getColorStateList(R.color.episode_number_collected));
      holder.number.setActivated(inCollection);
    } else {
      holder.number.setTextColor(resources.getColorStateList(R.color.episode_number_watched));
      holder.number.setActivated(watched);
    }
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    @InjectView(R.id.screen) RemoteImageView screen;

    @InjectView(R.id.title) TextView title;
    @InjectView(R.id.firstAired) TimeStamp firstAired;
    @InjectView(R.id.number) TextView number;

    ViewHolder(View v) {
      super(v);
      ButterKnife.inject(this, v);
    }
  }
}
