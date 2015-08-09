/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Show;
import net.simonvt.cathode.api.entity.UpdatedItem;
import net.simonvt.cathode.api.service.ShowsService;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.provider.ShowWrapper;
import timber.log.Timber;

public class SyncUpdatedShows extends Job {

  private static final int LIMIT = 100;

  @Inject transient ShowsService showsService;

  private String updatedSince;

  private int page;

  public SyncUpdatedShows(String updatedSince, int page) {
    super();
    this.updatedSince = updatedSince;
    this.page = page;
  }

  @Override public String key() {
    return "SyncUpdatedShows" + "&updatedSince=" + updatedSince + "&page=" + page;
  }

  @Override public int getPriority() {
    return PRIORITY_UPDATED;
  }

  @Override public void perform() {
    if (updatedSince == null) {
      return;
    }

    List<Long> showSummaries = new ArrayList<Long>();

    List<UpdatedItem> updated = showsService.getUpdatedShows(updatedSince, page, LIMIT);

    for (UpdatedItem item : updated) {
      final String updatedAt = item.getUpdatedAt();

      Show show = item.getShow();
      final long traktId = show.getIds().getTrakt();
      final boolean exists = ShowWrapper.exists(getContentResolver(), traktId);
      if (exists) {
        final boolean needsUpdate =
            ShowWrapper.needsUpdate(getContentResolver(), traktId, updatedAt);
        if (needsUpdate) {
          Timber.d("Show: %s - last updated: %s", show.getTitle(), updatedAt);
          final long id = ShowWrapper.getShowId(getContentResolver(), traktId);
          if (ShowWrapper.shouldSyncFully(getContentResolver(), id)) {
            queue(new SyncShow(traktId));
          } else {
            showSummaries.add(traktId);
          }
        }
      }
    }

    if (showSummaries.size() > 0) {
      for (Long traktId : showSummaries) {
        queue(new SyncShow(traktId, false));
      }
    }

    if (updated.size() >= LIMIT) {
      queue(new SyncUpdatedShows(updatedSince, page + 1));
    }
  }
}
