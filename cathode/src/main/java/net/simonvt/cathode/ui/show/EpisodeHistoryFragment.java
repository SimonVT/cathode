/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import dagger.android.support.AndroidSupportInjection;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.common.entity.Episode;
import net.simonvt.cathode.common.ui.fragment.RefreshableAppBarFragment;
import net.simonvt.cathode.common.util.DateStringUtils;
import net.simonvt.cathode.common.util.Ids;
import net.simonvt.cathode.common.util.guava.Preconditions;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper;
import net.simonvt.cathode.provider.util.DataHelper;
import net.simonvt.cathode.sync.scheduler.EpisodeTaskScheduler;

public class EpisodeHistoryFragment extends RefreshableAppBarFragment {

  private static final String TAG = "net.simonvt.cathode.ui.show.EpisodeHistoryFragment";

  private static final String ARG_EPISODEID = TAG + ".episodeId";
  private static final String ARG_SHOW_TITLE = TAG + ".showTitle";

  enum Type {
    LOADING, ERROR, EMPTY, ITEM
  }

  static final String[] EPISODE_PROJECTION = new String[] {
      EpisodeColumns.TRAKT_ID, EpisodeColumns.TITLE, EpisodeColumns.SEASON, EpisodeColumns.EPISODE,
      EpisodeColumns.SCREENSHOT, EpisodeColumns.WATCHED, EpisodeColumns.SHOW_TITLE,
      EpisodeColumns.FIRST_AIRED,
  };

  private Episode episode;

  private EpisodeHistoryLiveData.Result result;

  @Inject EpisodeTaskScheduler episodeScheduler;
  @Inject SyncService syncService;
  @Inject EpisodeDatabaseHelper episodeHelper;

  private long episodeId;
  private String showTitle;

  @Inject EpisodeHistoryViewModelFactory viewModelFactory;
  private EpisodeHistoryViewModel viewModel;

  @BindView(R.id.topTitle) TextView title;
  @BindView(R.id.topSubtitle) TextView firstAired;

  @BindView(R.id.watchedAtContainer) LinearLayout watchedAtContainer;

  public static String getTag(long episodeId) {
    return TAG + "/" + episodeId + "/history/" + Ids.newId();
  }

  public static Bundle getArgs(long episodeId, String showTitle) {
    Preconditions.checkArgument(episodeId >= 0, "episodeId must be >= 0, was " + episodeId);

    Bundle args = new Bundle();
    args.putLong(ARG_EPISODEID, episodeId);
    args.putString(ARG_SHOW_TITLE, showTitle);
    return args;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    AndroidSupportInjection.inject(this);

    Bundle args = getArguments();
    episodeId = args.getLong(ARG_EPISODEID);
    showTitle = args.getString(ARG_SHOW_TITLE);
    setTitle(showTitle);

    viewModel = ViewModelProviders.of(this, viewModelFactory).get(EpisodeHistoryViewModel.class);
    viewModel.getEpisode().observe(this, new Observer<Episode>() {
      @Override public void onChanged(Episode episode) {
        setEpisode(episode);
      }
    });
    viewModel.getHistory().observe(this, new Observer<EpisodeHistoryLiveData.Result>() {
      @Override public void onChanged(EpisodeHistoryLiveData.Result result) {
        setResult(result);
        setRefreshing(false);
      }
    });
  }

