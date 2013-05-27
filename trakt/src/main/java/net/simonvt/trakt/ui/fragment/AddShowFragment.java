package net.simonvt.trakt.ui.fragment;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import net.simonvt.trakt.R;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.event.ShowSearchResult;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.provider.TraktDatabase;
import net.simonvt.trakt.ui.LibraryType;
import net.simonvt.trakt.ui.ShowsNavigationListener;
import net.simonvt.trakt.ui.adapter.ShowSearchAdapter;
import net.simonvt.trakt.util.LogWrapper;
import net.simonvt.trakt.util.ShowSearchHandler;

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
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.SearchView;

import java.util.List;

import javax.inject.Inject;

public class AddShowFragment extends AbsAdapterFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "AddShowFragment";

    private static final String ARGS_QUERY = "net.simonvt.trakt.ui.AddShowFragment.query";

    private static final String STATE_QUERY = "net.simonvt.trakt.ui.AddShowFragment.query";

    private static final int LOADER_SEARCH = 200;

    @Inject ShowSearchHandler mSearchHandler;

    @Inject Bus mBus;

    private ShowSearchAdapter mShowsAdapter;

    private List<Long> mSearchShowIds;

    private String mQuery;

    private ShowsNavigationListener mNavigationListener;

    public static AddShowFragment newInstance(String query) {
        AddShowFragment f = new AddShowFragment();

        Bundle args = new Bundle();
        args.putString(ARGS_QUERY, query);
        f.setArguments(args);

        return f;
    }

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
    public void onCreate(Bundle state) {
        super.onCreate(state);
        TraktApp.inject(getActivity(), this);
        mBus.register(this);
        setHasOptionsMenu(true);

        if (state == null) {
            Bundle args = getArguments();
            mQuery = args.getString(ARGS_QUERY);
            mSearchHandler.search(mQuery);

        } else {
            mQuery = state.getString(STATE_QUERY);
            if (mSearchShowIds == null && !mSearchHandler.isSearching()) {
                mSearchHandler.search(mQuery);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_QUERY, mQuery);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list_cards, container, false);
    }

    @Override
    public void onDestroy() {
        mBus.unregister(this);
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_add_show, menu);

        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                LogWrapper.v(TAG, "Query: " + query);
                mSearchHandler.search(query);
                mShowsAdapter = null;
                setAdapter(null);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    @Override
    protected void onItemClick(AdapterView l, View v, int position, long id) {
        mNavigationListener.onDisplayShow(id, LibraryType.WATCHED);
    }

    @Subscribe
    public void onShowSearchEvent(ShowSearchResult result) {
        mSearchShowIds = result.getShowIds();
        getLoaderManager().restartLoader(LOADER_SEARCH, null, this);
    }

    private void setCursor(Cursor cursor) {
        if (mShowsAdapter == null) {
            mShowsAdapter = new ShowSearchAdapter(getActivity());
            setAdapter(mShowsAdapter);
        }

        mShowsAdapter.changeCursor(cursor);
    }

    protected static final String[] PROJECTION = new String[] {
            TraktDatabase.Tables.SHOWS + "." + BaseColumns._ID,
            TraktDatabase.Tables.SHOWS + "." + TraktContract.Shows.TITLE,
            TraktDatabase.Tables.SHOWS + "." + TraktContract.Shows.OVERVIEW,
            TraktDatabase.Tables.SHOWS + "." + TraktContract.Shows.POSTER,
    };

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        StringBuilder where = new StringBuilder();
        where.append(TraktContract.Shows._ID).append(" in (");
        final int showCount = mSearchShowIds.size();
        String[] ids = new String[showCount];
        for (int i = 0; i < showCount; i++) {
            ids[i] = String.valueOf(mSearchShowIds.get(i));

            where.append("?");
            if (i < showCount - 1) {
                where.append(",");
            }
        }
        where.append(")");

        CursorLoader loader =
                new CursorLoader(getActivity(), TraktContract.Shows.CONTENT_URI, PROJECTION, where.toString(),
                        ids, null);

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
