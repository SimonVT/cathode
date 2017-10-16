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
package net.simonvt.cathode.ui.show;

import android.content.res.Resources;
import android.database.Cursor;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.Injector;
import net.simonvt.cathode.common.ui.adapter.RecyclerCursorAdapter;
import net.simonvt.cathode.common.widget.RemoteImageView;
import net.simonvt.cathode.common.widget.TimeStamp;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.LastModifiedColumns;
import net.simonvt.cathode.provider.util.DataHelper;
import net.simonvt.cathode.sync.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.sync.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.history.AddToHistoryDialog;
import net.simonvt.cathode.ui.history.RemoveFromHistoryDialog;
import net.simonvt.cathode.ui.listener.EpisodeClickListener;
import net.simonvt.schematic.Cursors;

public class SeasonAdapter extends RecyclerCursorAdapter<SeasonAdapter.ViewHolder> {

  public static final String[] PROJECTION = {
      EpisodeColumns.ID, EpisodeColumns.TITLE, EpisodeColumns.SEASON, EpisodeColumns.EPISODE,
      EpisodeColumns.WATCHED, EpisodeColumns.IN_COLLECTION, EpisodeColumns.FIRST_AIRED,
      EpisodeColumns.SHOW_TITLE, LastModifiedColumns.LAST_MODIFIED,
  };

  @Inject ShowTaskScheduler showScheduler;
  @Inject EpisodeTaskScheduler episodeScheduler;

  private FragmentActivity activity;
  private Resources resources;

  private LibraryType type;

  private EpisodeClickListener clickListener;

  public SeasonAdapter(FragmentActivity activity, EpisodeClickListener clickListener, Cursor cursor,
      LibraryType type) {
    super(activity, cursor);
    Injector.inject(this);
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
        final int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
          clickListener.onEpisodeClick(holder.getItemId());
        }
      }
    });

    holder.number.setOnClickListener(new OnClickListener() {
      @Override public void onClick(View v) {
        if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
          final boolean activated = holder.number.isActivated();
          if (type == LibraryType.COLLECTION) {
            holder.number.setActivated(!activated);
            episodeScheduler.setIsInCollection(holder.getItemId(), !activated);
          } else {
            if (activated) {
              RemoveFromHistoryDialog.newInstance(RemoveFromHistoryDialog.Type.EPISODE,
                  holder.getItemId(), holder.episodeTitle, holder.showTitle)
                  .show(activity.getSupportFragmentManager(), RemoveFromHistoryDialog.TAG);
            } else {
              AddToHistoryDialog.newInstance(AddToHistoryDialog.Type.EPISODE, holder.getItemId(),
                  holder.episodeTitle)
                  .show(activity.getSupportFragmentManager(), AddToHistoryDialog.TAG);
            }
          }
        }
      }
    });

    return holder;
  }

  @Override protected void onBindViewHolder(ViewHolder holder, Cursor cursor, int position) {
    final long id = Cursors.getLong(cursor, EpisodeColumns.ID);
    final int season = Cursors.getInt(cursor, EpisodeColumns.SEASON);
    final int episode = Cursors.getInt(cursor, EpisodeColumns.EPISODE);
    final boolean watched = Cursors.getBoolean(cursor, EpisodeColumns.WATCHED);
    final boolean inCollection = Cursors.getBoolean(cursor, EpisodeColumns.IN_COLLECTION);
    final long firstAired = DataHelper.getFirstAired(cursor);
    final String title = DataHelper.getEpisodeTitle(getContext(), cursor, season, episode, watched);
    final String showTitle = Cursors.getString(cursor, EpisodeColumns.SHOW_TITLE);
    final String screenshotUri = ImageUri.create(ImageUri.ITEM_EPISODE, ImageType.STILL, id);

    holder.episodeTitle = title;
    holder.showTitle = showTitle;

    holder.screen.setImage(screenshotUri);

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

    @BindView(R.id.screen) RemoteImageView screen;

    @BindView(R.id.title) TextView title;
    @BindView(R.id.firstAired) TimeStamp firstAired;
    @BindView(R.id.number) TextView number;

    String episodeTitle;
    String showTitle;

    ViewHolder(View v) {
      super(v);
      ButterKnife.bind(this, v);
    }
  }
}
