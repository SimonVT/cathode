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
import com.uwetrottmann.tmdb2.services.TvService;
import java.io.IOException;
import javax.inject.Inject;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import net.simonvt.cathode.tmdb.TmdbRateLimiter;
import net.simonvt.cathode.util.Closeables;
import net.simonvt.schematic.Cursors;
import retrofit2.Response;

public class ShowRequestHandler extends ItemRequestHandler {

  @Inject TvService tvService;

  @Inject ShowDatabaseHelper showHelper;

  public ShowRequestHandler(Context context, ImageDownloader downloader) {
    super(context, downloader);
  }

  @Override public boolean canHandleRequest(Request data) {
    return ImageUri.ITEM_SHOW.equals(data.uri.getScheme());
  }

  @Override protected int getTmdbId(long id) {
    return showHelper.getTmdbId(id);
  }

  @Override protected String getCachedPath(ImageType imageType, long id) {
    Cursor c = null;

    try {
      c = context.getContentResolver().query(Shows.withId(id), new String[] {
          ShowColumns.POSTER, ShowColumns.BACKDROP,
      }, null, null, null);
      c.moveToFirst();

      if (imageType == ImageType.POSTER) {
        String posterPath = Cursors.getString(c, ShowColumns.POSTER);
        if (!TextUtils.isEmpty(posterPath)) {
          return posterPath;
        }
      } else if (imageType == ImageType.BACKDROP) {
        String backdropPath = Cursors.getString(c, ShowColumns.BACKDROP);
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
    values.putNull(ShowColumns.BACKDROP);
    values.putNull(ShowColumns.POSTER);
    context.getContentResolver().update(Shows.withId(id), values, null, null);
  }

  @Override protected String queryPath(ImageType imageType, long id, int tmdbId)
      throws IOException {
    String path = null;

    TmdbRateLimiter.acquire();
    Response<Images> response = tvService.images(tmdbId, "en").execute();

    if (response.isSuccessful()) {
      Images images = response.body();

      ContentValues values = new ContentValues();

      if (images.backdrops.size() > 0) {
        Image backdrop = images.backdrops.get(0);
        final String backdropPath =
            ImageUri.create(ImageType.BACKDROP, backdrop.file_path);

        values.put(ShowColumns.BACKDROP, backdropPath);

        if (imageType == ImageType.BACKDROP) {
          path = backdropPath;
        }
      } else {
        values.putNull(ShowColumns.BACKDROP);
      }

      if (images.posters.size() > 0) {
        Image poster = images.posters.get(0);
        final String posterPath = ImageUri.create(ImageType.POSTER, poster.file_path);

        values.put(ShowColumns.POSTER, posterPath);

        if (imageType == ImageType.POSTER) {
          path = posterPath;
        }
      } else {
        values.putNull(ShowColumns.POSTER);
      }

      final long showId = showHelper.getIdFromTmdb(tmdbId);

      context.getContentResolver().update(Shows.withId(showId), values, null, null);
    }

    return path;
  }
}
