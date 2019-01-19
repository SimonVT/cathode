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

package net.simonvt.cathode.provider.helper;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import net.simonvt.cathode.api.entity.Season;
import net.simonvt.cathode.common.util.guava.Preconditions;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Seasons;
import net.simonvt.cathode.settings.FirstAiredOffsetPreference;
import net.simonvt.schematic.Cursors;

public final class SeasonDatabaseHelper {

  public static final long WATCHED_RELEASE = -1L;

  private static final Object LOCK_ID = new Object();

  private Context context;
  private ShowDatabaseHelper showHelper;

  private ContentResolver resolver;

  public SeasonDatabaseHelper(Context context, ShowDatabaseHelper showHelper) {
    this.context = context;
    this.showHelper = showHelper;

    resolver = context.getContentResolver();
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

  public int getTmdbId(long seasonId) {
    synchronized (LOCK_ID) {
      Cursor c = null;
      try {
        c = resolver.query(Seasons.withId(seasonId), new String[] {
            SeasonColumns.TMDB_ID,
        }, null, null, null);

        return !c.moveToFirst() ? -1 : Cursors.getInt(c, SeasonColumns.TMDB_ID);
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
    Preconditions.checkArgument(showId >= 0, "showId must be >=0, was %d", showId);
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
    ContentValues values = new ContentValues();
    values.put(SeasonColumns.SHOW_ID, showId);
    values.put(SeasonColumns.SEASON, season);
    values.put(SeasonColumns.NEEDS_SYNC, true);

    Uri uri = resolver.insert(Seasons.SEASONS, values);
    return Seasons.getId(uri);
  }

  public long updateSeason(long showId, Season season) {
    IdResult result = getIdOrCreate(showId, season.getNumber());
    final long seasonId = result.id;

    ContentValues values = getValues(season);
    resolver.update(Seasons.withId(seasonId), values, null, null);

    return seasonId;
  }

  public void addToHistory(long seasonId, long watchedAt) {
    ContentValues values = new ContentValues();
    values.put(EpisodeColumns.WATCHED, true);

    final long firstAiredOffset = FirstAiredOffsetPreference.getInstance().getOffsetMillis();
    final long millis = System.currentTimeMillis() - firstAiredOffset;

    resolver.update(Episodes.fromSeason(seasonId), values, EpisodeColumns.FIRST_AIRED + "<?",
        new String[] {
            String.valueOf(millis),
        });

    if (watchedAt == WATCHED_RELEASE) {
      values.clear();

      Cursor episodes = resolver.query(Episodes.fromSeason(seasonId), new String[] {
          EpisodeColumns.ID, EpisodeColumns.FIRST_AIRED,
      }, EpisodeColumns.WATCHED
          + " AND "
          + EpisodeColumns.FIRST_AIRED
          + ">"
          + EpisodeColumns.LAST_WATCHED_AT, null, null);

      while (episodes.moveToNext()) {
        final long episodeId = Cursors.getLong(episodes, EpisodeColumns.ID);
        final long firstAired = Cursors.getLong(episodes, EpisodeColumns.FIRST_AIRED);
        values.put(EpisodeColumns.LAST_WATCHED_AT, firstAired);
        resolver.update(Episodes.withId(episodeId), values, null, null);
      }

      episodes.close();
    } else {
      values.clear();
      values.put(EpisodeColumns.LAST_WATCHED_AT, watchedAt);

      resolver.update(Episodes.fromSeason(seasonId), values,
          EpisodeColumns.WATCHED + " AND " + EpisodeColumns.LAST_WATCHED_AT + "<?", new String[] {
              String.valueOf(watchedAt),
          });
    }
  }

  public void removeFromHistory(long seasonId) {
    ContentValues values = new ContentValues();
    values.put(EpisodeColumns.WATCHED, false);
    values.put(EpisodeColumns.LAST_WATCHED_AT, 0L);
    resolver.update(Episodes.fromSeason(seasonId), values, null, null);
  }

  public void setIsInCollection(long showTraktId, int seasonNumber, boolean collected,
      long collectedAt) {
    final long showId = showHelper.getId(showTraktId);
    final long seasonId = getId(showId, seasonNumber);
    setIsInCollection(seasonId, collected, collectedAt);
  }

  public void setIsInCollection(long seasonId, boolean collected, long collectedAt) {
    Cursor episodes = resolver.query(Episodes.fromSeason(seasonId), new String[] {
        EpisodeColumns.ID, EpisodeColumns.IN_COLLECTION,
    }, null, null, null);

    ContentValues values = new ContentValues();
    values.put(EpisodeColumns.IN_COLLECTION, collected);
    values.put(EpisodeColumns.COLLECTED_AT, collectedAt);

    while (episodes.moveToNext()) {
      final boolean isCollected = Cursors.getBoolean(episodes, EpisodeColumns.IN_COLLECTION);
      if (isCollected != collected) {
        final long episodeId = Cursors.getLong(episodes, EpisodeColumns.ID);
        resolver.update(Episodes.withId(episodeId), values, null, null);
      }
    }

    episodes.close();
  }

  private static ContentValues getValues(Season season) {
    ContentValues values = new ContentValues();
    values.put(SeasonColumns.SEASON, season.getNumber());

    values.put(SeasonColumns.TVDB_ID, season.getIds().getTvdb());
    values.put(SeasonColumns.TMDB_ID, season.getIds().getTmdb());
    values.put(SeasonColumns.TVRAGE_ID, season.getIds().getTvrage());

    if (season.getRating() != null) {
      values.put(SeasonColumns.RATING, season.getRating());
    }
    if (season.getVotes() != null) {
      values.put(SeasonColumns.VOTES, season.getVotes());
    }

    return values;
  }
}
