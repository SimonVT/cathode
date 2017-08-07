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

package net.simonvt.cathode.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;
import java.util.List;
import net.simonvt.cathode.Injector;
import net.simonvt.cathode.api.entity.Show;
import net.simonvt.cathode.api.util.TimeUtils;
import net.simonvt.cathode.common.util.TextUtils;
import net.simonvt.cathode.database.DatabaseUtils;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.settings.FirstAiredOffsetPreference;
import net.simonvt.schematic.Cursors;
import timber.log.Timber;

public final class ShowDatabaseHelper {

  public static final long WATCHED_RELEASE = -1L;

  private static volatile ShowDatabaseHelper instance;

  public static ShowDatabaseHelper getInstance(Context context) {
    if (instance == null) {
      synchronized (ShowDatabaseHelper.class) {
        if (instance == null) {
          instance = new ShowDatabaseHelper(context.getApplicationContext());
        }
      }
    }
    return instance;
  }

  private static final Object LOCK_ID = new Object();

  private Context context;

  private ContentResolver resolver;

  private ShowDatabaseHelper(Context context) {
    this.context = context;

    resolver = context.getContentResolver();

    Injector.inject(this);
  }

  public long getId(long traktId) {
    synchronized (LOCK_ID) {
      Cursor c = resolver.query(Shows.SHOWS, new String[] {
          ShowColumns.ID,
      }, ShowColumns.TRAKT_ID + "=?", new String[] {
          String.valueOf(traktId),
      }, null);

      long id = -1L;

      if (c.moveToFirst()) {
        id = Cursors.getLong(c, ShowColumns.ID);
      }

      c.close();

      return id;
    }
  }

  public long getIdFromTmdb(int tmdbId) {
    synchronized (LOCK_ID) {
      Cursor c = resolver.query(Shows.SHOWS, new String[] {
          ShowColumns.ID,
      }, ShowColumns.TMDB_ID + "=?", new String[] {
          String.valueOf(tmdbId),
      }, null);

      long id = -1L;

      if (c.moveToFirst()) {
        id = Cursors.getLong(c, ShowColumns.ID);
      }

      c.close();

      return id;
    }
  }

  public long getTraktId(long showId) {
    Cursor c = resolver.query(Shows.withId(showId), new String[] {
        ShowColumns.TRAKT_ID,
    }, null, null, null);

    long traktId = -1L;
    if (c.moveToFirst()) {
      traktId = Cursors.getLong(c, ShowColumns.TRAKT_ID);
    }

    c.close();

    return traktId;
  }

  public int getTmdbId(long showId) {
    Cursor c = resolver.query(Shows.withId(showId), new String[] {
        ShowColumns.TMDB_ID,
    }, null, null, null);

    int tmdbId = -1;
    if (c.moveToFirst()) {
      tmdbId = Cursors.getInt(c, ShowColumns.TMDB_ID);
    }

    c.close();

    return tmdbId;
  }

  public static final class IdResult {

    public long showId;

    public boolean didCreate;

    public IdResult(long showId, boolean didCreate) {
      this.showId = showId;
      this.didCreate = didCreate;
    }
  }

  public IdResult getIdOrCreate(long traktId) {
    synchronized (LOCK_ID) {
      long id = getId(traktId);

      if (id == -1L) {
        id = create(traktId);
        return new IdResult(id, true);
      } else {
        return new IdResult(id, false);
      }
    }
  }

  private long create(long traktId) {
    ContentValues values = new ContentValues();
    values.put(ShowColumns.TRAKT_ID, traktId);
    values.put(ShowColumns.NEEDS_SYNC, true);

    return Shows.getShowId(resolver.insert(Shows.SHOWS, values));
  }

  public long fullUpdate(Show show) {
    IdResult result = getIdOrCreate(show.getIds().getTrakt());
    final long id = result.showId;

    ContentValues values = getValues(show);
    values.put(ShowColumns.NEEDS_SYNC, false);
    values.put(ShowColumns.LAST_SYNC, System.currentTimeMillis());
    resolver.update(Shows.withId(id), values, null, null);

    if (show.getGenres() != null) {
      insertShowGenres(id, show.getGenres());
    }

    return id;
  }

  /**
   * Creates the show if it does not exist.
   */
  public long partialUpdate(Show show) {
    IdResult result = getIdOrCreate(show.getIds().getTrakt());
    final long id = result.showId;

    ContentValues values = getValues(show);
    resolver.update(Shows.withId(id), values, null, null);

    if (show.getGenres() != null) {
      insertShowGenres(id, show.getGenres());
    }

    return id;
  }

