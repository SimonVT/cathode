/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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
package net.simonvt.cathode.remote.sync;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.TvShow;
import net.simonvt.cathode.api.service.ShowsService;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.provider.CathodeProvider;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;
import timber.log.Timber;

public class SyncTrendingShowsTask extends TraktTask {

  @Inject transient ShowsService showsService;

  @Override protected void doTask() {
    try {
      ContentResolver resolver = getContentResolver();

      List<TvShow> shows = showsService.trending();

      ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
      Cursor c = resolver.query(CathodeContract.Shows.SHOWS_TRENDING, null, null, null, null);

      List<Long> showIds = new ArrayList<Long>();
      while (c.moveToNext()) {
        final long showId = c.getLong(c.getColumnIndex(CathodeContract.Shows._ID));
        showIds.add(showId);
      }
      c.close();

      for (int i = 0, count = Math.min(shows.size(), 25); i < count; i++) {
        TvShow show = shows.get(i);
        if (show.getTvdbId() == null) {
          continue;
        }
        long showId = ShowWrapper.getShowId(resolver, show);
        if (showId == -1L) {
          showId = ShowWrapper.insertShow(resolver, show);
        }

        ContentValues cv = new ContentValues();
        cv.put(CathodeContract.Shows.TRENDING_INDEX, i);
        ContentProviderOperation op =
            ContentProviderOperation.newUpdate(CathodeContract.Shows.buildFromId(showId))
                .withValues(cv)
                .build();
        ops.add(op);

        showIds.remove(showId);
      }

      for (Long showId : showIds) {
        ContentProviderOperation op =
            ContentProviderOperation.newUpdate(CathodeContract.Shows.buildFromId(showId))
                .withValue(CathodeContract.Shows.TRENDING_INDEX, -1)
                .build();
        ops.add(op);
      }

      resolver.applyBatch(CathodeProvider.AUTHORITY, ops);
      postOnSuccess();
    } catch (RemoteException e) {
      Timber.e(e, "SyncTrendingShowsTask failed");
      postOnFailure();
    } catch (OperationApplicationException e) {
      Timber.e(e, "SyncTrendingShowsTask failed");
      postOnFailure();
    }
  }
}
