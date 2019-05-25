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
import android.text.format.DateUtils;
import net.simonvt.cathode.api.entity.Episode;
import net.simonvt.cathode.common.database.Cursors;
import net.simonvt.cathode.common.util.guava.Preconditions;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.ProviderSchematic;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;

public final class EpisodeDatabaseHelper {

  public static final long WATCHED_RELEASE = -1L;

  private static final Object LOCK_ID = new Object();

  private Context context;

  private ShowDatabaseHelper showHelper;
  private SeasonDatabaseHelper seasonHelper;

  private ContentResolver resolver;

  public EpisodeDatabaseHelper(Context context, ShowDatabaseHelper showHelper,
      SeasonDatabaseHelper seasonHelper) {
    this.context = context;
    this.showHelper = showHelper;
    this.seasonHelper = seasonHelper;

    resolver = context.getContentResolver();
  }

  public Cursor query(long id, String... columns) {
    return resolver.query(Episodes.withId(id), columns, null, null, null);
  }

  public long getId(long traktId) {
    synchronized (LOCK_ID) {
      Cursor c = resolver.query(Episodes.EPISODES, new String[] {
          EpisodeColumns.ID,
      }, EpisodeColumns.TRAKT_ID + "=?", new String[] {
          String.valueOf(traktId),
      }, null);

      long id = !c.moveToFirst() ? -1L : Cursors.getLong(c, EpisodeColumns.ID);

      c.close();

      return id;
    }
  }

  public long getId(long showId, int season, int episode) {
    synchronized (LOCK_ID) {
      Cursor c = resolver.query(Episodes.EPISODES, new String[] {
          EpisodeColumns.ID,
      }, EpisodeColumns.SHOW_ID
          + "=? AND "
          + EpisodeColumns.SEASON
          + "=? AND "
          + EpisodeColumns.EPISODE
          + "=?", new String[] {
          String.valueOf(showId), String.valueOf(season), String.valueOf(episode),
      }, null);

      long id = !c.moveToFirst() ? -1L : Cursors.getLong(c, EpisodeColumns.ID);

      c.close();

      return id;
    }
  }

  public long getId(long showId, long seasonId, int episode) {
    synchronized (LOCK_ID) {
      Cursor c = resolver.query(Episodes.EPISODES, new String[] {
          EpisodeColumns.ID,
      }, EpisodeColumns.SHOW_ID
          + "=? AND "
          + EpisodeColumns.SEASON_ID
          + "=? AND "
          + EpisodeColumns.EPISODE
          + "=?", new String[] {
          String.valueOf(showId), String.valueOf(seasonId), String.valueOf(episode),
      }, null);

      long id = !c.moveToFirst() ? -1L : Cursors.getLong(c, EpisodeColumns.ID);

      c.close();

      return id;
    }
  }

  public long getTraktId(long episodeId) {
    synchronized (LOCK_ID) {
      Cursor c = resolver.query(Episodes.withId(episodeId), new String[] {
          EpisodeColumns.TRAKT_ID,
      }, null, null, null);

      long traktId = !c.moveToFirst() ? -1L : Cursors.getLong(c, EpisodeColumns.TRAKT_ID);

      c.close();

      return traktId;
    }
  }

