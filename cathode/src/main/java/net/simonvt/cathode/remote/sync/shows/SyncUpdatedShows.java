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

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Show;
import net.simonvt.cathode.api.entity.UpdatedItem;
import net.simonvt.cathode.api.service.ShowsService;
import net.simonvt.cathode.api.util.TimeUtils;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;
import net.simonvt.cathode.settings.Settings;

public class SyncUpdatedShows extends TraktTask {

  @Inject transient ShowsService showsService;

  @Override protected void doTask() {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
    String lastUpdated = settings.getString(Settings.SHOWS_LAST_UPDATED, null);

    List<Long> showSummaries = new ArrayList<Long>();

    String currentTime = TimeUtils.getIsoTime();

    if (lastUpdated != null) {
      List<UpdatedItem> updated = showsService.getUpdatedShows(lastUpdated);

      for (UpdatedItem item : updated) {
        final String updatedAt = item.getUpdatedAt();

        Show show = item.getShow();
        final long traktId = show.getIds().getTrakt();
        final boolean exists = ShowWrapper.exists(getContentResolver(), traktId);
        if (exists) {
          final boolean needsUpdate =
              ShowWrapper.needsUpdate(getContentResolver(), traktId, updatedAt);
          if (needsUpdate) {
            final long id = ShowWrapper.getShowId(getContentResolver(), traktId);
            if (ShowWrapper.shouldSyncFully(getContentResolver(), id)) {
              queueTask(new SyncShowTask(traktId));
            } else {
              showSummaries.add(traktId);
            }
          }
        }
      }

      if (showSummaries.size() > 0) {
        for (Long traktId : showSummaries) {
          queueTask(new SyncShowTask(traktId, false));
        }
      }
    }

    settings.edit().putString(Settings.SHOWS_LAST_UPDATED, currentTime).apply();
    postOnSuccess();
  }
}
