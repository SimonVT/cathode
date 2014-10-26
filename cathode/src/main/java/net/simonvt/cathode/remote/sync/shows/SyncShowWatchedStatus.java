/*
 * Copyright (C) 2014 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.simonvt.cathode.remote.sync.shows;

import android.content.ContentProviderOperation;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.api.entity.ShowProgress;
import net.simonvt.cathode.api.service.ShowsService;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.EpisodeWrapper;
import net.simonvt.cathode.provider.ProviderSchematic;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;
import timber.log.Timber;

public class SyncShowWatchedStatus extends TraktTask {

  @Inject transient ShowsService showsService;

  private long traktId;

  public SyncShowWatchedStatus(long traktId) {
    this.traktId = traktId;
  }

  @Override protected void doTask() {
    if (traktId == 225) {
      postOnSuccess();
      return;
    }
    ShowProgress progress = showsService.getWatchedProgress(traktId);
    final long showId = ShowWrapper.getShowId(getContentResolver(), traktId);

    List<ShowProgress.Season> seasons = progress.getSeasons();

    ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

    for (ShowProgress.Season season : seasons) {
      final int seasonNumber = season.getNumber();

      List<ShowProgress.Episode> episodes = season.getEpisodes();
      for (ShowProgress.Episode episode : episodes) {
        final long episodeId =
            EpisodeWrapper.getEpisodeId(getContentResolver(), showId, seasonNumber,
                episode.getNumber());
        ContentProviderOperation.Builder builder =
            ContentProviderOperation.newUpdate(ProviderSchematic.Episodes.withId(episodeId));
        builder.withValue(DatabaseContract.EpisodeColumns.WATCHED, episode.getCompleted());
        ops.add(builder.build());
      }
    }

    try {
      getContentResolver().applyBatch(BuildConfig.PROVIDER_AUTHORITY, ops);
    } catch (RemoteException e) {
      e.printStackTrace();
      Timber.e(e, "SyncShowWatchedStatus failed");
    } catch (OperationApplicationException e) {
      e.printStackTrace();
      Timber.e(e, "SyncShowWatchedStatus failed");
    }

    postOnSuccess();
  }
}
