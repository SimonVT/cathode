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
import com.uwetrottmann.tmdb2.entities.TvShow;
import com.uwetrottmann.tmdb2.services.ConfigurationService;
import com.uwetrottmann.tmdb2.services.TvShowService;
import java.io.IOException;
import net.simonvt.cathode.common.tmdb.TmdbRateLimiter;
import net.simonvt.cathode.common.util.Closeables;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import net.simonvt.schematic.Cursors;
import retrofit2.Response;

public class ShowRequestHandler extends ItemRequestHandler {

  private TvShowService tvShowService;
  private ShowDatabaseHelper showHelper;

  public ShowRequestHandler(Context context, ConfigurationService configurationService,
      ImageDownloader downloader, TvShowService tvShowService, ShowDatabaseHelper showHelper) {
    super(context, configurationService, downloader);
    this.tvShowService = tvShowService;
    this.showHelper = showHelper;
  }

  @Override public boolean canHandleRequest(Request data) {
    return ImageUri.ITEM_SHOW.equals(data.uri.getScheme());
  }

  @Override protected int getTmdbId(long id) {
    return showHelper.getTmdbId(id);
  }

  @Override public long getLastCacheUpdate(long id) {
    Cursor c = context.getContentResolver().query(Shows.withId(id), new String[] {
        ShowColumns.IMAGES_LAST_UPDATE,
    }, null, null, null);
    c.moveToFirst();
    final long lastUpdate = Cursors.getLong(c, ShowColumns.IMAGES_LAST_UPDATE);
    c.close();
    return lastUpdate;
  }

  @Override protected String getCachedPath(ImageType imageType, long id) {
    Cursor c = null;

    try {
      c = context.getContentResolver().query(Shows.withId(id), new String[] {
          ShowColumns.POSTER, ShowColumns.BACKDROP,
      }, null, null, null);
      c.moveToFirst();

      if (imageType == ImageType.POSTER) {
        return Cursors.getString(c, ShowColumns.POSTER);
      } else if (imageType == ImageType.BACKDROP) {
        return Cursors.getString(c, ShowColumns.BACKDROP);
      } else {
        throw new IllegalArgumentException("Unsupported image type: " + imageType.toString());
      }
    } finally {
      Closeables.closeQuietly(c);
    }
  }

  protected void clearCachedPaths(long id) {
    ContentValues values = new ContentValues();
    values.put(ShowColumns.IMAGES_LAST_UPDATE, 0L);
    values.putNull(ShowColumns.BACKDROP);
    values.putNull(ShowColumns.POSTER);
    context.getContentResolver().update(Shows.withId(id), values, null, null);
  }

  @Override protected boolean updateCache(ImageType imageType, long id, int tmdbId)
      throws IOException {
    TmdbRateLimiter.acquire();
    Response<TvShow> response = tvShowService.tv(tmdbId, "en").execute();

    if (response.isSuccessful()) {
      TvShow show = response.body();
      retainImages(context, imageType, id, show);
      return true;
    }

    return false;
  }

  public static void retainImages(Context context, long id, TvShow show) {
    retainImages(context, null, id, show);
  }

  private static void retainImages(Context context, ImageType imageType, long id, TvShow show) {
    ContentValues values = new ContentValues();
    values.put(ShowColumns.IMAGES_LAST_UPDATE, System.currentTimeMillis());

    if (show.backdrop_path != null) {
      final String backdropPath = ImageUri.create(ImageType.BACKDROP, show.backdrop_path);
      values.put(ShowColumns.BACKDROP, backdropPath);
    } else {
      values.putNull(ShowColumns.BACKDROP);
    }

    if (show.poster_path != null) {
      final String posterPath = ImageUri.create(ImageType.POSTER, show.poster_path);
      values.put(ShowColumns.POSTER, posterPath);
    } else {
      values.putNull(ShowColumns.POSTER);
    }

    context.getContentResolver().update(Shows.withId(id), values, null, null);
  }
}
