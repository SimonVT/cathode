package net.simonvt.trakt.remote.sync;

import javax.inject.Inject;
import net.simonvt.trakt.api.entity.LastActivity;
import net.simonvt.trakt.api.service.UserService;
import net.simonvt.trakt.remote.TraktTask;
import net.simonvt.trakt.settings.ActivityWrapper;
import net.simonvt.trakt.util.LogWrapper;
import retrofit.RetrofitError;

public class SyncTask extends TraktTask {

  private static final String TAG = "SyncTask";

  @Inject transient UserService userService;

  @Override
  protected void doTask() {
    LogWrapper.v(TAG, "[doTask]");

    try {
      queueTask(new SyncUpdatedShows());
      queueTask(new SyncUpdatedMovies());

      LastActivity lastActivity = userService.lastActivity();

      long episodeLastWatched = lastActivity.getEpisode().getWatched();
      long episodeLastCollected = lastActivity.getEpisode().getCollection();
      long episodeLastWatchlist = lastActivity.getEpisode().getWatchlist();

      long showLastWatchlist = lastActivity.getShow().getWatchlist();

      long movieLastWatched = lastActivity.getMovie().getWatched();
      long movieLastCollected = lastActivity.getMovie().getCollection();
      long movieLastWatchlist = lastActivity.getMovie().getWatchlist();

      if (ActivityWrapper.episodeWatchedNeedsUpdate(service, episodeLastWatched)) {
        queueTask(new SyncShowsWatchedTask());
      }

      if (ActivityWrapper.episodeCollectedNeedsUpdate(service, episodeLastCollected)) {
        queueTask(new SyncShowsCollectionTask());
      }

      if (ActivityWrapper.episodeWatchlistNeedsUpdate(service, episodeLastWatchlist)) {
        queueTask(new SyncEpisodeWatchlistTask());
      }

      if (ActivityWrapper.showWatchlistNeedsUpdate(service, showLastWatchlist)) {
        queueTask(new SyncShowsWatchlistTask());
      }

      if (ActivityWrapper.movieWatchedNeedsUpdate(service, movieLastWatched)) {
        queueTask(new SyncMoviesWatchedTask());
      }

      if (ActivityWrapper.movieCollectedNeedsUpdate(service, movieLastCollected)) {
        queueTask(new SyncMoviesCollectionTask());
      }

      if (ActivityWrapper.movieWatchlistNeedsUpdate(service, movieLastWatchlist)) {
        queueTask(new SyncMoviesWatchlistTask());
      }

      ActivityWrapper.update(service, lastActivity);

      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
