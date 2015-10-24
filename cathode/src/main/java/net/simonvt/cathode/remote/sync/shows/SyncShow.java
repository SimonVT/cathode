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
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.api.service.SeasonService;
import net.simonvt.cathode.api.service.ShowsService;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.sync.comments.SyncComments;
import retrofit.Call;

public class SyncShow extends CallJob<Show> {

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

  @Override public Call<Show> getCall() {
    return showsService.getSummary(traktId, Extended.FULL_IMAGES);
  }

  @Override public void handleResponse(Show show) {
    showHelper.updateShow(show);

    if (!syncAdditionalInfo) {
      return;
    }

    queue(new SyncSeasons(traktId));
    queue(new SyncShowCollectedStatus(traktId));
    queue(new SyncShowWatchedStatus(traktId));
    queue(new SyncShowCast(traktId));
    queue(new SyncComments(ItemType.SHOW, traktId));
  }
}
