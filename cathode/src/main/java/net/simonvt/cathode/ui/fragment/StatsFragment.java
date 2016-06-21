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

package net.simonvt.cathode.ui.fragment;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.util.DateUtils;
import net.simonvt.schematic.Cursors;
import timber.log.Timber;

public class StatsFragment extends BaseFragment implements LoaderCallbacks<StatsFragment.Stats> {

  public static final String TAG = "net.simonvt.cathode.ui.fragment.StatsFragment";

  private static final int LOADER_STATS = 1;

  @BindView(R.id.stats_shows) View statsShows;
  @BindView(R.id.episodeTime) TextView episodeTime;
  @BindView(R.id.episodeCount) TextView episodeCount;
  @BindView(R.id.showCount) TextView showCount;

  @BindView(R.id.stats_movies) View statsMovies;
  @BindView(R.id.movieCount) TextView movieCount;
  @BindView(R.id.moviesTime) TextView movieTime;

  private Stats stats;

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTitle(R.string.navigation_stats);
    getLoaderManager().initLoader(LOADER_STATS, null, this);
  }

  @Override public boolean displaysMenuIcon() {
    return true;
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_stats, container, false);
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    updateStats();
  }

  @Override public Loader<Stats> onCreateLoader(int id, Bundle args) {
    return new StatsLoader(getContext());
  }

  private void updateStats() {
    if (stats != null && getView() != null) {
      statsShows.setVisibility(View.VISIBLE);
      episodeTime.setText(DateUtils.getStatsString(getContext(), stats.episodeTime));
      episodeCount.setText(getResources().getString(R.string.stats_episodes, stats.episodeCount));
      showCount.setText(getResources().getString(R.string.stats_shows, stats.showCount));

      statsMovies.setVisibility(View.VISIBLE);
      movieCount.setText(getResources().getString(R.string.stats_movies, stats.movieCount));
      movieTime.setText(DateUtils.getStatsString(getContext(), stats.moviesTime));
    }
  }

  @Override public void onLoadFinished(Loader<Stats> loader, Stats stats) {
    this.stats = stats;
    updateStats();
  }

  @Override public void onLoaderReset(Loader<Stats> loader) {
  }

  public static class Stats {

    long episodeTime;
    int episodeCount;
    int showCount;

    long moviesTime;
    int movieCount;

    public Stats(long episodeTime, int episodeCount, int showCount, long moviesTime,
        int movieCount) {
      this.episodeTime = episodeTime;
      this.episodeCount = episodeCount;
      this.showCount = showCount;
      this.moviesTime = moviesTime;
      this.movieCount = movieCount;
    }
  }

  public static class StatsLoader extends AsyncTaskLoader<Stats> {

    private final List<Uri> notificationUris = new ArrayList<>();
    private final Map<Uri, ContentObserver> observers = new HashMap<>();

    Stats stats;

    public StatsLoader(Context context) {
      super(context);

      registerUri(Shows.SHOWS);
      registerUri(Episodes.EPISODES);
      registerUri(Movies.MOVIES);
    }

    private void registerUri(Uri uri) {
      synchronized (notificationUris) {
        notificationUris.add(uri);

        ContentObserver observer = new ForceLoadContentObserver();
        observers.put(uri, observer);

        if (isStarted()) {
          getContext().getContentResolver().registerContentObserver(uri, true, observer);
        }
      }
    }

    @Override public Stats loadInBackground() {
      Cursor shows = getContext().getContentResolver().query(Shows.SHOWS_WATCHED, new String[] {
          ShowColumns.WATCHED_COUNT, ShowColumns.RUNTIME,
      }, null, null, null);

      final int showCount = shows.getCount();
      int episodeCount = 0;
      long episodeTime = 0L;
      while (shows.moveToNext()) {
        final int watchedCount = Cursors.getInt(shows, ShowColumns.WATCHED_COUNT);
        final int runtime = Cursors.getInt(shows, ShowColumns.RUNTIME);

        episodeCount += watchedCount;
        episodeTime += (watchedCount * runtime);
      }

      shows.close();

      Cursor movies = getContext().getContentResolver().query(Movies.MOVIES_WATCHED, new String[] {
          MovieColumns.RUNTIME,
      }, null, null, null);

      final int movieCount = movies.getCount();
      long moviesTime = 0L;

      while (movies.moveToNext()) {
        final int runtime = Cursors.getInt(movies, MovieColumns.RUNTIME);
        moviesTime += runtime;
      }

      movies.close();

      return new Stats(episodeTime, episodeCount, showCount, moviesTime, movieCount);
    }

    @Override public void deliverResult(Stats stats) {
      if (isStarted()) {
        super.deliverResult(stats);
      }
    }

    /**
     * Handles a request to start the Loader.
     */
    @Override protected void onStartLoading() {
      if (stats != null) {
        deliverResult(stats);
      }

      synchronized (notificationUris) {
        for (Uri uri : notificationUris) {
          ContentObserver observer = observers.get(uri);
          getContext().getContentResolver().registerContentObserver(uri, true, observer);
          Timber.d("Registering observer");
        }
      }

      forceLoad();
    }

    @Override protected void onStopLoading() {
      cancelLoad();

      for (ContentObserver observer : observers.values()) {
        getContext().getContentResolver().unregisterContentObserver(observer);
        Timber.d("unregistering observer");
      }
    }

    @Override protected void onReset() {
      super.onReset();
      onStopLoading();
    }
  }
}
