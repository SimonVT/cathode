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

import android.content.ContentValues;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Show;
import net.simonvt.cathode.api.entity.UpdatedItem;
import net.simonvt.cathode.api.service.ShowsService;
import net.simonvt.cathode.api.util.TimeUtils;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import net.simonvt.cathode.remote.CallJob;
import retrofit2.Call;

public class SyncUpdatedShows extends CallJob<List<UpdatedItem>> {

  private static final int LIMIT = 100;

  @Inject transient ShowsService showsService;

  @Inject transient ShowDatabaseHelper showHelper;

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

  @Override public Call<List<UpdatedItem>> getCall() {
    if (updatedSince == null) {
      updatedSince = TimeUtils.getIsoTime();
    }
    return showsService.getUpdatedShows(updatedSince, page, LIMIT);
  }

  @Override public void handleResponse(List<UpdatedItem> updated) {
    List<Long> showSummaries = new ArrayList<>();

    for (UpdatedItem item : updated) {
      final String updatedAt = item.getUpdatedAt();

      Show show = item.getShow();
      final long traktId = show.getIds().getTrakt();
      final long id = showHelper.getId(traktId);
      if (id != -1L) {
        final boolean shouldUpdate = showHelper.shouldUpdate(traktId, updatedAt);
        if (shouldUpdate) {
          queue(new SyncShow(traktId));
        } else {
          ContentValues values = new ContentValues();
          values.put(ShowColumns.NEEDS_SYNC, true);
          getContentResolver().update(Shows.withId(id), values, null, null);
        }
      }
    }

    if (updated.size() >= LIMIT) {
      queue(new SyncUpdatedShows(updatedSince, page + 1));
    }
  }
}
