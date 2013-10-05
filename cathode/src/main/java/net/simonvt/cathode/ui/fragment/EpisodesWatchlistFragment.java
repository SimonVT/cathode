package net.simonvt.cathode.ui.fragment;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.provider.CathodeDatabase;
import net.simonvt.cathode.ui.BaseActivity;
import net.simonvt.cathode.ui.ShowsNavigationListener;
import net.simonvt.cathode.ui.adapter.EpisodeWatchlistAdapter;

public class EpisodesWatchlistFragment extends AbsAdapterFragment
    implements LoaderManager.LoaderCallbacks<Cursor> {

  private static final String TAG = "EpisodesWatchlistFragment";

  private EpisodeWatchlistAdapter adapter;

  private ShowsNavigationListener navigationListener;

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
    setHasOptionsMenu(true);
    getLoaderManager().initLoader(BaseActivity.LOADER_EPISODES_WATCHLIST, null, this);
  }

  @Override public String getTitle() {
    return getResources().getString(R.string.title_shows_episode_watchlist);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_episodes_watchlist, container, false);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.fragment_watchlist_episode, menu);
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

  @Override
  public void onDestroy() {
    if (getActivity().isFinishing() || isRemoving()) {
      getLoaderManager().destroyLoader(BaseActivity.LOADER_EPISODES_WATCHLIST);
    }
    super.onDestroy();
  }

  @Override
  protected void onItemClick(AdapterView l, View v, int position, long id) {
    Cursor c = (Cursor) adapter.getItem(position);
    navigationListener.onDisplayEpisode(id,
        c.getString(c.getColumnIndex(CathodeContract.Shows.TITLE)));
  }

  private void setCursor(Cursor cursor) {
    if (adapter == null) {
      adapter = new EpisodeWatchlistAdapter(getActivity(), cursor, 0);
      setAdapter(adapter);
      return;
    }

    adapter.changeCursor(cursor);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
    CursorLoader loader =
        new CursorLoader(getActivity(), CathodeContract.Episodes.WATCHLIST_URI, new String[] {
            CathodeDatabase.Tables.EPISODES + "." + CathodeContract.Episodes._ID,
            CathodeDatabase.Tables.EPISODES + "." + CathodeContract.Episodes.SCREEN,
            CathodeDatabase.Tables.EPISODES + "." + CathodeContract.Episodes.TITLE,
            CathodeDatabase.Tables.EPISODES + "." + CathodeContract.Episodes.FIRST_AIRED,
            CathodeDatabase.Tables.EPISODES + "." + CathodeContract.Episodes.SEASON,
            CathodeDatabase.Tables.EPISODES + "." + CathodeContract.Episodes.EPISODE,
            CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.TITLE,
        }, null, null, CathodeContract.Episodes.SHOW_ID + " ASC");
    loader.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
    return loader;
  }

  @Override
  public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
    setCursor(cursor);
  }

  @Override
  public void onLoaderReset(Loader<Cursor> cursorLoader) {
    setCursor(null);
  }
}
