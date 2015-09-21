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

import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Show;
import net.simonvt.cathode.api.enumeration.Extended;
import net.simonvt.cathode.api.service.SeasonService;
import net.simonvt.cathode.api.service.ShowsService;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import net.simonvt.cathode.jobqueue.Job;

public class SyncShow extends Job {

  @Inject transient ShowsService showsService;
  @Inject transient SeasonService seasonService;

  @Inject transient ShowDatabaseHelper showHelper;

  private long traktId;

  private boolean syncAdditionalInfo;

  public SyncShow(long traktId) {
    this(traktId, true);
  }

  public SyncShow(long traktId, boolean syncAdditionalInfo) {
    this.traktId = traktId;
    this.syncAdditionalInfo = syncAdditionalInfo;
  }

  @Override public String key() {
    return "SyncShow" + "&traktId=" + traktId + "&syncAdditionalInfo=" + syncAdditionalInfo;
  }

  @Override public int getPriority() {
    return PRIORITY_SHOWS;
  }

  @Override public void perform() {
    Show show = showsService.getSummary(traktId, Extended.FULL_IMAGES);
    showHelper.updateShow(show);

    if (!syncAdditionalInfo) {
      return;
    }

    queue(new SyncSeasons(traktId));
    queue(new SyncShowCollectedStatus(traktId));
    queue(new SyncShowWatchedStatus(traktId));
    queue(new SyncShowCast(traktId));
  }
}
