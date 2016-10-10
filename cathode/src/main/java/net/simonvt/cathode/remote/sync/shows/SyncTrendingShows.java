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
import net.simonvt.cathode.api.enumeration.Extended;
import net.simonvt.cathode.api.service.ShowsService;
import net.simonvt.cathode.jobqueue.JobFailedException;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.schematic.Cursors;
import retrofit2.Call;
import timber.log.Timber;

public class SyncTrendingShows extends CallJob<List<TrendingItem>> {

  private static final int LIMIT = 50;

  @Inject transient ShowsService showsService;

  @Inject transient ShowDatabaseHelper showHelper;

  @Override public String key() {
    return "SyncTrendingShows";
  }

  @Override public int getPriority() {
    return PRIORITY_SUGGESTIONS;
  }

  @Override public Call<List<TrendingItem>> getCall() {
    return showsService.getTrendingShows(LIMIT, Extended.FULL);
  }

  @Override public void handleResponse(List<TrendingItem> shows) {
    try {
      ContentResolver resolver = getContentResolver();

      ArrayList<ContentProviderOperation> ops = new ArrayList<>();
      Cursor c = resolver.query(Shows.SHOWS_TRENDING, null, null, null, null);

      List<Long> showIds = new ArrayList<>();
      while (c.moveToNext()) {
        final long showId = Cursors.getLong(c, ShowColumns.ID);
        showIds.add(showId);
      }
      c.close();

      for (int i = 0, count = shows.size(); i < count; i++) {
        TrendingItem item = shows.get(i);
        Show show = item.getShow();
        final long showId = showHelper.partialUpdate(show);

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
