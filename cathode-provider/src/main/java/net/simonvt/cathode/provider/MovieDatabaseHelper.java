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

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.text.format.DateUtils;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.simonvt.cathode.Injector;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.common.util.TextUtils;
import net.simonvt.cathode.database.DatabaseUtils;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieGenreColumns;
import net.simonvt.cathode.provider.ProviderSchematic.MovieGenres;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.generated.CathodeProvider;
import net.simonvt.schematic.Cursors;
import timber.log.Timber;

public final class MovieDatabaseHelper {

  public static final long WATCHED_RELEASE = -1L;

  private static volatile MovieDatabaseHelper instance;

  public static MovieDatabaseHelper getInstance(Context context) {
    if (instance == null) {
      synchronized (MovieDatabaseHelper.class) {
        if (instance == null) {
          instance = new MovieDatabaseHelper(context.getApplicationContext());
        }
      }
    }
    return instance;
  }

  private static final Object LOCK_ID = new Object();

  private Context context;

  private ContentResolver resolver;

  private MovieDatabaseHelper(Context context) {
    this.context = context;

    resolver = context.getContentResolver();

    Injector.inject(this);
  }

  public long getTraktId(long movieId) {
    Cursor c = resolver.query(Movies.withId(movieId), new String[] {
        MovieColumns.TRAKT_ID,
    }, null, null, null);

    long traktId = -1;
    if (c.moveToFirst()) {
      traktId = Cursors.getLong(c, MovieColumns.TRAKT_ID);
    }

    c.close();

    return traktId;
  }

  public int getTmdbId(long movieId) {
    Cursor c = resolver.query(Movies.withId(movieId), new String[] {
        MovieColumns.TMDB_ID,
    }, null, null, null);

    int traktId = -1;
    if (c.moveToFirst()) {
      traktId = Cursors.getInt(c, MovieColumns.TMDB_ID);
    }

    c.close();

    return traktId;
  }

  public long getId(long traktId) {
    synchronized (LOCK_ID) {
      Cursor c = resolver.query(Movies.MOVIES, new String[] {
          MovieColumns.ID,
      }, MovieColumns.TRAKT_ID + "=?", new String[] {
          String.valueOf(traktId),
      }, null);

      long id = !c.moveToFirst() ? -1L : Cursors.getLong(c, MovieColumns.ID);

      c.close();

      return id;
    }
  }

  public long getIdFromTmdb(int tmdbId) {
    synchronized (LOCK_ID) {
      Cursor c = resolver.query(Movies.MOVIES, new String[] {
          MovieColumns.ID,
      }, MovieColumns.TMDB_ID + "=?", new String[] {
          String.valueOf(tmdbId),
      }, null);

      long id = !c.moveToFirst() ? -1L : Cursors.getLong(c, MovieColumns.ID);

      c.close();

      return id;
    }
  }

  public boolean needsSync(long movieId) {
    Cursor movie = null;
    try {
      movie = resolver.query(Movies.withId(movieId), new String[] {
          MovieColumns.NEEDS_SYNC,
      }, null, null, null);

      if (movie.moveToFirst()) {
        return Cursors.getBoolean(movie, MovieColumns.NEEDS_SYNC);
      }

      return false;
    } finally {
      if (movie != null) {
        movie.close();
      }
    }
  }

  public long lastSync(long movieId) {
    Cursor movie = null;
    try {
      movie = resolver.query(Movies.withId(movieId), new String[] {
          MovieColumns.LAST_SYNC,
      }, null, null, null);

      if (movie.moveToFirst()) {
        return Cursors.getLong(movie, MovieColumns.LAST_SYNC);
      }

      return 0L;
    } finally {
      if (movie != null) {
        movie.close();
      }
    }
  }

