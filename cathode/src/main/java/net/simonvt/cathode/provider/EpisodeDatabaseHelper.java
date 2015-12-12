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
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.api.entity.Episode;
import net.simonvt.cathode.api.entity.Images;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;

public final class EpisodeDatabaseHelper {

  private static volatile EpisodeDatabaseHelper instance;

  public static EpisodeDatabaseHelper getInstance(Context context) {
    if (instance == null) {
      synchronized (EpisodeDatabaseHelper.class) {
        if (instance == null) {
          instance = new EpisodeDatabaseHelper(context);
        }
      }
    }
    return instance;
  }

  private static final Object LOCK_ID = new Object();

  @Inject ShowDatabaseHelper showHelper;
  @Inject SeasonDatabaseHelper seasonHelper;

  private Context context;

  private ContentResolver resolver;

  private EpisodeDatabaseHelper(Context context) {
    this.context = context;

    resolver = context.getContentResolver();

    CathodeApp.inject(context, this);
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

      long id = !c.moveToFirst() ? -1L : c.getLong(c.getColumnIndex(EpisodeColumns.ID));

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

      long id = !c.moveToFirst() ? -1L : c.getLong(c.getColumnIndex(EpisodeColumns.ID));

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

      long id = !c.moveToFirst() ? -1L : c.getLong(c.getColumnIndex(EpisodeColumns.ID));

      c.close();

      return id;
    }
  }

  public long getTraktId(long episodeId) {
    synchronized (LOCK_ID) {
      Cursor c = resolver.query(Episodes.withId(episodeId), new String[] {
          EpisodeColumns.TRAKT_ID,
      }, null, null, null);

      long traktId = !c.moveToFirst() ? -1L : c.getLong(c.getColumnIndex(EpisodeColumns.TRAKT_ID));

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

    ContentValues cv = new ContentValues();
    cv.put(EpisodeColumns.SHOW_ID, showId);
    cv.put(EpisodeColumns.SEASON_ID, seasonId);
    cv.put(EpisodeColumns.SEASON, season);
    cv.put(EpisodeColumns.EPISODE, episode);
    cv.put(EpisodeColumns.NEEDS_SYNC, 1);

    return Episodes.getId(resolver.insert(Episodes.EPISODES, cv));
  }

  public long updateEpisode(long episodeId, Episode episode) {
    ContentValues cv = getEpisodeCVs(episode);
    resolver.update(Episodes.withId(episodeId), cv, null, null);
    return episodeId;
  }

  public long getShowId(long episodeId) {
    Cursor c = resolver.query(Episodes.withId(episodeId), new String[] {
        EpisodeColumns.SHOW_ID,
    }, null, null, null);

    long id = -1L;
    if (c.moveToFirst()) {
      id = c.getLong(c.getColumnIndex(EpisodeColumns.SHOW_ID));
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
      season = c.getInt(c.getColumnIndex(EpisodeColumns.SEASON));
    }

    c.close();

    return season;
  }

  public int getNumber(long episodeId) {
    Cursor c = resolver.query(Episodes.withId(episodeId), new String[] {
        EpisodeColumns.EPISODE,
    }, null, null, null);

    int number = !c.moveToFirst() ? -1 : c.getInt(c.getColumnIndex(EpisodeColumns.EPISODE));

    c.close();

    return number;
  }

  public void setWatched(long traktId, int season, int episode, boolean watched, long watchedAt) {
    final long showId = showHelper.getId(traktId);
    final long episodeId = getId(showId, season, episode);
    setWatched(showId, episodeId, watched, watchedAt);
  }

  public void setWatched(long showId, long episodeId, boolean watched, long watchedAt) {
    ContentValues cv = new ContentValues();
    cv.put(EpisodeColumns.WATCHED, watched);
    resolver.update(Episodes.withId(episodeId), cv, null, null);

    if (watched) {
      Cursor c = resolver.query(ProviderSchematic.Shows.withId(showId), new String[] {
          DatabaseContract.ShowColumns.LAST_WATCHED_AT,
      }, null, null, null);
      c.moveToFirst();
      final long lastWatched =
          c.getLong(c.getColumnIndex(DatabaseContract.ShowColumns.LAST_WATCHED_AT));
      c.close();
      if (watchedAt > lastWatched) {
        cv = new ContentValues();
        cv.put(DatabaseContract.ShowColumns.LAST_WATCHED_AT, watchedAt);
        resolver.update(ProviderSchematic.Shows.withId(showId), cv, null, null);
      }
    }
  }

  public void setInCollection(long traktId, int season, int episode, boolean inCollection,
      long collectedAt) {
    final long showId = showHelper.getId(traktId);
    final long episodeId = getId(showId, season, episode);
    setInCollection(episodeId, inCollection, collectedAt);
  }

  public void setInCollection(long episodeId, boolean inCollection, long collectedAt) {
    ContentValues cv = new ContentValues();
    cv.put(EpisodeColumns.IN_COLLECTION, inCollection);
    cv.put(EpisodeColumns.COLLECTED_AT, collectedAt);

    resolver.update(Episodes.withId(episodeId), cv, null, null);
  }

  public void setIsInWatchlist(long traktId, int season, int episode, boolean inWatchlist,
      long listedAt) {
    final long showId = showHelper.getId(traktId);
    final long episodeId = getId(showId, season, episode);
    setIsInWatchlist(episodeId, inWatchlist, listedAt);
  }

  public void setIsInWatchlist(long episodeId, boolean inWatchlist, long listedAt) {
    ContentValues cv = new ContentValues();
    cv.put(EpisodeColumns.IN_WATCHLIST, inWatchlist);
    cv.put(EpisodeColumns.LISTED_AT, listedAt);

    resolver.update(Episodes.withId(episodeId), cv, null, null);
  }

  public ContentValues getEpisodeCVs(Episode episode) {
    ContentValues cv = new ContentValues();

    cv.put(EpisodeColumns.NEEDS_SYNC, 0);

    cv.put(EpisodeColumns.SEASON, episode.getSeason());
    cv.put(EpisodeColumns.EPISODE, episode.getNumber());
    cv.put(EpisodeColumns.NUMBER_ABS, episode.getNumberAbs());

    if (episode.getTitle() != null) cv.put(EpisodeColumns.TITLE, episode.getTitle());
    if (episode.getOverview() != null) cv.put(EpisodeColumns.OVERVIEW, episode.getOverview());

    if (episode.getFirstAired() != null) {
      cv.put(EpisodeColumns.FIRST_AIRED, episode.getFirstAired().getTimeInMillis());
    }
    if (episode.getUpdatedAt() != null) {
      cv.put(EpisodeColumns.UPDATED_AT, episode.getUpdatedAt().getTimeInMillis());
    }

    cv.put(EpisodeColumns.TRAKT_ID, episode.getIds().getTrakt());
    cv.put(EpisodeColumns.IMDB_ID, episode.getIds().getImdb());
    cv.put(EpisodeColumns.TVDB_ID, episode.getIds().getTvdb());
    cv.put(EpisodeColumns.TMDB_ID, episode.getIds().getTmdb());
    cv.put(EpisodeColumns.TVRAGE_ID, episode.getIds().getTvrage());

    if (episode.getImages() != null) {
      Images images = episode.getImages();
      if (images.getScreenshot() != null) {
        cv.put(EpisodeColumns.SCREENSHOT, images.getScreenshot().getFull());
      }
    }

    if (episode.getRating() != null) {
      cv.put(EpisodeColumns.RATING, episode.getRating());
    }
    if (episode.getVotes() != null) {
      cv.put(EpisodeColumns.VOTES, episode.getVotes());
    }

    return cv;
  }
}
