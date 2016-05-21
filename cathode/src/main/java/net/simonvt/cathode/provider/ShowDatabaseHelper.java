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
import java.util.Calendar;
import java.util.List;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.api.entity.Images;
import net.simonvt.cathode.api.entity.Show;
import net.simonvt.cathode.api.util.TimeUtils;
import net.simonvt.cathode.database.DatabaseUtils;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.schematic.Cursors;

public final class ShowDatabaseHelper {

  private static volatile ShowDatabaseHelper instance;

  public static ShowDatabaseHelper getInstance(Context context) {
    if (instance == null) {
      synchronized (ShowDatabaseHelper.class) {
        if (instance == null) {
          instance = new ShowDatabaseHelper(context);
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

    CathodeApp.inject(context, this);
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

  public boolean exists(long traktId) {
    return getId(traktId) != -1L;
  }

  public long getTraktId(long showId) {
    Cursor c = resolver.query(Shows.withId(showId), new String[] {
        ShowColumns.TRAKT_ID,
    }, null, null, null);

    long traktId = -1L;
    if (c.moveToFirst()) {
      traktId = Cursors.getInt(c, ShowColumns.TRAKT_ID);
    }

    c.close();

    return traktId;
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
    ContentValues cv = new ContentValues();
    cv.put(ShowColumns.TRAKT_ID, traktId);
    cv.put(ShowColumns.NEEDS_SYNC, 1);

    return Shows.getShowId(resolver.insert(Shows.SHOWS, cv));
  }

  /**
   * Creates the show if it does not exist.
   */
  public long updateShow(Show show) {
    IdResult result = getIdOrCreate(show.getIds().getTrakt());
    final long id = result.showId;

    ContentValues cv = getShowCVs(show);
    resolver.update(Shows.withId(id), cv, null, null);

    if (show.getGenres() != null) {
      insertShowGenres(id, show.getGenres());
    }

    return id;
  }

  public boolean needsUpdate(long traktId, String lastUpdatedIso) {
    long lastUpdated = TimeUtils.getMillis(lastUpdatedIso);
    Cursor c = null;
    try {
      c = resolver.query(Shows.SHOWS, new String[] {
          ShowColumns.LAST_UPDATED,
      }, ShowColumns.TRAKT_ID + "=?", new String[] {
          String.valueOf(traktId),
      }, null);

      boolean exists = c.moveToFirst();
      if (exists) {
        return lastUpdated > Cursors.getLong(c, ShowColumns.LAST_UPDATED);
      }

      return true;
    } finally {
      if (c != null) c.close();
    }
  }

  public boolean shouldSyncFully(long id) {
    Cursor c = null;
    try {
      c = resolver.query(Shows.withId(id), new String[] {
          ShowColumns.IN_WATCHLIST, ShowColumns.FULL_SYNC_REQUESTED,
          ShowColumns.IN_COLLECTION_COUNT, ShowColumns.WATCHED_COUNT,
      }, null, null, null);

      if (c.moveToFirst()) {
        final boolean inWatchlist = Cursors.getInt(c, ShowColumns.IN_WATCHLIST) == 1;
        final long fullSyncRequested = Cursors.getLong(c, ShowColumns.FULL_SYNC_REQUESTED);
        final int watchedCount = Cursors.getInt(c, ShowColumns.WATCHED_COUNT);
        final int collectionCount = Cursors.getInt(c, ShowColumns.IN_COLLECTION_COUNT);

        return inWatchlist || watchedCount > 0 || collectionCount > 0 || fullSyncRequested > 0;
      }

      return false;
    } finally {
      if (c != null) {
        c.close();
      }
    }
  }

  public void insertShowGenres(long showId, List<String> genres) {
    resolver.delete(ProviderSchematic.ShowGenres.fromShow(showId), null, null);

    for (String genre : genres) {
      ContentValues cv = new ContentValues();

      cv.put(DatabaseContract.ShowGenreColumns.SHOW_ID, showId);
      cv.put(DatabaseContract.ShowGenreColumns.GENRE, genre);

      resolver.insert(ProviderSchematic.ShowGenres.fromShow(showId), cv);
    }
  }

  public void setWatched(long showId, boolean watched) {
    ContentValues cv = new ContentValues();
    cv.put(DatabaseContract.EpisodeColumns.WATCHED, watched);

    Calendar cal = Calendar.getInstance();
    final long millis = cal.getTimeInMillis();

    resolver.update(ProviderSchematic.Episodes.fromShow(showId), cv,
        DatabaseContract.EpisodeColumns.FIRST_AIRED + "<?", new String[] {
            String.valueOf(millis),
        });
  }

  public void setIsInWatchlist(long showId, boolean inWatchlist) {
    setIsInWatchlist(showId, inWatchlist, 0);
  }

  public void setIsInWatchlist(long showId, boolean inWatchlist, long listedAt) {
    ContentValues cv = new ContentValues();
    cv.put(ShowColumns.IN_WATCHLIST, inWatchlist);
    cv.put(ShowColumns.LISTED_AT, listedAt);

    resolver.update(Shows.withId(showId), cv, null, null);
  }

  public void setIsInCollection(long traktId, boolean inCollection) {
    final long showId = getId(traktId);
    ContentValues cv = new ContentValues();
    cv.put(DatabaseContract.EpisodeColumns.IN_COLLECTION, inCollection);

    Calendar cal = Calendar.getInstance();
    final long millis = cal.getTimeInMillis();

    resolver.update(ProviderSchematic.Episodes.fromShow(showId), cv,
        DatabaseContract.EpisodeColumns.FIRST_AIRED + "<?", new String[] {
            String.valueOf(millis),
        });
  }

  private static ContentValues getShowCVs(Show show) {
    ContentValues cv = new ContentValues();

    cv.put(ShowColumns.NEEDS_SYNC, 0);

    cv.put(ShowColumns.TITLE, show.getTitle());
    cv.put(ShowColumns.TITLE_NO_ARTICLE, DatabaseUtils.removeLeadingArticle(show.getTitle()));
    if (show.getYear() != null) cv.put(ShowColumns.YEAR, show.getYear());
    if (show.getCountry() != null) cv.put(ShowColumns.COUNTRY, show.getCountry());
    if (show.getOverview() != null) {
      cv.put(ShowColumns.OVERVIEW, show.getOverview());
    }
    if (show.getRuntime() != null) cv.put(ShowColumns.RUNTIME, show.getRuntime());
    if (show.getNetwork() != null) cv.put(ShowColumns.NETWORK, show.getNetwork());
    if (show.getAirs() != null) {
      cv.put(ShowColumns.AIR_DAY, show.getAirs().getDay());
      cv.put(ShowColumns.AIR_TIME, show.getAirs().getTime());
      cv.put(ShowColumns.AIR_TIMEZONE, show.getAirs().getTimezone());
    }
    if (show.getCertification() != null) {
      cv.put(ShowColumns.CERTIFICATION, show.getCertification());
    }

    if (show.getTrailer() != null) cv.put(ShowColumns.TRAILER, show.getTrailer());
    if (show.getHomepage() != null) {
      cv.put(ShowColumns.HOMEPAGE, show.getHomepage());
    }
    if (show.getStatus() != null) {
      cv.put(ShowColumns.STATUS, show.getStatus().toString());
    }

    cv.put(ShowColumns.TRAKT_ID, show.getIds().getTrakt());
    cv.put(ShowColumns.SLUG, show.getIds().getSlug());
    cv.put(ShowColumns.IMDB_ID, show.getIds().getImdb());
    cv.put(ShowColumns.TVDB_ID, show.getIds().getTvdb());
    cv.put(ShowColumns.TMDB_ID, show.getIds().getTmdb());
    cv.put(ShowColumns.TVRAGE_ID, show.getIds().getTvrage());
    if (show.getUpdatedAt() != null) {
      cv.put(ShowColumns.LAST_UPDATED, show.getUpdatedAt().getTimeInMillis());
    }
    if (show.getImages() != null) {
      Images images = show.getImages();
      if (images.getFanart() != null) {
        cv.put(ShowColumns.FANART, images.getFanart().getFull());
      }
      if (images.getPoster() != null) {
        cv.put(ShowColumns.POSTER, images.getPoster().getFull());
      }
      if (images.getLogo() != null) {
        cv.put(ShowColumns.LOGO, images.getLogo().getFull());
      }
      if (images.getClearart() != null) {
        cv.put(ShowColumns.CLEARART, images.getClearart().getFull());
      }
      if (images.getBanner() != null) {
        cv.put(ShowColumns.BANNER, images.getBanner().getFull());
      }
      if (images.getThumb() != null) {
        cv.put(ShowColumns.THUMB, images.getThumb().getFull());
      }
    }

    if (show.getRating() != null) {
      cv.put(ShowColumns.RATING, show.getRating());
    }
    if (show.getVotes() != null) {
      cv.put(ShowColumns.VOTES, show.getVotes());
    }

    return cv;
  }
}
