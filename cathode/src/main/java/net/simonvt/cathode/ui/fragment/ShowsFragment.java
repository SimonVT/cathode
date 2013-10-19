package net.simonvt.cathode.ui.fragment;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.remote.TraktTaskQueue;
import net.simonvt.cathode.remote.sync.SyncTask;
import net.simonvt.cathode.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.ShowsNavigationListener;
import net.simonvt.cathode.ui.adapter.ShowsWithNextAdapter;
import net.simonvt.cathode.util.LogWrapper;

public abstract class ShowsFragment extends AbsAdapterFragment
    implements LoaderManager.LoaderCallbacks<Cursor> {

  private static final String TAG = "ShowsFragment";

  @Inject EpisodeTaskScheduler episodeScheduler;

  @Inject TraktTaskQueue queue;

  private ShowsWithNextAdapter showsAdapter;

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
  public void onCreate(Bundle inState) {
    super.onCreate(inState);
    CathodeApp.inject(getActivity(), this);

    setHasOptionsMenu(true);

    getLoaderManager().initLoader(getLoaderId(), null, this);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
  }

  @Override
  public void onDestroy() {
    if (getActivity().isFinishing() || isRemoving()) {
      getLoaderManager().destroyLoader(getLoaderId());
    }
    super.onDestroy();
  }

  @Override
  public void onDetach() {
    super.onDetach();
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.fragment_shows, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_refresh:
        queue.add(new SyncTask());
        return true;

      case R.id.menu_search:
        navigationListener.onStartShowSearch();
        return true;

      default:
        return false;
    }
  }

  @Override
  protected void onItemClick(AdapterView l, View v, int position, long id) {
    Cursor c = (Cursor) getAdapter().getItem(position);
    navigationListener.onDisplayShow(id, c.getString(c.getColumnIndex(CathodeContract.Shows.TITLE)),
        getLibraryType());
  }

  private void setCursor(Cursor cursor) {
    if (showsAdapter == null) {
      showsAdapter = new ShowsWithNextAdapter(getActivity(), cursor, getLibraryType());
      setAdapter(showsAdapter);
      return;
    }

    showsAdapter.changeCursor(cursor);
  }

  protected abstract LibraryType getLibraryType();

  protected abstract int getLoaderId();

  @Override
  public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
    LogWrapper.d(TAG, "[onLoadFinished]");
    setCursor(cursor);
  }

  @Override
  public void onLoaderReset(Loader<Cursor> cursorLoader) {
    showsAdapter.changeCursor(null);
  }
}