  public int getTmdbId(long episodeId) {
    synchronized (LOCK_ID) {
      Cursor c = resolver.query(Episodes.withId(episodeId), new String[] {
          EpisodeColumns.TMDB_ID,
      }, null, null, null);

      int traktId = !c.moveToFirst() ? -1 : Cursors.getInt(c, EpisodeColumns.TMDB_ID);

      c.close();

      return traktId;
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

  public IdResult getIdOrCreate(long showId, long seasonId, int episode) {
    Preconditions.checkArgument(showId >= 0, "showId must be >=0, was %d", showId);
    Preconditions.checkArgument(seasonId >= 0, "seasonId must be >=0, was %d", seasonId);
    synchronized (LOCK_ID) {
      long id = getId(showId, seasonId, episode);

      if (id == -1L) {
        id = create(showId, seasonId, episode);
        return new IdResult(id, true);
      } else {
        return new IdResult(id, false);
      }
    }
  }

  private long create(long showId, long seasonId, int episode) {
    final int season = seasonHelper.getNumber(seasonId);

    ContentValues values = new ContentValues();
    values.put(EpisodeColumns.SHOW_ID, showId);
    values.put(EpisodeColumns.SEASON_ID, seasonId);
    values.put(EpisodeColumns.SEASON, season);
    values.put(EpisodeColumns.EPISODE, episode);

    return Episodes.getId(resolver.insert(Episodes.EPISODES, values));
  }

  public long updateEpisode(long episodeId, Episode episode) {
    ContentValues values = getValues(episode);
    resolver.update(Episodes.withId(episodeId), values, null, null);
    return episodeId;
  }

  public long getShowId(long episodeId) {
    Cursor c = resolver.query(Episodes.withId(episodeId), new String[] {
        EpisodeColumns.SHOW_ID,
    }, null, null, null);

    long id = -1L;
    if (c.moveToFirst()) {
      id = Cursors.getLong(c, EpisodeColumns.SHOW_ID);
    }

    c.close();

    return id;
  }

  public int getSeason(long episodeId) {
    Cursor c = resolver.query(Episodes.withId(episodeId), new String[] {
        EpisodeColumns.SEASON,
    }, null, null, null);

    int season = -1;
    if (c.moveToFirst()) {
      season = Cursors.getInt(c, EpisodeColumns.SEASON);
    }

    c.close();

    return season;
  }

  public int getNumber(long episodeId) {
    Cursor c = resolver.query(Episodes.withId(episodeId), new String[] {
        EpisodeColumns.EPISODE,
    }, null, null, null);

    int number = !c.moveToFirst() ? -1 : Cursors.getInt(c, EpisodeColumns.EPISODE);

    c.close();

    return number;
  }

  public void checkIn(long episodeId) {
    final long currentTime = System.currentTimeMillis();
    final long showId = getShowId(episodeId);

    Cursor show =
        context.getContentResolver().query(ProviderSchematic.Shows.withId(showId), new String[] {
            DatabaseContract.ShowColumns.RUNTIME,
        }, null, null, null);

    show.moveToFirst();
    final int runtime = Cursors.getInt(show, DatabaseContract.ShowColumns.RUNTIME);
    show.close();

    final long startedAt = currentTime;
    final long expiresAt = startedAt + runtime * DateUtils.MINUTE_IN_MILLIS;

    ContentValues values = new ContentValues();
    values.put(EpisodeColumns.CHECKED_IN, true);
    values.put(EpisodeColumns.STARTED_AT, startedAt);
    values.put(EpisodeColumns.EXPIRES_AT, expiresAt);
    context.getContentResolver().update(Episodes.withId(episodeId), values, null, null);
  }

  public void addToHistory(long episodeId, long watchedAt) {
    ContentValues values = new ContentValues();
    values.put(EpisodeColumns.WATCHED, true);

    resolver.update(Episodes.withId(episodeId), values, null, null);

    if (watchedAt == WATCHED_RELEASE) {
      values.clear();

      Cursor episode = resolver.query(Episodes.withId(episodeId), new String[] {
          EpisodeColumns.FIRST_AIRED,
      }, EpisodeColumns.WATCHED
          + " AND "
          + EpisodeColumns.FIRST_AIRED
          + ">"
          + EpisodeColumns.LAST_WATCHED_AT, null, null);

      if (episode.moveToNext()) {
        final long firstAired = Cursors.getLong(episode, EpisodeColumns.FIRST_AIRED);
        values.put(EpisodeColumns.LAST_WATCHED_AT, firstAired);
        resolver.update(Episodes.withId(episodeId), values, null, null);
      }

      episode.close();
    } else {
      values.clear();
      values.put(EpisodeColumns.LAST_WATCHED_AT, watchedAt);

      resolver.update(Episodes.withId(episodeId), values,
          EpisodeColumns.WATCHED + " AND " + EpisodeColumns.LAST_WATCHED_AT + "<?", new String[] {
              String.valueOf(watchedAt),
          });
    }
  }

  public void removeFromHistory(long episodeId) {
    ContentValues values = new ContentValues();
    values.put(EpisodeColumns.WATCHED, false);
    values.put(EpisodeColumns.LAST_WATCHED_AT, 0L);
    resolver.update(Episodes.withId(episodeId), values, null, null);
  }

  public void setInCollection(long traktId, int season, int episode, boolean inCollection,
      long collectedAt) {
    final long showId = showHelper.getId(traktId);
    final long episodeId = getId(showId, season, episode);
    setInCollection(episodeId, inCollection, collectedAt);
  }

  public void setInCollection(long episodeId, boolean inCollection, long collectedAt) {
    ContentValues values = new ContentValues();
    values.put(EpisodeColumns.IN_COLLECTION, inCollection);
    values.put(EpisodeColumns.COLLECTED_AT, collectedAt);

    resolver.update(Episodes.withId(episodeId), values, null, null);
  }

  public void setIsInWatchlist(long traktId, int season, int episode, boolean inWatchlist,
      long listedAt) {
    final long showId = showHelper.getId(traktId);
    final long episodeId = getId(showId, season, episode);
    setIsInWatchlist(episodeId, inWatchlist, listedAt);
  }

  public void setIsInWatchlist(long episodeId, boolean inWatchlist, long listedAt) {
    ContentValues values = new ContentValues();
    values.put(EpisodeColumns.IN_WATCHLIST, inWatchlist);
    values.put(EpisodeColumns.LISTED_AT, listedAt);

    resolver.update(Episodes.withId(episodeId), values, null, null);
  }

  public ContentValues getValues(Episode episode) {
    ContentValues values = new ContentValues();

    values.put(EpisodeColumns.SEASON, episode.getSeason());
    values.put(EpisodeColumns.EPISODE, episode.getNumber());
    values.put(EpisodeColumns.NUMBER_ABS, episode.getNumber_abs());

    if (episode.getTitle() != null) values.put(EpisodeColumns.TITLE, episode.getTitle());
    if (episode.getOverview() != null) values.put(EpisodeColumns.OVERVIEW, episode.getOverview());

    if (episode.getFirst_aired() != null) {
      values.put(EpisodeColumns.FIRST_AIRED, episode.getFirst_aired().getTimeInMillis());
    }
    if (episode.getUpdated_at() != null) {
      values.put(EpisodeColumns.UPDATED_AT, episode.getUpdated_at().getTimeInMillis());
    }

    values.put(EpisodeColumns.TRAKT_ID, episode.getIds().getTrakt());
    values.put(EpisodeColumns.IMDB_ID, episode.getIds().getImdb());
    values.put(EpisodeColumns.TVDB_ID, episode.getIds().getTvdb());
    values.put(EpisodeColumns.TMDB_ID, episode.getIds().getTmdb());
    values.put(EpisodeColumns.TVRAGE_ID, episode.getIds().getTvrage());

    if (episode.getRating() != null) {
      values.put(EpisodeColumns.RATING, episode.getRating());
    }
    if (episode.getVotes() != null) {
      values.put(EpisodeColumns.VOTES, episode.getVotes());
    }

    return values;
  }
}
