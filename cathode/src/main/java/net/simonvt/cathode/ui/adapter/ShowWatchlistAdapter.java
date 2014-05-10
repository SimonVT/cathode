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
import android.database.Cursor;
import android.provider.BaseColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.provider.CathodeDatabase;
import net.simonvt.cathode.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.widget.CircularProgressIndicator;
import net.simonvt.cathode.widget.IndicatorView;
import net.simonvt.cathode.widget.OverflowView;
import net.simonvt.cathode.widget.RemoteImageView;
import net.simonvt.cathode.widget.TimeStamp;

public class ShowWatchlistAdapter extends BaseAdapter {

  public interface RemoveListener {

    void onRemoveItem(View view, int position);
  }

  public static final String[] PROJECTION_SHOW = new String[] {
      CathodeDatabase.Tables.SHOWS + "." + BaseColumns._ID,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.TITLE,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.OVERVIEW,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.POSTER,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.TVDB_ID,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.WATCHED_COUNT,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.IN_COLLECTION_COUNT,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.IN_WATCHLIST,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.RATING_PERCENTAGE,
  };

  public static final String[] PROJECTION_EPISODE = new String[] {
      CathodeDatabase.Tables.EPISODES + "." + CathodeContract.Episodes._ID,
      CathodeDatabase.Tables.EPISODES + "." + CathodeContract.Episodes.SCREEN,
      CathodeDatabase.Tables.EPISODES + "." + CathodeContract.Episodes.TITLE,
      CathodeDatabase.Tables.EPISODES + "." + CathodeContract.Episodes.FIRST_AIRED,
      CathodeDatabase.Tables.EPISODES + "." + CathodeContract.Episodes.SEASON,
      CathodeDatabase.Tables.EPISODES + "." + CathodeContract.Episodes.EPISODE,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.TITLE,
  };

  private static final int NO_POSITION = -1;

  @Inject ShowTaskScheduler showScheduler;

  @Inject EpisodeTaskScheduler episodeScheduler;

  private Context context;

  private RemoveListener listener;

  private Cursor showCursor;

  private Cursor episodeCursor;

  private int showCursorCount = 0;

  private int showHeaderPosition = NO_POSITION;

  private static final long SHOW_HEADER_ID = Long.MAX_VALUE;

  private int episodeCursorCount = 0;

  private int episodeHeaderPosition = NO_POSITION;

  private static final long EPISODE_HEADER_ID = Long.MAX_VALUE - 1;

  public ShowWatchlistAdapter(Context context, RemoveListener listener) {
    super();
    this.context = context;
    this.listener = listener;
    CathodeApp.inject(context, this);
  }

  public void changeShowCursor(Cursor cursor) {
    if (this.showCursor != null && !this.showCursor.isClosed()) {
      this.showCursor.close();
    }

    this.showCursor = cursor;

    notifyDataSetChanged();
  }

  public void changeEpisodeCursor(Cursor cursor) {
    if (this.episodeCursor != null && !this.episodeCursor.isClosed()) {
      this.episodeCursor.close();
    }

    this.episodeCursor = cursor;

    notifyDataSetChanged();
  }

  @Override public void notifyDataSetChanged() {
    showCursorCount = showCursor != null ? showCursor.getCount() : 0;
    episodeCursorCount = episodeCursor != null ? episodeCursor.getCount() : 0;
    if (showCursorCount > 0) {
      showHeaderPosition = 0;

      if (episodeCursorCount > 0) {
        episodeHeaderPosition = showCursorCount + 1;
      } else {
        episodeHeaderPosition = NO_POSITION;
      }
    } else {
      showHeaderPosition = NO_POSITION;

      if (episodeCursorCount > 0) {
        episodeHeaderPosition = 0;
      } else {
        episodeHeaderPosition = NO_POSITION;
      }
    }

    super.notifyDataSetChanged();
  }

  @Override public int getCount() {
    int count = 0;

    if (showCursorCount > 0) {
      count += showCursorCount + 1;
    }
    if (episodeCursorCount > 0) {
      count += episodeCursorCount + 1;
    }

    return count;
  }

  @Override public Cursor getItem(int position) {
    if (position == showHeaderPosition || position == episodeHeaderPosition) {
      return null;
    }

    if (showCursorCount > 0) {
      if (position <= showCursorCount) {
        showCursor.moveToPosition(position - 1);
        return showCursor;
      } else {
        episodeCursor.moveToPosition(position - 2 - showCursorCount);
        return episodeCursor;
      }
    } else {
      episodeCursor.moveToPosition(position - 1);
      return episodeCursor;
    }
  }

  public int getCorrectedPosition(int position) {
    if (position == showHeaderPosition || position == episodeHeaderPosition) {
      return -1;
    }

    int correctedPosition;

    if (showCursorCount > 0) {
      if (position <= showCursorCount) {
        correctedPosition = position - 1;
      } else {
        episodeCursor.moveToPosition(position - 2 - showCursorCount);
        correctedPosition = position - 2 - showCursorCount;
      }
    } else {
      correctedPosition = position - 1;
    }

    return correctedPosition;
  }

  public boolean isShow(int position) {
    if (showCursorCount == 0) {
      return false;
    }
    if (position > showCursorCount) {
      return false;
    }
    return true;
  }

  @Override public int getViewTypeCount() {
    return 3;
  }

  @Override public int getItemViewType(int position) {
    if (position == showHeaderPosition || position == episodeHeaderPosition) {
      return 0;
    }

    if (showCursorCount == 0 || position > showCursorCount) {
      return 2;
    }

    return 1;
  }

  @Override public boolean hasStableIds() {
    return true;
  }

