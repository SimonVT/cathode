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

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.Injector;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.util.DateStringUtils;
import net.simonvt.cathode.common.util.Ids;
import net.simonvt.cathode.common.util.guava.Preconditions;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.ui.fragment.RefreshableAppBarFragment;
import net.simonvt.cathode.util.DataHelper;
import net.simonvt.schematic.Cursors;

public class EpisodeHistoryFragment extends RefreshableAppBarFragment {

  private static final String TAG = "net.simonvt.cathode.ui.show.EpisodeHistoryFragment";

  private static final String ARG_EPISODEID = TAG + ".episodeId";
  private static final String ARG_SHOW_TITLE = TAG + ".showTitle";

  private static final int LOADER_EPISODE = 1;
  private static final int LOADER_HISTORY = 2;

  enum Type {
    LOADING, ERROR, EMPTY, ITEM
  }

  private Cursor episode;

  private EpisodeHistoryLoader.Result result;

  @Inject EpisodeTaskScheduler episodeScheduler;

  private long episodeId;
  private String showTitle;

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
    Injector.obtain().inject(this);

    Bundle args = getArguments();
    episodeId = args.getLong(ARG_EPISODEID);
    showTitle = args.getString(ARG_SHOW_TITLE);
    setTitle(showTitle);

    getLoaderManager().initLoader(LOADER_EPISODE, null, episodeCallbacks);
    getLoaderManager().initLoader(LOADER_HISTORY, null, historyCallbacks);
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
    getLoaderManager().getLoader(LOADER_HISTORY).forceLoad();
  }

  private void setEpisode(Cursor episode) {
    this.episode = episode;

    if (episode == null) {
      return;
    }

    if (!episode.moveToFirst()) {
      return;
    }

    final int season = Cursors.getInt(episode, EpisodeColumns.SEASON);
    final int episodeNumber = Cursors.getInt(episode, EpisodeColumns.EPISODE);
    final boolean watched = Cursors.getBoolean(episode, EpisodeColumns.WATCHED);
    final String title =
        DataHelper.getEpisodeTitle(getContext(), episode, season, episodeNumber, watched);
    final String screenshot = Cursors.getString(episode, EpisodeColumns.SCREENSHOT);
    final String firstAiredString =
        DateStringUtils.getAirdateInterval(getActivity(), DataHelper.getFirstAired(episode), true);
    showTitle = Cursors.getString(episode, EpisodeColumns.SHOW_TITLE);

    setTitle(showTitle);
    this.title.setText(title);
    this.firstAired.setText(firstAiredString);
    setBackdrop(screenshot);
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

  private void setResult(EpisodeHistoryLoader.Result result) {
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
            TextView watchedAt = ButterKnife.findById(v, R.id.watchedAt);
            View remove = ButterKnife.findById(v, R.id.remove);

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
        TextView watchedAt = ButterKnife.findById(v, R.id.watchedAt);
        View remove = ButterKnife.findById(v, R.id.remove);

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

  private static final String[] EPISODE_PROJECTION = new String[] {
      EpisodeColumns.TRAKT_ID, EpisodeColumns.TITLE, EpisodeColumns.SEASON, EpisodeColumns.EPISODE,
      EpisodeColumns.SCREENSHOT, EpisodeColumns.WATCHED, EpisodeColumns.SHOW_TITLE,
  };

  private LoaderManager.LoaderCallbacks<SimpleCursor> episodeCallbacks =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          return new SimpleCursorLoader(getActivity(), Episodes.withId(episodeId),
              EPISODE_PROJECTION, null, null, null);
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> cursorLoader, SimpleCursor data) {
          setEpisode(data);
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> cursorLoader) {
        }
      };

  LoaderManager.LoaderCallbacks<EpisodeHistoryLoader.Result> historyCallbacks =
      new LoaderManager.LoaderCallbacks<EpisodeHistoryLoader.Result>() {
        @Override public Loader<EpisodeHistoryLoader.Result> onCreateLoader(int id, Bundle args) {
          return new EpisodeHistoryLoader(getContext(), episodeId);
        }

        @Override public void onLoadFinished(Loader<EpisodeHistoryLoader.Result> loader,
            EpisodeHistoryLoader.Result result) {
          setResult(result);
          setRefreshing(false);
        }

        @Override public void onLoaderReset(Loader<EpisodeHistoryLoader.Result> loader) {
        }
      };
}
