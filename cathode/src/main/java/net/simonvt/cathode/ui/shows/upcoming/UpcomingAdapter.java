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
package net.simonvt.cathode.ui.shows.upcoming;

import android.database.Cursor;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.ui.adapter.HeaderCursorAdapter;
import net.simonvt.cathode.common.widget.OverflowView.OverflowActionListener;
import net.simonvt.cathode.common.widget.RemoteImageView;
import net.simonvt.cathode.common.widget.TimeStamp;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.LastModifiedColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.util.DataHelper;
import net.simonvt.cathode.ui.dialog.CheckInDialog;
import net.simonvt.cathode.ui.dialog.CheckInDialog.Type;
import net.simonvt.cathode.ui.history.AddToHistoryDialog;
import net.simonvt.cathode.widget.CheckInView;
import net.simonvt.schematic.Cursors;

public class UpcomingAdapter extends HeaderCursorAdapter<RecyclerView.ViewHolder> {

  public interface Callbacks {

    void onEpisodeClicked(long episodeId, String showTitle);

    void onCheckin(long episodeId);

    void onCancelCheckin();
  }

  private static final String COLUMN_EPISODE_ID = "episodeId";
  private static final String COLUMN_EPISODE_LAST_UPDATED = "episodeLastUpdated";

  public static final String[] PROJECTION = new String[] {
      Tables.SHOWS + "." + ShowColumns.ID,
      Tables.SHOWS + "." + ShowColumns.TITLE,
      Tables.SHOWS + "." + ShowColumns.STATUS,
      ShowColumns.AIRED_COUNT,
      Tables.SHOWS + "." + ShowColumns.WATCHED_COUNT,
      ShowColumns.WATCHING,
      Tables.SHOWS + "." + ShowColumns.LAST_MODIFIED,
      ShowColumns.WATCHING_EPISODE_ID,
      Tables.EPISODES + "." + EpisodeColumns.ID + " AS " + COLUMN_EPISODE_ID,
      Tables.EPISODES + "." + EpisodeColumns.TITLE,
      Tables.EPISODES + "." + EpisodeColumns.WATCHED,
      Tables.EPISODES + "." + EpisodeColumns.FIRST_AIRED,
      Tables.EPISODES + "." + EpisodeColumns.SEASON,
      Tables.EPISODES + "." + EpisodeColumns.EPISODE,
      Tables.EPISODES + "." + EpisodeColumns.LAST_MODIFIED + " AS " + COLUMN_EPISODE_LAST_UPDATED,
  };

  private static final int TYPE_ITEM = 0;

  public interface OnRemoveListener {
    void onRemove(long showId);
  }

  private OnRemoveListener onRemoveListener;

  private FragmentActivity activity;

  private Callbacks callbacks;

  public UpcomingAdapter(FragmentActivity activity, Callbacks callbacks,
      OnRemoveListener onRemoveListener) {
    this.activity = activity;
    this.callbacks = callbacks;
    this.onRemoveListener = onRemoveListener;

    setHasStableIds(true);
  }

  @Override public long getLastModified(int position) {
    if (!isHeader(position)) {
      Cursor cursor = getCursor(position);

      final long showLastModified = Cursors.getLong(cursor, LastModifiedColumns.LAST_MODIFIED);
      final long episodeLastModified = Cursors.getLong(cursor, COLUMN_EPISODE_LAST_UPDATED);

      return showLastModified + episodeLastModified;
    }

    return super.getLastModified(position);
  }

