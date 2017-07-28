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

package net.simonvt.cathode.tmdb.api.movie;

import com.uwetrottmann.tmdb2.entities.Movie;
import com.uwetrottmann.tmdb2.services.MoviesService;
import javax.inject.Inject;
import net.simonvt.cathode.images.MovieRequestHandler;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.provider.MovieDatabaseHelper;
import net.simonvt.cathode.tmdb.api.TmdbCallJob;
import retrofit2.Call;

public class SyncMovieImages extends TmdbCallJob<Movie> {

  @Inject transient MoviesService moviesService;

  @Inject transient MovieDatabaseHelper movieHelper;

  private int tmdbId;

  public SyncMovieImages(int tmdbId) {
    this.tmdbId = tmdbId;
  }

  @Override public String key() {
    return "SyncMovieImages" + "&tmdbId=" + tmdbId;
  }

  @Override public int getPriority() {
    return JobPriority.MOVIES;
  }

  @Override public Call<Movie> getCall() {
    return moviesService.summary(tmdbId, "en");
  }

  @Override public boolean handleResponse(Movie movie) {
    final long movieId = movieHelper.getIdFromTmdb(tmdbId);
    MovieRequestHandler.retainImages(getContext(), movieId, movie);
    return true;
  }
}
