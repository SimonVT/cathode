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
package net.simonvt.cathode.remote.sync.shows;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.api.entity.Show;
import net.simonvt.cathode.api.entity.TrendingItem;
import net.simonvt.cathode.api.service.ShowsService;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobFailedException;
import timber.log.Timber;

public class SyncTrendingShows extends Job {

  private static final int LIMIT = 20;

  @Inject transient ShowsService showsService;

  @Override public String key() {
    return "SyncTrendingShows";
  }

  @Override public int getPriority() {
    return PRIORITY_RECOMMENDED_TRENDING;
  }

  @Override public void perform() {
    try {
      ContentResolver resolver = getContentResolver();

      List<TrendingItem> shows = showsService.getTrendingShows(LIMIT);

      ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
      Cursor c = resolver.query(Shows.SHOWS_TRENDING, null, null, null, null);

      List<Long> showIds = new ArrayList<Long>();
      while (c.moveToNext()) {
        final long showId = c.getLong(c.getColumnIndex(ShowColumns.ID));
        showIds.add(showId);
      }
      c.close();

      for (int i = 0, count = Math.min(shows.size(), 25); i < count; i++) {
        TrendingItem item = shows.get(i);
        Show show = item.getShow();
        final long traktId = show.getIds().getTrakt();

        long showId = ShowWrapper.getShowId(resolver, traktId);
        if (showId == -1L) {
          showId = ShowWrapper.createShow(resolver, traktId);
          queue(new SyncShow(traktId, false));
        }

        ContentValues cv = new ContentValues();
        cv.put(ShowColumns.TRENDING_INDEX, i);
        ContentProviderOperation op =
            ContentProviderOperation.newUpdate(Shows.withId(showId)).withValues(cv).build();
        ops.add(op);

        showIds.remove(showId);
      }

      for (Long showId : showIds) {
        ContentProviderOperation op = ContentProviderOperation.newUpdate(Shows.withId(showId))
            .withValue(ShowColumns.TRENDING_INDEX, -1)
            .build();
        ops.add(op);
      }

      resolver.applyBatch(BuildConfig.PROVIDER_AUTHORITY, ops);
    } catch (RemoteException e) {
      Timber.e(e, "SyncTrendingShowsTask failed");
    } catch (OperationApplicationException e) {
      Timber.e(e, "SyncTrendingShowsTask failed");
      throw new JobFailedException(e);
    }
  }
}
