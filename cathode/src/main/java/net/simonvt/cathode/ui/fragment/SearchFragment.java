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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import butterknife.OnClick;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.search.SearchHandler;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.Loaders;
import net.simonvt.cathode.ui.adapter.RecyclerCursorAdapter;
import net.simonvt.cathode.ui.adapter.SuggestionsAdapter;
import net.simonvt.cathode.ui.dialog.ListDialog;
import net.simonvt.cathode.widget.SearchView;
import timber.log.Timber;

public abstract class SearchFragment extends OverlayToolbarGridFragment<RecyclerView.ViewHolder>
    implements LoaderManager.LoaderCallbacks<SimpleCursor>, ListDialog.Callback,
    SearchHandler.SearchListener {

  public enum SortBy {
    TITLE("title"),
    RATING("rating"),
    RELEVANCE("relevance");

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

  private static final String ARGS_QUERY = "net.simonvt.cathode.ui.SearchActivity.query";

  private static final String STATE_QUERY = "net.simonvt.cathode.ui.SearchActivity.query";

  private static final String DIALOG_SORT =
      "net.simonvt.cathode.ui.fragment.SearchActivity.sortDialog";

  SearchView searchView;

  private SharedPreferences settings;

  private SortBy sortBy;

  private RecyclerCursorAdapter adapter;

  private String query;

  private List<Long> resultIds;

  private boolean scrollToTop;

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    CathodeApp.inject(getContext(), this);

    settings = PreferenceManager.getDefaultSharedPreferences(getContext());

    sortBy = getSortBy();

    if (inState != null) {
      query = inState.getString(STATE_QUERY);
    }

    getSearchHandler().addListener(this);

    if (!getSearchHandler().isSearching() && getSearchHandler().noResults()) {
      setOverlay(R.string.search_initial);
    }
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString(STATE_QUERY, query);
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    searchView = (SearchView) LayoutInflater.from(toolbar.getContext())
        .inflate(R.layout.search_view, toolbar, false);
    toolbar.addView(searchView);

    searchView.setAdapter(getSuggestionsAdapter(searchView.getContext()));
    searchView.setListener(new SearchView.SearchViewListener() {
      @Override public void onTextChanged(String newText) {
      }

      @Override public void onSubmit(String query) {
        query(query);
        searchView.clearFocus();
      }

      @Override public void onSuggestionSelected(Object suggestion) {
        SearchFragment.this.onSuggestionSelected(suggestion);
        searchView.clearFocus();
      }
    });

    Timber.d("onViewCreated");
  }

  @OnClick(android.R.id.empty) void onResendQuery() {
    query(query);
  }

  @Override public void onDestroy() {
    super.onDestroy();
    getSearchHandler().removeListener(this);

    if (isRemoving()) {
      getSearchHandler().clear();
    }
  }

  @Override public void createMenu(Toolbar toolbar) {
    super.createMenu(toolbar);
    toolbar.inflateMenu(R.menu.fragment_search);
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.sort_by:
        ArrayList<ListDialog.Item> items = new ArrayList<>();
        items.add(new ListDialog.Item(R.id.sort_relevance, R.string.sort_relevance));
        items.add(new ListDialog.Item(R.id.sort_rating, R.string.sort_rating));
        items.add(new ListDialog.Item(R.id.sort_title, R.string.sort_title));
        ListDialog.newInstance(R.string.action_sort_by, items, this)
            .show(getFragmentManager(), DIALOG_SORT);
        return true;

      default:
        return super.onMenuItemClick(item);
    }
  }

  @Override public void onItemSelected(int id) {
    switch (id) {
      case R.id.sort_relevance:
        if (sortBy != SortBy.RELEVANCE) {
          sortBy = SortBy.RELEVANCE;
          settings.edit().putString(Settings.Sort.SHOW_SEARCH, SortBy.RELEVANCE.getKey()).apply();
          if (getLoaderManager().getLoader(Loaders.SEARCH) != null) {
            getLoaderManager().restartLoader(Loaders.SEARCH, null, this);
          }
          scrollToTop = true;
        }
        break;

      case R.id.sort_rating:
        if (sortBy != SortBy.RATING) {
          sortBy = SortBy.RATING;
          settings.edit().putString(Settings.Sort.SHOW_SEARCH, SortBy.RATING.getKey()).apply();
          if (getLoaderManager().getLoader(Loaders.SEARCH) != null) {
            getLoaderManager().restartLoader(Loaders.SEARCH, null, this);
          }
          scrollToTop = true;
        }
        break;

      case R.id.sort_title:
        if (sortBy != SortBy.TITLE) {
          sortBy = SortBy.TITLE;
          settings.edit().putString(Settings.Sort.SHOW_SEARCH, SortBy.TITLE.getKey()).apply();
          if (getLoaderManager().getLoader(Loaders.SEARCH) != null) {
            getLoaderManager().restartLoader(Loaders.SEARCH, null, this);
          }
          scrollToTop = true;
        }
        break;
    }
  }

  public abstract void onSuggestionSelected(Object suggestion);

  protected void query(String query) {
    Timber.d("Query: %s", query);
    setOverlay(0);

    this.query = query;

    getLoaderManager().destroyLoader(Loaders.SEARCH);
    resultIds = null;

    setForceDisplayProgress(true);

    getSearchHandler().search(query);
  }

  public void onSearchSuccess(List<Long> resultIds) {
    Timber.d("onSearchSuccess");
    this.resultIds = resultIds;

    getLoaderManager().initLoader(Loaders.SEARCH, null, this);
    setEmptyText(getResources().getString(R.string.no_results, query));
  }

  public void onSearchFailure() {
    setOverlay(R.string.search_failure);
  }

  public String getQuery() {
    return query;
  }

  public abstract SortBy getSortBy();

  public abstract SuggestionsAdapter getSuggestionsAdapter(Context context);

  public abstract SearchHandler getSearchHandler();

  public abstract RecyclerCursorAdapter createAdapter(Cursor cursor);

  public RecyclerCursorAdapter getCursorAdapter() {
    return adapter;
  }

  private void setCursor(Cursor cursor) {
    Timber.d("setCursor");
    setForceDisplayProgress(false);

    if (adapter == null) {
      adapter = createAdapter(cursor);
      setAdapter(adapter);
    } else {
      adapter.changeCursor(cursor);
    }

    if (scrollToTop) {
      getRecyclerView().scrollToPosition(0);
      scrollToTop = false;
    }
  }

  public abstract Uri getUri();

  public abstract String[] getProjection();

  public abstract String getSortString(SortBy sortBy);

  @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
    Timber.d("Creating loader");
    StringBuilder where = new StringBuilder();
    where.append(BaseColumns._ID).append(" in (");
    final int resultCount = resultIds.size();
    String[] ids = new String[resultCount];
    for (int i = 0; i < resultCount; i++) {
      ids[i] = String.valueOf(resultIds.get(i));

      where.append("?");
      if (i < resultCount - 1) {
        where.append(",");
      }
    }
    where.append(")");

    return new SimpleCursorLoader(getContext(), getUri(), getProjection(), where.toString(), ids,
        getSortString(sortBy));
  }

  @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
    setCursor(data);
  }

  @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
  }
}
