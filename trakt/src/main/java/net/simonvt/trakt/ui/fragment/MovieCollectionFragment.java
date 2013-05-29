package net.simonvt.trakt.ui.fragment;

import net.simonvt.trakt.provider.TraktContract;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;

public class MovieCollectionFragment extends MoviesFragment {

    private static final String TAG = "WatchedMoviesFragment";

    @Override
    protected int getLoaderId() {
        return 202;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        CursorLoader loader = new CursorLoader(getActivity(), TraktContract.Movies.CONTENT_URI, null,
                TraktContract.Movies.IN_COLLECTION, null, null);
        loader.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
        return loader;
    }
}
