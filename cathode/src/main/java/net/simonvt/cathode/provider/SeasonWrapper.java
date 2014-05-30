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
package net.simonvt.cathode.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import net.simonvt.cathode.api.entity.Images;
import net.simonvt.cathode.api.entity.Season;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Seasons;
import net.simonvt.cathode.util.ApiUtils;

public final class SeasonWrapper {

  private SeasonWrapper() {
  }

  public static long getSeasonId(ContentResolver resolver, int showTvdbId, int season) {
    return getSeasonId(resolver, ShowWrapper.getShowId(resolver, showTvdbId), season);
  }

  public static long getSeasonId(ContentResolver resolver, long showId, int season) {
    Cursor c = null;
    try {
      c = resolver.query(Seasons.SEASONS, new String[] {
              SeasonColumns.ID,
          }, SeasonColumns.SHOW_ID + "=? AND " + SeasonColumns.SEASON + "=?", new String[] {
              String.valueOf(showId), String.valueOf(season),
          }, null
      );

      return !c.moveToFirst() ? -1L : c.getLong(c.getColumnIndex(SeasonColumns.ID));
    } finally {
      if (c != null) c.close();
    }
  }

  public static long getSeasonId(ContentResolver resolver, Season season) {
    Cursor c = null;
    try {
      c = resolver.query(Seasons.SEASONS, new String[] {
          SeasonColumns.ID,
      }, SeasonColumns.URL + "=?", new String[] {
          season.getUrl(),
      }, null);

      return !c.moveToFirst() ? -1L : c.getLong(c.getColumnIndex(SeasonColumns.ID));
    } finally {
      if (c != null) c.close();
    }
  }

  public static long getShowId(ContentResolver resolver, long seasonId) {
    Cursor c = null;
    try {
      c = resolver.query(Seasons.withId(seasonId), new String[] {
          SeasonColumns.SHOW_ID,
      }, null, null, null);

      if (c.moveToFirst()) {
        return c.getLong(0);
      }

      return -1L;
    } finally {
      if (c != null) c.close();
    }
  }

  public static long updateOrInsertSeason(ContentResolver resolver, Season season, long showId) {
    if (showId < 0L) {
      throw new IllegalArgumentException("Invalid show id: " + showId);
    }

    long seasonId = getSeasonId(resolver, season);

    if (seasonId == -1) {
      seasonId = insertSeason(resolver, showId, season);
    } else {
      updateSeason(resolver, seasonId, season);
    }

    return seasonId;
  }

  public static void updateSeason(ContentResolver resolver, long seasonId, Season season) {
    ContentValues cv = getSeasonCVs(season);
    resolver.update(Seasons.withId(seasonId), cv, null, null);
  }

  public static long insertSeason(ContentResolver resolver, long showId, Season season) {
    ContentValues cv = getSeasonCVs(season);
    cv.put(SeasonColumns.SHOW_ID, showId);

    Uri uri = resolver.insert(Seasons.SEASONS, cv);
    return Long.valueOf(Seasons.getId(uri));
  }

  private static ContentValues getSeasonCVs(Season season) {
    ContentValues cv = new ContentValues();

    cv.put(SeasonColumns.SEASON, season.getSeason());
    cv.put(SeasonColumns.EPISODES, season.getEpisodes().getCount());
    cv.put(SeasonColumns.URL, season.getUrl());
    if (season.getImages() != null) {
      Images images = season.getImages();
      if (!ApiUtils.isPlaceholder(images.getPoster())) {
        cv.put(SeasonColumns.POSTER, images.getPoster());
      }
    }

    return cv;
  }
}