  @Override protected RecyclerView.ViewHolder onCreateItemHolder(ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(activity).inflate(R.layout.list_row_upcoming, parent, false);
    final ItemViewHolder holder = new ItemViewHolder(v);
    holder.itemView.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        final int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
          Cursor cursor = getCursor(position);
          final String showTitle = Cursors.getString(cursor, ShowColumns.TITLE);

          if (holder.watching) {
            callbacks.onEpisodeClicked(holder.watchingId, showTitle);
          } else {
            final long episodeId = Cursors.getLong(cursor, COLUMN_EPISODE_ID);
            callbacks.onEpisodeClicked(episodeId, showTitle);
          }
        }
      }
    });
    return holder;
  }

  @Override protected RecyclerView.ViewHolder onCreateHeaderHolder(ViewGroup parent) {
    View v =
        LayoutInflater.from(activity).inflate(R.layout.list_row_upcoming_header, parent, false);
    return new HeaderViewHolder((TextView) v);
  }

  @Override public void onViewRecycled(RecyclerView.ViewHolder holder) {
    if (holder instanceof ItemViewHolder) {
      ItemViewHolder itemHolder = (ItemViewHolder) holder;
      itemHolder.checkIn.dismiss();
      itemHolder.checkIn.reset();

      itemHolder.watching = false;
      itemHolder.watchingId = null;
    }
  }

  @Override protected void onBindHeader(RecyclerView.ViewHolder holder, int headerRes) {
    HeaderViewHolder vh = (HeaderViewHolder) holder;
    vh.header.setText(headerRes);
  }

  @Override protected void onBindViewHolder(final RecyclerView.ViewHolder holder, Cursor cursor,
      int position) {
    final ItemViewHolder vh = (ItemViewHolder) holder;

    final long id = Cursors.getLong(cursor, ShowColumns.ID);

    final String showTitle = Cursors.getString(cursor, ShowColumns.TITLE);
    final boolean watching = Cursors.getBoolean(cursor, ShowColumns.WATCHING);

    final int airedCount = Cursors.getInt(cursor, ShowColumns.AIRED_COUNT);
    final int watchedCount = Cursors.getInt(cursor, ShowColumns.WATCHED_COUNT);

    final long episodeId = Cursors.getLong(cursor, COLUMN_EPISODE_ID);
    final long episodeFirstAired = DataHelper.getFirstAired(cursor);
    final int episodeSeasonNumber = Cursors.getInt(cursor, EpisodeColumns.SEASON);
    final int episodeNumber = Cursors.getInt(cursor, EpisodeColumns.EPISODE);
    final boolean watched = Cursors.getBoolean(cursor, EpisodeColumns.WATCHED);
    final String episodeTitle =
        DataHelper.getEpisodeTitle(activity, cursor, episodeSeasonNumber, episodeNumber, watched,
            true);
    final Long watchingEpisodeId = Cursors.getLongOrNull(cursor, ShowColumns.WATCHING_EPISODE_ID);

    final String showPosterUri = ImageUri.create(ImageUri.ITEM_SHOW, ImageType.POSTER, id);

    vh.title.setText(showTitle);
    vh.poster.setImage(showPosterUri);

    vh.watching = watching;
    vh.watchingId = watchingEpisodeId;

    if (watching) {
      vh.nextEpisode.setText(R.string.show_watching);
    } else {
      vh.nextEpisode.setText(episodeTitle);
    }

    vh.firstAired.setVisibility(View.VISIBLE);
    vh.firstAired.setTimeInMillis(episodeFirstAired);

    vh.checkIn.setWatching(watching);
    vh.checkIn.setId(id);
    vh.checkIn.setListener(new OverflowActionListener() {
      @Override public void onPopupShown() {
      }

      @Override public void onPopupDismissed() {
      }

      @Override public void onActionSelected(int action) {
        final int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
          switch (action) {
            case R.id.action_checkin_cancel:
              callbacks.onCancelCheckin();
              vh.checkIn.setWatching(false);
              break;

            case R.id.action_checkin:
              if (!CheckInDialog.showDialogIfNecessary(activity, Type.SHOW, episodeTitle,
                  episodeId)) {
                callbacks.onCheckin(episodeId);
                vh.checkIn.setWatching(true);
              }
              break;

            case R.id.action_history_add:
              if (watchedCount + 1 >= airedCount) {
                onRemoveListener.onRemove(id);
              }

              AddToHistoryDialog.newInstance(AddToHistoryDialog.Type.EPISODE, episodeId,
                  episodeTitle).show(activity.getSupportFragmentManager(), AddToHistoryDialog.TAG);
              break;
          }
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

    @BindView(R.id.title) TextView title;
    @BindView(R.id.nextEpisode) TextView nextEpisode;
    @BindView(R.id.firstAired) TimeStamp firstAired;
    @BindView(R.id.check_in) CheckInView checkIn;
    @BindView(R.id.poster) RemoteImageView poster;

    boolean watching;
    Long watchingId;

    ItemViewHolder(View v) {
      super(v);
      ButterKnife.bind(this, v);
    }
  }
}
