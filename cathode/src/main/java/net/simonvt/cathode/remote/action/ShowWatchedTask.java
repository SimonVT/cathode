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
package net.simonvt.cathode.remote.action;

import android.database.Cursor;
import javax.inject.Inject;
import net.simonvt.cathode.api.body.ShowBody;
import net.simonvt.cathode.api.service.ShowService;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;

public class ShowWatchedTask extends TraktTask {

  @Inject transient ShowService showService;

  private int tvdbId;

  private boolean watched;

  public ShowWatchedTask(int tvdbId, boolean watched) {
    this.tvdbId = tvdbId;
    this.watched = watched;
  }

  @Override protected void doTask() {
    if (watched) {
      showService.seen(new ShowBody(tvdbId));
    } else {
      // Trakt doesn't expose an unseen api..
      final long showId = ShowWrapper.getShowId(getContentResolver(), tvdbId);
      Cursor c = getContentResolver()
          .query(CathodeContract.Episodes.buildFromShowId(showId), new String[] {
              CathodeContract.Episodes.SEASON, CathodeContract.Episodes.EPISODE,
          }, null, null, null);

      while (c.moveToNext()) {
        queuePriorityTask(new EpisodeWatchedTask(tvdbId,
            c.getInt(c.getColumnIndex(CathodeContract.Episodes.SEASON)),
            c.getInt(c.getColumnIndex(CathodeContract.Episodes.EPISODE)), false));
      }

      c.close();
    }

    postOnSuccess();
  }
}