  public boolean isUpdated(long traktId, long lastUpdated) {
    Cursor movie = null;
    try {
      movie = resolver.query(Movies.MOVIES, new String[] {
          MovieColumns.LAST_UPDATED,
      }, MovieColumns.TRAKT_ID + "=?", new String[] {
          String.valueOf(traktId),
      }, null);

      if (movie.moveToFirst()) {
        final long movieLastUpdated = Cursors.getLong(movie, MovieColumns.LAST_UPDATED);
        final long currentTime = System.currentTimeMillis();
        if (movieLastUpdated > currentTime) {
          Timber.e(new IllegalArgumentException(
                  "Last updated: " + movieLastUpdated + " - current time: " + currentTime),
              "Wrong LAST_UPDATED");
          return true;
        }
        return lastUpdated > movieLastUpdated;
      }

      return false;
    } finally {
      if (movie != null) movie.close();
    }
  }

  public static final class IdResult {

    public long movieId;

    public boolean didCreate;

    public IdResult(long movieId, boolean didCreate) {
      this.movieId = movieId;
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
    values.put(MovieColumns.TRAKT_ID, traktId);
    values.put(MovieColumns.NEEDS_SYNC, true);

    return Movies.getId(resolver.insert(Movies.MOVIES, values));
  }

  public long fullUpdate(Movie movie) {
    IdResult result = getIdOrCreate(movie.getIds().getTrakt());
    final long id = result.movieId;

    ContentValues values = getValues(movie);
    values.put(MovieColumns.NEEDS_SYNC, false);
    values.put(MovieColumns.LAST_SYNC, System.currentTimeMillis());
    resolver.update(Movies.withId(id), values, null, null);

    if (movie.getGenres() != null) {
      insertGenres(id, movie.getGenres());
    }

    return id;
  }

  public long partialUpdate(Movie movie) {
    IdResult result = getIdOrCreate(movie.getIds().getTrakt());
    final long id = result.movieId;

    ContentValues values = getValues(movie);
    resolver.update(Movies.withId(id), values, null, null);

    if (movie.getGenres() != null) {
      insertGenres(id, movie.getGenres());
    }

    return id;
  }

  public void insertGenres(long movieId, List<String> genres) {
    try {
      ArrayList<ContentProviderOperation> ops = new ArrayList<>();
      ContentProviderOperation op;

      op = ContentProviderOperation.newDelete(MovieGenres.fromMovie(movieId)).build();
      ops.add(op);

      for (String genre : genres) {
        op = ContentProviderOperation.newInsert(MovieGenres.fromMovie(movieId))
            .withValue(MovieGenreColumns.MOVIE_ID, movieId)
            .withValue(MovieGenreColumns.GENRE, TextUtils.upperCaseFirstLetter(genre))
            .build();
        ops.add(op);
      }

      resolver.applyBatch(CathodeProvider.AUTHORITY, ops);
    } catch (RemoteException e) {
      Timber.e(e, "Updating movie genres failed");
    } catch (OperationApplicationException e) {
      Timber.e(e, "Updating movie genres failed");
    }
  }

  private static ContentValues getValues(Movie movie) {
    ContentValues values = new ContentValues();

    values.put(MovieColumns.TITLE, movie.getTitle());
    values.put(MovieColumns.TITLE_NO_ARTICLE, DatabaseUtils.removeLeadingArticle(movie.getTitle()));
    if (movie.getYear() != null) values.put(MovieColumns.YEAR, movie.getYear());
    if (movie.getReleased() != null) values.put(MovieColumns.RELEASED, movie.getReleased());
    if (movie.getRuntime() != null) values.put(MovieColumns.RUNTIME, movie.getRuntime());
    if (movie.getTagline() != null) values.put(MovieColumns.TAGLINE, movie.getTagline());
    if (movie.getOverview() != null) values.put(MovieColumns.OVERVIEW, movie.getOverview());

    values.put(MovieColumns.TRAKT_ID, movie.getIds().getTrakt());
    values.put(MovieColumns.SLUG, movie.getIds().getSlug());
    values.put(MovieColumns.IMDB_ID, movie.getIds().getImdb());
    values.put(MovieColumns.TMDB_ID, movie.getIds().getTmdb());

    if (movie.getLanguage() != null) values.put(MovieColumns.LANGUAGE, movie.getLanguage());

    if (movie.getCertification() != null) {
      values.put(MovieColumns.CERTIFICATION, movie.getCertification());
    }

    if (movie.getTrailer() != null) {
      values.put(MovieColumns.TRAILER, movie.getTrailer());
    }

    if (movie.getHomepage() != null) {
      values.put(MovieColumns.HOMEPAGE, movie.getHomepage());
    }

    if (movie.getRating() != null) {
      values.put(MovieColumns.RATING, movie.getRating());
    }
    if (movie.getVotes() != null) {
      values.put(MovieColumns.VOTES, movie.getVotes());
    }

    return values;
  }

