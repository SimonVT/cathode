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
package net.simonvt.cathode.ui.movies;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.widget.Toolbar;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.RecyclerView;
import dagger.android.support.AndroidSupportInjection;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.ui.adapter.RecyclerCursorAdapter;
import net.simonvt.cathode.common.ui.fragment.ToolbarSwipeRefreshRecyclerFragment;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.provider.database.SimpleCursor;
import net.simonvt.cathode.sync.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.ui.MoviesNavigationListener;

public abstract class MoviesFragment
    extends ToolbarSwipeRefreshRecyclerFragment<MoviesAdapter.ViewHolder>
    implements LoaderManager.LoaderCallbacks<SimpleCursor>, BaseMoviesAdapter.Callbacks {

  @Inject protected JobManager jobManager;

  @Inject MovieTaskScheduler movieScheduler;

  private MoviesNavigationListener navigationListener;

  private int columnCount;

  protected boolean scrollToTop;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    navigationListener = (MoviesNavigationListener) activity;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    AndroidSupportInjection.inject(this);

    getLoaderManager().initLoader(getLoaderId(), null, this);

    columnCount = getResources().getInteger(R.integer.movieColumns);
  }

  @Override public boolean displaysMenuIcon() {
    return amITopLevel();
  }

  @Override public void createMenu(Toolbar toolbar) {
    super.createMenu(toolbar);
    toolbar.inflateMenu(R.menu.fragment_movies);
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_search:
        navigationListener.onSearchClicked();
        return true;

      default:
        return super.onMenuItemClick(item);
    }
  }

  @Override protected int getColumnCount() {
    return columnCount;
  }

  @Override public void onMovieClicked(long movieId, String title, String overview) {
    navigationListener.onDisplayMovie(movieId, title, overview);
  }

  @Override public void onCheckin(long movieId) {
    movieScheduler.checkin(movieId, null, false, false, false);
  }

  @Override public void onCancelCheckin() {
    movieScheduler.cancelCheckin();
  }

  @Override public void onWatchlistAdd(long movieId) {
    movieScheduler.setIsInWatchlist(movieId, true);
  }

  @Override public void onWatchlistRemove(long movieId) {
    movieScheduler.setIsInWatchlist(movieId, false);
  }

  @Override public void onCollectionAdd(long movieId) {
    movieScheduler.setIsInCollection(movieId, true);
  }

  @Override public void onCollectionRemove(long movieId) {
    movieScheduler.setIsInCollection(movieId, false);
  }

  protected RecyclerView.Adapter<MoviesAdapter.ViewHolder> getAdapter(Cursor cursor) {
    return new MoviesAdapter(getActivity(), this, cursor);
  }

  void setCursor(Cursor cursor) {
    if (getAdapter() == null) {
      setAdapter(getAdapter(cursor));
    } else {
      ((RecyclerCursorAdapter) getAdapter()).changeCursor(cursor);
    }

    if (scrollToTop) {
      getRecyclerView().scrollToPosition(0);
      scrollToTop = false;
    }
  }

  protected abstract int getLoaderId();

  @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
    setCursor(data);
  }

  @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
  }
}
