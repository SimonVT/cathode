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

import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.api.ResponseParser;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.entity.Response;
import net.simonvt.cathode.api.enumeration.DetailLevel;
import net.simonvt.cathode.api.service.MovieService;
import net.simonvt.cathode.provider.MovieWrapper;
import net.simonvt.cathode.remote.TraktTask;
import retrofit.RetrofitError;

public class SyncMovieTask extends TraktTask {

  @Inject transient MovieService movieService;

  private long tmdbId;

  public SyncMovieTask(long tmdbId) {
    this.tmdbId = tmdbId;
  }

  @Override protected void doTask() {
    try {
      Movie movie = movieService.summary(tmdbId, DetailLevel.EXTENDED);
      MovieWrapper.updateOrInsertMovie(getContentResolver(), movie);
      postOnSuccess();
    } catch (RetrofitError e) {
      retrofit.client.Response r = e.getResponse();
      if (r != null) {
        ResponseParser parser = new ResponseParser();
        CathodeApp.inject(getContext(), parser);
        Response response = parser.tryParse(e);
        if (response != null && "movie not found".equals(response.getError())) {
          postOnSuccess();
          return;
        }
      }
      logError(e);
      postOnFailure();
    }
  }
}
