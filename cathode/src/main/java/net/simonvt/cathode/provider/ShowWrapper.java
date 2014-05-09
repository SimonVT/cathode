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
import java.util.TimeZone;
import net.simonvt.cathode.api.entity.Images;
import net.simonvt.cathode.api.entity.Person;
import net.simonvt.cathode.api.entity.TvShow;
import net.simonvt.cathode.api.entity.TvShow.People;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowActorColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowGenreColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Seasons;
import net.simonvt.cathode.provider.ProviderSchematic.ShowActors;
import net.simonvt.cathode.provider.ProviderSchematic.ShowGenres;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.util.ApiUtils;
import net.simonvt.cathode.util.DateUtils;
import timber.log.Timber;

public final class ShowWrapper {

  private static final String TAG = "ShowWrapper";

  private ShowWrapper() {
  }

  private static void log(ContentResolver resolver, long showId, String message) {
    final int tvdbId = getTvdbId(resolver, showId);
    try {
      throw new Exception("tvdbId: " + tvdbId + " - " + message);
    } catch (Exception e) {
      Timber.e(e, "tvdbId: " + tvdbId + " - " + message);
    }
  }

  public static int getTvdbId(ContentResolver resolver, long showId) {
    Cursor c = resolver.query(Shows.withId(showId), new String[] {
        ShowColumns.TVDB_ID,
    }, null, null, null);

    int tvdbId = -1;
    if (c.moveToFirst()) {
      tvdbId = c.getInt(c.getColumnIndex(ShowColumns.TVDB_ID));
    }

    c.close();

    return tvdbId;
  }

  public static void insertShowGenres(ContentResolver resolver, long showId, List<String> genres) {
    resolver.delete(ShowGenres.fromShow(showId), null, null);

    for (String genre : genres) {
      ContentValues cv = new ContentValues();

      cv.put(ShowGenreColumns.SHOW_ID, showId);
      cv.put(ShowGenreColumns.GENRE, genre);

      resolver.insert(ShowGenres.fromShow(showId), cv);
    }
  }

  public static long getShowId(ContentResolver resolver, TvShow show) {
    Cursor c = resolver.query(Shows.SHOWS, new String[] {
        ShowColumns.ID,
    }, ShowColumns.TVDB_ID + "=?", new String[] {
        String.valueOf(show.getTvdbId()),
    }, null);

    long id = !c.moveToFirst() ? -1L : c.getLong(c.getColumnIndex(ShowColumns.ID));

    c.close();

    return id;
  }

  public static long getShowId(ContentResolver resolver, int tvdbId) {
    Cursor c = resolver.query(Shows.SHOWS, new String[] {
        ShowColumns.ID,
    }, ShowColumns.TVDB_ID + "=?", new String[] {
        String.valueOf(tvdbId),
    }, null);

    long id = !c.moveToFirst() ? -1L : c.getLong(c.getColumnIndex(ShowColumns.ID));

    c.close();

    return id;
  }

  public static String getShowName(ContentResolver resolver, long showId) {
    Cursor c = resolver.query(Shows.SHOWS, new String[] {
        ShowColumns.TITLE,
    }, ShowColumns.ID + "=?", new String[] {
        String.valueOf(showId),
    }, null);

    String title = null;
    if (c.moveToFirst()) {
      title = c.getString(c.getColumnIndex(ShowColumns.TITLE));
    }

    c.close();

    return title;
  }

  public static long getSeasonId(ContentResolver resolver, long showId, int seasonNumber) {
    Cursor c = resolver.query(Seasons.SEASONS, new String[] {
            SeasonColumns.ID,
        }, SeasonColumns.SHOW_ID + "=? AND " + SeasonColumns.SEASON + "=?", new String[] {
            String.valueOf(showId), String.valueOf(seasonNumber),
        }, null
    );

    long id = !c.moveToFirst() ? -1L : c.getLong(c.getColumnIndex(SeasonColumns.ID));

    c.close();

    return id;
  }

  public static boolean exists(ContentResolver resolver, int tvdbId) {
    Cursor c = null;
    try {
      c = resolver.query(Shows.SHOWS, new String[] {
          ShowColumns.LAST_UPDATED,
      }, ShowColumns.TVDB_ID + "=?", new String[] {
          String.valueOf(tvdbId),
      }, null);

      return c.moveToFirst();
    } finally {
      if (c != null) c.close();
    }
  }

