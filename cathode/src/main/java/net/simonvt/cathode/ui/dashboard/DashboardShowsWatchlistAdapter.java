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
import android.provider.BaseColumns;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.LastModifiedColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.ui.adapter.AdapterNotifier;
import net.simonvt.cathode.ui.adapter.BaseAdapter;
import net.simonvt.cathode.util.DataHelper;
import net.simonvt.cathode.widget.RemoteImageView;
import net.simonvt.schematic.Cursors;

public class DashboardShowsWatchlistAdapter extends BaseAdapter<RecyclerView.ViewHolder> {

  public static final String[] PROJECTION = new String[] {
      Tables.SHOWS + "." + ShowColumns.ID, Tables.SHOWS + "." + ShowColumns.TITLE,
      Tables.SHOWS + "." + ShowColumns.OVERVIEW, Tables.SHOWS + "." + ShowColumns.POSTER,
      Tables.SHOWS + "." + ShowColumns.LAST_MODIFIED,
  };

  public static final String[] PROJECTION_EPISODE = new String[] {
      Tables.EPISODES + "." + EpisodeColumns.ID, Tables.EPISODES + "." + EpisodeColumns.SCREENSHOT,
      Tables.EPISODES + "." + EpisodeColumns.TITLE,
      Tables.EPISODES + "." + EpisodeColumns.FIRST_AIRED,
      Tables.EPISODES + "." + EpisodeColumns.SEASON, Tables.EPISODES + "." + EpisodeColumns.EPISODE,
      Tables.EPISODES + "." + LastModifiedColumns.LAST_MODIFIED,
      Tables.SHOWS + "." + ShowColumns.TITLE,
  };

  private static final int TYPE_SHOW = 0;
  private static final int TYPE_EPISODE = 1;

  private Context context;

  private DashboardFragment.OverviewCallback callback;

  private AdapterNotifier notifier;

  private Cursor showsWatchlist;

  private Cursor episodeWatchlist;

  public DashboardShowsWatchlistAdapter(Context context,
      DashboardFragment.OverviewCallback callback) {
    this.context = context;
    this.callback = callback;

    notifier = new AdapterNotifier(this);
  }

  public void changeShowsCursor(Cursor cursor) {
    showsWatchlist = cursor;
    notifier.notifyChanged();
  }

  public void changeEpisodeCursor(Cursor cursor) {
    episodeWatchlist = cursor;
    notifier.notifyChanged();
  }

  @Override public int getItemViewType(int position) {
    if (position < showsWatchlist.getCount()) {
      return TYPE_SHOW;
    }

    return TYPE_EPISODE;
  }

  private Cursor getCursor(int position) {
    final int showCount = showsWatchlist.getCount();
    if (position < showCount) {
      showsWatchlist.moveToPosition(position);
      return showsWatchlist;
    } else {
      episodeWatchlist.moveToPosition(position - showCount);
      return episodeWatchlist;
    }
  }

  @Override public long getItemId(int position) {
    return Cursors.getLong(getCursor(position), BaseColumns._ID);
  }

  @Override public int getItemCount() {
    if (showsWatchlist == null || episodeWatchlist == null) {
      return 0;
    }

    return showsWatchlist.getCount() + episodeWatchlist.getCount();
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
            Cursor cursor = getCursor(position);
            final long id = Cursors.getLong(cursor, ShowColumns.ID);
            final String title = Cursors.getString(cursor, ShowColumns.TITLE);
            final String overview = Cursors.getString(cursor, ShowColumns.OVERVIEW);
            callback.onDisplayShow(id, title, overview);
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
            Cursor cursor = getCursor(position);
            final long id = Cursors.getLong(cursor, EpisodeColumns.ID);
            final String showTitle = Cursors.getString(cursor, ShowColumns.TITLE);
            callback.onDisplayEpisode(id, showTitle);
          }
        }
      });

      return holder;
    }
  }

  @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    Cursor cursor = getCursor(position);
    if (holder.getItemViewType() == TYPE_SHOW) {
      ShowViewHolder showHolder = (ShowViewHolder) holder;
      final String poster = Cursors.getString(cursor, ShowColumns.POSTER);
      final String title = Cursors.getString(cursor, ShowColumns.TITLE);

      showHolder.poster.setImage(poster);
      showHolder.title.setText(title);
    } else {
      EpisodeViewHolder episodeHolder = (EpisodeViewHolder) holder;

      final long id = Cursors.getLong(cursor, EpisodeColumns.ID);
      final String screenshotUrl = Cursors.getString(cursor, EpisodeColumns.SCREENSHOT);
      final int season = Cursors.getInt(cursor, EpisodeColumns.SEASON);
      final int episode = Cursors.getInt(cursor, EpisodeColumns.EPISODE);
      final String title = DataHelper.getEpisodeTitle(context, cursor, season, episode);

      episodeHolder.screenshot.setImage(screenshotUrl);
      String episodeText =
          context.getString(R.string.upcoming_episode_next, season, episode, title);
      episodeHolder.title.setText(episodeText);
    }
  }

  @Override public long getLastModified(int position) {
    return Cursors.getLong(getCursor(position), LastModifiedColumns.LAST_MODIFIED);
  }

  static class ShowViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.poster) RemoteImageView poster;
    @BindView(R.id.title) TextView title;

    public ShowViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }

  static class EpisodeViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.screenshot) RemoteImageView screenshot;
    @BindView(R.id.title) TextView title;

    public EpisodeViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }
}
