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
import java.util.ArrayList;
import java.util.List;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.api.entity.Images;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.util.TimeUtils;
import net.simonvt.cathode.database.DatabaseUtils;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieGenreColumns;
import net.simonvt.cathode.provider.ProviderSchematic.MovieGenres;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.generated.CathodeProvider;
import net.simonvt.cathode.util.TextUtils;
import net.simonvt.schematic.Cursors;
import timber.log.Timber;

public final class MovieDatabaseHelper {

  private static volatile MovieDatabaseHelper instance;

  public static MovieDatabaseHelper getInstance(Context context) {
    if (instance == null) {
      synchronized (MovieDatabaseHelper.class) {
        if (instance == null) {
          instance = new MovieDatabaseHelper(context);
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

    CathodeApp.inject(context, this);
  }

  public long getTraktId(long movieId) {
    Cursor c = resolver.query(Movies.withId(movieId), new String[] {
        MovieColumns.TRAKT_ID,
    }, null, null, null);

    int traktId = -1;
    if (c.moveToFirst()) {
      traktId = Cursors.getInt(c, MovieColumns.TRAKT_ID);
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

  public boolean isUpdated(long traktId, String lastUpdated) {
    Cursor movie = null;
    try {
      movie = resolver.query(Movies.MOVIES, new String[] {
          MovieColumns.LAST_UPDATED,
      }, MovieColumns.TRAKT_ID + "=?", new String[] {
          String.valueOf(traktId),
      }, null);

      if (movie.moveToFirst()) {
        return TimeUtils.getMillis(lastUpdated) > Cursors.getLong(movie, MovieColumns.LAST_UPDATED);
      }

      return false;
    } finally {
      if (movie != null) movie.close();
    }
  }

  public boolean shouldUpdate(long traktId, String lastUpdated) {
    if (lastUpdated == null) return true;

    Cursor movie = null;
    try {
      movie = resolver.query(Movies.MOVIES, new String[] {
          MovieColumns.WATCHED, MovieColumns.IN_COLLECTION, MovieColumns.IN_WATCHLIST,
      }, MovieColumns.TRAKT_ID + "=?", new String[] {
          String.valueOf(traktId),
      }, null);

      if (movie.moveToFirst()) {
        final boolean watched = Cursors.getBoolean(movie, MovieColumns.WATCHED);
        final boolean collected = Cursors.getBoolean(movie, MovieColumns.IN_COLLECTION);
        final boolean inWatchlist = Cursors.getBoolean(movie, MovieColumns.IN_WATCHLIST);

        if (watched || collected || inWatchlist) {
          return true;
        }
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
    ContentValues cv = new ContentValues();
    cv.put(MovieColumns.TRAKT_ID, traktId);
    cv.put(MovieColumns.NEEDS_SYNC, true);

    return ProviderSchematic.Movies.getId(resolver.insert(Movies.MOVIES, cv));
  }

  public long fullUpdate(Movie movie) {
    IdResult result = getIdOrCreate(movie.getIds().getTrakt());
    final long id = result.movieId;

    ContentValues cv = getContentValues(movie);
    cv.put(MovieColumns.NEEDS_SYNC, false);
    resolver.update(Movies.withId(id), cv, null, null);

    if (movie.getGenres() != null) {
      insertGenres(id, movie.getGenres());
    }

    return id;
  }

  public long partialUpdate(Movie movie) {
    IdResult result = getIdOrCreate(movie.getIds().getTrakt());
    final long id = result.movieId;

    ContentValues cv = getContentValues(movie);
    resolver.update(Movies.withId(id), cv, null, null);

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

  private static ContentValues getContentValues(Movie movie) {
    ContentValues cv = new ContentValues();

    cv.put(MovieColumns.TITLE, movie.getTitle());
    cv.put(MovieColumns.TITLE_NO_ARTICLE, DatabaseUtils.removeLeadingArticle(movie.getTitle()));
    if (movie.getYear() != null) cv.put(MovieColumns.YEAR, movie.getYear());
    if (movie.getReleased() != null) cv.put(MovieColumns.RELEASED, movie.getReleased());
    if (movie.getRuntime() != null) cv.put(MovieColumns.RUNTIME, movie.getRuntime());
    if (movie.getTagline() != null) cv.put(MovieColumns.TAGLINE, movie.getTagline());
    if (movie.getOverview() != null) cv.put(MovieColumns.OVERVIEW, movie.getOverview());

    cv.put(MovieColumns.TRAKT_ID, movie.getIds().getTrakt());
    cv.put(MovieColumns.SLUG, movie.getIds().getSlug());
    cv.put(MovieColumns.IMDB_ID, movie.getIds().getImdb());
    cv.put(MovieColumns.TMDB_ID, movie.getIds().getTmdb());

    if (movie.getLanguage() != null) cv.put(MovieColumns.LANGUAGE, movie.getLanguage());

    if (movie.getImages() != null) {
      Images images = movie.getImages();
      if (images.getFanart() != null) cv.put(MovieColumns.FANART, images.getFanart().getFull());
      if (images.getPoster() != null) cv.put(MovieColumns.POSTER, images.getPoster().getFull());
      if (images.getLogo() != null) cv.put(MovieColumns.LOGO, images.getLogo().getFull());
      if (images.getClearart() != null) {
        cv.put(MovieColumns.CLEARART, images.getClearart().getFull());
      }
      if (images.getBanner() != null) cv.put(MovieColumns.BANNER, images.getBanner().getFull());
      if (images.getThumb() != null) cv.put(MovieColumns.THUMB, images.getThumb().getFull());
    }

    if (movie.getCertification() != null) {
      cv.put(MovieColumns.CERTIFICATION, movie.getCertification());
    }

    if (movie.getTrailer() != null) {
      cv.put(MovieColumns.TRAILER, movie.getTrailer());
    }

    if (movie.getHomepage() != null) {
      cv.put(MovieColumns.HOMEPAGE, movie.getHomepage());
    }

    if (movie.getRating() != null) {
      cv.put(MovieColumns.RATING, movie.getRating());
    }
    if (movie.getVotes() != null) {
      cv.put(MovieColumns.VOTES, movie.getVotes());
    }

    return cv;
  }

  public void setWatched(long movieId, boolean isWatched, long watchedAt) {
    ContentValues cv = new ContentValues();
    cv.put(MovieColumns.WATCHED, isWatched);
    cv.put(MovieColumns.WATCHED_AT, watchedAt);
    resolver.update(Movies.withId(movieId), cv, null, null);
  }

  public void setIsInCollection(long movieId, boolean collected) {
    setIsInCollection(movieId, collected, 0);
  }

  public void setIsInCollection(long movieId, boolean collected, long collectedAt) {
    ContentValues cv = new ContentValues();
    cv.put(MovieColumns.IN_COLLECTION, collected);
    cv.put(MovieColumns.COLLECTED_AT, collectedAt);
    resolver.update(Movies.withId(movieId), cv, null, null);
  }

  public void setIsInWatchlist(long movieId, boolean inWatchlist) {
    setIsInWatchlist(movieId, inWatchlist, 0);
  }

  public void setIsInWatchlist(long movieId, boolean inWatchlist, long listedAt) {
    ContentValues cv = new ContentValues();
    cv.put(MovieColumns.IN_WATCHLIST, inWatchlist);
    cv.put(MovieColumns.LISTED_AT, listedAt);
    resolver.update(Movies.withId(movieId), cv, null, null);
  }
}
