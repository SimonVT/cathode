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
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.LastModifiedColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.ui.dialog.CheckInDialog;
import net.simonvt.cathode.ui.dialog.CheckInDialog.Type;
import net.simonvt.cathode.widget.OverflowView;
import net.simonvt.cathode.widget.OverflowView.OverflowActionListener;
import net.simonvt.cathode.widget.RemoteImageView;
import net.simonvt.cathode.widget.TimeStamp;

public class UpcomingAdapter extends HeaderCursorAdapter<RecyclerView.ViewHolder> {

  public interface OnItemClickListener {

    void onShowClicked(View v, int position, long id);
  }

  private static final String COLUMN_EPISODE_ID = "episodeId";
  private static final String COLUMN_EPISODE_LAST_UPDATED = "episodeLastUpdated";

  public static final String[] PROJECTION = new String[] {
      Tables.SHOWS + "." + ShowColumns.ID, Tables.SHOWS + "." + ShowColumns.TITLE,
      Tables.SHOWS + "." + ShowColumns.POSTER, Tables.SHOWS + "." + ShowColumns.STATUS,
      Tables.SHOWS + "." + ShowColumns.HIDDEN, ShowColumns.AIRED_COUNT,
      Tables.SHOWS + "." + ShowColumns.WATCHED_COUNT, ShowColumns.WATCHING,
      Tables.SHOWS + "." + ShowColumns.LAST_MODIFIED,
      Tables.EPISODES + "." + EpisodeColumns.ID + " AS " + COLUMN_EPISODE_ID,
      Tables.EPISODES + "." + EpisodeColumns.TITLE,
      Tables.EPISODES + "." + EpisodeColumns.FIRST_AIRED,
      Tables.EPISODES + "." + EpisodeColumns.SEASON, Tables.EPISODES + "." + EpisodeColumns.EPISODE,
      Tables.EPISODES + "." + EpisodeColumns.LAST_MODIFIED + " AS " + COLUMN_EPISODE_LAST_UPDATED,
  };

  private static final int TYPE_ITEM = 0;

  public interface OnRemoveListener {
    void onRemove(View view, int position, long id);
  }

  @Inject ShowTaskScheduler showScheduler;

  @Inject EpisodeTaskScheduler episodeScheduler;

  private OnRemoveListener onRemoveListener;

  private FragmentActivity activity;

  private OnItemClickListener onItemClickListener;

  public UpcomingAdapter(FragmentActivity activity, OnItemClickListener onItemClickListener,
      OnRemoveListener onRemoveListener) {
    super();
    this.activity = activity;
    this.onItemClickListener = onItemClickListener;
    this.onRemoveListener = onRemoveListener;

    CathodeApp.inject(activity, this);

    setHasStableIds(true);
  }

  @Override public long getLastModified(int position) {
    if (!isHeader(position)) {
      Cursor cursor = getCursor(position);

      final long showLastModified =
          cursor.getLong(cursor.getColumnIndexOrThrow(LastModifiedColumns.LAST_MODIFIED));
      final long episodeLastModified =
          cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_EPISODE_LAST_UPDATED));

