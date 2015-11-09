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
package net.simonvt.cathode.remote.sync.movies;

import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.entity.UpdatedItem;
import net.simonvt.cathode.api.service.MoviesService;
import net.simonvt.cathode.provider.MovieDatabaseHelper;
import net.simonvt.cathode.remote.CallJob;
import retrofit.Call;

public class SyncUpdatedMovies extends CallJob<List<UpdatedItem>> {

  private static final int LIMIT = 100;

  @Inject transient MoviesService moviesService;

  @Inject transient MovieDatabaseHelper movieHelper;

  private String updatedSince;

  private int page;

  public SyncUpdatedMovies(String updatedSince, int page) {
    super();
    this.updatedSince = updatedSince;
    this.page = page;
  }

  @Override public String key() {
    return "SyncUpdatedMovies" + "&updatedSince=" + updatedSince + "&page=" + page;
  }

  @Override public int getPriority() {
    return PRIORITY_UPDATED;
  }

  @Override public Call<List<UpdatedItem>> getCall() {
    return moviesService.updated(updatedSince, page, LIMIT);
  }

  @Override public void handleResponse(List<UpdatedItem> updated) {
    if (updatedSince == null) {
      return;
    }

    for (UpdatedItem item : updated) {
      final String updatedAt = item.getUpdatedAt();
      final Movie movie = item.getMovie();
      final long traktId = movie.getIds().getTrakt();

      final long movieId = movieHelper.getId(traktId);
      if (movieId != -1L) {
        final boolean needsUpdate = movieHelper.needsUpdate(traktId, updatedAt);
        if (needsUpdate) {
          queue(new SyncMovie(traktId));
        }
      }
    }

    if (updated.size() >= LIMIT) {
      queue(new SyncUpdatedMovies(updatedSince, page + 1));
    }
  }
}
