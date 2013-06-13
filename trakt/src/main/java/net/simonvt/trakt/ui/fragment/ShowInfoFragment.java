package net.simonvt.trakt.ui.fragment;

import butterknife.InjectView;
import butterknife.Views;

import com.squareup.otto.Bus;

import net.simonvt.trakt.R;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.provider.CollectLoader;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.provider.TraktContract.Shows;
import net.simonvt.trakt.provider.WatchedLoader;
import net.simonvt.trakt.scheduler.EpisodeTaskScheduler;
import net.simonvt.trakt.scheduler.ShowTaskScheduler;
import net.simonvt.trakt.sync.TraktTaskQueue;
import net.simonvt.trakt.ui.LibraryType;
import net.simonvt.trakt.ui.ShowsNavigationListener;
import net.simonvt.trakt.ui.dialog.RatingDialog;
import net.simonvt.trakt.util.DateUtils;
import net.simonvt.trakt.widget.OverflowView;
import net.simonvt.trakt.widget.RemoteImageView;

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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import javax.inject.Inject;

public class ShowInfoFragment extends BaseFragment {

    private static final String TAG = "ShowInfoFragment";

    private static final String ARG_SHOWID = "net.simonvt.trakt.ui.fragment.ShowInfoFragment.showId";
    private static final String ARG_TYPE = "net.simonvt.trakt.ui.fragment.ShowInfoFragment.type";

    private static final String DIALOG_RATING = "net.simonvt.trakt.ui.fragment.ShowInfoFragment.ratingDialog";

    private static final int LOADER_SHOW = 10;
    private static final int LOADER_GENRES = 11;
    private static final int LOADER_WATCH = 12;
    private static final int LOADER_COLLECT = 13;

    private static final String[] SHOW_PROJECTION = new String[] {
            Shows.TITLE,
            Shows.YEAR,
            Shows.AIR_TIME,
            Shows.AIR_DAY,
            Shows.NETWORK,
            Shows.CERTIFICATION,
            Shows.BANNER,
            Shows.RATING_PERCENTAGE,
            Shows.RATING,
            Shows.OVERVIEW,
            Shows.IN_WATCHLIST,
            Shows.IN_COLLECTION_COUNT,
            Shows.WATCHED_COUNT,
    };

    private static final String[] EPISODE_PROJECTION = new String[] {
            BaseColumns._ID,
            TraktContract.Episodes.TITLE,
            TraktContract.Episodes.SCREEN,
            TraktContract.Episodes.FIRST_AIRED,
            TraktContract.Episodes.SEASON,
            TraktContract.Episodes.EPISODE,
    };

    private static final String[] GENRES_PROJECTION = new String[] {
            TraktContract.ShowGenres.GENRE,
    };

    private ShowsNavigationListener mNavigationCallbacks;

    private long mShowId;

    @InjectView(R.id.title) TextView mTitle;
    @InjectView(R.id.ratingContainer) View mRatingContainer;
    @InjectView(R.id.rating) RatingBar mRating;
    @InjectView(R.id.allRatings) TextView mAllRatings;
    //@InjectView(R.id.year) TextView mYear;
    @InjectView(R.id.airtime) TextView mAirTime;
    @InjectView(R.id.certification) TextView mCertification;
    @InjectView(R.id.banner) RemoteImageView mBanner;
    @InjectView(R.id.genres) TextView mGenres;
    @InjectView(R.id.overview) TextView mOverview;
    @InjectView(R.id.inCollection) TextView mCollection;
    @InjectView(R.id.inWatchlist) TextView mWatchlist;

    @InjectView(R.id.episodesTitle) View mEpisodesTitle;
    @InjectView(R.id.episodes) LinearLayout mEpisodes;

    @InjectView(R.id.watchTitle) View mWatchTitle;
    @InjectView(R.id.collectTitle) View mCollectTitle;

    @InjectView(R.id.toWatch) View mToWatch;
    private EpisodeHolder mToWatchHolder;
    private long mToWatchId = -1;

    @InjectView(R.id.lastWatched) View mLastWatched;
    private EpisodeHolder mLastWatchedHolder;
    private long mLastWatchedId = -1;

