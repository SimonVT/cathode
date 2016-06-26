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
package net.simonvt.cathode.search;

import android.content.Context;
import android.text.TextUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.entity.SearchResult;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.api.service.SearchService;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.provider.MovieDatabaseHelper;
import net.simonvt.cathode.remote.sync.movies.SyncMovie;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public class MovieSearchHandler extends SearchHandler {

  @Inject SearchService searchService;

  @Inject MovieDatabaseHelper movieHelper;

  @Inject JobManager jobManager;

  public MovieSearchHandler(Context context) {
    super(context);
  }

  @Override protected List<Long> performSearch(String query) throws SearchFailedException {
    try {
      Call<List<SearchResult>> call = searchService.query(ItemType.MOVIE, query);
      Response<List<SearchResult>> response = call.execute();

      if (response.isSuccessful()) {
        List<SearchResult> results = response.body();
        final List<Long> movieIds = new ArrayList<>(results.size());

        for (SearchResult result : results) {
          Movie movie = result.getMovie();
          if (!TextUtils.isEmpty(movie.getTitle())) {
            final long traktId = movie.getIds().getTrakt();
            MovieDatabaseHelper.IdResult idResult = movieHelper.getIdOrCreate(traktId);

            final long movieId = idResult.movieId;
            movieHelper.partialUpdate(movie);
            movieIds.add(movieId);

            if (idResult.didCreate) jobManager.addJob(new SyncMovie(traktId));
          }
        }

        return movieIds;
      }
    } catch (IOException e) {
      Timber.d(e, "Search failed");
    }

    throw new SearchFailedException();
  }
}
