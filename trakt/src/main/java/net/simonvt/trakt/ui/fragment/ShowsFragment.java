package net.simonvt.trakt.ui.fragment;

import net.simonvt.trakt.R;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.scheduler.EpisodeTaskScheduler;
import net.simonvt.trakt.sync.TraktTaskQueue;
import net.simonvt.trakt.sync.task.SyncTask;
import net.simonvt.trakt.ui.LibraryType;
import net.simonvt.trakt.ui.ShowsNavigationListener;
import net.simonvt.trakt.ui.adapter.ShowsAdapter;
import net.simonvt.trakt.util.LogWrapper;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.SearchView;

import javax.inject.Inject;

public abstract class ShowsFragment extends AbsAdapterFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "ShowsFragment";

    @Inject EpisodeTaskScheduler mEpisodeScheduler;

    @Inject TraktTaskQueue mQueue;

    private ShowsAdapter mShowsAdapter;

    private ShowsNavigationListener mNavigationListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mNavigationListener = (ShowsNavigationListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement ShowsNavigationListener");
        }
    }

    @Override
    public void onCreate(Bundle inState) {
        super.onCreate(inState);
        TraktApp.inject(getActivity(), this);

        setHasOptionsMenu(true);

        setEmptyText("Loading..."); // TODO: Tell user to add show
        getLoaderManager().initLoader(getLoaderId(), null, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shows, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroy() {
        getLoaderManager().destroyLoader(getLoaderId());
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_shows, menu);

        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                LogWrapper.v(TAG, "Query: " + query);
                mNavigationListener.onSearchShow(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                mQueue.add(new SyncTask());
                return true;

            default:
                return false;
        }
    }

    @Override
    protected void onItemClick(AdapterView l, View v, int position, long id) {
        mNavigationListener.onDisplayShow(id, getLibraryType());
    }

    private void setCursor(Cursor cursor) {
        if (mShowsAdapter == null) {
            mShowsAdapter = new ShowsAdapter(getActivity(), cursor, getLibraryType());
            setAdapter(mShowsAdapter);
            return;
        }

        mShowsAdapter.changeCursor(cursor);
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
        mShowsAdapter.changeCursor(null);
    }
}
