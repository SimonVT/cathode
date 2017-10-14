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
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.LastModifiedColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.util.DataHelper;
import net.simonvt.schematic.Cursors;

public class DashboardUpcomingShowsAdapter
    extends RecyclerCursorAdapter<DashboardUpcomingShowsAdapter.ViewHolder> {

  private static final String COLUMN_EPISODE_ID = "episodeId";
  private static final String COLUMN_EPISODE_LAST_UPDATED = "episodeLastUpdated";

  public static final String[] PROJECTION = new String[] {
      Tables.SHOWS + "." + ShowColumns.ID,
      Tables.SHOWS + "." + ShowColumns.TITLE,
      Tables.SHOWS + "." + ShowColumns.OVERVIEW,
      Tables.SHOWS + "." + ShowColumns.STATUS,
      ShowColumns.WATCHING,
      Tables.SHOWS + "." + ShowColumns.LAST_MODIFIED,
      Tables.EPISODES + "." + EpisodeColumns.ID + " AS " + COLUMN_EPISODE_ID,
      Tables.EPISODES + "." + EpisodeColumns.TITLE,
      Tables.EPISODES + "." + EpisodeColumns.FIRST_AIRED,
      Tables.EPISODES + "." + EpisodeColumns.SEASON,
      Tables.EPISODES + "." + EpisodeColumns.EPISODE,
      Tables.EPISODES + "." + EpisodeColumns.WATCHED,
      Tables.EPISODES + "." + EpisodeColumns.LAST_MODIFIED + " AS " + COLUMN_EPISODE_LAST_UPDATED,
  };

  @Inject ShowTaskScheduler showScheduler;

  private DashboardFragment.OverviewCallback callback;

  public DashboardUpcomingShowsAdapter(Context context,
      DashboardFragment.OverviewCallback callback) {
    super(context);
    this.callback = callback;

    Injector.inject(this);
  }

  @Override public long getLastModified(int position) {
    Cursor cursor = getCursor(position);

    final long showLastModified = Cursors.getLong(cursor, LastModifiedColumns.LAST_MODIFIED);
    final long episodeLastModified = Cursors.getLong(cursor, COLUMN_EPISODE_LAST_UPDATED);

    return showLastModified + episodeLastModified;
  }

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.list_row_dashboard_show_upcoming, parent, false);
    final ViewHolder holder = new ViewHolder(view);

    view.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        final int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
          Cursor cursor = getCursor(position);
          final long episodeId = Cursors.getLong(cursor, COLUMN_EPISODE_ID);
          final String showTitle = Cursors.getString(cursor, ShowColumns.TITLE);
          callback.onDisplayEpisode(episodeId, showTitle);
        }
      }
    });

    return holder;
  }

  @Override protected void onBindViewHolder(ViewHolder holder, Cursor cursor, int position) {
    final long id = Cursors.getLong(cursor, ShowColumns.ID);
    final String title = Cursors.getString(cursor, ShowColumns.TITLE);
    final boolean watching = Cursors.getBoolean(cursor, ShowColumns.WATCHING);
    final int season = Cursors.getInt(cursor, EpisodeColumns.SEASON);
    final int episode = Cursors.getInt(cursor, EpisodeColumns.EPISODE);
    final boolean watched = Cursors.getBoolean(cursor, EpisodeColumns.WATCHED);
    final String episodeTitle =
        DataHelper.getEpisodeTitle(getContext(), cursor, season, episode, watched, true);

    final String poster = ImageUri.create(ImageUri.ITEM_SHOW, ImageType.POSTER, id);

    holder.poster.setImage(poster);
    holder.title.setText(title);

    if (watching) {
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
