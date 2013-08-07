package net.simonvt.trakt.remote.sync;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.api.ResponseParser;
import net.simonvt.trakt.api.entity.Episode;
import net.simonvt.trakt.api.entity.TraktResponse;
import net.simonvt.trakt.api.service.ShowService;
import net.simonvt.trakt.provider.EpisodeWrapper;
import net.simonvt.trakt.provider.SeasonWrapper;
import net.simonvt.trakt.provider.ShowWrapper;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.provider.TraktProvider;
import net.simonvt.trakt.remote.TraktTask;
import net.simonvt.trakt.util.LogWrapper;
import retrofit.RetrofitError;

public class SyncSeasonTask extends TraktTask {

  private static final String TAG = "SyncSeasonTask";

  @Inject transient ShowService showService;

  private final int tvdbId;

  private final int season;

  public SyncSeasonTask(int tvdbId, int season) {
    this.tvdbId = tvdbId;
    this.season = season;
  }

  @Override
  protected void doTask() {
    LogWrapper.v(TAG, "[doTask]");

    try {
      LogWrapper.v(TAG, "Syncing season " + season + " of show " + tvdbId);

      List<Episode> episodes = showService.season(tvdbId, season);

      ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

      final ContentResolver resolver = service.getContentResolver();

      for (Episode episode : episodes) {
        if (!(episode.getSeason() == 0 && episode.getNumber() == 0)) {
          Episode summary;
          try {
            summary =
                showService.episodeSummary(tvdbId, season, episode.getNumber()).getEpisode();
          } catch (RetrofitError e) {
            // TODO: Cleanup in aisle 5
            final int statusCode = e.getResponse().getStatus();
            LogWrapper.e(TAG, "URL: " + e.getUrl() + " - Status code: " + statusCode, e);
            if (statusCode == 400) {
              ResponseParser parser = new ResponseParser();
              TraktApp.inject(service, parser);
              TraktResponse response = parser.tryParse(e);
              if (response != null && "episode not found".equals(response.getError())) {
                postOnSuccess();
                return;
              }
            }
            continue;
          }

          final long showId = ShowWrapper.getShowId(resolver, tvdbId);
          final long seasonId = ShowWrapper.getSeasonId(resolver, showId, season);
          final long id =
              EpisodeWrapper.getEpisodeId(resolver, showId, season, episode.getNumber());
          final boolean exists = id != -1L;

          ContentProviderOperation.Builder builder;
          if (exists) {
            builder = ContentProviderOperation.newUpdate(TraktContract.Episodes.buildFromId(id));
          } else {
            builder = ContentProviderOperation.newInsert(TraktContract.Episodes.CONTENT_URI);
          }

          ContentValues cv = EpisodeWrapper.getEpisodeCVs(summary);
          cv.put(TraktContract.Episodes.SHOW_ID, showId);
          cv.put(TraktContract.Episodes.SEASON_ID, seasonId);
          builder.withValues(cv);

          ops.add(builder.build());
        }
      }

      resolver.applyBatch(TraktProvider.AUTHORITY, ops);

      SeasonWrapper.updateSeasonCounts(resolver, tvdbId, season);
      ShowWrapper.updateShowCounts(resolver, tvdbId);

      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      LogWrapper.d(TAG, "[RetrofitError] " + e.getUrl());
      postOnFailure();
    } catch (RemoteException e) {
      e.printStackTrace();
      postOnFailure();
    } catch (OperationApplicationException e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
