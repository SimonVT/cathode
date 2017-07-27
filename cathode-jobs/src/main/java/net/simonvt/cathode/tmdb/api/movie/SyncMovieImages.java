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

import android.content.ContentValues;
import com.uwetrottmann.tmdb2.entities.Image;
import com.uwetrottmann.tmdb2.entities.Images;
import com.uwetrottmann.tmdb2.services.MoviesService;
import javax.inject.Inject;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.MovieDatabaseHelper;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.tmdb.api.TmdbCallJob;
import retrofit2.Call;
import timber.log.Timber;

public class SyncMovieImages extends TmdbCallJob<Images> {

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

  @Override public Call<Images> getCall() {
    return moviesService.images(tmdbId, "en");
  }

  @Override public boolean handleResponse(Images images) {
    final long movieId = movieHelper.getIdFromTmdb(tmdbId);

    ContentValues values = new ContentValues();
    values.put(MovieColumns.IMAGES_LAST_UPDATE, System.currentTimeMillis());

    if (images.backdrops.size() > 0) {
      Image backdrop = images.backdrops.get(0);
      final String path = ImageUri.create(ImageType.BACKDROP, backdrop.file_path);
      Timber.d("Backdrop: %s", path);

      values.put(MovieColumns.BACKDROP, path);
    } else {
      values.putNull(MovieColumns.BACKDROP);
    }

    if (images.posters.size() > 0) {
      Image poster = images.posters.get(0);
      final String path = ImageUri.create(ImageType.POSTER, poster.file_path);
      Timber.d("Poster: %s", path);

      values.put(MovieColumns.POSTER, path);
    } else {
      values.putNull(MovieColumns.POSTER);
    }

    getContentResolver().update(Movies.withId(movieId), values, null, null);

    return true;
  }
}
