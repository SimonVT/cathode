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

package net.simonvt.cathode.images;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import com.squareup.picasso.Request;
import com.uwetrottmann.tmdb2.entities.Movie;
import com.uwetrottmann.tmdb2.services.ConfigurationService;
import com.uwetrottmann.tmdb2.services.MoviesService;
import java.io.IOException;
import net.simonvt.cathode.common.database.Cursors;
import net.simonvt.cathode.common.tmdb.TmdbRateLimiter;
import net.simonvt.cathode.common.util.Closeables;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper;
import retrofit2.Response;

public class MovieRequestHandler extends ItemRequestHandler {

  private MoviesService moviesService;
  private MovieDatabaseHelper movieHelper;

  public MovieRequestHandler(Context context, ConfigurationService configurationService,
      ImageDownloader downloader, MoviesService moviesService, MovieDatabaseHelper movieHelper) {
    super(context, configurationService, downloader);
    this.moviesService = moviesService;
    this.movieHelper = movieHelper;
  }

  @Override public boolean canHandleRequest(Request data) {
    return ImageUri.ITEM_MOVIE.equals(data.uri.getScheme());
  }

  @Override protected int getTmdbId(long id) {
    return movieHelper.getTmdbId(id);
  }

  @Override public long getLastCacheUpdate(long id) {
    Cursor c = context.getContentResolver().query(Movies.withId(id), new String[] {
        MovieColumns.IMAGES_LAST_UPDATE,
    }, null, null, null);
    c.moveToFirst();
    final long lastUpdate = Cursors.getLong(c, MovieColumns.IMAGES_LAST_UPDATE);
    c.close();
    return lastUpdate;
  }

  @Override protected String getCachedPath(ImageType imageType, long id) {
    Cursor c = null;

    try {
      c = context.getContentResolver().query(Movies.withId(id), new String[] {
          MovieColumns.IMAGES_LAST_UPDATE, MovieColumns.POSTER, MovieColumns.BACKDROP,
      }, null, null, null);
      c.moveToFirst();

      if (imageType == ImageType.POSTER) {
        return Cursors.getString(c, MovieColumns.POSTER);
      } else if (imageType == ImageType.BACKDROP) {
        return Cursors.getString(c, MovieColumns.BACKDROP);
      } else {
        throw new IllegalArgumentException("Unsupported image type: " + imageType.toString());
      }
    } finally {
      Closeables.closeQuietly(c);
    }
  }

  protected void clearCachedPaths(long id) {
    ContentValues values = new ContentValues();
    values.put(MovieColumns.IMAGES_LAST_UPDATE, 0L);
    values.putNull(MovieColumns.BACKDROP);
    values.putNull(MovieColumns.POSTER);
    context.getContentResolver().update(Movies.withId(id), values, null, null);
  }

  @Override protected boolean updateCache(ImageType imageType, long id, int tmdbId)
      throws IOException {
    TmdbRateLimiter.acquire();
    Response<Movie> response = moviesService.summary(tmdbId, "en").execute();

    if (response.isSuccessful()) {
      Movie movie = response.body();
      retainImages(context, imageType, id, movie);
      return true;
    }

    return false;
  }

  public static void retainImages(Context context, long id, Movie movie) {
    retainImages(context, null, id, movie);
  }

  private static void retainImages(Context context, ImageType imageType, long id, Movie movie) {
    ContentValues values = new ContentValues();
    values.put(MovieColumns.IMAGES_LAST_UPDATE, System.currentTimeMillis());

    if (movie.backdrop_path != null) {
      final String backdropPath = ImageUri.create(ImageType.BACKDROP, movie.backdrop_path);
      values.put(MovieColumns.BACKDROP, backdropPath);
    } else {
      values.putNull(MovieColumns.BACKDROP);
    }

    if (movie.poster_path != null) {
      final String posterPath = ImageUri.create(ImageType.POSTER, movie.poster_path);
      values.put(MovieColumns.POSTER, posterPath);
    } else {
      values.putNull(MovieColumns.POSTER);
    }

    context.getContentResolver().update(Movies.withId(id), values, null, null);
  }
}
