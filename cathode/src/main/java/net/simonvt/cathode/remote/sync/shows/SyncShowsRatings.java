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
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.provider.generated.CathodeProvider;
import net.simonvt.cathode.remote.TraktTask;
import timber.log.Timber;

public class SyncShowsRatings extends TraktTask {

  @Inject transient SyncService syncService;

  @Override protected void doTask() {
    List<RatingItem> ratings = syncService.getShowRatings();

    Cursor shows = getContentResolver().query(Shows.SHOWS, new String[] {
        ShowColumns.ID,
    }, ShowColumns.RATED_AT + ">0", null, null);
    List<Long> showIds = new ArrayList<Long>();
    while (shows.moveToNext()) {
      final long showId = shows.getLong(shows.getColumnIndex(ShowColumns.ID));
      showIds.add(showId);
    }
    shows.close();

    ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

    for (RatingItem rating : ratings) {
      final long traktId = rating.getShow().getIds().getTrakt();
      long showId = ShowWrapper.getShowId(getContentResolver(), traktId);
      if (showId == -1L) {
        showId = ShowWrapper.createShow(getContentResolver(), traktId);
        queueTask(new SyncShowTask(traktId));
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

    try {
      getContentResolver().applyBatch(CathodeProvider.AUTHORITY, ops);
      postOnSuccess();
    } catch (RemoteException e) {
      Timber.e(e, "Unable to sync show ratings");
      postOnFailure();
    } catch (OperationApplicationException e) {
      Timber.e(e, "Unable to sync show ratings");
      postOnFailure();
    }
  }
}
