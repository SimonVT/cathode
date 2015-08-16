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
package net.simonvt.cathode.ui.fragment;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.MenuItem;
import android.view.View;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.remote.sync.SyncJob;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.Loaders;
import net.simonvt.cathode.ui.ShowsNavigationListener;
import net.simonvt.cathode.ui.adapter.HeaderSpanLookup;
import net.simonvt.cathode.ui.adapter.ShowSuggestionAdapter;
import net.simonvt.cathode.ui.adapter.ShowWatchlistAdapter;
import net.simonvt.cathode.ui.adapter.SuggestionsAdapter;
import net.simonvt.cathode.widget.SearchView;

public class ShowsWatchlistFragment extends ToolbarGridFragment<RecyclerView.ViewHolder>
    implements ShowWatchlistAdapter.RemoveListener, ShowWatchlistAdapter.OnItemClickListener {

  @Inject JobManager jobManager;

  private ShowsNavigationListener navigationListener;

  private int columnCount;

  private ShowWatchlistAdapter adapter;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      navigationListener = (ShowsNavigationListener) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString() + " must implement ShowsNavigationListener");
    }
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    CathodeApp.inject(getActivity(), this);

    getLoaderManager().initLoader(Loaders.SHOWS_WATCHLIST, null, showsCallback);
    getLoaderManager().initLoader(Loaders.EPISODES_WATCHLIST, null, episodeCallback);

    columnCount = getResources().getInteger(R.integer.showsColumns);

    setEmptyText(R.string.empty_show_watchlist);
    setTitle(R.string.title_shows_watchlist);
  }

  @Override public boolean displaysMenuIcon() {
    return true;
  }

  @Override protected int getColumnCount() {
    return columnCount;
  }

  @Override protected GridLayoutManager.SpanSizeLookup getSpanSizeLookup() {
    return new HeaderSpanLookup(ensureAdapter(), columnCount);
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    toolbar.setNavigationOnClickListener(navigationClickListener);
  }

  private View.OnClickListener navigationClickListener = new View.OnClickListener() {
    @Override public void onClick(View v) {
      navigationListener.onHomeClicked();
    }
  };

  @Override public void createMenu(Toolbar toolbar) {
    super.createMenu(toolbar);
    toolbar.inflateMenu(R.menu.fragment_shows);

    final MenuItem searchItem = toolbar.getMenu().findItem(R.id.menu_search);
    SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
    searchView.setAdapter(new ShowSuggestionAdapter(searchView.getContext()));

    searchView.setListener(new SearchView.SearchViewListener() {
      @Override public void onTextChanged(String newText) {
      }

      @Override public void onSubmit(String query) {
        navigationListener.searchShow(query);

        MenuItemCompat.collapseActionView(searchItem);
      }

      @Override public void onSuggestionSelected(Object suggestion) {
        SuggestionsAdapter.Suggestion item = (SuggestionsAdapter.Suggestion) suggestion;
        if (item.getId() != null) {
          navigationListener.onDisplayShow(item.getId(), item.getTitle(), LibraryType.WATCHED);
        } else {
          navigationListener.searchShow(item.getTitle());
        }

        MenuItemCompat.collapseActionView(searchItem);
      }
    });
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_refresh:
        jobManager.addJob(new SyncJob());
        return true;

      default:
        return super.onMenuItemClick(item);
    }
  }

  @Override public void onShowClicked(int position, long id) {
    Cursor c = ((ShowWatchlistAdapter) getAdapter()).getCursor(position);
    navigationListener.onDisplayShow(id, c.getString(c.getColumnIndex(ShowColumns.TITLE)),
        LibraryType.WATCHED);
  }

  @Override public void onEpisodeClicked(int position, long id) {
    Cursor c = ((ShowWatchlistAdapter) getAdapter()).getCursor(position);
    navigationListener.onDisplayEpisode(id, c.getString(c.getColumnIndex(EpisodeColumns.TITLE)));
  }

  private void throttleLoaders() {
    Loader l = getLoaderManager().getLoader(Loaders.EPISODES_WATCHLIST);
    SimpleCursorLoader loader = (SimpleCursorLoader) l;
    loader.throttle(2000);

    l = getLoaderManager().getLoader(Loaders.SHOWS_WATCHLIST);
    loader = (SimpleCursorLoader) l;
    loader.throttle(2000);
  }

  @Override public void onRemoveItem(View view, int position, long id) {
    throttleLoaders();
    SimpleCursor cursor =
        (SimpleCursor) (((ShowWatchlistAdapter) getAdapter()).getCursor(position));
    cursor.remove(id);
    ((ShowWatchlistAdapter) getAdapter()).notifyChanged();
  }

  private ShowWatchlistAdapter ensureAdapter() {
    if (adapter == null) {
      adapter = new ShowWatchlistAdapter(getActivity(), this, this);
      adapter.addHeader(R.string.header_shows);
      adapter.addHeader(R.string.header_episodes);
    }

    return adapter;
  }

  private void setShowCursor(Cursor cursor) {
    if (getAdapter() == null) {
      setAdapter(ensureAdapter());
    }

    //throttleLoaders();
    ((ShowWatchlistAdapter) getAdapter()).updateCursorForHeader(R.string.header_shows, cursor);
  }

  private void setEpisodeCursor(Cursor cursor) {
    if (getAdapter() == null) {
      setAdapter(ensureAdapter());
    }

    //throttleLoaders();
    ((ShowWatchlistAdapter) getAdapter()).updateCursorForHeader(R.string.header_episodes, cursor);
  }

  private LoaderManager.LoaderCallbacks<SimpleCursor> showsCallback =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          final Uri contentUri = Shows.SHOWS_WATCHLIST;
          SimpleCursorLoader loader = new SimpleCursorLoader(getActivity(), contentUri,
              ShowWatchlistAdapter.PROJECTION_SHOW, null, null, Shows.DEFAULT_SORT);
          loader.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
          return loader;
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
          setShowCursor(data);
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
        }
      };

  private LoaderManager.LoaderCallbacks<SimpleCursor> episodeCallback =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          SimpleCursorLoader loader =
              new SimpleCursorLoader(getActivity(), Episodes.EPISODES_IN_WATCHLIST,
                  ShowWatchlistAdapter.PROJECTION_EPISODE,
                  Tables.EPISODES + "." + EpisodeColumns.NEEDS_SYNC + "=0", null,
                  EpisodeColumns.SHOW_ID + " ASC");
          loader.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
          return loader;
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
          setEpisodeCursor(data);
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
        }
      };
}