  @Override public long getItemId(int position) {
    if (position == showHeaderPosition) {
      return SHOW_HEADER_ID;
    }
    if (position == episodeHeaderPosition) {
      return EPISODE_HEADER_ID;
    }
    Cursor c = getItem(position);
    return c.getLong(c.getColumnIndex(BaseColumns._ID));
  }

  @Override public boolean areAllItemsEnabled() {
    return false;
  }

  @Override public boolean isEnabled(int position) {
    return position != showHeaderPosition && position != episodeHeaderPosition;
  }

  @Override public View getView(final int position, View convertView, ViewGroup parent) {
    View v = convertView;

    if (position == showHeaderPosition) {
      if (v == null) {
        v = LayoutInflater.from(context).inflate(R.layout.list_row_upcoming_header, parent, false);
      }

      ((TextView) v).setText(R.string.header_shows);
    } else if (position == episodeHeaderPosition) {
      if (v == null) {
        v = LayoutInflater.from(context).inflate(R.layout.list_row_upcoming_header, parent, false);
      }

      ((TextView) v).setText(R.string.header_episodes);
    } else {
      Cursor cursor = getItem(position);

      if (cursor == showCursor) {
        if (v == null) {
          v = LayoutInflater.from(context)
              .inflate(R.layout.list_row_show_description, parent, false);
          ShowViewHolder vh = new ShowViewHolder(v);
          v.setTag(vh);
          vh.overflow.addItem(R.id.action_watchlist_remove, R.string.action_watchlist_remove);
        }

        ShowViewHolder vh = (ShowViewHolder) v.getTag();

        final long id = cursor.getLong(cursor.getColumnIndex(CathodeContract.Shows._ID));
        final boolean watched =
            cursor.getInt(cursor.getColumnIndex(CathodeContract.Shows.WATCHED_COUNT)) > 0;
        final boolean inCollection =
            cursor.getInt(cursor.getColumnIndex(CathodeContract.Shows.IN_COLLECTION_COUNT)) > 1;
        final boolean inWatchlist =
            cursor.getInt(cursor.getColumnIndex(CathodeContract.Shows.IN_WATCHLIST)) == 1;
        final int rating =
            cursor.getInt(cursor.getColumnIndex(CathodeContract.Shows.RATING_PERCENTAGE));

        vh.indicator.setWatched(watched);
        vh.indicator.setCollected(inCollection);
        vh.indicator.setInWatchlist(inWatchlist);

        vh.poster.setImage(cursor.getString(cursor.getColumnIndex(CathodeContract.Shows.POSTER)));
        vh.title.setText(cursor.getString(cursor.getColumnIndex(CathodeContract.Shows.TITLE)));
        vh.overview.setText(
            cursor.getString(cursor.getColumnIndex(CathodeContract.Shows.OVERVIEW)));

        vh.rating.setValue(rating);

        final View view = v;
        vh.overflow.setListener(new OverflowView.OverflowActionListener() {
          @Override public void onPopupShown() {
          }

          @Override public void onPopupDismissed() {
          }

          @Override public void onActionSelected(int action) {
            switch (action) {
              case R.id.action_watchlist_remove:
                listener.onRemoveItem(view, position);
                showScheduler.setIsInWatchlist(id, false);
            }
          }
        });
      } else {
        if (v == null) {
          v = LayoutInflater.from(context)
              .inflate(R.layout.list_row_watchlist_episode, parent, false);

          EpisodeViewHolder vh = new EpisodeViewHolder(v);
          v.setTag(vh);

          vh.overflow.addItem(R.id.action_watched, R.string.action_watched);
          vh.overflow.addItem(R.id.action_watchlist_remove, R.string.action_watchlist_remove);
        }

        EpisodeViewHolder vh = (EpisodeViewHolder) v.getTag();

        final long id = cursor.getLong(cursor.getColumnIndex(CathodeContract.Episodes._ID));
        final String posterUrl =
            cursor.getString(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.SCREEN));
        final String title =
            cursor.getString(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.TITLE));
        final long firstAired =
            cursor.getLong(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.FIRST_AIRED));
        final int season =
            cursor.getInt(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.SEASON));
        final int episode =
            cursor.getInt(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.EPISODE));

        vh.screen.setImage(posterUrl);
        vh.title.setText(title);
        vh.firstAired.setTimeInMillis(firstAired);
        vh.episode.setText(season + "x" + episode);
        final View view = v;
        vh.overflow.setListener(new OverflowView.OverflowActionListener() {
          @Override public void onPopupShown() {
          }

          @Override public void onPopupDismissed() {
          }

          @Override public void onActionSelected(int action) {

            switch (action) {
              case R.id.action_watched:
                episodeScheduler.setWatched(id, true);
                break;

              case R.id.action_watchlist_remove:
                listener.onRemoveItem(view, position);
                episodeScheduler.setIsInWatchlist(id, false);
                break;
            }
          }
        });
      }
    }

    return v;
  }

  static class ShowViewHolder {

    @InjectView(R.id.poster) RemoteImageView poster;
    @InjectView(R.id.indicator) IndicatorView indicator;
    @InjectView(R.id.title) TextView title;
    @InjectView(R.id.overview) TextView overview;
    @InjectView(R.id.overflow) OverflowView overflow;
    @InjectView(R.id.rating) CircularProgressIndicator rating;

    ShowViewHolder(View v) {
      ButterKnife.inject(this, v);
    }
  }

  static class EpisodeViewHolder {

    @InjectView(R.id.screen) RemoteImageView screen;
    @InjectView(R.id.title) TextView title;
    @InjectView(R.id.firstAired) TimeStamp firstAired;
    @InjectView(R.id.episode) TextView episode;
    @InjectView(R.id.overflow) OverflowView overflow;

    public EpisodeViewHolder(View v) {
      ButterKnife.inject(this, v);
    }
  }
}
