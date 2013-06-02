package net.simonvt.trakt.ui.fragment;

import butterknife.InjectView;
import butterknife.Views;

import net.simonvt.trakt.R;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.widget.RemoteImageView;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MovieFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "MovieFragment";

    private static final String ARG_ID = "net.simonvt.trakt.ui.fragment.MovieFragment.id";

    private static final int LOADER_MOVIE = 400;

    @InjectView(R.id.title) TextView mTitle;
    @InjectView(R.id.year) TextView mYear;
    @InjectView(R.id.banner) RemoteImageView mBanner;
    @InjectView(R.id.overview) TextView mOverview;
    @InjectView(R.id.inCollection) TextView mCollection;
    @InjectView(R.id.inWatchlist) TextView mWatchlist;

    private long mMovieId;

    public static MovieFragment newInstance(long movieId) {
        MovieFragment f = new MovieFragment();

        Bundle args = new Bundle();
        args.putLong(ARG_ID, movieId);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TraktApp.inject(getActivity(), this);

        Bundle args = getArguments();
        mMovieId = args.getLong(ARG_ID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_movie, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Views.inject(this, view);

        getLoaderManager().initLoader(LOADER_MOVIE, null, this);
    }

    @Override
    public void onDestroyView() {
        getLoaderManager().destroyLoader(LOADER_MOVIE);
        super.onDestroyView();
    }

    private void updateView(final Cursor cursor) {
        if (cursor == null || !cursor.moveToFirst()) return;

        final String title = cursor.getString(cursor.getColumnIndex(TraktContract.Movies.TITLE));
        final int year = cursor.getInt(cursor.getColumnIndex(TraktContract.Movies.YEAR));
        final String fanart = cursor.getString(cursor.getColumnIndex(TraktContract.Movies.FANART));
        if (fanart != null) {
            mBanner.setImage(fanart);
        }

        final String overview = cursor.getString(cursor.getColumnIndex(TraktContract.Movies.OVERVIEW));
        final boolean inWatchlist = cursor.getInt(cursor.getColumnIndex(TraktContract.Movies.IN_WATCHLIST)) == 1;
        final boolean inCollection = cursor.getInt(cursor.getColumnIndex(TraktContract.Movies.IN_COLLECTION)) == 1;

        mCollection.setVisibility(inCollection ? View.VISIBLE : View.GONE);
        mWatchlist.setVisibility(inWatchlist ? View.VISIBLE : View.GONE);

        mTitle.setText(title);
        mYear.setText(String.valueOf(year));
        mOverview.setText(overview);

    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        CursorLoader loader =
                new CursorLoader(getActivity(), TraktContract.Movies.buildMovieUri(mMovieId), null, null, null, null);
        loader.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        updateView(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
    }
}
