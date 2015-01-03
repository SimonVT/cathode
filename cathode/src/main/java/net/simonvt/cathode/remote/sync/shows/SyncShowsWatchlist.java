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
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.jobqueue.Job;

public class SyncShowsWatchlist extends Job {

  @Inject transient SyncService syncService;

  @Override public String key() {
    return "SyncShowsWatchlist";
  }

  @Override public int getPriority() {
    return PRIORITY_4;
  }

  @Override public void perform() {
    Cursor c = getContentResolver().query(Shows.SHOWS, new String[] {
        DatabaseSchematic.Tables.SHOWS + "." + ShowColumns.ID,
    }, ShowColumns.IN_WATCHLIST + "=0", null, null);

    List<Long> showIds = new ArrayList<Long>();

    while (c.moveToNext()) {
      showIds.add(c.getLong(c.getColumnIndex(ShowColumns.ID)));
    }
    c.close();

    List<WatchlistItem> watchlist = syncService.getShowWatchlist();

    for (WatchlistItem item : watchlist) {
      final Show show = item.getShow();
      final long listedAt = item.getListedAt().getTimeInMillis();
      final long traktId = show.getIds().getTrakt();

      long showId = ShowWrapper.getShowId(getContentResolver(), traktId);
      if (showId == -1L) {
        showId = ShowWrapper.createShow(getContentResolver(), traktId);
        queue(new SyncShow(traktId));
      }

      if (!showIds.remove(showId)) {
        ShowWrapper.setIsInWatchlist(getContentResolver(), showId, true, listedAt);
      }
    }

    for (Long showId : showIds) {
      ShowWrapper.setIsInWatchlist(getContentResolver(), showId, false);
    }
  }
}