  public long getNextEpisodeId(long showId) {
    int lastWatchedSeason = -1;
    int lastWatchedEpisode = -1;

    Cursor lastWatchedCursor = resolver.query(Episodes.fromShow(showId), new String[] {
            EpisodeColumns.ID, EpisodeColumns.SEASON, EpisodeColumns.EPISODE,
        }, EpisodeColumns.WATCHED + "=1", null,
        EpisodeColumns.SEASON + " DESC, " + EpisodeColumns.EPISODE + " DESC LIMIT 1");

    if (lastWatchedCursor.moveToFirst()) {
      lastWatchedSeason = Cursors.getInt(lastWatchedCursor, EpisodeColumns.SEASON);
      lastWatchedEpisode = Cursors.getInt(lastWatchedCursor, EpisodeColumns.EPISODE);
    }
    lastWatchedCursor.close();

    Cursor nextEpisode = resolver.query(Episodes.fromShow(showId), new String[] {
        EpisodeColumns.ID,
    }, EpisodeColumns.SEASON
        + ">0 AND ("
        + EpisodeColumns.SEASON
        + ">? OR ("
        + EpisodeColumns.SEASON
        + "=? AND "
        + EpisodeColumns.EPISODE
        + ">?)) AND "
        + EpisodeColumns.FIRST_AIRED
        + " NOT NULL", new String[] {
        String.valueOf(lastWatchedSeason), String.valueOf(lastWatchedSeason),
        String.valueOf(lastWatchedEpisode),
    }, EpisodeColumns.SEASON + " ASC, " + EpisodeColumns.EPISODE + " ASC LIMIT 1");

    long nextEpisodeId = -1L;

    if (nextEpisode.moveToFirst()) {
      nextEpisodeId = Cursors.getLong(nextEpisode, EpisodeColumns.ID);
    }

    nextEpisode.close();

    return nextEpisodeId;
  }

  public boolean needsSync(long showId) {
    Cursor show = null;
    try {
      show = resolver.query(Shows.withId(showId), new String[] {
          ShowColumns.NEEDS_SYNC,
      }, null, null, null);

      if (show.moveToFirst()) {
        return Cursors.getBoolean(show, ShowColumns.NEEDS_SYNC);
      }

      return false;
    } finally {
      if (show != null) {
        show.close();
      }
    }
  }

  public boolean isUpdated(long traktId, long lastUpdated) {
    Cursor show = null;
    try {
      show = resolver.query(Shows.SHOWS, new String[] {
          ShowColumns.LAST_UPDATED,
      }, ShowColumns.TRAKT_ID + "=?", new String[] {
          String.valueOf(traktId),
      }, null);

      if (show.moveToFirst()) {
        final long showLastUpdated = Cursors.getLong(show, ShowColumns.LAST_UPDATED);
        final long currentTime = System.currentTimeMillis();

        // Every so often shows can get "stuck" out of date. Try to debug this.
        if (showLastUpdated > currentTime + DateUtils.DAY_IN_MILLIS) {
          Timber.e(new IllegalArgumentException("Last updated: "
              + TimeUtils.getIsoTime(showLastUpdated)
              + " - current time: "
              + TimeUtils.getIsoTime(currentTime)), "Wrong LAST_UPDATED");
          return true;
        }

        return lastUpdated > showLastUpdated;
      }

      return false;
    } finally {
      if (show != null) show.close();
    }
  }

  public void insertShowGenres(long showId, List<String> genres) {
    resolver.delete(ProviderSchematic.ShowGenres.fromShow(showId), null, null);

    for (String genre : genres) {
      ContentValues values = new ContentValues();

      values.put(DatabaseContract.ShowGenreColumns.SHOW_ID, showId);
      values.put(DatabaseContract.ShowGenreColumns.GENRE, TextUtils.upperCaseFirstLetter(genre));

      resolver.insert(ProviderSchematic.ShowGenres.fromShow(showId), values);
    }
  }

