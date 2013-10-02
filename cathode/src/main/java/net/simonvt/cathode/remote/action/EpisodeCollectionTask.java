package net.simonvt.cathode.remote.action;

import javax.inject.Inject;
import net.simonvt.cathode.api.body.ShowEpisodeBody;
import net.simonvt.cathode.api.entity.Response;
import net.simonvt.cathode.api.service.ShowService;
import net.simonvt.cathode.provider.EpisodeWrapper;
import net.simonvt.cathode.remote.TraktTask;
import net.simonvt.cathode.util.LogWrapper;
import retrofit.RetrofitError;

public class EpisodeCollectionTask extends TraktTask {

  private static final String TAG = "EpisodeCollectionTask";

  @Inject transient ShowService showService;

  private final int tvdbId;

  private final int season;

  private final int episode;

  private final boolean inCollection;

  public EpisodeCollectionTask(int tvdbId, int season, int episode, boolean inCollection) {
    this.tvdbId = tvdbId;
    this.season = season;
    this.episode = episode;
    this.inCollection = inCollection;
  }

  @Override
  protected void doTask() {
    LogWrapper.v(TAG, "[doTask]");

    try {
      if (inCollection) {
        Response response =
            showService.episodeLibrary(new ShowEpisodeBody(tvdbId, season, episode));
      } else {
        Response response =
            showService.episodeUnlibrary(new ShowEpisodeBody(tvdbId, season, episode));
      }

      EpisodeWrapper.setInCollection(service.getContentResolver(), tvdbId, season, episode,
          inCollection);

      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
