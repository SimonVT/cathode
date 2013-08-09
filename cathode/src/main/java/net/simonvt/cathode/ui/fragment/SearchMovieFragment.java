package net.simonvt.cathode.ui.fragment;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
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
import net.simonvt.cathode.event.MovieSearchResult;
import net.simonvt.cathode.event.OnTitleChangedEvent;
import net.simonvt.cathode.event.SearchFailureEvent;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.provider.CathodeDatabase;
import net.simonvt.cathode.ui.MoviesNavigationListener;
import net.simonvt.cathode.ui.adapter.MovieSearchAdapter;
import net.simonvt.cathode.util.MovieSearchHandler;

public class SearchMovieFragment extends AbsAdapterFragment
    implements LoaderManager.LoaderCallbacks<Cursor> {

  private static final String TAG = "SearchMovieFragment";

  private static final String ARGS_QUERY = "net.simonvt.cathode.ui.SearchMovieFragment.query";

  private static final String STATE_QUERY = "net.simonvt.cathode.ui.SearchMovieFragment.query";

  private static final int LOADER_SEARCH = 300;

  @Inject MovieSearchHandler searchHandler;

  @Inject Bus bus;

  private MovieSearchAdapter movieAdapter;

  private List<Long> searchMovieIds;

  private String query;

  private MoviesNavigationListener navigationListener;

  public static Bundle getArgs(String query) {
    Bundle args = new Bundle();
    args.putString(ARGS_QUERY, query);
    return args;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      navigationListener = (MoviesNavigationListener) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(
          activity.toString() + " must implement MoviesNavigationListener");
    }
  }

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    CathodeApp.inject(getActivity(), this);

    if (state == null) {
      Bundle args = getArguments();
      query = args.getString(ARGS_QUERY);
      searchHandler.search(query);
    } else {
      query = state.getString(STATE_QUERY);
      if (searchMovieIds == null && !searchHandler.isSearching()) {
        searchHandler.search(query);
      }
    }

    bus.register(this);
    setHasOptionsMenu(true);
  }

  @Override
  public String getTitle() {
    return query;
  }

  @Override
  public String getSubtitle() {
    return searchMovieIds != null ? searchMovieIds.size() + "results" : null;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString(STATE_QUERY, query);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_movies_watched, container, false);
  }

  @Override
  public void onDestroy() {
    bus.unregister(this);
    super.onDestroy();
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.search, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_search:
        navigationListener.onStartMovieSearch();
        return true;

      default:
        return false;
    }
  }

  public void query(String query) {
    this.query = query;
    searchHandler.search(query);
    movieAdapter = null;
    setAdapter(null);
    bus.post(new OnTitleChangedEvent());
  }

  @Override
  protected void onItemClick(AdapterView l, View v, int position, long id) {
    Cursor c = (Cursor) getAdapter().getItem(position);
    navigationListener.onDisplayMovie(id,
        c.getString(c.getColumnIndex(CathodeContract.Movies.TITLE)));
  }

  @Subscribe
  public void onSearchEvent(MovieSearchResult result) {
    searchMovieIds = result.getMovieIds();
    getLoaderManager().restartLoader(LOADER_SEARCH, null, this);
    setEmptyText(R.string.no_results, query);
  }

  @Subscribe
  public void onSearchFailure(SearchFailureEvent event) {
    if (event.getType() == SearchFailureEvent.Type.MOVIE) {
      setCursor(null);
      setEmptyText(R.string.search_failure, query);
    }
  }

  private void setCursor(Cursor cursor) {
    if (movieAdapter == null) {
      movieAdapter = new MovieSearchAdapter(getActivity());
      setAdapter(movieAdapter);
    }

    movieAdapter.changeCursor(cursor);
  }

  protected static final String[] PROJECTION = new String[] {
      CathodeDatabase.Tables.MOVIES + "." + CathodeContract.Movies._ID,
      CathodeDatabase.Tables.MOVIES + "." + CathodeContract.Movies.TITLE,
      CathodeDatabase.Tables.MOVIES + "." + CathodeContract.Movies.OVERVIEW,
      CathodeDatabase.Tables.MOVIES + "." + CathodeContract.Movies.POSTER,
      CathodeDatabase.Tables.MOVIES + "." + CathodeContract.Movies.TMDB_ID,
      CathodeDatabase.Tables.MOVIES + "." + CathodeContract.Movies.WATCHED,
      CathodeDatabase.Tables.MOVIES + "." + CathodeContract.Movies.IN_COLLECTION,
      CathodeDatabase.Tables.MOVIES + "." + CathodeContract.Movies.IN_WATCHLIST,
  };

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    StringBuilder where = new StringBuilder();
    where.append(CathodeContract.Movies._ID).append(" in (");
    final int showCount = searchMovieIds.size();
    String[] ids = new String[showCount];
    for (int i = 0; i < showCount; i++) {
      ids[i] = String.valueOf(searchMovieIds.get(i));

      where.append("?");
      if (i < showCount - 1) {
        where.append(",");
      }
    }
    where.append(")");

    CursorLoader loader =
        new CursorLoader(getActivity(), CathodeContract.Movies.CONTENT_URI, PROJECTION,
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
