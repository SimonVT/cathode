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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import dagger.android.support.AndroidSupportInjection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.common.ui.fragment.RefreshableAppBarFragment;
import net.simonvt.cathode.common.util.Ids;
import net.simonvt.cathode.common.util.guava.Preconditions;
import net.simonvt.cathode.entity.Movie;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper;
import net.simonvt.cathode.sync.scheduler.MovieTaskScheduler;

public class MovieHistoryFragment extends RefreshableAppBarFragment {

  private static final String TAG = "net.simonvt.cathode.ui.show.MovieHistoryFragment";

  private static final String ARG_MOVIEID = TAG + ".movieId";
  private static final String ARG_MOVIETITLE = TAG + ".movieTitle";

  enum Type {
    LOADING, ERROR, EMPTY, ITEM
  }

  private Movie movie;

  private MovieHistoryLiveData.Result result;

  @Inject MovieTaskScheduler movieScheduler;
  @Inject SyncService syncService;
  @Inject MovieDatabaseHelper movieHelper;

  private long movieId;
  private String movieTitle;

  @Inject MovieHistoryViewModelFactory viewModelFactory;
  private MovieHistoryViewModel viewModel;

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

  @Override public void onCreate(@Nullable Bundle inState) {
    super.onCreate(inState);
    AndroidSupportInjection.inject(this);

    Bundle args = getArguments();
    movieId = args.getLong(ARG_MOVIEID);
    movieTitle = args.getString(ARG_MOVIETITLE);
    setTitle(movieTitle);

    viewModel = ViewModelProviders.of(this, viewModelFactory).get(MovieHistoryViewModel.class);
    viewModel.setMovieId(movieId);
    viewModel.getMovie().observe(this, new Observer<Movie>() {
      @Override public void onChanged(Movie movie) {
        setMovie(movie);
      }
    });
    viewModel.getHistory().observe(this, new Observer<MovieHistoryLiveData.Result>() {
      @Override public void onChanged(MovieHistoryLiveData.Result result) {
        setResult(result);
        setRefreshing(false);
      }
    });
  }

  @Override
  protected View createView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle inState) {
    return inflater.inflate(R.layout.history_fragment, container, false);
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle inState) {
    super.onViewCreated(view, inState);
    setResult(result);
  }

  public void onRemoveHistoryItem(long historyId, int position) {
    result.getItems().remove(position);
    setResult(result);

    movieScheduler.removeHistoryItem(movieId, historyId, result.getItems().size() == 0);
  }

  @Override public void onRefresh() {
    viewModel.getHistory().loadData();
  }

  private void setMovie(Movie movie) {
    this.movie = movie;
    movieTitle = movie.getTitle();

    final String backdropUri = ImageUri.create(ImageUri.ITEM_MOVIE, ImageType.BACKDROP, movieId);
    setTitle(movieTitle);
    setBackdrop(backdropUri);
    this.released.setText(movie.getReleased());
    this.rating.setText(String.format(Locale.getDefault(), "%.1f", movie.getRating()));
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

  private void setResult(MovieHistoryLiveData.Result result) {
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
