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
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Seasons;

public final class SeasonWrapper {

  private SeasonWrapper() {
  }

  public static long getSeasonId(ContentResolver resolver, long showId, int season) {
    Cursor c = null;
    try {
      c = resolver.query(Seasons.SEASONS, new String[] {
          SeasonColumns.ID,
      }, SeasonColumns.SHOW_ID + "=? AND " + SeasonColumns.SEASON + "=?", new String[] {
          String.valueOf(showId), String.valueOf(season),
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

  public static int getSeasonNumber(ContentResolver resolver, long seasonId) {
    Cursor c = null;
    try {
      c = resolver.query(Seasons.withId(seasonId), new String[] {
          SeasonColumns.SEASON,
      }, null, null, null);

      if (c.moveToFirst()) {
        return c.getInt(0);
      }

      return -1;
    } finally {
      if (c != null) c.close();
    }
  }

  public static void setWatched(ContentResolver resolver, long traktId, int season, boolean watched,
      long watchedAt) {
    final long showId = ShowWrapper.getShowId(resolver, traktId);
    final long seasonId = getSeasonId(resolver, showId, season);
    setWatched(resolver, showId, seasonId, watched, watchedAt);
  }

  public static void setWatched(ContentResolver resolver, long showId, long seasonId, boolean watched,
      long watchedAt) {
    Cursor episodes = resolver.query(Episodes.fromSeason(seasonId), new String[] {
        EpisodeColumns.ID, EpisodeColumns.WATCHED,
    }, null, null, null);

    ContentValues cv = new ContentValues();
    cv.put(EpisodeColumns.WATCHED, watched);

    while (episodes.moveToNext()) {
      final boolean isWatched =
          episodes.getInt(episodes.getColumnIndex(EpisodeColumns.WATCHED)) == 1;
      if (isWatched != watched) {
        final long episodeId = episodes.getLong(episodes.getColumnIndex(EpisodeColumns.ID));
        resolver.update(Episodes.withId(episodeId), cv, null, null);
      }
    }

    if (watched) {
      Cursor c = resolver.query(ProviderSchematic.Shows.withId(showId), new String[] {
          DatabaseContract.ShowColumns.LAST_WATCHED_AT,
      }, null, null, null);
      c.moveToFirst();
      final long lastWatched = c.getLong(c.getColumnIndex(DatabaseContract.ShowColumns.LAST_WATCHED_AT));
      c.close();
      if (watchedAt > lastWatched) {
        cv = new ContentValues();
        cv.put(DatabaseContract.ShowColumns.LAST_WATCHED_AT, watchedAt);
        resolver.update(ProviderSchematic.Shows.withId(showId), cv, null, null);
      }
    }

    episodes.close();
  }

  public static void setIsInCollection(ContentResolver resolver, long showTraktId, int seasonNumber,
      boolean collected, long collectedAt) {
    final long showId = ShowWrapper.getShowId(resolver, showTraktId);
    final long seasonId = getSeasonId(resolver, showId, seasonNumber);
    setIsInCollection(resolver, seasonId, collected, collectedAt);
  }

  public static void setIsInCollection(ContentResolver resolver, long seasonId, boolean collected,
      long collectedAt) {
    Cursor episodes = resolver.query(Episodes.fromSeason(seasonId), new String[] {
        EpisodeColumns.ID, EpisodeColumns.IN_COLLECTION,
    }, null, null, null);

    ContentValues cv = new ContentValues();
    cv.put(EpisodeColumns.IN_COLLECTION, collected);
    cv.put(EpisodeColumns.COLLECTED_AT, collectedAt);

    // TODO: How does trakt handle setting collectedAt if item is already watched?
    while (episodes.moveToNext()) {
      final boolean isCollected =
          episodes.getInt(episodes.getColumnIndex(EpisodeColumns.IN_COLLECTION)) == 1;
      if (isCollected != collected) {
        final long episodeId = episodes.getLong(episodes.getColumnIndex(EpisodeColumns.ID));
        resolver.update(Episodes.withId(episodeId), cv, null, null);
      }
    }

    episodes.close();
  }

  public static long updateOrInsertSeason(ContentResolver resolver, Season season, long showId) {
    if (showId < 0L) {
      throw new IllegalArgumentException("Invalid show id: " + showId);
    }

    long seasonId = getSeasonId(resolver, showId, season.getNumber());

    if (seasonId == -1) {
      seasonId = insertSeason(resolver, showId, season);
    } else {
      updateSeason(resolver, seasonId, season);
    }

    return seasonId;
  }

  public static long createSeason(ContentResolver resolver, long showId, int season) {
    ContentValues cv = new ContentValues();
    cv.put(SeasonColumns.SHOW_ID, showId);
    cv.put(SeasonColumns.SEASON, season);
    cv.put(SeasonColumns.NEEDS_SYNC, 1);

    Uri uri = resolver.insert(Seasons.SEASONS, cv);
    return Seasons.getId(uri);
  }

  public static void updateSeason(ContentResolver resolver, long seasonId, Season season) {
    ContentValues cv = getSeasonCVs(season);
    resolver.update(Seasons.withId(seasonId), cv, null, null);
  }

  public static long insertSeason(ContentResolver resolver, long showId, Season season) {
    ContentValues cv = getSeasonCVs(season);
    cv.put(SeasonColumns.SHOW_ID, showId);

    Uri uri = resolver.insert(Seasons.SEASONS, cv);
    return Seasons.getId(uri);
  }

  private static ContentValues getSeasonCVs(Season season) {
    ContentValues cv = new ContentValues();

    cv.put(SeasonColumns.NEEDS_SYNC, 0);

    cv.put(SeasonColumns.SEASON, season.getNumber());

    cv.put(SeasonColumns.TVDB_ID, season.getIds().getTvdb());
    cv.put(SeasonColumns.TMDB_ID, season.getIds().getTmdb());
    cv.put(SeasonColumns.TVRAGE_ID, season.getIds().getTvrage());

    if (season.getImages() != null) {
      Images images = season.getImages();
      cv.put(SeasonColumns.POSTER, images.getPoster().getFull());
      cv.put(SeasonColumns.THUMB, images.getThumb().getFull());
    }

    if (season.getRating() != null) {
      cv.put(SeasonColumns.RATING, season.getRating());
    }
    if (season.getVotes() != null) {
      cv.put(SeasonColumns.VOTES, season.getVotes());
    }

    return cv;
  }
}
