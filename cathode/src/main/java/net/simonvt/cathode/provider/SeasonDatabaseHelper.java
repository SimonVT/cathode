/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.api.entity.Images;
import net.simonvt.cathode.api.entity.Season;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Seasons;
import net.simonvt.schematic.Cursors;

public final class SeasonDatabaseHelper {

  private static volatile SeasonDatabaseHelper instance;

  public static SeasonDatabaseHelper getInstance(Context context) {
    if (instance == null) {
      synchronized (SeasonDatabaseHelper.class) {
        if (instance == null) {
          instance = new SeasonDatabaseHelper(context);
        }
      }
    }
    return instance;
  }

  private static final Object LOCK_ID = new Object();

  @Inject ShowDatabaseHelper showHelper;

  private Context context;

  private ContentResolver resolver;

  private SeasonDatabaseHelper(Context context) {
    this.context = context;

    resolver = context.getContentResolver();

    CathodeApp.inject(context, this);
  }

  public long getId(long showId, int season) {
    synchronized (LOCK_ID) {
      Cursor c = null;
      try {
        c = resolver.query(Seasons.SEASONS, new String[] {
            SeasonColumns.ID,
        }, SeasonColumns.SHOW_ID + "=? AND " + SeasonColumns.SEASON + "=?", new String[] {
            String.valueOf(showId), String.valueOf(season),
        }, null);

        return !c.moveToFirst() ? -1L : Cursors.getLong(c, SeasonColumns.ID);
      } finally {
        if (c != null) c.close();
      }
    }
  }

  public int getNumber(long seasonId) {
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

  public long getShowId(long seasonId) {
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

  public static final class IdResult {

    public long id;

    public boolean didCreate;

    public IdResult(long id, boolean didCreate) {
      this.id = id;
      this.didCreate = didCreate;
    }
  }

  public IdResult getIdOrCreate(long showId, int season) {
    synchronized (LOCK_ID) {
      long id = getId(showId, season);

      if (id == -1L) {
        id = create(showId, season);
        return new IdResult(id, true);
      } else {
        return new IdResult(id, false);
      }
    }
  }

  private long create(long showId, int season) {
    ContentValues cv = new ContentValues();
    cv.put(SeasonColumns.SHOW_ID, showId);
    cv.put(SeasonColumns.SEASON, season);
    cv.put(SeasonColumns.NEEDS_SYNC, 1);

    Uri uri = resolver.insert(Seasons.SEASONS, cv);
    return Seasons.getId(uri);
  }

  public long updateSeason(long showId, Season season) {
    IdResult result = getIdOrCreate(showId, season.getNumber());
    final long seasonId = result.id;

    ContentValues cv = getSeasonCVs(season);
    resolver.update(Seasons.withId(seasonId), cv, null, null);

    return seasonId;
  }

  public void setWatched(long traktId, int season, boolean watched, long watchedAt) {
    final long showId = showHelper.getId(traktId);
    final long seasonId = getId(showId, season);
    if (seasonId == -1L) {
      return;
    }
    setWatched(showId, seasonId, watched, watchedAt);
  }

  public void setWatched(long showId, long seasonId, boolean watched, long watchedAt) {
    Cursor episodes = resolver.query(ProviderSchematic.Episodes.fromSeason(seasonId), new String[] {
        EpisodeColumns.ID, EpisodeColumns.WATCHED,
    }, null, null, null);

    ContentValues cv = new ContentValues();
    cv.put(EpisodeColumns.WATCHED, watched);

    while (episodes.moveToNext()) {
      final boolean isWatched = Cursors.getInt(episodes, EpisodeColumns.WATCHED) == 1;
      if (isWatched != watched) {
        final long episodeId = Cursors.getLong(episodes, EpisodeColumns.ID);
        resolver.update(ProviderSchematic.Episodes.withId(episodeId), cv, null, null);
      }
    }

    if (watched) {
      Cursor c = resolver.query(ProviderSchematic.Shows.withId(showId), new String[] {
          DatabaseContract.ShowColumns.LAST_WATCHED_AT,
      }, null, null, null);
      c.moveToFirst();
      final long lastWatched = Cursors.getLong(c, DatabaseContract.ShowColumns.LAST_WATCHED_AT);
      c.close();
      if (watchedAt > lastWatched) {
        cv = new ContentValues();
        cv.put(DatabaseContract.ShowColumns.LAST_WATCHED_AT, watchedAt);
        resolver.update(ProviderSchematic.Shows.withId(showId), cv, null, null);
      }
    }

    episodes.close();
  }

  public void setIsInCollection(long showTraktId, int seasonNumber, boolean collected,
      long collectedAt) {
    final long showId = showHelper.getId(showTraktId);
    final long seasonId = getId(showId, seasonNumber);
    setIsInCollection(seasonId, collected, collectedAt);
  }

  public void setIsInCollection(long seasonId, boolean collected, long collectedAt) {
    Cursor episodes = resolver.query(ProviderSchematic.Episodes.fromSeason(seasonId), new String[] {
        EpisodeColumns.ID, EpisodeColumns.IN_COLLECTION,
    }, null, null, null);

    ContentValues cv = new ContentValues();
    cv.put(EpisodeColumns.IN_COLLECTION, collected);
    cv.put(EpisodeColumns.COLLECTED_AT, collectedAt);

    while (episodes.moveToNext()) {
      final boolean isCollected = Cursors.getInt(episodes, EpisodeColumns.IN_COLLECTION) == 1;
      if (isCollected != collected) {
        final long episodeId = Cursors.getLong(episodes, EpisodeColumns.ID);
        resolver.update(ProviderSchematic.Episodes.withId(episodeId), cv, null, null);
      }
    }

    episodes.close();
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
