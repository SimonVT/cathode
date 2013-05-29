package net.simonvt.trakt.ui.fragment;

import butterknife.InjectView;
import butterknife.Views;

import net.simonvt.trakt.R;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.util.LogWrapper;
import net.simonvt.trakt.util.DateUtils;
import net.simonvt.trakt.widget.RemoteImageView;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class EpisodeFragment extends BaseFragment {

    private static final String TAG = "EpisodeFragment";

    private static final String ARG_EPISODEID = "net.simonvt.trakt.ui.fragment.EpisodeFragment.episodeId";

    private static final int LOADER_EPISODE = 30;

    private long mEpisodeId;

    @InjectView(R.id.title) TextView mTitle;
    @InjectView(R.id.screen) RemoteImageView mScreen;
    @InjectView(R.id.overview) TextView mOverview;
    @InjectView(R.id.firstAired) TextView mFirstAired;

    public static EpisodeFragment newInstance(long episodeId) {
        EpisodeFragment f = new EpisodeFragment();

        Bundle args = new Bundle();
        args.putLong(ARG_EPISODEID, episodeId);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mEpisodeId = args.getLong(ARG_EPISODEID);
        getLoaderManager().initLoader(LOADER_EPISODE, null, mEpisodeCallbacks);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_episode, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Views.inject(this, view);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        getLoaderManager().destroyLoader(LOADER_EPISODE);
        super.onDestroy();
    }

    private void updateEpisodeViews(final Cursor cursor) {
        if (cursor.moveToFirst()) {
            mTitle.setText(cursor.getString(cursor.getColumnIndex(TraktContract.Episodes.TITLE)));
            mOverview.setText(cursor.getString(cursor.getColumnIndex(TraktContract.Episodes.OVERVIEW)));
            mScreen.setImage(cursor.getString(cursor.getColumnIndex(TraktContract.Episodes.SCREEN)));
            mFirstAired.setText(DateUtils.secondsToDate(getActivity(),
                    cursor.getLong(cursor.getColumnIndex(TraktContract.Episodes.FIRST_AIRED))));
        }
    }

    private static final String[] EPISODE_PROJECTION = new String[] {
            TraktContract.Episodes.TITLE,
            TraktContract.Episodes.SCREEN,
            TraktContract.Episodes.OVERVIEW,
            TraktContract.Episodes.FIRST_AIRED,
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
