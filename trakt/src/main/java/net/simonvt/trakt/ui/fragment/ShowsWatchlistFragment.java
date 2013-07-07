package net.simonvt.trakt.ui.fragment;

import net.simonvt.trakt.R;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.provider.TraktDatabase;
import net.simonvt.trakt.ui.LibraryType;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ShowsWatchlistFragment extends ShowsFragment {

    protected static final String[] PROJECTION = new String[] {
            TraktDatabase.Tables.SHOWS + "." + BaseColumns._ID,
            TraktDatabase.Tables.SHOWS + "." + TraktContract.Shows.TITLE,
            TraktDatabase.Tables.SHOWS + "." + TraktContract.Shows.POSTER,
            TraktDatabase.Tables.SHOWS + "." + TraktContract.Shows.AIRDATE_COUNT,
            TraktDatabase.Tables.SHOWS + "." + TraktContract.Shows.UNAIRED_COUNT,
            TraktDatabase.Tables.SHOWS + "." + TraktContract.Shows.WATCHED_COUNT,
            TraktDatabase.Tables.SHOWS + "." + TraktContract.Shows.STATUS,
            TraktDatabase.Tables.EPISODES + "." + TraktContract.Episodes.TITLE,
            TraktDatabase.Tables.EPISODES + "." + TraktContract.Episodes.FIRST_AIRED,
            TraktDatabase.Tables.EPISODES + "." + TraktContract.Episodes.SEASON,
            TraktDatabase.Tables.EPISODES + "." + TraktContract.Episodes.EPISODE,
    };

    @Override
    public String getTitle() {
        return getResources().getString(R.string.title_shows_watchlist);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shows_watchlist, container, false);
    }

    @Override
    protected LibraryType getLibraryType() {
        return LibraryType.WATCHLIST;
    }

    @Override
    protected int getLoaderId() {
        return 102;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        final Uri contentUri = TraktContract.Shows.SHOWS_WATCHLIST;
        CursorLoader cl =
                new CursorLoader(getActivity(), contentUri, PROJECTION, null, null, TraktContract.Shows.DEFAULT_SORT);
        cl.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
        return cl;
    }
}