    @InjectView(R.id.toCollect) View mToCollect;
    private EpisodeHolder mToCollectHolder;
    private long mToCollectId = -1;

    @InjectView(R.id.lastCollected) View mLastCollected;
    private EpisodeHolder mLastCollectedHolder;
    private long mLastCollectedId = -1;

    static class EpisodeHolder {

        @InjectView(R.id.episode) TextView mEpisode;
        @InjectView(R.id.episodeBanner) RemoteImageView mEpisodeBanner;
        @InjectView(R.id.episodeTitle) TextView mEpisodeTitle;
        @InjectView(R.id.episodeAirTime) TextView mEpisodeAirTime;
        @InjectView(R.id.episodeEpisode) TextView mEpisodeEpisode;
        @InjectView(R.id.episodeOverflow) OverflowView mEpisodeOverflow;

        public EpisodeHolder(View v) {
            Views.inject(this, v);
        }
    }

    @Inject ShowTaskScheduler mShowScheduler;
    @Inject EpisodeTaskScheduler mEpisodeScheduler;
    @Inject TraktTaskQueue mQueue;

    @Inject Bus mBus;

    private String mShowTitle;

    private int mCurrentRating;

    private LibraryType mType;

    public static ShowInfoFragment newInstance(long showId, LibraryType type) {
        ShowInfoFragment f = new ShowInfoFragment();

        Bundle args = new Bundle();
        args.putLong(ARG_SHOWID, showId);
        args.putSerializable(ARG_TYPE, type);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mNavigationCallbacks = (ShowsNavigationListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement ShowsNavigationListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TraktApp.inject(getActivity(), this);

        setHasOptionsMenu(true);

        Bundle args = getArguments();
        mShowId = args.getLong(ARG_SHOWID);
        mType = (LibraryType) args.getSerializable(ARG_TYPE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_show_info, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Views.inject(this, view);

        mRatingContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RatingDialog.newInstance(RatingDialog.Type.SHOW, mShowId, mCurrentRating)
                        .show(getFragmentManager(), DIALOG_RATING);
            }
        });

