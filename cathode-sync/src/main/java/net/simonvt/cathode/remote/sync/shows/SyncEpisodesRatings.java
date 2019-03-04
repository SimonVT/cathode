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
import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.RatingItem;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.common.database.Cursors;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper;
import net.simonvt.cathode.provider.helper.SeasonDatabaseHelper;
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import retrofit2.Call;

public class SyncEpisodesRatings extends CallJob<List<RatingItem>> {

  @Inject transient SyncService syncService;

  @Inject transient ShowDatabaseHelper showHelper;
  @Inject transient SeasonDatabaseHelper seasonHelper;
  @Inject transient EpisodeDatabaseHelper episodeHelper;

  public SyncEpisodesRatings() {
    super(Flags.REQUIRES_AUTH);
  }

  @Override public String key() {
    return "SyncEpisodeRatings";
  }

  @Override public int getPriority() {
    return JobPriority.EXTRAS;
  }

  @Override public Call<List<RatingItem>> getCall() {
    return syncService.getEpisodeRatings();
  }

  @Override public boolean handleResponse(List<RatingItem> ratings) {
    Cursor episodes = getContentResolver().query(Episodes.EPISODES, new String[] {
        EpisodeColumns.ID,
    }, EpisodeColumns.RATED_AT + ">0", null, null);
    List<Long> episodeIds = new ArrayList<>();
    while (episodes.moveToNext()) {
      final long episodeId = Cursors.getLong(episodes, EpisodeColumns.ID);
      episodeIds.add(episodeId);
    }
    episodes.close();

    ArrayList<ContentProviderOperation> ops = new ArrayList<>();

    for (RatingItem rating : ratings) {
      final int seasonNumber = rating.getEpisode().getSeason();
      final int episodeNumber = rating.getEpisode().getNumber();

      final long showTraktId = rating.getShow().getIds().getTrakt();
      ShowDatabaseHelper.IdResult showResult = showHelper.getIdOrCreate(showTraktId);
      final long showId = showResult.showId;
      final boolean didShowExist = !showResult.didCreate;

      SeasonDatabaseHelper.IdResult seasonResult = seasonHelper.getIdOrCreate(showId, seasonNumber);
      final long seasonId = seasonResult.id;
      final boolean didSeasonExist = !seasonResult.didCreate;
      if (seasonResult.didCreate && didShowExist) {
        showHelper.markPending(showId);
      }

      EpisodeDatabaseHelper.IdResult episodeResult =
          episodeHelper.getIdOrCreate(showId, seasonId, episodeNumber);
      final long episodeId = episodeResult.id;
      if (episodeResult.didCreate && didShowExist && didSeasonExist) {
        showHelper.markPending(showId);
      }

      episodeIds.remove(episodeId);

      ContentProviderOperation op = ContentProviderOperation.newUpdate(Episodes.withId(episodeId))
          .withValue(EpisodeColumns.USER_RATING, rating.getRating())
          .withValue(EpisodeColumns.RATED_AT, rating.getRatedAt().getTimeInMillis())
          .build();
      ops.add(op);
    }

    SyncPendingShows.schedule(getContext());

    for (Long episodeId : episodeIds) {
      ContentProviderOperation op = ContentProviderOperation.newUpdate(Episodes.withId(episodeId))
          .withValue(EpisodeColumns.USER_RATING, 0)
          .withValue(EpisodeColumns.RATED_AT, 0)
          .build();
      ops.add(op);
    }

    return applyBatch(ops);
  }
}
