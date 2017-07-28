/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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

package net.simonvt.cathode.tmdb.api.show;

import com.uwetrottmann.tmdb2.entities.TvShow;
import com.uwetrottmann.tmdb2.services.TvShowService;
import javax.inject.Inject;
import net.simonvt.cathode.images.ShowRequestHandler;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import net.simonvt.cathode.tmdb.api.TmdbCallJob;
import retrofit2.Call;

public class SyncShowImages extends TmdbCallJob<TvShow> {

  @Inject transient TvShowService tvShowService;

  @Inject transient ShowDatabaseHelper showHelper;

  private int tmdbId;

  public SyncShowImages(int tmdbId) {
    this.tmdbId = tmdbId;
  }

  @Override public String key() {
    return "SyncShowImages" + "&tmdbId=" + tmdbId;
  }

  @Override public int getPriority() {
    return JobPriority.IMAGES;
  }

  @Override public Call<TvShow> getCall() {
    return tvShowService.tv(tmdbId, "en");
  }

  @Override public boolean handleResponse(TvShow show) {
    final long showId = showHelper.getIdFromTmdb(tmdbId);
    ShowRequestHandler.retainImages(getContext(), showId, show);
    return true;
  }
}
