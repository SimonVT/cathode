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

import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.TvShow;
import net.simonvt.cathode.api.service.UserService;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.provider.CathodeDatabase;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;

public class SyncShowsWatchlistTask extends TraktTask {

  @Inject transient UserService userService;

  @Override protected void doTask() {
    Cursor c =
        service.getContentResolver().query(CathodeContract.Shows.SHOWS_WATCHLIST, new String[] {
            CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows._ID,
        }, null, null, null);

    List<Long> showIds = new ArrayList<Long>();

    while (c.moveToNext()) {
      showIds.add(c.getLong(c.getColumnIndex(CathodeContract.Shows._ID)));
    }
    c.close();

    List<TvShow> shows = userService.watchlistShows();

    for (TvShow show : shows) {
      if (show.getTvdbId() == null) {
        continue;
      }
      final int tvdbId = show.getTvdbId();
      final long showId = ShowWrapper.getShowId(service.getContentResolver(), tvdbId);

      if (showId != -1L) {
        if (!showIds.remove(showId)) {
          ShowWrapper.setIsInWatchlist(service.getContentResolver(), tvdbId, true);
        }
      } else {
        queueTask(new SyncShowTask(tvdbId));
      }
    }

    for (Long showId : showIds) {
      ShowWrapper.setIsInWatchlist(service.getContentResolver(), showId, false);
    }

    postOnSuccess();
  }
}