  public void checkIn(long movieId) {
    Cursor movie = context.getContentResolver().query(Movies.withId(movieId), new String[] {
        MovieColumns.RUNTIME,
    }, null, null, null);

    movie.moveToFirst();
    final int runtime = Cursors.getInt(movie, MovieColumns.RUNTIME);
    movie.close();

    final long startedAt = System.currentTimeMillis();
    final long expiresAt = startedAt + runtime * DateUtils.MINUTE_IN_MILLIS;

    ContentValues values = new ContentValues();
    values.put(MovieColumns.CHECKED_IN, true);
    values.put(MovieColumns.STARTED_AT, startedAt);
    values.put(MovieColumns.EXPIRES_AT, expiresAt);
    context.getContentResolver().update(Movies.withId(movieId), values, null, null);
  }

  public void addToHistory(final long movieId, final long watchedAt) {
    ContentValues values = new ContentValues();
    values.put(MovieColumns.WATCHED, true);

    if (watchedAt == WATCHED_RELEASE) {
      Cursor movie = context.getContentResolver().query(Movies.withId(movieId), new String[] {
          MovieColumns.RELEASED,
      }, null, null, null);
      if (movie.moveToFirst()) {
        final String released = Cursors.getString(movie, MovieColumns.RELEASED);
        if (android.text.TextUtils.isEmpty(released)) {
          // No release date, just use current date. Trakt will mark it watched on release
          // and it will get synced back later.
          values.put(MovieColumns.WATCHED_AT, System.currentTimeMillis());
        } else {
          SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
          try {
            final long releaseDate = df.parse(released).getTime();
            values.put(MovieColumns.WATCHED_AT, releaseDate);
          } catch (ParseException e) {
            Timber.e("Parsing release date %s failed", released);
            // Use current date.
            values.put(MovieColumns.WATCHED_AT, System.currentTimeMillis());
          }
        }
      }
      movie.close();
    } else {
      values.put(MovieColumns.WATCHED_AT, watchedAt);
    }

    resolver.update(Movies.withId(movieId), values, null, null);
  }

  public void removeFromHistory(final long movieId) {
    ContentValues values = new ContentValues();
    values.put(MovieColumns.WATCHED, false);
    values.put(MovieColumns.WATCHED_AT, 0L);
    resolver.update(Movies.withId(movieId), values, null, null);
  }

  public void setIsInCollection(long movieId, boolean collected) {
    setIsInCollection(movieId, collected, 0);
  }

  public void setIsInCollection(long movieId, boolean collected, long collectedAt) {
    ContentValues values = new ContentValues();
    values.put(MovieColumns.IN_COLLECTION, collected);
    values.put(MovieColumns.COLLECTED_AT, collectedAt);
    resolver.update(Movies.withId(movieId), values, null, null);
  }

  public void setIsInWatchlist(long movieId, boolean inWatchlist) {
    setIsInWatchlist(movieId, inWatchlist, 0);
  }

  public void setIsInWatchlist(long movieId, boolean inWatchlist, long listedAt) {
    ContentValues values = new ContentValues();
    values.put(MovieColumns.IN_WATCHLIST, inWatchlist);
    values.put(MovieColumns.LISTED_AT, listedAt);
    resolver.update(Movies.withId(movieId), values, null, null);
  }
}
