package net.simonvt.trakt.ui.fragment;

import net.simonvt.trakt.R;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.provider.TraktDatabase;
import net.simonvt.trakt.ui.ShowsNavigationListener;
import net.simonvt.trakt.ui.adapter.EpisodeWatchlistAdapter;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

public class EpisodesWatchlistFragment extends AbsAdapterFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "EpisodesWatchlistFragment";

    private static final int LOADER_WATCHLIST = 40;

    private EpisodeWatchlistAdapter mAdapter;

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
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getLoaderManager().initLoader(LOADER_WATCHLIST, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list_cards, container, false);
    }

    @Override
    public void onDestroy() {
        getLoaderManager().destroyLoader(LOADER_WATCHLIST);
        super.onDestroy();
    }

    @Override
    protected void onItemClick(AdapterView l, View v, int position, long id) {
        Cursor c = (Cursor) mAdapter.getItem(position);
        mNavigationListener.onDisplayEpisode(id, c.getString(c.getColumnIndex(TraktContract.Shows.TITLE)));
    }

    private void setCursor(Cursor cursor) {
        if (mAdapter == null) {
            mAdapter = new EpisodeWatchlistAdapter(getActivity(), cursor, 0);
            setAdapter(mAdapter);
            return;
        }

        mAdapter.changeCursor(cursor);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        CursorLoader loader = new CursorLoader(getActivity(), TraktContract.Episodes.WATCHLIST_URI, new String[] {
                TraktDatabase.Tables.EPISODES + "." + TraktContract.Episodes._ID,
                TraktDatabase.Tables.EPISODES + "." + TraktContract.Episodes.SCREEN,
                TraktDatabase.Tables.EPISODES + "." + TraktContract.Episodes.TITLE,
                TraktDatabase.Tables.EPISODES + "." + TraktContract.Episodes.FIRST_AIRED,
                TraktDatabase.Tables.EPISODES + "." + TraktContract.Episodes.SEASON,
                TraktDatabase.Tables.EPISODES + "." + TraktContract.Episodes.EPISODE,
                TraktDatabase.Tables.SHOWS + "." + TraktContract.Shows.TITLE,
        }, null, null, TraktContract.Episodes.SHOW_ID + " ASC");
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