  public void addToHistory(long showId, long watchedAt) {
    ContentValues values = new ContentValues();
    values.put(EpisodeColumns.WATCHED, true);

    final long firstAiredOffset = FirstAiredOffsetPreference.getInstance().getOffsetMillis();
    final long millis = System.currentTimeMillis() - firstAiredOffset;

    resolver.update(Episodes.fromShow(showId), values, EpisodeColumns.FIRST_AIRED + "<?",
        new String[] {
            String.valueOf(millis),
        });

    if (watchedAt == WATCHED_RELEASE) {
      values.clear();

      Cursor episodes = resolver.query(Episodes.fromShow(showId), new String[] {
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

      resolver.update(Episodes.fromShow(showId), values,
          EpisodeColumns.WATCHED + " AND " + EpisodeColumns.LAST_WATCHED_AT + "<?", new String[] {
              String.valueOf(watchedAt),
          });
    }
  }

  public void removeFromHistory(long showId) {
    ContentValues values = new ContentValues();
    values.put(EpisodeColumns.WATCHED, false);
    values.put(EpisodeColumns.LAST_WATCHED_AT, 0);
    resolver.update(Episodes.fromShow(showId), null, null, null);
  }

  public void setIsInWatchlist(long showId, boolean inWatchlist) {
    setIsInWatchlist(showId, inWatchlist, 0);
  }

  public void setIsInWatchlist(long showId, boolean inWatchlist, long listedAt) {
    ContentValues values = new ContentValues();
    values.put(ShowColumns.IN_WATCHLIST, inWatchlist);
    values.put(ShowColumns.LISTED_AT, listedAt);

    resolver.update(Shows.withId(showId), values, null, null);
  }

  public void setIsInCollection(long traktId, boolean inCollection) {
    final long showId = getId(traktId);
    ContentValues values = new ContentValues();
    values.put(EpisodeColumns.IN_COLLECTION, inCollection);

    final long firstAiredOffset = FirstAiredOffsetPreference.getInstance().getOffsetMillis();
    final long millis = System.currentTimeMillis() - firstAiredOffset;

    resolver.update(Episodes.fromShow(showId), values, EpisodeColumns.FIRST_AIRED + "<?", new String[] {
        String.valueOf(millis),
    });
  }

  private static ContentValues getValues(Show show) {
    ContentValues values = new ContentValues();

    values.put(ShowColumns.TITLE, show.getTitle());
    values.put(ShowColumns.TITLE_NO_ARTICLE, DatabaseUtils.removeLeadingArticle(show.getTitle()));
    if (show.getYear() != null) values.put(ShowColumns.YEAR, show.getYear());
    if (show.getCountry() != null) values.put(ShowColumns.COUNTRY, show.getCountry());
    if (show.getOverview() != null) {
      values.put(ShowColumns.OVERVIEW, show.getOverview());
    }
    if (show.getRuntime() != null) values.put(ShowColumns.RUNTIME, show.getRuntime());
    if (show.getNetwork() != null) values.put(ShowColumns.NETWORK, show.getNetwork());
    if (show.getAirs() != null) {
      values.put(ShowColumns.AIR_DAY, show.getAirs().getDay());
      values.put(ShowColumns.AIR_TIME, show.getAirs().getTime());
      values.put(ShowColumns.AIR_TIMEZONE, show.getAirs().getTimezone());
    }
    if (show.getCertification() != null) {
      values.put(ShowColumns.CERTIFICATION, show.getCertification());
    }

    if (show.getTrailer() != null) values.put(ShowColumns.TRAILER, show.getTrailer());
    if (show.getHomepage() != null) {
      values.put(ShowColumns.HOMEPAGE, show.getHomepage());
    }
    if (show.getStatus() != null) {
      values.put(ShowColumns.STATUS, show.getStatus().toString());
    }

    values.put(ShowColumns.TRAKT_ID, show.getIds().getTrakt());
    values.put(ShowColumns.SLUG, show.getIds().getSlug());
    values.put(ShowColumns.IMDB_ID, show.getIds().getImdb());
    values.put(ShowColumns.TVDB_ID, show.getIds().getTvdb());
    values.put(ShowColumns.TMDB_ID, show.getIds().getTmdb());
    values.put(ShowColumns.TVRAGE_ID, show.getIds().getTvrage());
    if (show.getUpdatedAt() != null) {
      values.put(ShowColumns.LAST_UPDATED, show.getUpdatedAt().getTimeInMillis());
    }

    if (show.getRating() != null) {
      values.put(ShowColumns.RATING, show.getRating());
    }
    if (show.getVotes() != null) {
      values.put(ShowColumns.VOTES, show.getVotes());
    }

    return values;
  }
}