  @Override
  protected View createView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.history_fragment, container, false);
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    setResult(result);
  }

  public void onRemoveHistoryItem(long historyId, int position) {
    result.getItems().remove(position);
    setResult(result);

    episodeScheduler.removeHistoryItem(episodeId, historyId, result.getItems().size() == 0);
  }

  @Override public void onRefresh() {
    viewModel.getHistory().loadData();
  }

  private void setEpisode(Episode episode) {
    this.episode = episode;

    final String title =
        DataHelper.getEpisodeTitle(getContext(), episode.getTitle(), episode.getSeason(),
            episode.getEpisode(), episode.getWatched());

    final String firstAiredString =
        DateStringUtils.getAirdateInterval(getActivity(), episode.getFirstAired(), true);
    this.firstAired.setText(firstAiredString);

    showTitle = episode.getShowTitle();

    setTitle(showTitle);
    this.title.setText(title);
    final String screenshotUri = ImageUri.create(ImageUri.ITEM_EPISODE, ImageType.STILL, episodeId);
    setBackdrop(screenshotUri);
  }

  private boolean typeOrClear(ViewGroup parent, Type type) {
    if (parent.getChildCount() > 0) {
      View firstChild = parent.getChildAt(0);
      final Type childType = (Type) firstChild.getTag();
      if (childType == type) {
        return true;
      } else {
        parent.removeAllViews();
      }
    }

    return false;
  }

  private void setResult(EpisodeHistoryLiveData.Result result) {
    this.result = result;

    if (getView() == null) {
      return;
    }

    if (result == null) {
      if (typeOrClear(watchedAtContainer, Type.LOADING)) {
        return;
      }

      View v = LayoutInflater.from(watchedAtContainer.getContext())
          .inflate(R.layout.history_progress, watchedAtContainer, false);
      v.setTag(Type.LOADING);
      watchedAtContainer.addView(v);
    } else if (!result.isSuccessful()) {
      if (typeOrClear(watchedAtContainer, Type.ERROR)) {
        return;
      }

      View v = LayoutInflater.from(watchedAtContainer.getContext())
          .inflate(R.layout.history_error, watchedAtContainer, false);
      v.setTag(Type.ERROR);
      watchedAtContainer.addView(v);
    } else if (result.getItems().size() == 0) {

      if (typeOrClear(watchedAtContainer, Type.EMPTY)) {
        return;
      }

      View v = LayoutInflater.from(watchedAtContainer.getContext())
          .inflate(R.layout.history_empty, watchedAtContainer, false);
      v.setTag(Type.ERROR);
      watchedAtContainer.addView(v);
    } else {
      List<HistoryItem> items = result.getItems();
      List<Long> ids = new ArrayList<>();
      for (HistoryItem item : items) {
        ids.add(item.historyId);
      }

      for (int i = watchedAtContainer.getChildCount() - 1; i >= 0; i--) {
        View v = watchedAtContainer.getChildAt(i);
        if (v.getTag() != Type.ITEM) {
          watchedAtContainer.removeViewAt(i);
          continue;
        }

        final long id = (long) v.getTag(R.id.historyId);
        if (!ids.contains(id)) {
          watchedAtContainer.removeViewAt(i);
        }
      }

      for (int i = 0, size = items.size(); i < size; i++) {
        final HistoryItem item = items.get(i);
        final int position = i;

        View v = null;

        if (i < watchedAtContainer.getChildCount()) {
          v = watchedAtContainer.getChildAt(i);
        }

        if (v != null) {
          final long id = (long) v.getTag(R.id.historyId);

          if (item.historyId == id) {
            TextView watchedAt = v.findViewById(R.id.watchedAt);
            View remove = v.findViewById(R.id.remove);

            watchedAt.setText(item.watchedAt);
            remove.setOnClickListener(new View.OnClickListener() {
              @Override public void onClick(View v) {
                onRemoveHistoryItem(item.historyId, position);
              }
            });

            continue;
          }
        }

        v = LayoutInflater.from(watchedAtContainer.getContext())
            .inflate(R.layout.history_row, watchedAtContainer, false);
        TextView watchedAt = v.findViewById(R.id.watchedAt);
        View remove = v.findViewById(R.id.remove);

        v.setTag(Type.ITEM);
        v.setTag(R.id.historyId, item.historyId);
        watchedAt.setText(item.watchedAt);
        remove.setOnClickListener(new View.OnClickListener() {
          @Override public void onClick(View v) {
            onRemoveHistoryItem(item.historyId, position);
          }
        });

        watchedAtContainer.addView(v, i);
      }
    }
  }
}
