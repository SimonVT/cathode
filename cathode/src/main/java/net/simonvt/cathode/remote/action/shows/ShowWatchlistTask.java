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
package net.simonvt.cathode.remote.action.shows;

import javax.inject.Inject;
import net.simonvt.cathode.api.body.SyncItems;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.remote.TraktTask;

public class ShowWatchlistTask extends TraktTask {

  @Inject transient SyncService syncService;

  private long traktId;

  private boolean inWatchlist;

  private String listedAt;

  public ShowWatchlistTask(long traktId, boolean inWatchlist, String listedAt) {
    this.traktId = traktId;
    this.inWatchlist = inWatchlist;
    this.listedAt = listedAt;
  }

  @Override protected void doTask() {
    SyncItems items = new SyncItems();
    SyncItems.Show show = items.show(traktId);
    if (inWatchlist) {
      show.listedAt(listedAt);
      syncService.watchlist(items);
    } else {
      syncService.unwatchlist(items);
    }

    postOnSuccess();
  }
}