  public static boolean needsUpdate(ContentResolver resolver, int tvdbId, long lastUpdated) {
    if (lastUpdated == 0) return true;

    Cursor c = null;
    try {
      c = resolver.query(Shows.SHOWS, new String[] {
          ShowColumns.LAST_UPDATED,
      }, ShowColumns.TVDB_ID + "=?", new String[] {
          String.valueOf(tvdbId),
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
    Cursor c = resolver.query(Shows.withId(id), new String[] {
        ShowColumns.IN_WATCHLIST, ShowColumns.FULL_SYNC_REQUESTED, ShowColumns.IN_COLLECTION_COUNT,
        ShowColumns.WATCHED_COUNT,
    }, null, null, null);

    if (c.moveToFirst()) {
      final boolean inWatchlist = c.getInt(c.getColumnIndex(ShowColumns.IN_WATCHLIST)) == 1;
      final long fullSyncRequested = c.getLong(c.getColumnIndex(ShowColumns.FULL_SYNC_REQUESTED));
      final int watchedCount = c.getInt(c.getColumnIndex(ShowColumns.WATCHED_COUNT));
      final int collectionCount = c.getInt(c.getColumnIndex(ShowColumns.IN_COLLECTION_COUNT));

      return inWatchlist || watchedCount > 0 || collectionCount > 0 || fullSyncRequested > 0;
    }

    return false;
  }

  public static long updateOrInsertShow(ContentResolver resolver, TvShow show) {
    long showId = getShowId(resolver, show);

    if (showId == -1) {
      showId = insertShow(resolver, show);
    } else {
      updateShow(resolver, show);
    }

    return showId;
  }

  public static void updateShow(ContentResolver resolver, TvShow show) {
    final long id = getShowId(resolver, show);
    ContentValues cv = getShowCVs(show);
    resolver.update(Shows.withId(id), cv, null, null);

    if (show.getGenres() != null) insertShowGenres(resolver, id, show.getGenres());
    if (show.getPeople() != null) insertPeople(resolver, id, show.getPeople());
  }

  public static long insertShow(ContentResolver resolver, TvShow show) {
    ContentValues cv = getShowCVs(show);

    Uri uri = resolver.insert(Shows.SHOWS, cv);
    final long showId = Long.valueOf(Shows.getShowId(uri));

    if (show.getGenres() != null) insertShowGenres(resolver, showId, show.getGenres());
    if (show.getPeople() != null) insertPeople(resolver, showId, show.getPeople());

    return showId;
  }

  private static void insertPeople(ContentResolver resolver, long showId, People people) {
    resolver.delete(ShowActors.fromShow(showId), null, null);
    List<Person> actors = people.getActors();
    for (Person actor : actors) {
      String name = actor.getName();
      if (name == null) {
        continue;
      }

      ContentValues cv = new ContentValues();
      cv.put(ShowActorColumns.SHOW_ID, showId);
      cv.put(ShowActorColumns.NAME, actor.getName());
      cv.put(ShowActorColumns.CHARACTER, actor.getCharacter());
      if (actor.getImages() != null && !ApiUtils.isPlaceholder(actor.getImages().getHeadshot())) {
        cv.put(ShowActorColumns.HEADSHOT, actor.getImages().getHeadshot());
      }

      resolver.insert(ShowActors.fromShow(showId), cv);
    }
  }

  public static void setWatched(ContentResolver resolver, int tvdbId, boolean watched) {
    setWatched(resolver, getShowId(resolver, tvdbId), watched);
  }

  public static void setWatched(ContentResolver resolver, long showId, boolean watched) {
    ContentValues cv = new ContentValues();
    cv.put(EpisodeColumns.WATCHED, watched);

    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT-8:00"));
    final long millis = cal.getTimeInMillis();

    resolver.update(Episodes.fromShow(showId), cv, EpisodeColumns.FIRST_AIRED + "<?", new String[] {
            String.valueOf(millis),
        }
    );
  }

  public static void setIsInWatchlist(ContentResolver resolver, int tvdbId, boolean inWatchlist) {
    setIsInWatchlist(resolver, getShowId(resolver, tvdbId), inWatchlist);
  }

  public static void setIsInWatchlist(ContentResolver resolver, long showId, boolean inWatchlist) {
    ContentValues cv = new ContentValues();
    cv.put(ShowColumns.IN_WATCHLIST, inWatchlist);

    resolver.update(Shows.withId(showId), cv, null, null);
  }

  public static void setIsInCollection(ContentResolver resolver, int tvdbId, boolean inCollection) {
    setIsInCollection(resolver, getShowId(resolver, tvdbId), inCollection);
  }

  public static void setIsInCollection(ContentResolver resolver, long showId,
      boolean inCollection) {
    ContentValues cv = new ContentValues();
    cv.put(EpisodeColumns.IN_COLLECTION, inCollection);

    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT-8:00"));
    final long millis = cal.getTimeInMillis();

    resolver.update(Episodes.fromShow(showId), cv, EpisodeColumns.FIRST_AIRED + "<?", new String[] {
            String.valueOf(millis),
        }
    );
  }

  public static void setIsHidden(ContentResolver resolver, long showId, boolean isHidden) {
    ContentValues cv = new ContentValues();
    cv.put(ShowColumns.HIDDEN, isHidden);

    resolver.update(Shows.withId(showId), cv, null, null);
  }

  private static ContentValues getShowCVs(TvShow show) {
    ContentValues cv = new ContentValues();

    cv.put(ShowColumns.TITLE, show.getTitle());
    cv.put(ShowColumns.YEAR, show.getYear());
    cv.put(ShowColumns.URL, show.getUrl());
    if (show.getFirstAiredIso() != null) {
      cv.put(ShowColumns.FIRST_AIRED, DateUtils.getMillis(show.getFirstAiredIso()));
    }
    cv.put(ShowColumns.COUNTRY, show.getCountry());
    cv.put(ShowColumns.OVERVIEW, show.getOverview());
    cv.put(ShowColumns.RUNTIME, show.getRuntime());
    if (show.getNetwork() != null) cv.put(ShowColumns.NETWORK, show.getNetwork());
    if (show.getAirDay() != null) cv.put(ShowColumns.AIR_DAY, show.getAirDay().toString());
    if (show.getAirTime() != null) cv.put(ShowColumns.AIR_TIME, show.getAirTime());
    if (show.getCertification() != null) cv.put(ShowColumns.CERTIFICATION, show.getCertification());
    cv.put(ShowColumns.IMDB_ID, show.getImdbId());
    cv.put(ShowColumns.TVDB_ID, show.getTvdbId());
    cv.put(ShowColumns.TVRAGE_ID, show.getTvrageId());
    if (show.getLastUpdated() != null) cv.put(ShowColumns.LAST_UPDATED, show.getLastUpdated());
    if (show.getImages() != null) {
      Images images = show.getImages();
      if (!ApiUtils.isPlaceholder(images.getPoster())) {
        cv.put(ShowColumns.POSTER, images.getPoster());
      }
      if (!ApiUtils.isPlaceholder(images.getFanart())) {
        cv.put(ShowColumns.FANART, images.getFanart());
      }
      if (!ApiUtils.isPlaceholder(images.getBanner())) {
        cv.put(ShowColumns.BANNER, images.getBanner());
      }
    }
    if (show.getRatings() != null) {
      cv.put(ShowColumns.RATING_PERCENTAGE, show.getRatings().getPercentage());
      cv.put(ShowColumns.RATING_VOTES, show.getRatings().getVotes());
      cv.put(ShowColumns.RATING_LOVED, show.getRatings().getLoved());
      cv.put(ShowColumns.RATING_HATED, show.getRatings().getHated());
    }
    if (show.getStats() != null) {
      cv.put(ShowColumns.WATCHERS, show.getStats().getWatchers());
      cv.put(ShowColumns.PLAYS, show.getStats().getPlays());
      cv.put(ShowColumns.SCROBBLES, show.getStats().getScrobbles());
      cv.put(ShowColumns.CHECKINS, show.getStats().getCheckins());
    }
    if (show.getStatus() != null) cv.put(ShowColumns.STATUS, show.getStatus().toString());
    if (show.getRatingAdvanced() != null) {
      cv.put(ShowColumns.RATING, show.getRatingAdvanced());
    }
    if (show.isInWatchlist() != null) cv.put(ShowColumns.IN_WATCHLIST, show.isInWatchlist());

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

  public static void setRating(ContentResolver resolver, long showId, int rating) {
    ContentValues cv = new ContentValues();
    cv.put(ShowColumns.RATING, rating);
    resolver.update(Shows.withId(showId), cv, null, null);
  }
}
