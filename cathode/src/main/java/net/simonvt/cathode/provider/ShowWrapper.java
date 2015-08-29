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
import java.util.Calendar;
import java.util.List;
import net.simonvt.cathode.api.entity.Images;
import net.simonvt.cathode.api.entity.Show;
import net.simonvt.cathode.api.util.TimeUtils;
import net.simonvt.cathode.database.DatabaseUtils;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowGenreColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Seasons;
import net.simonvt.cathode.provider.ProviderSchematic.ShowGenres;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;

public final class ShowWrapper {

  private ShowWrapper() {
  }

  public static long getTraktId(ContentResolver resolver, long showId) {
    Cursor c = resolver.query(Shows.withId(showId), new String[] {
        ShowColumns.TRAKT_ID,
    }, null, null, null);

    long traktId = -1L;
    if (c.moveToFirst()) {
      traktId = c.getInt(c.getColumnIndex(ShowColumns.TRAKT_ID));
    }

    c.close();

    return traktId;
  }

  public static long getShowId(ContentResolver resolver, Show show) {
    return getShowId(resolver, show.getIds().getTrakt());
  }

  public static long getShowId(ContentResolver resolver, long traktId) {
    Cursor c = resolver.query(Shows.SHOWS, new String[] {
        ShowColumns.ID,
    }, ShowColumns.TRAKT_ID + "=?", new String[] {
        String.valueOf(traktId),
    }, null);

    long id = -1L;

    if (c.moveToFirst()) {
      id = c.getLong(c.getColumnIndex(ShowColumns.ID));
    }

    c.close();

    return id;
  }

  public static long getSeasonId(ContentResolver resolver, long showId, int seasonNumber) {
    Cursor c = resolver.query(Seasons.SEASONS, new String[] {
        SeasonColumns.ID,
    }, SeasonColumns.SHOW_ID + "=? AND " + SeasonColumns.SEASON + "=?", new String[] {
        String.valueOf(showId), String.valueOf(seasonNumber),
    }, null);

    long id = !c.moveToFirst() ? -1L : c.getLong(c.getColumnIndex(SeasonColumns.ID));

    c.close();

    return id;
  }

  public static boolean exists(ContentResolver resolver, long traktId) {
    Cursor c = null;
    try {
      c = resolver.query(Shows.SHOWS, new String[] {
          ShowColumns.ID,
      }, ShowColumns.TRAKT_ID + "=?", new String[] {
          String.valueOf(traktId),
      }, null);

      return c.moveToFirst();
    } finally {
      if (c != null) c.close();
    }
  }

  public static boolean needsUpdate(ContentResolver resolver, long traktId, String lastUpdatedIso) {
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
        return lastUpdated > c.getLong(c.getColumnIndex(ShowColumns.LAST_UPDATED));
      }

