package net.simonvt.cathode.ui.fragment;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.event.OnTitleChangedEvent;
import net.simonvt.cathode.event.SearchFailureEvent;
import net.simonvt.cathode.event.ShowSearchResult;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.provider.CathodeDatabase;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.ShowsNavigationListener;
import net.simonvt.cathode.ui.adapter.ShowSearchAdapter;
import net.simonvt.cathode.util.ShowSearchHandler;

public class SearchShowFragment extends AbsAdapterFragment
    implements LoaderManager.LoaderCallbacks<Cursor> {

  private static final String TAG = "SearchShowFragment";

  private static final String ARGS_QUERY = "net.simonvt.cathode.ui.SearchShowFragment.query";

  private static final String STATE_QUERY = "net.simonvt.cathode.ui.SearchShowFragment.query";

  private static final int LOADER_SEARCH = 200;

  @Inject ShowSearchHandler searchHandler;

  @Inject Bus bus;

  private ShowSearchAdapter showsAdapter;

  private List<Long> searchShowIds;

  private String query;

  private ShowsNavigationListener navigationListener;

  public static Bundle getArgs(String query) {
    Bundle args = new Bundle();
    args.putString(ARGS_QUERY, query);
    return args;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      navigationListener = (ShowsNavigationListener) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString() + " must implement ShowsNavigationListener");
    }
  }

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    CathodeApp.inject(getActivity(), this);

    bus.register(this);

    if (state == null) {
      Bundle args = getArguments();
      query = args.getString(ARGS_QUERY);
      searchHandler.search(query);
    } else {
      query = state.getString(STATE_QUERY);
      if (searchShowIds == null && !searchHandler.isSearching()) {
        searchHandler.search(query);
      }
    }

    setHasOptionsMenu(true);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString(STATE_QUERY, query);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_shows_watched, container, false);
  }

  @Override
  public void onDestroy() {
    bus.unregister(this);
    super.onDestroy();
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.fragment_add_show, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_search:
        navigationListener.onStartShowSearch();
        return true;

      default:
        return false;
    }
  }

  public void query(String query) {
    this.query = query;
    searchHandler.search(query);
    showsAdapter = null;
    setAdapter(null);
    bus.post(new OnTitleChangedEvent());
  }

  @Override
  protected void onItemClick(AdapterView l, View v, int position, long id) {
    Cursor c = (Cursor) getAdapter().getItem(position);
    navigationListener.onDisplayShow(id, c.getString(c.getColumnIndex(CathodeContract.Shows.TITLE)),
        LibraryType.WATCHED);
  }

  @Subscribe
  public void onSearchEvent(ShowSearchResult result) {
    searchShowIds = result.getShowIds();
    getLoaderManager().restartLoader(LOADER_SEARCH, null, this);
    setEmptyText(R.string.no_results, query);
  }

  @Subscribe
  public void onSearchFailure(SearchFailureEvent event) {
    if (event.getType() == SearchFailureEvent.Type.SHOW) {
      setCursor(null);
      setEmptyText(R.string.search_failure, query);
    }
  }

  private void setCursor(Cursor cursor) {
    if (showsAdapter == null) {
      showsAdapter = new ShowSearchAdapter(getActivity());
      setAdapter(showsAdapter);
    }

    showsAdapter.changeCursor(cursor);
  }

  protected static final String[] PROJECTION = new String[] {
      CathodeDatabase.Tables.SHOWS + "." + BaseColumns._ID,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.TITLE,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.OVERVIEW,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.POSTER,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.TVDB_ID,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.WATCHED_COUNT,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.IN_COLLECTION_COUNT,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.IN_WATCHLIST,
  };

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    StringBuilder where = new StringBuilder();
    where.append(CathodeContract.Shows._ID).append(" in (");
    final int showCount = searchShowIds.size();
    String[] ids = new String[showCount];
    for (int i = 0; i < showCount; i++) {
      ids[i] = String.valueOf(searchShowIds.get(i));

      where.append("?");
      if (i < showCount - 1) {
        where.append(",");
      }
    }
    where.append(")");

    CursorLoader loader =
        new CursorLoader(getActivity(), CathodeContract.Shows.CONTENT_URI, PROJECTION,
            where.toString(), ids, null);

    return loader;
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    setCursor(data);
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
  }
}
