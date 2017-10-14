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
package net.simonvt.cathode.ui.movie;

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
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.Injector;
import net.simonvt.cathode.common.ui.fragment.RefreshableAppBarFragment;
import net.simonvt.cathode.common.util.Ids;
import net.simonvt.cathode.common.util.guava.Preconditions;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.database.SimpleCursor;
import net.simonvt.cathode.provider.database.SimpleCursorLoader;
import net.simonvt.cathode.scheduler.MovieTaskScheduler;
import net.simonvt.schematic.Cursors;

public class MovieHistoryFragment extends RefreshableAppBarFragment {

  private static final String TAG = "net.simonvt.cathode.ui.show.MovieHistoryFragment";

  private static final String ARG_MOVIEID = TAG + ".movieId";
  private static final String ARG_MOVIETITLE = TAG + ".movieTitle";

  private static final int LOADER_MOVIE = 1;
  private static final int LOADER_HISTORY = 2;

  enum Type {
    LOADING, ERROR, EMPTY, ITEM
  }

  private Cursor movie;

  private MovieHistoryLoader.Result result;

  @Inject MovieTaskScheduler movieScheduler;

  private long movieId;
  private String movieTitle;

  @BindView(R.id.topTitle) TextView released;
  @BindView(R.id.topSubtitle) TextView rating;

  @BindView(R.id.watchedAtContainer) LinearLayout watchedAtContainer;

  public static String getTag(long movieId) {
    return TAG + "/" + movieId + "/history/" + Ids.newId();
  }

  public static Bundle getArgs(long movieId, String movieTitle) {
    Preconditions.checkArgument(movieId >= 0, "movieId must be >= 0, was " + movieId);

    Bundle args = new Bundle();
    args.putLong(ARG_MOVIEID, movieId);
    args.putString(ARG_MOVIETITLE, movieTitle);
    return args;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    Injector.inject(this);

    Bundle args = getArguments();
    movieId = args.getLong(ARG_MOVIEID);
    movieTitle = args.getString(ARG_MOVIETITLE);
    setTitle(movieTitle);

    getLoaderManager().initLoader(LOADER_MOVIE, null, movieCallbacks);
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

    movieScheduler.removeHistoryItem(movieId, historyId, result.getItems().size() == 0);
  }

  @Override public void onRefresh() {
    getLoaderManager().getLoader(LOADER_HISTORY).forceLoad();
  }

  private void setMovie(Cursor movie) {
    this.movie = movie;

    if (movie == null) {
      return;
    }

    if (!movie.moveToFirst()) {
      return;
    }

    movieTitle = Cursors.getString(movie, MovieColumns.TITLE);
    final String backdrop = Cursors.getString(movie, MovieColumns.BACKDROP);
    final String released = Cursors.getString(movie, MovieColumns.RELEASED);
    final String rating = Cursors.getString(movie, MovieColumns.RATING);

    setTitle(movieTitle);
    setBackdrop(backdrop);
    this.released.setText(released);
    this.rating.setText(rating);
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

  private void setResult(MovieHistoryLoader.Result result) {
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

  private static final String[] MOVIE_PROJECTION = new String[] {
      MovieColumns.TRAKT_ID, MovieColumns.TITLE, MovieColumns.BACKDROP, MovieColumns.RELEASED,
      MovieColumns.RATING,
  };

  private LoaderManager.LoaderCallbacks<SimpleCursor> movieCallbacks =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          return new SimpleCursorLoader(getActivity(), Movies.withId(movieId), MOVIE_PROJECTION,
              null, null, null);
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> cursorLoader, SimpleCursor data) {
          setMovie(data);
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> cursorLoader) {
        }
      };

  LoaderManager.LoaderCallbacks<MovieHistoryLoader.Result> historyCallbacks =
      new LoaderManager.LoaderCallbacks<MovieHistoryLoader.Result>() {
        @Override public Loader<MovieHistoryLoader.Result> onCreateLoader(int id, Bundle args) {
          return new MovieHistoryLoader(getContext(), movieId);
        }

        @Override public void onLoadFinished(Loader<MovieHistoryLoader.Result> loader,
            MovieHistoryLoader.Result result) {
          setResult(result);
          setRefreshing(false);
        }

        @Override public void onLoaderReset(Loader<MovieHistoryLoader.Result> loader) {
        }
      };
}