      return true;
    } finally {
      if (c != null) c.close();
    }
  }

  public static boolean shouldSyncFully(ContentResolver resolver, long id) {
    Cursor c = null;
    try {
      c = resolver.query(Shows.withId(id), new String[] {
          ShowColumns.IN_WATCHLIST, ShowColumns.FULL_SYNC_REQUESTED,
          ShowColumns.IN_COLLECTION_COUNT, ShowColumns.WATCHED_COUNT,
      }, null, null, null);

      if (c.moveToFirst()) {
        final boolean inWatchlist = c.getInt(c.getColumnIndex(ShowColumns.IN_WATCHLIST)) == 1;
        final long fullSyncRequested = c.getLong(c.getColumnIndex(ShowColumns.FULL_SYNC_REQUESTED));
        final int watchedCount = c.getInt(c.getColumnIndex(ShowColumns.WATCHED_COUNT));
        final int collectionCount = c.getInt(c.getColumnIndex(ShowColumns.IN_COLLECTION_COUNT));

        return inWatchlist || watchedCount > 0 || collectionCount > 0 || fullSyncRequested > 0;
      }

      return false;
    } finally {
      if (c != null) {
        c.close();
      }
    }
  }

  public static long createShow(ContentResolver resolver, long traktId) {
    final long showId = getShowId(resolver, traktId);
    if (showId != -1L) {
      throw new IllegalStateException("Trying to create show that already exists");
    }

    ContentValues cv = new ContentValues();
    cv.put(ShowColumns.TRAKT_ID, traktId);
    cv.put(ShowColumns.NEEDS_SYNC, 1);

    return Shows.getShowId(resolver.insert(Shows.SHOWS, cv));
  }

  public static long updateOrInsertShow(ContentResolver resolver, Show show) {
    long showId = getShowId(resolver, show);

    if (showId == -1) {
      showId = insertShow(resolver, show);
    } else {
      updateShow(resolver, show);
    }

    return showId;
  }

  private static void updateShow(ContentResolver resolver, Show show) {
    final long id = getShowId(resolver, show);
    ContentValues cv = getShowCVs(show);
    resolver.update(Shows.withId(id), cv, null, null);

    if (show.getGenres() != null) {
      insertShowGenres(resolver, id, show.getGenres());
    }
  }

  public static long insertShow(ContentResolver resolver, Show show) {
    ContentValues cv = getShowCVs(show);

    Uri uri = resolver.insert(Shows.SHOWS, cv);
    final long id = Shows.getShowId(uri);

    if (show.getGenres() != null) {
      insertShowGenres(resolver, id, show.getGenres());
    }

    return id;
  }

  private static void insertShowGenres(ContentResolver resolver, long showId, List<String> genres) {
    resolver.delete(ShowGenres.fromShow(showId), null, null);

    for (String genre : genres) {
      ContentValues cv = new ContentValues();

      cv.put(ShowGenreColumns.SHOW_ID, showId);
      cv.put(ShowGenreColumns.GENRE, genre);

      resolver.insert(ShowGenres.fromShow(showId), cv);
    }
  }

  public static void setWatched(ContentResolver resolver, int tvdbId, boolean watched) {
    setWatched(resolver, getShowId(resolver, tvdbId), watched);
  }

  public static void setWatched(ContentResolver resolver, long showId, boolean watched) {
    ContentValues cv = new ContentValues();
    cv.put(EpisodeColumns.WATCHED, watched);

    Calendar cal = Calendar.getInstance();
    final long millis = cal.getTimeInMillis();

    resolver.update(Episodes.fromShow(showId), cv, EpisodeColumns.FIRST_AIRED + "<?", new String[] {
        String.valueOf(millis),
    });
  }

  public static void setIsInWatchlist(ContentResolver resolver, long showId, boolean inWatchlist) {
    setIsInWatchlist(resolver, showId, inWatchlist, 0);
  }

  public static void setIsInWatchlist(ContentResolver resolver, long showId, boolean inWatchlist,
      long listedAt) {
    ContentValues cv = new ContentValues();
    cv.put(ShowColumns.IN_WATCHLIST, inWatchlist);
    cv.put(ShowColumns.LISTED_AT, listedAt);

    resolver.update(Shows.withId(showId), cv, null, null);
  }

  public static void setIsInCollection(ContentResolver resolver, long traktId,
      boolean inCollection) {
    final long showId = getShowId(resolver, traktId);
    ContentValues cv = new ContentValues();
    cv.put(EpisodeColumns.IN_COLLECTION, inCollection);

    Calendar cal = Calendar.getInstance();
    final long millis = cal.getTimeInMillis();

    resolver.update(Episodes.fromShow(showId), cv, EpisodeColumns.FIRST_AIRED + "<?", new String[] {
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
    if (show.getOverview() != null) cv.put(ShowColumns.OVERVIEW, show.getOverview());
    if (show.getRuntime() != null) cv.put(ShowColumns.RUNTIME, show.getRuntime());
    if (show.getNetwork() != null) cv.put(ShowColumns.NETWORK, show.getNetwork());
    if (show.getAirs() != null) {
      cv.put(ShowColumns.AIR_DAY, show.getAirs().getDay());
      cv.put(ShowColumns.AIR_TIME, show.getAirs().getTime());
      cv.put(ShowColumns.AIR_TIMEZONE, show.getAirs().getTimezone());
    }
    if (show.getCertification() != null) cv.put(ShowColumns.CERTIFICATION, show.getCertification());

    if (show.getTrailer() != null) cv.put(ShowColumns.TRAILER, show.getTrailer());
    if (show.getHomepage() != null) cv.put(ShowColumns.HOMEPAGE, show.getHomepage());
    if (show.getStatus() != null) cv.put(ShowColumns.STATUS, show.getStatus().toString());

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
      if (images.getFanart() != null) cv.put(ShowColumns.FANART, images.getFanart().getFull());
      if (images.getPoster() != null) cv.put(ShowColumns.POSTER, images.getPoster().getFull());
      if (images.getLogo() != null) cv.put(ShowColumns.LOGO, images.getLogo().getFull());
      if (images.getClearart() != null) {
        cv.put(ShowColumns.CLEARART, images.getClearart().getFull());
      }
      if (images.getBanner() != null) cv.put(ShowColumns.BANNER, images.getBanner().getFull());
      if (images.getThumb() != null) cv.put(ShowColumns.THUMB, images.getThumb().getFull());
    }

    if (show.getRating() != null) {
      cv.put(ShowColumns.RATING, show.getRating());
    }
    if (show.getVotes() != null) {
      cv.put(ShowColumns.VOTES, show.getVotes());
    }

    return cv;
  }

  public static long getLastUpdated(ContentResolver resolver) {
    Cursor c = resolver.query(Shows.SHOWS, new String[] {
        ShowColumns.LAST_UPDATED,
    }, null, null, ShowColumns.LAST_UPDATED + " DESC LIMIT 1");

    long lastUpdated = -1L;
    if (c.moveToFirst()) {
      lastUpdated = c.getLong(c.getColumnIndex(ShowColumns.LAST_UPDATED));
    }

    c.close();

    return lastUpdated;
  }
}
