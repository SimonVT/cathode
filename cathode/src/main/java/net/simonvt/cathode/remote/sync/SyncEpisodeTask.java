package net.simonvt.cathode.remote.sync;

import android.content.ContentResolver;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.api.ResponseParser;
import net.simonvt.cathode.api.entity.Episode;
import net.simonvt.cathode.api.entity.Response;
import net.simonvt.cathode.api.service.ShowService;
import net.simonvt.cathode.provider.EpisodeWrapper;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;
import net.simonvt.cathode.util.LogWrapper;
import retrofit.RetrofitError;

public class SyncEpisodeTask extends TraktTask {

  private static final String TAG = "SyncEpisodeTask";

  @Inject transient ShowService showService;

  private final int tvdbId;

  private final int season;

  private final int episode;

  public SyncEpisodeTask(int tvdbId, int season, int episode) {
    this.tvdbId = tvdbId;
    this.season = season;
    this.episode = episode;
  }

  @Override protected void doTask() {
    try {
      LogWrapper.v(TAG, "Syncing episode: " + tvdbId + "-" + season + "-" + episode);

      Episode episode = showService.episodeSummary(tvdbId, season, this.episode).getEpisode();

      final ContentResolver resolver = service.getContentResolver();
      final long showId = ShowWrapper.getShowId(resolver, tvdbId);
      final long seasonId = ShowWrapper.getSeasonId(resolver, showId, season);

      EpisodeWrapper.updateOrInsertEpisode(service.getContentResolver(), episode, showId,
          seasonId);

      postOnSuccess();
    } catch (RetrofitError e) {
      final int statusCode = e.getResponse().getStatus();
      LogWrapper.e(TAG, "URL: " + e.getUrl() + " - Status code: " + statusCode, e);
      if (statusCode == 400) {
        ResponseParser parser = new ResponseParser();
        CathodeApp.inject(service, parser);
        Response response = parser.tryParse(e);
        if (response != null && "episode not found".equals(response.getError())) {
          postOnSuccess();
          return;
        }
      }
    }

    postOnFailure();
  }
}
