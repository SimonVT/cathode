package net.simonvt.trakt.ui.fragment;

import net.simonvt.trakt.R;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.provider.TraktDatabase;
import net.simonvt.trakt.scheduler.EpisodeTaskScheduler;
import net.simonvt.trakt.sync.TraktTaskQueue;
import net.simonvt.trakt.sync.task.SyncShowsCollectionTask;
import net.simonvt.trakt.ui.LibraryType;
import net.simonvt.trakt.ui.ShowsNavigationListener;
import net.simonvt.trakt.ui.adapter.ShowsAdapter;
import net.simonvt.trakt.util.LogWrapper;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
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
import android.widget.SearchView;

import javax.inject.Inject;

public class ShowsCollectionFragment extends AbsAdapterFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "ShowsCollectionFragment";

    private static final int LOADER_COLLECTION = 104;

    protected static final String[] PROJECTION = new String[] {
            TraktDatabase.Tables.SHOWS + "." + BaseColumns._ID,
            TraktDatabase.Tables.SHOWS + "." + TraktContract.Shows.TITLE,
            TraktDatabase.Tables.SHOWS + "." + TraktContract.Shows.POSTER,
            TraktDatabase.Tables.SHOWS + "." + TraktContract.Shows.AIRDATE_COUNT,
            TraktDatabase.Tables.SHOWS + "." + TraktContract.Shows.UNAIRED_COUNT,
            TraktDatabase.Tables.SHOWS + "." + TraktContract.Shows.IN_COLLECTION_COUNT,
            TraktDatabase.Tables.SHOWS + "." + TraktContract.Shows.STATUS,
            TraktDatabase.Tables.EPISODES + "." + TraktContract.Episodes.TITLE,
            TraktDatabase.Tables.EPISODES + "." + TraktContract.Episodes.FIRST_AIRED,
            TraktDatabase.Tables.EPISODES + "." + TraktContract.Episodes.SEASON,
            TraktDatabase.Tables.EPISODES + "." + TraktContract.Episodes.EPISODE,
    };

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
        getLoaderManager().initLoader(LOADER_COLLECTION, null, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list_cards, container, false);
    }

    @Override
    public void onDestroy() {
        getLoaderManager().destroyLoader(LOADER_COLLECTION);
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
                mQueue.add(new SyncShowsCollectionTask());
                return true;

            default:
                return false;
        }
    }

    @Override
    protected void onItemClick(AdapterView l, View v, int position, long id) {
        mNavigationListener.onDisplayShow(id, LibraryType.COLLECTION);
    }

    @Override
    public String getTitle() {
        return getResources().getString(R.string.title_shows_collection);
    }

    private void setCursor(Cursor cursor) {
        if (mShowsAdapter == null) {
            mShowsAdapter = new ShowsAdapter(getActivity(), cursor, LibraryType.COLLECTION);
            setAdapter(mShowsAdapter);
            return;
        }

        mShowsAdapter.changeCursor(cursor);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        final Uri contentUri = TraktContract.Shows.SHOWS_COLLECTION;
        CursorLoader cl = new CursorLoader(getActivity(), contentUri, PROJECTION, null,
                null, TraktContract.Shows.DEFAULT_SORT);
        cl.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
        return cl;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        cursor.setNotificationUri(getActivity().getContentResolver(), TraktContract.Shows.CONTENT_URI);
        setCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mShowsAdapter.changeCursor(null);
    }
}
