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
package net.simonvt.cathode.ui.search;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.BindView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.Injector;
import net.simonvt.cathode.common.ui.fragment.ToolbarGridFragment;
import net.simonvt.cathode.common.util.Debouncer;
import net.simonvt.cathode.common.widget.ErrorView;
import net.simonvt.cathode.common.widget.SearchView;
import net.simonvt.cathode.provider.DatabaseContract.RecentQueriesColumns;
import net.simonvt.cathode.provider.ProviderSchematic.RecentQueries;
import net.simonvt.cathode.provider.database.SimpleCursorLoader;
import net.simonvt.cathode.scheduler.SearchTaskScheduler;
import net.simonvt.cathode.search.Result;
import net.simonvt.cathode.search.SearchHandler;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.NavigationListener;
import net.simonvt.schematic.Cursors;

public class SearchFragment extends ToolbarGridFragment<SearchAdapter.ViewHolder>
    implements SearchHandler.SearchListener, SearchAdapter.OnResultClickListener {

  public enum SortBy {
    TITLE("title"), RATING("rating"), RELEVANCE("relevance");

    private String key;

    SortBy(String key) {
      this.key = key;
    }

    public String getKey() {
      return key;
    }

    @Override public String toString() {
      return key;
    }

    private static final Map<String, SortBy> STRING_MAPPING = new HashMap<>();

    static {
      for (SortBy via : SortBy.values()) {
        STRING_MAPPING.put(via.toString().toUpperCase(Locale.US), via);
      }
    }

    public static SortBy fromValue(String value) {
      return STRING_MAPPING.get(value.toUpperCase(Locale.US));
    }
  }

  public static final String TAG = "net.simonvt.cathode.ui.search.SearchFragment";

  private static final int LOADER_RECENTS = 1;

  private static final long DEBOUNCE_MILLIS = 350;

  @Inject SearchHandler searchHandler;

  @Inject SearchTaskScheduler searchScheduler;

  @BindView(R.id.errorView) ErrorView errorView;

  private SearchView searchView;
  private boolean requestFocus;

  private SortBy sortBy;

  private SearchAdapter adapter;

  private List<Result> results;
  private boolean localResults;

  private boolean resetScrollPosition;

  private NavigationListener navigationListener;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    navigationListener = (NavigationListener) activity;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    Injector.inject(this);

    sortBy = SortBy.fromValue(
        Settings.get(getContext()).getString(Settings.Sort.SEARCH, SortBy.TITLE.getKey()));

    adapter = new SearchAdapter(this);
    setAdapter(adapter);
    setEmptyText(R.string.search_empty);

    getLoaderManager().initLoader(LOADER_RECENTS, null, recents);

    searchHandler.addListener(this);

    if (inState == null) {
      requestFocus = true;
    }
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.search_fragment, container, false);
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    searchView = (SearchView) LayoutInflater.from(getToolbar().getContext())
        .inflate(R.layout.search_view, getToolbar(), false);
    getToolbar().addView(searchView);

    searchView.setListener(new SearchView.SearchViewListener() {
      @Override public void onTextChanged(String newText) {
        queryChanged(newText);
      }

      @Override public void onSubmit(String query) {
        query(query);
        searchView.clearFocus();
      }
    });

    updateErroView();

    if (requestFocus) {
      requestFocus = false;
      searchView.onActionViewExpanded();
    }
  }

  @Override public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
    super.onViewStateRestored(savedInstanceState);
    updateScrollPosition();
  }

  private void updateScrollPosition() {
    if (getView() != null && resetScrollPosition) {
      resetScrollPosition = false;
      getRecyclerView().scrollToPosition(0);
    }
  }

  private void updateErroView() {
    if (localResults) {
      errorView.show();
    } else {
      errorView.hide();
    }
  }

  @Override public void onDestroy() {
    super.onDestroy();
    searchHandler.removeListener(this);

    if (isRemoving()) {
      searchHandler.clear();
    }
  }

  private void queryChanged(String query) {
    if (TextUtils.isEmpty(query)) {
      Debouncer.remove("search");
      adapter.setResults(null);
      searchHandler.clear();
    } else {
      Debouncer.debounce("search", getSearchRunnable(query), DEBOUNCE_MILLIS);
    }
  }

  protected void query(String query) {
    Debouncer.remove("search");

    if (TextUtils.isEmpty(query)) {
      adapter.setResults(null);
    } else {
      adapter.setSearching(true);
      searchHandler.forceSearch(query);
      searchScheduler.insertRecentQuery(query);
    }
  }

  private Runnable getSearchRunnable(final String query) {
    return new Runnable() {
      @Override public void run() {
        searchHandler.search(query);
      }
    };
  }

  @Override public void onShowClicked(long showId, String title, String overview) {
    navigationListener.onDisplayShow(showId, title, overview, LibraryType.WATCHED);

    searchScheduler.insertRecentQuery(title);
  }

  @Override public void onMovieClicked(long movieId, String title, String overview) {
    navigationListener.onDisplayMovie(movieId, title, overview);

    searchScheduler.insertRecentQuery(title);
  }

  @Override public void onQueryClicked(String query) {
    searchView.clearFocus();
    searchView.setQuery(query);

    adapter.setSearching(true);
    searchHandler.forceSearch(query);
  }

  @Override public void onSearchResult(List<Result> results, boolean localResults) {
    adapter.setSearching(false);

    this.results = results;
    this.localResults = localResults;

    adapter.setResults(results);

    resetScrollPosition = true;

    if (getView() != null) {
      updateScrollPosition();
      updateErroView();
    }
  }

  private LoaderManager.LoaderCallbacks<Cursor> recents =
      new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override public Loader<Cursor> onCreateLoader(int id, Bundle args) {
          CursorLoader loader =
              new CursorLoader(getActivity(), RecentQueries.RECENT_QUERIES, new String[] {
                  RecentQueriesColumns.QUERY,
              }, null, null, RecentQueriesColumns.QUERIED_AT + " DESC LIMIT 3");
          loader.setUpdateThrottle(SimpleCursorLoader.DEFAULT_UPDATE_THROTTLE);
          return loader;
        }

        @Override public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
          List<String> recentQueries = new ArrayList<>();
          data.moveToPosition(-1);
          while (data.moveToNext()) {
            final String query = Cursors.getString(data, RecentQueriesColumns.QUERY);
            recentQueries.add(query);
          }
          adapter.setRecentQueries(recentQueries);
        }

        @Override public void onLoaderReset(Loader<Cursor> loader) {
        }
      };
}
