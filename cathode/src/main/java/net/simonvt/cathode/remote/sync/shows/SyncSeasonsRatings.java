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
import android.database.Cursor;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.RatingItem;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.jobqueue.JobFailedException;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Seasons;
import net.simonvt.cathode.provider.SeasonDatabaseHelper;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import net.simonvt.cathode.provider.generated.CathodeProvider;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.schematic.Cursors;
import retrofit2.Call;
import timber.log.Timber;

public class SyncSeasonsRatings extends CallJob<List<RatingItem>> {

  @Inject transient SyncService syncService;

  @Inject transient ShowDatabaseHelper showHelper;
  @Inject transient SeasonDatabaseHelper seasonHelper;

  public SyncSeasonsRatings() {
    super(Flags.REQUIRES_AUTH);
  }

  @Override public String key() {
    return "SyncSeasonsRatings";
  }

  @Override public int getPriority() {
    return PRIORITY_EXTRAS;
  }

  @Override public Call<List<RatingItem>> getCall() {
    return syncService.getSeasonRatings();
  }

  @Override public void handleResponse(List<RatingItem> ratings) {
    Cursor seasons = getContentResolver().query(Seasons.SEASONS, new String[] {
        SeasonColumns.ID,
    }, SeasonColumns.RATED_AT + ">0", null, null);
    List<Long> seasonIds = new ArrayList<>();
    while (seasons.moveToNext()) {
      final long seasonId = Cursors.getLong(seasons, SeasonColumns.ID);
      seasonIds.add(seasonId);
    }
    seasons.close();

    ArrayList<ContentProviderOperation> ops = new ArrayList<>();

    for (RatingItem rating : ratings) {
      final int seasonNumber = rating.getSeason().getNumber();

      final long showTraktId = rating.getShow().getIds().getTrakt();
      ShowDatabaseHelper.IdResult showResult = showHelper.getIdOrCreate(showTraktId);
      final long showId = showResult.showId;
      final boolean didShowExist = !showResult.didCreate;
      if (showResult.didCreate) {
        queue(new SyncShow(showTraktId));
      }

      SeasonDatabaseHelper.IdResult seasonResult = seasonHelper.getIdOrCreate(showId, seasonNumber);
      final long seasonId = seasonResult.id;
      if (seasonResult.didCreate) {
        if (didShowExist) {
          queue(new SyncShow(showTraktId));
        }
      }

      seasonIds.remove(seasonId);

      ContentProviderOperation op = ContentProviderOperation.newUpdate(Seasons.withId(seasonId))
          .withValue(SeasonColumns.USER_RATING, rating.getRating())
          .withValue(SeasonColumns.RATED_AT, rating.getRatedAt().getTimeInMillis())
          .build();
      ops.add(op);
    }

    for (Long seasonId : seasonIds) {
      ContentProviderOperation op = ContentProviderOperation.newUpdate(Seasons.withId(seasonId))
          .withValue(SeasonColumns.USER_RATING, 0)
          .withValue(SeasonColumns.RATED_AT, 0)
          .build();
      ops.add(op);
    }

    try {
      getContentResolver().applyBatch(CathodeProvider.AUTHORITY, ops);
    } catch (RemoteException e) {
      Timber.e(e, "Unable to sync season ratings");
      throw new JobFailedException(e);
    } catch (OperationApplicationException e) {
      Timber.e(e, "Unable to sync season ratings");
      throw new JobFailedException(e);
    }
  }
}
