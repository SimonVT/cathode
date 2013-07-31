package net.simonvt.trakt.ui.fragment;

import butterknife.InjectView;
import butterknife.Views;

import net.simonvt.trakt.R;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.scheduler.EpisodeTaskScheduler;
import net.simonvt.trakt.ui.dialog.RatingDialog;
import net.simonvt.trakt.util.DateUtils;
import net.simonvt.trakt.util.LogWrapper;
import net.simonvt.trakt.widget.RemoteImageView;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import javax.inject.Inject;

public class EpisodeFragment extends ProgressFragment {

    private static final String TAG = "EpisodeFragment";

    private static final String ARG_EPISODEID = "net.simonvt.trakt.ui.fragment.EpisodeFragment.episodeId";
    private static final String ARG_SHOW_TITLE = "net.simonvt.trakt.ui.fragment.EpisodeFragment.showTitle";

    private static final String DIALOG_RATING = "net.simonvt.trakt.ui.fragment.EpisodeFragment.ratingDialog";

    private static final int LOADER_EPISODE = 30;

    private long mEpisodeId;

    @Inject EpisodeTaskScheduler mEpisodeScheduler;

    @InjectView(R.id.title) TextView mTitle;
    @InjectView(R.id.screen) RemoteImageView mScreen;
    @InjectView(R.id.overview) TextView mOverview;
    @InjectView(R.id.firstAired) TextView mFirstAired;

    @InjectView(R.id.ratingContainer) View mRatingContainer;
    @InjectView(R.id.rating) RatingBar mRating;
    @InjectView(R.id.allRatings) TextView mAllRatings;

    @InjectView(R.id.watched) View mWatchedView;
    @InjectView(R.id.inCollection) View mInCollectionView;
    @InjectView(R.id.inWatchlist) View mInWatchlistView;

    private String mShowTitle;

    private int mCurrentRating;

    private boolean mLoaded;

    private boolean mWatched;

    private boolean mCollected;

    private boolean mInWatchlist;

    public static Bundle getArgs(long episodeId, String showTitle) {
        Bundle args = new Bundle();
        args.putLong(ARG_EPISODEID, episodeId);
        args.putString(ARG_SHOW_TITLE, showTitle);
        return args;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TraktApp.inject(getActivity(), this);

        Bundle args = getArguments();
        mEpisodeId = args.getLong(ARG_EPISODEID);
        mShowTitle = args.getString(ARG_SHOW_TITLE);
        getLoaderManager().initLoader(LOADER_EPISODE, null, mEpisodeCallbacks);

        setHasOptionsMenu(true);
    }

    @Override
    public String getTitle() {
        return mShowTitle;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_episode, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Views.inject(this, view);

        mRatingContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RatingDialog.newInstance(RatingDialog.Type.EPISODE, mEpisodeId, mCurrentRating)
                        .show(getFragmentManager(), DIALOG_RATING);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (getActivity().isFinishing() || isRemoving()) {
            getLoaderManager().destroyLoader(LOADER_EPISODE);
        }
        super.onDestroy();
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
                mEpisodeScheduler.setWatched(mEpisodeId, true);
                return true;

            case R.id.action_unwatched:
                mEpisodeScheduler.setWatched(mEpisodeId, false);
                return true;

            case R.id.action_collection_add:
                mEpisodeScheduler.setIsInCollection(mEpisodeId, true);
                return true;

            case R.id.action_collection_remove:
                mEpisodeScheduler.setIsInCollection(mEpisodeId, false);
                return true;

            case R.id.action_watchlist_add:
                mEpisodeScheduler.setIsInWatchlist(mEpisodeId, true);
                return true;

            case R.id.action_watchlist_remove:
                mEpisodeScheduler.setIsInWatchlist(mEpisodeId, false);
                return true;
        }

        return false;
    }

    private void updateEpisodeViews(final Cursor cursor) {
        if (cursor.moveToFirst()) {
            mLoaded = true;

            mTitle.setText(cursor.getString(cursor.getColumnIndex(TraktContract.Episodes.TITLE)));
            mOverview.setText(cursor.getString(cursor.getColumnIndex(TraktContract.Episodes.OVERVIEW)));
            mScreen.setImage(cursor.getString(cursor.getColumnIndex(TraktContract.Episodes.SCREEN)));
            mFirstAired.setText(DateUtils.millisToString(getActivity(),
                    cursor.getLong(cursor.getColumnIndex(TraktContract.Episodes.FIRST_AIRED)), true));

            mWatched = cursor.getInt(cursor.getColumnIndex(TraktContract.Episodes.WATCHED)) == 1;
            mCollected = cursor.getInt(cursor.getColumnIndex(TraktContract.Episodes.IN_COLLECTION)) == 1;
            mInWatchlist = cursor.getInt(cursor.getColumnIndex(TraktContract.Episodes.IN_WATCHLIST)) == 1;

            mWatchedView.setVisibility(mWatched ? View.VISIBLE : View.GONE);
            mInCollectionView.setVisibility(mCollected ? View.VISIBLE : View.GONE);
            mInWatchlistView.setVisibility(mInWatchlist ? View.VISIBLE : View.GONE);

            mCurrentRating = cursor.getInt(cursor.getColumnIndex(TraktContract.Episodes.RATING));
            final int ratingAll = cursor.getInt(cursor.getColumnIndex(TraktContract.Episodes.RATING_PERCENTAGE));
            mRating.setRating(mCurrentRating);
            mAllRatings.setText(ratingAll + "%");

            setContentVisible(true);
            getActivity().invalidateOptionsMenu();
        }
    }

    private static final String[] EPISODE_PROJECTION = new String[] {
            TraktContract.Episodes.TITLE,
            TraktContract.Episodes.SCREEN,
            TraktContract.Episodes.OVERVIEW,
            TraktContract.Episodes.FIRST_AIRED,
            TraktContract.Episodes.WATCHED,
            TraktContract.Episodes.IN_COLLECTION,
            TraktContract.Episodes.IN_WATCHLIST,
            TraktContract.Episodes.RATING,
            TraktContract.Episodes.RATING_PERCENTAGE,
    };

    private LoaderManager.LoaderCallbacks<Cursor> mEpisodeCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            CursorLoader cl =
                    new CursorLoader(getActivity(), TraktContract.Episodes.buildFromId(mEpisodeId),
                            EPISODE_PROJECTION,
                            null, null, null);
            cl.setUpdateThrottle(2 * android.text.format.DateUtils.SECOND_IN_MILLIS);
            return cl;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor data) {
            LogWrapper.d(TAG, "[onLoadFinished] size: " + data.getCount());
            updateEpisodeViews(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
        }
    };
}
