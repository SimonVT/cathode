package net.simonvt.trakt.remote.sync;

import retrofit.RetrofitError;

import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.api.ResponseParser;
import net.simonvt.trakt.api.entity.Episode;
import net.simonvt.trakt.api.entity.TraktResponse;
import net.simonvt.trakt.api.service.ShowService;
import net.simonvt.trakt.provider.EpisodeWrapper;
import net.simonvt.trakt.provider.ShowWrapper;
import net.simonvt.trakt.remote.TraktTask;
import net.simonvt.trakt.util.LogWrapper;

import android.content.ContentResolver;

import javax.inject.Inject;

public class SyncEpisodeTask extends TraktTask {

    private static final String TAG = "SyncEpisodeTask";

    @Inject transient ShowService mShowService;

    private final int mTvdbId;

    private final int mSeason;

    private final int mEpisode;

    public SyncEpisodeTask(int tvdbId, int season, int episode) {
        mTvdbId = tvdbId;
        mSeason = season;
        mEpisode = episode;
    }

    @Override
    protected void doTask() {
        LogWrapper.v(TAG, "[doTask]");

        try {
            LogWrapper.v(TAG, "Syncing episode: " + mTvdbId + "-" + mSeason + "-" + mEpisode);

            Episode episode = mShowService.episodeSummary(mTvdbId, mSeason, mEpisode).getEpisode();

            final ContentResolver resolver = mService.getContentResolver();
            final long showId = ShowWrapper.getShowId(resolver, mTvdbId);
            final long seasonId = ShowWrapper.getSeasonId(resolver, showId, mSeason);

            EpisodeWrapper.updateOrInsertEpisode(mService.getContentResolver(), episode, showId, seasonId);

            postOnSuccess();

        } catch (RetrofitError e) {
            final int statusCode = e.getResponse().getStatus();
            LogWrapper.e(TAG, "URL: " + e.getUrl() + " - Status code: " + statusCode, e);
            if (statusCode == 400) {
                ResponseParser parser = new ResponseParser();
                TraktApp.inject(mService, parser);
                TraktResponse response = parser.tryParse(e);
                if (response != null && "episode not found".equals(response.getError())) {
                    postOnSuccess();
                    return;
                }
            }
        }

        postOnFailure();
    }
}
