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

import android.database.Cursor;
import android.support.v4.app.FragmentActivity;
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
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic;
import net.simonvt.cathode.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.ui.dialog.CheckInDialog;
import net.simonvt.cathode.ui.dialog.CheckInDialog.Type;
import net.simonvt.cathode.widget.OverflowView;
import net.simonvt.cathode.widget.OverflowView.OverflowActionListener;
import net.simonvt.cathode.widget.RemoteImageView;
import net.simonvt.cathode.widget.TimeStamp;

public class UpcomingAdapter extends BaseAdapter {

  private static final String COLUMN_EPISODE_ID = "episodeId";

  public static final String[] PROJECTION = new String[] {
      DatabaseSchematic.Tables.SHOWS + "." + ShowColumns.ID,
      DatabaseSchematic.Tables.SHOWS + "." + ShowColumns.TITLE,
      DatabaseSchematic.Tables.SHOWS + "." + ShowColumns.POSTER,
      DatabaseSchematic.Tables.SHOWS + "." + ShowColumns.STATUS,
      DatabaseSchematic.Tables.SHOWS + "." + ShowColumns.HIDDEN,
      ShowColumns.AIRED_COUNT,
      DatabaseSchematic.Tables.SHOWS + "." + ShowColumns.WATCHED_COUNT,
      ShowColumns.WATCHING, DatabaseSchematic.Tables.EPISODES
      + "."
      + EpisodeColumns.ID
      + " AS "
      + COLUMN_EPISODE_ID, DatabaseSchematic.Tables.EPISODES + "." + EpisodeColumns.TITLE,
      DatabaseSchematic.Tables.EPISODES + "." + EpisodeColumns.FIRST_AIRED,
      DatabaseSchematic.Tables.EPISODES + "." + EpisodeColumns.SEASON,
      DatabaseSchematic.Tables.EPISODES + "." + EpisodeColumns.EPISODE,
  };

  public interface OnRemoveListener {
    void onRemove(View view, int position);
  }

  private static final int NO_POSITION = -1;

  @Inject ShowTaskScheduler showScheduler;

  @Inject EpisodeTaskScheduler episodeScheduler;

  private OnRemoveListener listener;

  private FragmentActivity activity;

  private Cursor airedCursor;

  private Cursor unairedCursor;

  private int airedCursorCount = 0;

  private int airedHeaderPosition = NO_POSITION;

  private static final long AIRED_HEADER_ID = Long.MAX_VALUE;

  private int unairedCursorCount = 0;

  private int unairedHeaderPosition = NO_POSITION;

  private static final long UNAIRED_HEADER_ID = Long.MAX_VALUE - 1;

  public UpcomingAdapter(FragmentActivity activity, OnRemoveListener listener) {
    this.activity = activity;
    this.listener = listener;

    CathodeApp.inject(activity, this);
  }

  public void changeCursors(Cursor airedCursor, Cursor unairedCursor) {
    if (this.airedCursor != null && !this.airedCursor.isClosed()) {
      this.airedCursor.close();
    }

    this.airedCursor = airedCursor;

    if (this.unairedCursor != null && !this.unairedCursor.isClosed()) {
      this.unairedCursor.close();
    }

    this.unairedCursor = unairedCursor;

    notifyDataSetChanged();
  }

  @Override public void notifyDataSetChanged() {
    airedCursorCount = airedCursor != null ? airedCursor.getCount() : 0;
    unairedCursorCount = unairedCursor != null ? unairedCursor.getCount() : 0;
    if (airedCursorCount > 0) {
      airedHeaderPosition = 0;

      if (unairedCursorCount > 0) {
        unairedHeaderPosition = airedCursorCount + 1;
      } else {
        unairedHeaderPosition = NO_POSITION;
      }
    } else {
      airedHeaderPosition = NO_POSITION;

      if (unairedCursorCount > 0) {
        unairedHeaderPosition = 0;
      } else {
        unairedHeaderPosition = NO_POSITION;
      }
    }

    super.notifyDataSetChanged();
  }

  @Override public int getCount() {
    int count = 0;

    final int airedCount = airedCursor != null ? airedCursor.getCount() : 0;
    if (airedCount > 0) {
      count += airedCount + 1;
    }
    final int unairedCount = unairedCursor != null ? unairedCursor.getCount() : 0;
    if (unairedCount > 0) {
      count += unairedCount + 1;
    }

    return count;
  }

  @Override public Cursor getItem(int position) {
    if (position == airedHeaderPosition || position == unairedHeaderPosition) {
      return null;
    }

    if (airedCursorCount > 0) {
      if (position <= airedCursorCount) {
        airedCursor.moveToPosition(position - 1);
        return airedCursor;
      } else {
        unairedCursor.moveToPosition(position - 2 - airedCursorCount);
        return unairedCursor;
      }
    } else {
      unairedCursor.moveToPosition(position - 1);
      return unairedCursor;
    }
  }

  public int getCorrectedPosition(int position) {
    if (position == airedHeaderPosition || position == unairedHeaderPosition) {
      return -1;
    }

    int correctedPosition = 0;

    if (airedCursorCount > 0) {
      if (position <= airedCursorCount) {
        correctedPosition = position - 1;
      } else {
        unairedCursor.moveToPosition(position - 2 - airedCursorCount);
        correctedPosition = position - 2 - airedCursorCount;
      }
    } else {
      correctedPosition = position - 1;
    }

    return correctedPosition;
  }

