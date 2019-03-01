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
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import retrofit2.Call;

public class SyncShowsRatings extends CallJob<List<RatingItem>> {

  @Inject transient SyncService syncService;
  @Inject transient ShowDatabaseHelper showHelper;

  public SyncShowsRatings() {
    super(Flags.REQUIRES_AUTH);
  }

  @Override public String key() {
    return "SyncShowsRatings";
  }

  @Override public int getPriority() {
    return JobPriority.EXTRAS;
  }

  @Override public Call<List<RatingItem>> getCall() {
    return syncService.getShowRatings();
  }

  @Override public boolean handleResponse(List<RatingItem> ratings) {
    Cursor shows = getContentResolver().query(Shows.SHOWS, new String[] {
        ShowColumns.ID,
    }, ShowColumns.RATED_AT + ">0", null, null);
    List<Long> showIds = new ArrayList<>();
    while (shows.moveToNext()) {
      final long showId = Cursors.getLong(shows, ShowColumns.ID);
      showIds.add(showId);
    }
    shows.close();

    ArrayList<ContentProviderOperation> ops = new ArrayList<>();

    for (RatingItem rating : ratings) {
      final long traktId = rating.getShow().getIds().getTrakt();
      ShowDatabaseHelper.IdResult showResult = showHelper.getIdOrCreate(traktId);
      final long showId = showResult.showId;
      if (showResult.didCreate) {
        queue(new SyncShow(traktId));
      }

      showIds.remove(showId);

      ContentProviderOperation op = ContentProviderOperation.newUpdate(Shows.withId(showId))
          .withValue(ShowColumns.USER_RATING, rating.getRating())
          .withValue(ShowColumns.RATED_AT, rating.getRatedAt().getTimeInMillis())
          .build();
      ops.add(op);
    }

    for (Long showId : showIds) {
      ContentProviderOperation op = ContentProviderOperation.newUpdate(Shows.withId(showId))
          .withValue(ShowColumns.USER_RATING, 0)
          .withValue(ShowColumns.RATED_AT, 0)
          .build();
      ops.add(op);
    }

    return applyBatch(ops);
  }
}
