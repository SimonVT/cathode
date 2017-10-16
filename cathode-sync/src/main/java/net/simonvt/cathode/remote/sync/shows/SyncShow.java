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
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper;
import net.simonvt.cathode.remote.CallJob;
import retrofit2.Call;

public class SyncShow extends CallJob<Show> {

  @Inject transient ShowsService showsService;
  @Inject transient SeasonService seasonService;

  @Inject transient ShowDatabaseHelper showHelper;

  private long traktId;

  public SyncShow(long traktId) {
    this.traktId = traktId;
  }

  @Override public String key() {
    return "SyncShow" + "&traktId=" + traktId;
  }

  @Override public int getPriority() {
    return JobPriority.SHOWS;
  }

  @Override public Call<Show> getCall() {
    return showsService.getSummary(traktId, Extended.FULL);
  }

  @Override public boolean handleResponse(Show show) {
    showHelper.fullUpdate(show);
    queue(new SyncSeasons(traktId));
    return true;
  }
}