      return showLastModified + episodeLastModified;
    }

    return super.getLastModified(position);
  }

  @Override protected RecyclerView.ViewHolder onCreateItemHolder(ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(activity).inflate(R.layout.list_row_upcoming, parent, false);
    final ItemViewHolder holder = new ItemViewHolder(v);
    holder.itemView.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        onItemClickListener.onShowClicked(holder.itemView, holder.getPosition(),
            holder.getItemId());
      }
    });
    return holder;
  }

  @Override protected RecyclerView.ViewHolder onCreateHeaderHolder(ViewGroup parent) {
    View v =
        LayoutInflater.from(activity).inflate(R.layout.list_row_upcoming_header, parent, false);
    return new HeaderViewHolder((TextView) v);
  }

  @Override protected void onBindHeader(RecyclerView.ViewHolder holder, int headerRes) {
    HeaderViewHolder vh = (HeaderViewHolder) holder;
    vh.header.setText(headerRes);
  }

  @Override protected void onBindViewHolder(final RecyclerView.ViewHolder holder, Cursor cursor,
      final int position) {
    final ItemViewHolder vh = (ItemViewHolder) holder;

    final long id = cursor.getLong(cursor.getColumnIndex(ShowColumns.ID));

    final String showPosterUrl = cursor.getString(cursor.getColumnIndex(ShowColumns.POSTER));
    final String showTitle = cursor.getString(cursor.getColumnIndex(ShowColumns.TITLE));
    final boolean isHidden = cursor.getInt(cursor.getColumnIndex(ShowColumns.HIDDEN)) == 1;
    final boolean watching = cursor.getInt(cursor.getColumnIndex(ShowColumns.WATCHING)) == 1;

    final int airedCount = cursor.getInt(cursor.getColumnIndex(ShowColumns.AIRED_COUNT));
    final int watchedCount = cursor.getInt(cursor.getColumnIndex(ShowColumns.WATCHED_COUNT));

    final long episodeId = cursor.getLong(cursor.getColumnIndex(COLUMN_EPISODE_ID));
    final String episodeTitle = cursor.getString(cursor.getColumnIndex(EpisodeColumns.TITLE));
    final long episodeFirstAired =
        cursor.getLong(cursor.getColumnIndex(EpisodeColumns.FIRST_AIRED));
    final int episodeSeasonNumber = cursor.getInt(cursor.getColumnIndex(EpisodeColumns.SEASON));
    final int episodeNumber = cursor.getInt(cursor.getColumnIndex(EpisodeColumns.EPISODE));

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
      vh.checkIn.setImageResource(R.drawable.ic_checkin_cancel);
      vh.checkIn.addItem(R.id.action_checkin_cancel, R.string.action_checkin_cancel);
    } else {
      vh.checkIn.setImageResource(R.drawable.ic_checkin);
      vh.checkIn.addItem(R.id.action_checkin, R.string.action_checkin);
      vh.checkIn.addItem(R.id.action_watched, R.string.action_watched);
    }
    vh.checkIn.setListener(new OverflowActionListener() {

      @Override public void onPopupShown() {
        holder.setIsRecyclable(false);
      }

      @Override public void onPopupDismissed() {
        holder.setIsRecyclable(false);
      }

      @Override public void onActionSelected(int action) {
        holder.setIsRecyclable(true);

        switch (action) {
          case R.id.action_checkin_cancel:
            showScheduler.cancelCheckin();
            break;

          case R.id.action_checkin:
            CheckInDialog.showDialogIfNecessary(activity, Type.SHOW, episodeTitle, episodeId);
            break;

          case R.id.action_watched:
            if (watchedCount + 1 >= airedCount) {
              onRemoveListener.onRemove(vh.itemView, position, id);
            }
            episodeScheduler.setWatched(episodeId, true);
            break;
        }
      }
    });
  }

  @Override protected int getItemViewType(int headerRes, Cursor cursor) {
    return TYPE_ITEM;
  }

  public static class HeaderViewHolder extends RecyclerView.ViewHolder {

    TextView header;

    public HeaderViewHolder(TextView header) {
      super(header);
      this.header = header;
    }
  }

  public static class ItemViewHolder extends RecyclerView.ViewHolder {

    @InjectView(R.id.infoParent) View infoParent;
    @InjectView(R.id.title) TextView title;
    @InjectView(R.id.nextEpisode) TextView nextEpisode;
    @InjectView(R.id.firstAired) TimeStamp firstAired;
    @InjectView(R.id.check_in) OverflowView checkIn;
    @InjectView(R.id.poster) RemoteImageView poster;

    ItemViewHolder(View v) {
      super(v);
      ButterKnife.inject(this, v);
    }
  }
}
