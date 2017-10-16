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

import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Show;
import net.simonvt.cathode.api.entity.WatchlistItem;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.schematic.Cursors;
import retrofit2.Call;

public class SyncShowsWatchlist extends CallJob<List<WatchlistItem>> {

  @Inject transient SyncService syncService;

  @Inject transient ShowDatabaseHelper showHelper;

  public SyncShowsWatchlist() {
    super(Flags.REQUIRES_AUTH);
  }

  @Override public String key() {
    return "SyncShowsWatchlist";
  }

  @Override public int getPriority() {
    return JobPriority.USER_DATA;
  }

  @Override public Call<List<WatchlistItem>> getCall() {
    return syncService.getShowWatchlist();
  }

  @Override public boolean handleResponse(List<WatchlistItem> watchlist) {
    Cursor c = getContentResolver().query(Shows.SHOWS, new String[] {
        DatabaseSchematic.Tables.SHOWS + "." + ShowColumns.ID,
    }, ShowColumns.IN_WATCHLIST, null, null);

    List<Long> showIds = new ArrayList<>();

    while (c.moveToNext()) {
      showIds.add(Cursors.getLong(c, ShowColumns.ID));
    }
    c.close();

    for (WatchlistItem item : watchlist) {
      final Show show = item.getShow();
      final long listedAt = item.getListedAt().getTimeInMillis();
      final long traktId = show.getIds().getTrakt();
      ShowDatabaseHelper.IdResult showResult = showHelper.getIdOrCreate(traktId);
      final long showId = showResult.showId;
      if (showResult.didCreate) {
        queue(new SyncShow(traktId));
      }

      if (!showIds.remove(showId)) {
        showHelper.setIsInWatchlist(showId, true, listedAt);

        if (showHelper.needsSync(showId)) {
          queue(new SyncShow(traktId));
        }
      }
    }

    for (Long showId : showIds) {
      showHelper.setIsInWatchlist(showId, false);
    }

    return true;
  }
}