  @Override public int getViewTypeCount() {
    return 2;
  }

  @Override public int getItemViewType(int position) {
    if (position == airedHeaderPosition || position == unairedHeaderPosition) {
      return 0;
    }

    return 1;
  }

  @Override public boolean hasStableIds() {
    return true;
  }

  @Override public long getItemId(int position) {
    if (position == airedHeaderPosition) {
      return AIRED_HEADER_ID;
    }
    if (position == unairedHeaderPosition) {
      return UNAIRED_HEADER_ID;
    }
    Cursor c = getItem(position);
    return c.getLong(c.getColumnIndex(ShowColumns.ID));
  }

  @Override public boolean areAllItemsEnabled() {
    return false;
  }

  @Override public boolean isEnabled(int position) {
    return position != airedHeaderPosition && position != unairedHeaderPosition;
  }

  @Override public View getView(final int position, View convertView, ViewGroup parent) {
    View v = convertView;

    if (position == airedHeaderPosition) {
      if (v == null) {
        v = LayoutInflater.from(activity).inflate(R.layout.list_row_upcoming_header, parent, false);
      }

      ((TextView) v).setText(R.string.header_aired);
    } else if (position == unairedHeaderPosition) {
      if (v == null) {
        v = LayoutInflater.from(activity).inflate(R.layout.list_row_upcoming_header, parent, false);
      }

      ((TextView) v).setText(R.string.header_upcoming);
    } else {
      Cursor cursor = getItem(position);

      if (v == null) {
        v = LayoutInflater.from(activity).inflate(R.layout.list_row_upcoming, parent, false);
        v.setTag(new ViewHolder(v));
      }

      ViewHolder vh = (ViewHolder) v.getTag();

      final long id = cursor.getLong(cursor.getColumnIndex(ShowColumns.ID));

      final String showPosterUrl =
          cursor.getString(cursor.getColumnIndex(ShowColumns.POSTER));
      final String showTitle = cursor.getString(cursor.getColumnIndex(ShowColumns.TITLE));
      final boolean isHidden =
          cursor.getInt(cursor.getColumnIndex(ShowColumns.HIDDEN)) == 1;
      final boolean watching =
          cursor.getInt(cursor.getColumnIndex(ShowColumns.WATCHING)) == 1;

      final int airedCount =
          cursor.getInt(cursor.getColumnIndex(ShowColumns.AIRED_COUNT));
      final int watchedCount =
          cursor.getInt(cursor.getColumnIndex(ShowColumns.WATCHED_COUNT));

      final long episodeId = cursor.getLong(cursor.getColumnIndex(COLUMN_EPISODE_ID));
      final String episodeTitle =
          cursor.getString(cursor.getColumnIndex(EpisodeColumns.TITLE));
      final long episodeFirstAired =
          cursor.getLong(cursor.getColumnIndex(EpisodeColumns.FIRST_AIRED));
      final int episodeSeasonNumber =
          cursor.getInt(cursor.getColumnIndex(EpisodeColumns.SEASON));
      final int episodeNumber =
          cursor.getInt(cursor.getColumnIndex(EpisodeColumns.EPISODE));

      vh.title.setText(showTitle);
      vh.poster.setImage(showPosterUrl);

      String episodeText;

      if (watching) {
        episodeText = activity.getString(R.string.show_watching);
      } else {
        episodeText =
            activity.getString(R.string.upcoming_episode_next, episodeSeasonNumber, episodeNumber,
                episodeTitle);
      }

      vh.firstAired.setVisibility(View.VISIBLE);
      vh.firstAired.setTimeInMillis(episodeFirstAired);

      vh.nextEpisode.setText(episodeText);
      vh.nextEpisode.setEnabled(episodeTitle != null);

      vh.checkIn.removeItems();
      if (watching) {
        vh.checkIn.setImageResource(R.drawable.ic_action_cancel_light);
        vh.checkIn.addItem(R.id.action_checkin_cancel, R.string.action_checkin_cancel);
      } else {
        vh.checkIn.setImageResource(R.drawable.ic_action_checkin_light);
        vh.checkIn.addItem(R.id.action_checkin, R.string.action_checkin);
        vh.checkIn.addItem(R.id.action_watched, R.string.action_watched);
      }
      final View view = v;
      vh.checkIn.setListener(new OverflowActionListener() {
        @Override public void onPopupShown() {
        }

        @Override public void onPopupDismissed() {
        }

        @Override public void onActionSelected(int action) {
          switch (action) {
            case R.id.action_checkin_cancel:
              showScheduler.cancelCheckin();
              break;

            case R.id.action_checkin:
              CheckInDialog.showDialogIfNecessary(activity, Type.SHOW, episodeTitle, episodeId);
              break;

            case R.id.action_watched:
              if (watchedCount + 1 >= airedCount) {
                listener.onRemove(view, position);
              }
              episodeScheduler.setWatched(episodeId, true);
              break;
          }
        }
      });
    }

    return v;
  }

  public static class ViewHolder {

    @InjectView(R.id.infoParent) View infoParent;
    @InjectView(R.id.title) TextView title;
    @InjectView(R.id.nextEpisode) TextView nextEpisode;
    @InjectView(R.id.firstAired) TimeStamp firstAired;
    @InjectView(R.id.check_in) OverflowView checkIn;
    @InjectView(R.id.poster) RemoteImageView poster;

    ViewHolder(View v) {
      ButterKnife.inject(this, v);
    }
  }
}
