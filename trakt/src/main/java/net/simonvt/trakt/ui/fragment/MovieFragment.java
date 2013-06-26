package net.simonvt.trakt.ui.fragment;

import butterknife.InjectView;
import butterknife.Views;

import net.simonvt.trakt.R;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.scheduler.MovieTaskScheduler;
import net.simonvt.trakt.ui.dialog.RatingDialog;
import net.simonvt.trakt.widget.RemoteImageView;

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
import android.widget.RatingBar;
import android.widget.TextView;

import javax.inject.Inject;

public class MovieFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "MovieFragment";

    private static final String ARG_ID = "net.simonvt.trakt.ui.fragment.MovieFragment.id";

    private static final String DIALOG_RATING = "net.simonvt.trakt.ui.fragment.MovieFragment.ratingDialog";

    private static final int LOADER_MOVIE = 400;

    @Inject MovieTaskScheduler mMovieScheduler;

    @InjectView(R.id.title) TextView mTitle;
    @InjectView(R.id.year) TextView mYear;
    @InjectView(R.id.banner) RemoteImageView mBanner;
    @InjectView(R.id.overview) TextView mOverview;
    @InjectView(R.id.inCollection) TextView mCollection;
    @InjectView(R.id.inWatchlist) TextView mWatchlist;
    @InjectView(R.id.ratingContainer) View mRatingContainer;
    @InjectView(R.id.rating) RatingBar mRating;

    private long mMovieId;

    private int mCurrentRating;

    private boolean mLoaded;

    private boolean mWatched;

    private boolean mCollected;

    private boolean mInWatchlist;

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

        setHasOptionsMenu(true);

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

        mRatingContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RatingDialog.newInstance(RatingDialog.Type.MOVIE, mMovieId, mCurrentRating)
                        .show(getFragmentManager(), DIALOG_RATING);
            }
        });

        getLoaderManager().initLoader(LOADER_MOVIE, null, this);
    }

    @Override
    public void onDestroyView() {
        getLoaderManager().destroyLoader(LOADER_MOVIE);
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mLoaded) {
            if (mWatched) {
                menu.add(0, R.id.action_unwatched, 1, R.string.action_unwatched);
            } else {
                menu.add(0, R.id.action_watched, 2, R.string.action_watched);
                if (mInWatchlist) {
                    menu.add(0, R.id.action_watchlist_remove, 5, R.string.action_watchlist_remove);
                } else {
                    menu.add(0, R.id.action_watchlist_add, 6, R.string.action_watchlist_add);
                }
            }

            if (mCollected) {
                menu.add(0, R.id.action_collection_remove, 3, R.string.action_collection_remove);
            } else {
                menu.add(0, R.id.action_collection_add, 4, R.string.action_collection_add);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_watched:
                mMovieScheduler.setWatched(mMovieId, true);
                return true;

            case R.id.action_unwatched:
                mMovieScheduler.setWatched(mMovieId, false);
                return true;

            case R.id.action_watchlist_add:
                mMovieScheduler.setIsInWatchlist(mMovieId, true);
                return true;

            case R.id.action_watchlist_remove:
                mMovieScheduler.setIsInWatchlist(mMovieId, false);
                return true;

            case R.id.action_collection_add:
                mMovieScheduler.setIsInCollection(mMovieId, true);
                return true;

            case R.id.action_collection_remove:
                mMovieScheduler.setIsInCollection(mMovieId, false);
                return true;
        }

        return false;
    }

    private void updateView(final Cursor cursor) {
        if (cursor == null || !cursor.moveToFirst()) return;
        mLoaded = true;

        final String title = cursor.getString(cursor.getColumnIndex(TraktContract.Movies.TITLE));
        final int year = cursor.getInt(cursor.getColumnIndex(TraktContract.Movies.YEAR));
        final String fanart = cursor.getString(cursor.getColumnIndex(TraktContract.Movies.FANART));
        if (fanart != null) {
            mBanner.setImage(fanart);
        }
        mCurrentRating = cursor.getInt(cursor.getColumnIndex(TraktContract.Movies.RATING));
        mRating.setRating(mCurrentRating);

        final String overview = cursor.getString(cursor.getColumnIndex(TraktContract.Movies.OVERVIEW));
        mWatched = cursor.getInt(cursor.getColumnIndex(TraktContract.Movies.WATCHED)) == 1;
        mCollected = cursor.getInt(cursor.getColumnIndex(TraktContract.Movies.IN_COLLECTION)) == 1;
        mInWatchlist = cursor.getInt(cursor.getColumnIndex(TraktContract.Movies.IN_WATCHLIST)) == 1;

        mCollection.setVisibility(mCollected ? View.VISIBLE : View.GONE);
        mWatchlist.setVisibility(mInWatchlist ? View.VISIBLE : View.GONE);

        mTitle.setText(title);
        mYear.setText(String.valueOf(year));
        mOverview.setText(overview);

        getActivity().invalidateOptionsMenu();
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
