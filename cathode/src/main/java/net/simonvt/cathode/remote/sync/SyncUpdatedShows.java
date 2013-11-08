package net.simonvt.cathode.remote.sync;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.ServerTime;
import net.simonvt.cathode.api.entity.UpdatedShows;
import net.simonvt.cathode.api.service.ServerService;
import net.simonvt.cathode.api.service.ShowsService;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;
import net.simonvt.cathode.settings.Settings;
import retrofit.RetrofitError;

public class SyncUpdatedShows extends TraktTask {

  private static final String TAG = "SyncUpdatedShows";

  @Inject transient ShowsService showsService;

  @Inject transient ServerService serverService;

  @Override protected void doTask() {
    try {
      SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(service);
      final long showsLastUpdated = settings.getLong(Settings.SHOWS_LAST_UPDATED, 0);

      long current;

      if (showsLastUpdated > 0) {
        UpdatedShows updatedShows = showsService.updated(showsLastUpdated);
        current = updatedShows.getTimestamps().getCurrent();

        List<UpdatedShows.ShowTimestamp> timestamps = updatedShows.getShows();
        for (UpdatedShows.ShowTimestamp timestamp : timestamps) {
          final int tvdbId = timestamp.getTvdbId();
          final boolean exists = ShowWrapper.exists(service.getContentResolver(), tvdbId);
          if (exists) {
            final boolean needsUpdate =
                ShowWrapper.needsUpdate(service.getContentResolver(), tvdbId,
                    timestamp.getLastUpdated());
            if (needsUpdate) {
              queueTask(new SyncShowTask(tvdbId));
            }
          }
        }
      } else {
        queueTask(new SyncShowsTask());
        ServerTime time = serverService.time();
        current = time.getTimestamp();
      }

      settings.edit().putLong(Settings.SHOWS_LAST_UPDATED, current).apply();

      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