        if (mToWatch != null) {
            mToWatchHolder = new EpisodeHolder(mToWatch);
            mToWatch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mToWatchId != -1) mNavigationCallbacks.onDisplayEpisode(mToWatchId, mType);
                }
            });

            mToWatchHolder.mEpisodeOverflow.addItem(R.id.action_watched, R.string.action_watched);
            mToWatchHolder.mEpisodeOverflow.setListener(new OverflowView.OverflowActionListener() {
                @Override
                public void onPopupShown() {
                }

                @Override
                public void onPopupDismissed() {
                }

                @Override
                public void onActionSelected(int action) {
                    switch (action) {
                        case R.id.action_watched:
                            if (mToWatchId != -1) {
                                mEpisodeScheduler.setWatched(mToWatchId, true);
                            }
                            break;
                    }
                }
            });
        }

        if (mLastWatched != null) {
            mLastWatchedHolder = new EpisodeHolder(mLastWatched);
            mLastWatched.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mLastWatchedId != -1) mNavigationCallbacks.onDisplayEpisode(mLastWatchedId, mType);
                }
            });

            mLastWatchedHolder.mEpisodeOverflow.addItem(R.id.action_unwatched, R.string.action_unwatched);
            mLastWatchedHolder.mEpisodeOverflow.setListener(new OverflowView.OverflowActionListener() {
                @Override
                public void onPopupShown() {
                }

                @Override
                public void onPopupDismissed() {
                }

                @Override
                public void onActionSelected(int action) {
                    switch (action) {
                        case R.id.action_watched:
                            if (mLastWatchedId != -1) {
                                mEpisodeScheduler.setWatched(mLastWatchedId, true);
                            }
                            break;
                    }
                }
            });
        }

        if (mToCollect != null) {
            mToCollectHolder = new EpisodeHolder(mToCollect);
            mToCollect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mToCollectId != -1) mNavigationCallbacks.onDisplayEpisode(mToCollectId, mType);
                }
            });

            mToCollectHolder.mEpisodeOverflow.addItem(R.id.action_collection_add, R.string.action_collection_add);
            mToCollectHolder.mEpisodeOverflow.setListener(new OverflowView.OverflowActionListener() {
                @Override
                public void onPopupShown() {
                }

                @Override
                public void onPopupDismissed() {
                }

                @Override
                public void onActionSelected(int action) {
                    switch (action) {
                        case R.id.action_watched:
                            if (mToCollectId != -1) {
                                mEpisodeScheduler.setWatched(mToCollectId, true);
                            }
                            break;
                    }
                }
            });
        }

        if (mLastCollected != null) {
            mLastCollectedHolder = new EpisodeHolder(mLastCollected);
            mLastCollected.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mLastCollectedId != -1) mNavigationCallbacks.onDisplayEpisode(mLastCollectedId, mType);
                }
            });

            mLastCollectedHolder.mEpisodeOverflow
                    .addItem(R.id.action_collection_remove, R.string.action_collection_remove);
            mLastCollectedHolder.mEpisodeOverflow.setListener(new OverflowView.OverflowActionListener() {
                @Override
                public void onPopupShown() {
                }

                @Override
                public void onPopupDismissed() {
                }

                @Override
                public void onActionSelected(int action) {
                    switch (action) {
                        case R.id.action_watched:
                            if (mLastCollectedId != -1) {
                                mEpisodeScheduler.setWatched(mLastCollectedId, true);
                            }
                            break;
                    }
                }
            });
        }

        getLoaderManager().initLoader(LOADER_SHOW, null, mLoaderCallbacks);
        getLoaderManager().initLoader(LOADER_GENRES, null, mGenreCallbacks);
        getLoaderManager().initLoader(LOADER_WATCH, null, mEpisodeWatchCallbacks);
        getLoaderManager().initLoader(LOADER_COLLECT, null, mEpisodeCollectCallbacks);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_show_info, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_seasons:
                mNavigationCallbacks.onDisplaySeasons(mShowId, mType);
                return true;
        }

        return false;
    }

    @Override
    public void onDestroyView() {
        getLoaderManager().destroyLoader(LOADER_SHOW);
        getLoaderManager().destroyLoader(LOADER_GENRES);
        getLoaderManager().destroyLoader(LOADER_WATCH);
        getLoaderManager().destroyLoader(LOADER_COLLECT);
        super.onDestroyView();
    }

    private void updateShowView(final Cursor cursor) {
        if (cursor == null || !cursor.moveToFirst()) return;

        mShowTitle = cursor.getString(cursor.getColumnIndex(Shows.TITLE));
        final int year = cursor.getInt(cursor.getColumnIndex(Shows.YEAR));
        final String airTime = cursor.getString(cursor.getColumnIndex(Shows.AIR_TIME));
        final String airDay = cursor.getString(cursor.getColumnIndex(Shows.AIR_DAY));
        final String network = cursor.getString(cursor.getColumnIndex(Shows.NETWORK));
        final String certification = cursor.getString(cursor.getColumnIndex(Shows.CERTIFICATION));
        final String bannerUrl = cursor.getString(cursor.getColumnIndex(Shows.BANNER));
        if (bannerUrl != null) {
            mBanner.setImage(bannerUrl);
        }
        mCurrentRating = cursor.getInt(cursor.getColumnIndex(Shows.RATING));
        final int ratingAll = cursor.getInt(cursor.getColumnIndex(Shows.RATING_PERCENTAGE));
        final String overview = cursor.getString(cursor.getColumnIndex(Shows.OVERVIEW));
        final boolean inWatchlist = cursor.getInt(cursor.getColumnIndex(Shows.IN_WATCHLIST)) == 1;
        final int inCollectionCount = cursor.getInt(cursor.getColumnIndex(Shows.IN_COLLECTION_COUNT));
        final int watchedCount = cursor.getInt(cursor.getColumnIndex(Shows.WATCHED_COUNT));

        mRating.setProgress(mCurrentRating);
        mAllRatings.setText(ratingAll + "%");

        mCollection.setVisibility(inCollectionCount > 0 ? View.VISIBLE : View.GONE);
        mWatchlist.setVisibility(inWatchlist ? View.VISIBLE : View.GONE);

        // mTitle.setText(title);
        // mYear.setText(String.valueOf(year));
        mAirTime.setText(airDay + " " + airTime + ", " + network);
        mCertification.setText(certification);
        // mRating.setText(String.valueOf(rating));
        mOverview.setText(overview);
    }

    private void updateGenreViews(final Cursor cursor) {
        StringBuilder sb = new StringBuilder();
        final int genreColumnIndex = cursor.getColumnIndex(TraktContract.ShowGenres.GENRE);

        while (cursor.moveToNext()) {
            sb.append(cursor.getString(genreColumnIndex));
            if (!cursor.isLast()) sb.append(", ");
        }

        mGenres.setText(sb.toString());
    }

    private void updateEpisodeWatchViews(Cursor cursor) {
        if (cursor.moveToFirst()) {
            mToWatch.setVisibility(View.VISIBLE);
            mWatchTitle.setVisibility(View.VISIBLE);

            mToWatchId = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));

            mToWatchHolder.mEpisodeTitle.setText(cursor.getString(cursor.getColumnIndex(TraktContract.Episodes.TITLE)));

            final long airTime = cursor.getLong(cursor.getColumnIndex(TraktContract.Episodes.FIRST_AIRED));
            final String airTimeStr = DateUtils.secondsToDate(getActivity(), airTime);
            mToWatchHolder.mEpisodeAirTime.setText(airTimeStr);

            final int season = cursor.getInt(cursor.getColumnIndex(TraktContract.Episodes.SEASON));
            final int episode = cursor.getInt(cursor.getColumnIndex(TraktContract.Episodes.EPISODE));
            mToWatchHolder.mEpisodeEpisode.setText("S" + season + "E" + episode);

            final String bannerUrl = cursor.getString(cursor.getColumnIndex(TraktContract.Episodes.SCREEN));
            mToWatchHolder.mEpisodeBanner.setImage(bannerUrl);
        } else {
            mToWatch.setVisibility(View.GONE);
            mWatchTitle.setVisibility(View.GONE);
            mToWatchId = -1;
        }

        if (mLastWatched != null) {
            if (cursor.moveToNext()) {
                mLastWatched.setVisibility(View.VISIBLE);

                mLastWatchedId = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));

                mLastWatchedHolder.mEpisodeTitle
                        .setText(cursor.getString(cursor.getColumnIndex(TraktContract.Episodes.TITLE)));

                final long airTime = cursor.getLong(cursor.getColumnIndex(TraktContract.Episodes.FIRST_AIRED));
                final String airTimeStr = DateUtils.secondsToDate(getActivity(), airTime);
                mLastWatchedHolder.mEpisodeAirTime.setText(airTimeStr);

                final int season = cursor.getInt(cursor.getColumnIndex(TraktContract.Episodes.SEASON));
                final int episode = cursor.getInt(cursor.getColumnIndex(TraktContract.Episodes.EPISODE));
                mLastWatchedHolder.mEpisodeEpisode.setText("S" + season + "E" + episode);

                final String bannerUrl = cursor.getString(cursor.getColumnIndex(TraktContract.Episodes.SCREEN));
                mLastWatchedHolder.mEpisodeBanner.setImage(bannerUrl);
            } else {
                mLastWatched.setVisibility(View.GONE);
                mLastWatchedId = -1;
            }
        }

        if (mToWatchId == -1 && mLastWatchedId == -1 && mToCollectId == -1 && mLastCollectedId == -1) {
            mEpisodes.setVisibility(View.GONE);
        } else {
            mEpisodes.setVisibility(View.VISIBLE);
        }
    }

    private void updateEpisodeCollectViews(Cursor cursor) {
        if (cursor.moveToFirst()) {
            mToCollect.setVisibility(View.VISIBLE);
            mCollectTitle.setVisibility(View.VISIBLE);

            mToCollectId = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));

            mToCollectHolder.mEpisodeTitle
                    .setText(cursor.getString(cursor.getColumnIndex(TraktContract.Episodes.TITLE)));

            final long airTime = cursor.getLong(cursor.getColumnIndex(TraktContract.Episodes.FIRST_AIRED));
            final String airTimeStr = DateUtils.secondsToDate(getActivity(), airTime);
            mToCollectHolder.mEpisodeAirTime.setText(airTimeStr);

            final int season = cursor.getInt(cursor.getColumnIndex(TraktContract.Episodes.SEASON));
            final int episode = cursor.getInt(cursor.getColumnIndex(TraktContract.Episodes.EPISODE));
            mToCollectHolder.mEpisodeEpisode.setText("S" + season + "E" + episode);

            final String bannerUrl = cursor.getString(cursor.getColumnIndex(TraktContract.Episodes.SCREEN));
            mToCollectHolder.mEpisodeBanner.setImage(bannerUrl);
        } else {
            mToCollect.setVisibility(View.GONE);
            mCollectTitle.setVisibility(View.GONE);
            mToCollectId = -1;
        }

        if (mLastCollected != null) {
            if (cursor.moveToNext()) {
                mLastCollected.setVisibility(View.VISIBLE);

                mLastCollectedId = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));

                mLastCollectedHolder.mEpisodeTitle
                        .setText(cursor.getString(cursor.getColumnIndex(TraktContract.Episodes.TITLE)));

                final long airTime = cursor.getLong(cursor.getColumnIndex(TraktContract.Episodes.FIRST_AIRED));
                final String airTimeStr = DateUtils.secondsToDate(getActivity(), airTime);
                mLastCollectedHolder.mEpisodeAirTime.setText(airTimeStr);

                final int season = cursor.getInt(cursor.getColumnIndex(TraktContract.Episodes.SEASON));
                final int episode = cursor.getInt(cursor.getColumnIndex(TraktContract.Episodes.EPISODE));
                mLastCollectedHolder.mEpisodeEpisode.setText("S" + season + "E" + episode);

                final String bannerUrl = cursor.getString(cursor.getColumnIndex(TraktContract.Episodes.SCREEN));
                mLastCollectedHolder.mEpisodeBanner.setImage(bannerUrl);
            } else {
                mLastCollectedId = -1;
                mLastCollected.setVisibility(View.GONE);
            }
        }

        if (mToWatchId == -1 && mLastWatchedId == -1 && mToCollectId == -1 && mLastCollectedId == -1) {
            mEpisodes.setVisibility(View.GONE);
        } else {
            mEpisodes.setVisibility(View.VISIBLE);
        }
    }

    private LoaderManager.LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            CursorLoader cl =
                    new CursorLoader(getActivity(), Shows.buildShowUri(mShowId), SHOW_PROJECTION, null, null, null);
            cl.setUpdateThrottle(2 * android.text.format.DateUtils.SECOND_IN_MILLIS);
            return cl;

        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor data) {
            updateShowView(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
        }
    };

    private LoaderManager.LoaderCallbacks<Cursor> mGenreCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            CursorLoader cl = new CursorLoader(getActivity(), TraktContract.ShowGenres.buildFromShowUri(mShowId),
                    GENRES_PROJECTION, null, null, TraktContract.ShowGenres.DEFAULT_SORT);
            cl.setUpdateThrottle(2 * android.text.format.DateUtils.SECOND_IN_MILLIS);
            return cl;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor data) {
            updateGenreViews(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
        }
    };

    private LoaderManager.LoaderCallbacks<Cursor> mEpisodeWatchCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            return new WatchedLoader(getActivity(), mShowId);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            updateEpisodeWatchViews(cursor);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
        }
    };

    private LoaderManager.LoaderCallbacks<Cursor> mEpisodeCollectCallbacks =
            new LoaderManager.LoaderCallbacks<Cursor>() {
                @Override
                public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
                    return new CollectLoader(getActivity(), mShowId);
                }

                @Override
                public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
                    updateEpisodeCollectViews(cursor);
                }

                @Override
                public void onLoaderReset(Loader<Cursor> cursorLoader) {
                }
            };
}
