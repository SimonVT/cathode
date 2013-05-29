package net.simonvt.trakt.ui.fragment;

import butterknife.InjectView;
import butterknife.Views;

import com.squareup.otto.Bus;

import net.simonvt.trakt.R;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.provider.TraktContract.Shows;
import net.simonvt.trakt.scheduler.EpisodeTaskScheduler;
import net.simonvt.trakt.scheduler.ShowTaskScheduler;
import net.simonvt.trakt.sync.TraktTaskQueue;
import net.simonvt.trakt.ui.LibraryType;
import net.simonvt.trakt.ui.ShowsNavigationListener;
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
import android.widget.TextView;

import javax.inject.Inject;

public class ShowInfoFragment extends BaseFragment {

    private static final String TAG = "ShowInfoFragment";

    private static final String ARG_SHOWID = "net.simonvt.trakt.ui.fragment.ShowInfoFragment.showId";
    private static final String ARG_TYPE = "net.simonvt.trakt.ui.fragment.ShowInfoFragment.type";

    private static final int LOADER_SHOW = 10;
    private static final int LOADER_GENRES = 11;
    private static final int LOADER_NEXT_EPISODE = 12;

    private static final String[] SHOW_PROJECTION = new String[] {
            Shows.TITLE,
            Shows.YEAR,
            Shows.AIR_TIME,
            Shows.AIR_DAY,
            Shows.NETWORK,
            Shows.CERTIFICATION,
            Shows.BANNER,
            Shows.RATING_PERCENTAGE,
            Shows.OVERVIEW,
            Shows.IN_WATCHLIST,
            Shows.IN_COLLECTION_COUNT,
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
    //@InjectView(R.id.year) TextView mYear;
    @InjectView(R.id.airtime) TextView mAirTime;
    @InjectView(R.id.certification) TextView mCertification;
    @InjectView(R.id.banner) RemoteImageView mBanner;
    //@InjectView(R.id.rating) TextView mRating;
    @InjectView(R.id.genres) TextView mGenres;
    @InjectView(R.id.overview) TextView mOverview;
    @InjectView(R.id.inCollection) TextView mCollection;
    @InjectView(R.id.inWatchlist) TextView mWatchlist;

    @InjectView(R.id.nextEpisodeContainer) View mNextEpisodeContainer;
    @InjectView(R.id.nextEpisodeDivider) View mNextEpisodeDivider;
    @InjectView(R.id.nextEpisode) TextView mNextEpisode;
    @InjectView(R.id.nextEpisodeBanner) RemoteImageView mNextEpisodeBanner;
    @InjectView(R.id.nextEpisodeTitle) TextView mNextEpisodeTitle;
    @InjectView(R.id.nextEpisodeAirTime) TextView mNextEpisodeAirTime;
    @InjectView(R.id.nextEpisodeEpisode) TextView mNextEpisodeEpisode;
    @InjectView(R.id.nextEpisodeOverflow) OverflowView mNextEpisodeOverflow;

    @Inject ShowTaskScheduler mShowScheduler;
    @Inject EpisodeTaskScheduler mEpisodeScheduler;
    @Inject TraktTaskQueue mQueue;

    @Inject Bus mBus;

    private String mShowTitle;

    private LibraryType mType;

    private long mNextEpisodeId = -1;

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

        mNextEpisodeContainer.setOnClickListener(mNextEpisodeClickListener);

        mNextEpisodeOverflow.setListener(new OverflowView.OverflowActionListener() {
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
                        if (mNextEpisodeId != -1) {
                            mEpisodeScheduler.setWatched(mNextEpisodeId, true);
                        }
                        break;
                }
            }
        });
        mNextEpisodeOverflow.addItem(R.id.action_watched, R.string.action_watched);

        getLoaderManager().initLoader(LOADER_SHOW, null, mLoaderCallbacks);
        getLoaderManager().initLoader(LOADER_GENRES, null, mGenreCallbacks);
        getLoaderManager().initLoader(LOADER_NEXT_EPISODE, null, mNextEpisodeCallbacks);
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
        super.onDestroyView();
        getLoaderManager().destroyLoader(LOADER_SHOW);
        getLoaderManager().destroyLoader(LOADER_GENRES);
        getLoaderManager().destroyLoader(LOADER_NEXT_EPISODE);
    }

    private View.OnClickListener mNextEpisodeClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mNextEpisodeId != -1) mNavigationCallbacks.onDisplayEpisode(mNextEpisodeId, mType);
        }
    };

    private void updateShowView(final Cursor cursor) {
        if (!cursor.moveToFirst()) return;

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
        final int rating = cursor.getInt(cursor.getColumnIndex(Shows.RATING_PERCENTAGE));
        final String overview = cursor.getString(cursor.getColumnIndex(Shows.OVERVIEW));
        final boolean inWatchlist = cursor.getInt(cursor.getColumnIndex(Shows.IN_WATCHLIST)) == 1;
        final int inCollectionCount = cursor.getInt(cursor.getColumnIndex(Shows.IN_COLLECTION_COUNT));

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

    private void updateNextEpisodeViews(final Cursor cursor) {
        if (cursor.moveToFirst()) {
            mNextEpisodeId = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));

            mNextEpisodeContainer.setVisibility(View.VISIBLE);
            mNextEpisodeBanner.setVisibility(View.VISIBLE);
            mNextEpisodeDivider.setVisibility(View.VISIBLE);
            mNextEpisode.setVisibility(View.VISIBLE);

            mNextEpisodeTitle.setText(cursor.getString(cursor.getColumnIndex(TraktContract.Episodes.TITLE)));
            final long airTime = cursor.getLong(cursor.getColumnIndex(TraktContract.Episodes.FIRST_AIRED));
            final String airTimeStr = DateUtils.secondsToDate(getActivity(), airTime);
            mNextEpisodeAirTime.setText(airTimeStr);
            final int season = cursor.getInt(cursor.getColumnIndex(TraktContract.Episodes.SEASON));
            final int episode = cursor.getInt(cursor.getColumnIndex(TraktContract.Episodes.EPISODE));
            mNextEpisodeEpisode.setText("S" + season + "E" + episode);

            final String bannerUrl = cursor.getString(cursor.getColumnIndex(TraktContract.Episodes.SCREEN));
            if (bannerUrl != null) {
                mNextEpisodeBanner.setImage(bannerUrl);
            }
        } else {
            mNextEpisodeContainer.setVisibility(View.GONE);
            mNextEpisodeBanner.setVisibility(View.GONE);
            mNextEpisodeDivider.setVisibility(View.GONE);
            mNextEpisode.setVisibility(View.GONE);
        }
    }

    private LoaderManager.LoaderCallbacks<Cursor> mNextEpisodeCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            StringBuilder where = new StringBuilder();
            switch (mType) {
                case WATCHLIST:
                case WATCHED:
                    where.append(TraktContract.Episodes.WATCHED)
                            .append("=0")
                            .append(" AND ")
                            .append(TraktContract.Episodes.SEASON)
                            .append(">0");
                    break;

                case COLLECTION:
                    where.append(TraktContract.Episodes.IN_COLLECTION)
                            .append("=0")
                            .append(" AND ")
                            .append(TraktContract.Episodes.SEASON)
                            .append(">0");
                    break;
            }
            CursorLoader cl = new CursorLoader(getActivity(), TraktContract.Episodes.buildFromShowId(mShowId),
                    EPISODE_PROJECTION, where.toString(), null,
                    TraktContract.Episodes.EPISODE + " ASC, " + TraktContract.Episodes.SEASON + " ASC LIMIT 1");
            cl.setUpdateThrottle(2 * android.text.format.DateUtils.SECOND_IN_MILLIS);
            return cl;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor data) {
            updateNextEpisodeViews(data);
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
}
