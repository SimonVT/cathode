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
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.api.entity.ShowProgress;
import net.simonvt.cathode.api.service.ShowsService;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobFailedException;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.EpisodeWrapper;
import net.simonvt.cathode.provider.ProviderSchematic;
import net.simonvt.cathode.provider.SeasonWrapper;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.Flags;
import timber.log.Timber;

public class SyncShowCollectedStatus extends Job {

  @Inject transient ShowsService showsService;

  private long traktId;

  public SyncShowCollectedStatus(long traktId) {
    super(Flags.REQUIRES_AUTH);
    this.traktId = traktId;
  }

  @Override public String key() {
    return "SyncShowCollectedStatus" + "&traktId=" + traktId;
  }

  @Override public int getPriority() {
    return PRIORITY_USER_DATA;
  }

  @Override public void perform() {
    ContentResolver resolver = getContentResolver();

    ShowProgress progress = showsService.getCollectionProgress(traktId);

    boolean didShowExist = true;
    long showId = ShowWrapper.getShowId(resolver, traktId);
    if (showId == -1L) {
      didShowExist = false;
      showId = ShowWrapper.createShow(resolver, traktId);
      queue(new SyncShow(traktId));
    }

    List<ShowProgress.Season> seasons = progress.getSeasons();

    ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

    for (ShowProgress.Season season : seasons) {
      final int seasonNumber = season.getNumber();

      boolean didSeasonExist = true;
      long seasonId = SeasonWrapper.getSeasonId(resolver, showId, seasonNumber);
      if (seasonId == -1L) {
        didSeasonExist = false;
        seasonId = SeasonWrapper.createSeason(resolver, showId, seasonNumber);
        if (didShowExist) {
          queue(new SyncSeason(traktId, seasonNumber));
        }
      }

      List<ShowProgress.Episode> episodes = season.getEpisodes();
      for (ShowProgress.Episode episode : episodes) {
        final int episodeNumber = episode.getNumber();

        long episodeId =
            EpisodeWrapper.getEpisodeId(getContentResolver(), showId, seasonNumber, episodeNumber);

        if (episodeId == -1L) {
          episodeId = EpisodeWrapper.createEpisode(resolver, showId, seasonId, episodeNumber);
          if (didShowExist && didSeasonExist) {
            queue(new SyncSeason(traktId, seasonNumber));
          }
        }

        ContentProviderOperation.Builder builder =
            ContentProviderOperation.newUpdate(ProviderSchematic.Episodes.withId(episodeId));
        builder.withValue(DatabaseContract.EpisodeColumns.IN_COLLECTION, episode.getCompleted());
        ops.add(builder.build());
      }
    }

    try {
      getContentResolver().applyBatch(BuildConfig.PROVIDER_AUTHORITY, ops);
    } catch (RemoteException e) {
      Timber.e(e, "SyncShowWatchedStatus failed");
      throw new JobFailedException(e);
    } catch (OperationApplicationException e) {
      Timber.e(e, "SyncShowWatchedStatus failed");
      throw new JobFailedException(e);
    }
  }
}
