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
package net.simonvt.cathode.remote.sync.movies;

import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.enumeration.Extended;
import net.simonvt.cathode.api.service.MoviesService;
import net.simonvt.cathode.provider.MovieWrapper;
import net.simonvt.cathode.jobqueue.Job;

public class SyncMovie extends Job {

  @Inject transient MoviesService moviesService;

  private long traktId;

  public SyncMovie(long traktId) {
    this.traktId = traktId;
  }

  @Override public String key() {
    return "SyncMovie" + "&traktId=" + traktId;
  }

  @Override public void perform() {
    Movie movie = moviesService.getSummary(traktId, Extended.FULL_IMAGES);
    MovieWrapper.updateOrInsertMovie(getContentResolver(), movie);
    queue(new SyncMovieCrew(traktId));
  }
}
