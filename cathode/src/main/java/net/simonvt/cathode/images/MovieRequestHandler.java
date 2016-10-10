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
import android.text.TextUtils;
import com.squareup.picasso.Request;
import com.uwetrottmann.tmdb2.entities.Image;
import com.uwetrottmann.tmdb2.entities.Images;
import com.uwetrottmann.tmdb2.services.MoviesService;
import java.io.IOException;
import javax.inject.Inject;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.MovieDatabaseHelper;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.tmdb.TmdbRateLimiter;
import net.simonvt.cathode.util.Closeables;
import net.simonvt.schematic.Cursors;
import retrofit2.Response;

public class MovieRequestHandler extends ItemRequestHandler {

  @Inject MoviesService moviesService;

  @Inject MovieDatabaseHelper movieHelper;

  public MovieRequestHandler(Context context, ImageDownloader downloader) {
    super(context, downloader);
  }

  @Override public boolean canHandleRequest(Request data) {
    return ImageUri.ITEM_MOVIE.equals(data.uri.getScheme());
  }

  @Override protected int getTmdbId(long id) {
    return movieHelper.getTmdbId(id);
  }

  @Override protected String getCachedPath(ImageType imageType, long id) {
    Cursor c = null;

    try {
      c = context.getContentResolver().query(Movies.withId(id), new String[] {
          MovieColumns.POSTER, MovieColumns.BACKDROP,
      }, null, null, null);
      c.moveToFirst();

      if (imageType == ImageType.POSTER) {
        String posterPath = Cursors.getString(c, MovieColumns.POSTER);
        if (!TextUtils.isEmpty(posterPath)) {
          return posterPath;
        }
      } else if (imageType == ImageType.BACKDROP) {
        String backdropPath = Cursors.getString(c, MovieColumns.BACKDROP);
        if (!TextUtils.isEmpty(backdropPath)) {
          return backdropPath;
        }
      } else {
        throw new IllegalArgumentException("Unsupported image type: " + imageType.toString());
      }
    } finally {
      Closeables.closeQuietly(c);
    }

    return null;
  }

  protected void clearCachedPaths(long id) {
    ContentValues values = new ContentValues();
    values.putNull(MovieColumns.BACKDROP);
    values.putNull(MovieColumns.POSTER);
    context.getContentResolver().update(Movies.withId(id), values, null, null);
  }

  @Override protected String queryPath(ImageType imageType, long id, int tmdbId)
      throws IOException {
    String path = null;

    TmdbRateLimiter.acquire();
    Response<Images> response = moviesService.images(tmdbId, "en").execute();

    if (response.isSuccessful()) {
      Images images = response.body();

      ContentValues values = new ContentValues();

      if (images.backdrops.size() > 0) {
        Image backdrop = images.backdrops.get(0);
        final String backdropPath =
            ImageUri.create(ImageType.BACKDROP, backdrop.file_path);

        values.put(MovieColumns.BACKDROP, backdropPath);

        if (imageType == ImageType.BACKDROP) {
          path = backdropPath;
        }
      } else {
        values.putNull(MovieColumns.BACKDROP);
      }

      if (images.posters.size() > 0) {
        Image poster = images.posters.get(0);
        final String posterPath = ImageUri.create(ImageType.POSTER, poster.file_path);

        values.put(MovieColumns.POSTER, posterPath);

        if (imageType == ImageType.POSTER) {
          path = posterPath;
        }
      } else {
        values.putNull(MovieColumns.POSTER);
      }

      final long movieId = movieHelper.getIdFromTmdb(tmdbId);

      context.getContentResolver().update(Movies.withId(movieId), values, null, null);
    }

    return path;
  }
}
