/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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
import android.text.format.DateUtils;
import com.squareup.picasso.Request;
import com.uwetrottmann.tmdb2.entities.TvSeason;
import com.uwetrottmann.tmdb2.services.TvSeasonsService;
import java.io.IOException;
import javax.inject.Inject;
import net.simonvt.cathode.common.tmdb.TmdbRateLimiter;
import net.simonvt.cathode.common.util.Closeables;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Seasons;
import net.simonvt.cathode.provider.SeasonDatabaseHelper;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import net.simonvt.schematic.Cursors;
import retrofit2.Response;

public class SeasonRequestHandler extends ItemRequestHandler {

  @Inject TvSeasonsService tvService;
  @Inject ShowDatabaseHelper showHelper;
  @Inject SeasonDatabaseHelper seasonHelper;

  public SeasonRequestHandler(Context context, ImageDownloader downloader) {
    super(context, downloader);
  }

  @Override public boolean canHandleRequest(Request data) {
    return ImageUri.ITEM_SEASON.equals(data.uri.getScheme());
  }

  @Override protected int getTmdbId(long id) {
    return seasonHelper.getTmdbId(id);
  }

  @Override protected String getCachedPath(ImageType imageType, long id) {
    Cursor c = null;

    try {
      c = context.getContentResolver().query(Seasons.withId(id), new String[] {
          SeasonColumns.IMAGES_LAST_UPDATE, SeasonColumns.POSTER,
      }, null, null, null);
      c.moveToFirst();

      final long lastUpdate = Cursors.getLong(c, SeasonColumns.IMAGES_LAST_UPDATE);
      final boolean needsUpdate =
          lastUpdate + DateUtils.WEEK_IN_MILLIS < System.currentTimeMillis();

      if (imageType == ImageType.POSTER) {
        String posterPath = Cursors.getString(c, SeasonColumns.POSTER);
        if (!needsUpdate && !TextUtils.isEmpty(posterPath)) {
          return posterPath;
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
    values.put(SeasonColumns.IMAGES_LAST_UPDATE, 0L);
    values.putNull(SeasonColumns.POSTER);
    context.getContentResolver().update(Seasons.withId(id), values, null, null);
  }

  @Override protected String queryPath(ImageType imageType, long id, int tmdbId)
      throws IOException {
    String path = null;

    final long showId = seasonHelper.getShowId(id);
    final int showTmdbId = showHelper.getTmdbId(showId);
    final int number = seasonHelper.getNumber(id);

    TmdbRateLimiter.acquire();
    Response<TvSeason> response = tvService.season(showTmdbId, number, "en").execute();

    if (response.isSuccessful()) {
      TvSeason season = response.body();

      ContentValues values = new ContentValues();
      values.put(SeasonColumns.IMAGES_LAST_UPDATE, System.currentTimeMillis());

      if (season.poster_path != null) {
        final String posterPath = ImageUri.create(ImageType.POSTER, season.poster_path);

        values.put(SeasonColumns.POSTER, posterPath);

        if (imageType == ImageType.POSTER) {
          path = posterPath;
        }
      } else {
        values.putNull(SeasonColumns.POSTER);
      }

      context.getContentResolver().update(Seasons.withId(id), values, null, null);
    }

    return path;
  }
}
