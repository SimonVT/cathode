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

import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.TvShow;
import net.simonvt.cathode.api.enumeration.DetailLevel;
import net.simonvt.cathode.api.service.UserService;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;
import retrofit.RetrofitError;

public class SyncShowsTask extends TraktTask {

  private static final String TAG = "SyncShowsTask";

  @Inject transient UserService userService;

  @Override protected void doTask() {
    try {
      List<TvShow> shows = userService.libraryShowsAll(DetailLevel.MIN);

      for (TvShow show : shows) {
        if (show.getTvdbId() == null) {
          continue;
        }
        final Integer tvdbId = show.getTvdbId();
        if (!ShowWrapper.exists(service.getContentResolver(), tvdbId)) {
          queueTask(new SyncShowTask(tvdbId));
        }
      }

      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
