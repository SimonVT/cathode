package net.simonvt.trakt.ui.fragment;

import butterknife.InjectView;

import com.squareup.otto.Bus;

import net.simonvt.trakt.R;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.scheduler.SeasonTaskScheduler;
import net.simonvt.trakt.ui.LibraryType;
import net.simonvt.trakt.ui.ShowsNavigationListener;
import net.simonvt.trakt.ui.adapter.SeasonsAdapter;
import net.simonvt.trakt.widget.RemoteImageView;

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
import android.widget.CursorAdapter;
import android.widget.TextView;

import javax.inject.Inject;

public class SeasonsFragment extends AbsAdapterFragment {

    private static final String TAG = "SeasonsFragment";

    private static final String ARG_SHOW_ID = "net.simonvt.trakt.ui.fragment.SeasonsFragment.showId";
    private static final String ARG_TYPE = "net.simonvt.trakt.ui.fragment.SeasonsFragment.type";

    private static final String STATE_SHOW_TITLE = "net.simonvt.trakt.ui.fragment.SeasonsFragment.showTitle";
    private static final String STATE_SHOW_BANNER = "net.simonvt.trakt.ui.fragment.SeasonsFragment.showBanner";

    private static final int LOADER_SEASONS = 20;

    private CursorAdapter mSeasonsAdapter;

    private long mShowId;

    private LibraryType mType;

    private String mTitle;

    private String mBannerUrl;

    private ShowsNavigationListener mDisplaySeasonListener;

    @Inject SeasonTaskScheduler mSeasonScheduler;

    @Inject Bus mBus;

    @InjectView(R.id.title) TextView mShowTitle;
    @InjectView(R.id.banner) RemoteImageView mShowBanner;

    public static Bundle getArgs(long showId, LibraryType type) {
        Bundle args = new Bundle();
        args.putLong(ARG_SHOW_ID, showId);
        args.putSerializable(ARG_TYPE, type);
        return args;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mDisplaySeasonListener = (ShowsNavigationListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement ShowsNavigationListener");
        }
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        TraktApp.inject(getActivity(), this);

        Bundle args = getArguments();
        mShowId = args.getLong(ARG_SHOW_ID);
        mType = (LibraryType) args.getSerializable(ARG_TYPE);

        if (state != null) {
            mTitle = state.getString(STATE_SHOW_TITLE);
            mBannerUrl = state.getString(STATE_SHOW_BANNER);
        }

        mSeasonsAdapter = new SeasonsAdapter(getActivity(), mType);
        setAdapter(mSeasonsAdapter);
        getLoaderManager().initLoader(LOADER_SEASONS, null, mSeasonsLoader);

        if (mTitle == null) {
            CursorLoader loader =
                    new CursorLoader(getActivity(), TraktContract.Shows.buildShowUri(mShowId), new String[] {
                            TraktContract.Shows.TITLE,
                            TraktContract.Shows.BANNER,
                    }, null, null, null);
            loader.registerListener(0, new Loader.OnLoadCompleteListener<Cursor>() {
                @Override
                public void onLoadComplete(Loader<Cursor> cursorLoader, Cursor cursor) {
                    cursorLoader.stopLoading();

                    cursor.moveToFirst();
                    mTitle = cursor.getString(cursor.getColumnIndex(TraktContract.Shows.TITLE));
                    mBannerUrl = cursor.getString(cursor.getColumnIndex(TraktContract.Shows.BANNER));
                    cursor.close();

                    if (mShowTitle != null) mShowTitle.setText(mTitle);
                    if (mShowBanner != null) mShowBanner.setImage(mBannerUrl);
                }
            });
            loader.startLoading();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SHOW_TITLE, mTitle);
        outState.putString(STATE_SHOW_BANNER, mBannerUrl);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_seasons, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mShowTitle.setText(mTitle);
        mShowBanner.setImage(mBannerUrl);
    }

    @Override
    public void onDestroy() {
        getLoaderManager().destroyLoader(LOADER_SEASONS);
        super.onDestroy();
    }

    @Override
    protected void onItemClick(AdapterView l, View v, int position, long id) {
        Cursor c = (Cursor) getAdapter().getItem(position);
        mDisplaySeasonListener.onDisplaySeason(mShowId, id, mTitle, c.getInt(c.getColumnIndex(
                TraktContract.Seasons.SEASON)), mType);
    }

    private LoaderManager.LoaderCallbacks<Cursor> mSeasonsLoader = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            CursorLoader cl =
                    new CursorLoader(getActivity(), TraktContract.Seasons.buildFromShowId(mShowId), null, null, null,
                            TraktContract.Seasons.DEFAULT_SORT);
            cl.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
            return cl;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor data) {
            mSeasonsAdapter.changeCursor(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
            mSeasonsAdapter.changeCursor(null);
        }
    };

}
