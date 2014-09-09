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
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;
import net.simonvt.cathode.api.entity.Images;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.entity.People;
import net.simonvt.cathode.api.util.TimeUtils;
import net.simonvt.cathode.database.DatabaseUtils;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.generated.CathodeProvider;
import timber.log.Timber;

import  net.simonvt.cathode.provider.DatabaseContract.MovieGenreColumns;
import  net.simonvt.cathode.provider.ProviderSchematic.MovieGenres;

public final class MovieWrapper {

  private MovieWrapper() {
  }

  public static long getTraktId(ContentResolver resolver, long movieId) {
    Cursor c = resolver.query(Movies.withId(movieId), new String[] {
        MovieColumns.TRAKT_ID,
    }, null, null, null);

    int traktId = -1;
    if (c.moveToFirst()) {
      traktId = c.getInt(c.getColumnIndex(MovieColumns.TRAKT_ID));
    }

    c.close();

    return traktId;
  }

  public static long getMovieId(ContentResolver resolver, Movie movie) {
    return getMovieId(resolver, movie.getIds().getTrakt());
  }

  public static long getMovieId(ContentResolver resolver, long traktId) {
    Cursor c = resolver.query(Movies.MOVIES, new String[] {
        MovieColumns.ID,
    }, MovieColumns.TRAKT_ID + "=?", new String[] {
        String.valueOf(traktId),
    }, null);

    long id = !c.moveToFirst() ? -1L : c.getLong(c.getColumnIndex(MovieColumns.ID));

    c.close();

    return id;
  }

  public static boolean exists(ContentResolver resolver, long traktId) {
    Cursor c = null;
    try {
      c = resolver.query(Movies.MOVIES, new String[] {
          MovieColumns.ID,
      }, MovieColumns.TRAKT_ID + "=?", new String[] {
          String.valueOf(traktId),
      }, null);

      return c.moveToFirst();
    } finally {
      if (c != null) c.close();
    }
  }

  public static boolean needsUpdate(ContentResolver resolver, long traktId, String lastUpdated) {
    if (lastUpdated == null) return true;

    Cursor c = null;
    try {
      c = resolver.query(Movies.MOVIES, new String[] {
          MovieColumns.LAST_UPDATED,
      }, MovieColumns.TRAKT_ID + "=?", new String[] {
          String.valueOf(traktId),
      }, null);

      boolean exists = c.moveToFirst();
      if (exists) {
        return TimeUtils.getMillis(lastUpdated) > TimeUtils.getMillis(
            c.getString(c.getColumnIndex(MovieColumns.LAST_UPDATED)));
      }

      return false;
    } finally {
      if (c != null) c.close();
    }
  }

  public static long createMovie(ContentResolver resolver, long traktId) {
    final long showId = getMovieId(resolver, traktId);
    if (showId != -1L) {
      throw new IllegalStateException("Trying to create movie that already exists");
    }

    ContentValues cv = new ContentValues();
    cv.put(MovieColumns.TRAKT_ID, traktId);
    cv.put(MovieColumns.NEEDS_SYNC, 1);

    return Movies.getId(resolver.insert(Movies.MOVIES, cv));
  }

  public static long updateOrInsertMovie(ContentResolver resolver, Movie movie) {
    long movieId = getMovieId(resolver, movie.getIds().getTrakt());

    if (movieId == -1) {
      movieId = insertMovie(resolver, movie);
    } else {
      updateMovie(resolver, movie);
    }

    return movieId;
  }

  public static void updateMovie(ContentResolver resolver, Movie movie) {
    final long movieId = getMovieId(resolver, movie.getIds().getTrakt());
    ContentValues cv = getContentValues(movie);
    resolver.update(Movies.withId(movieId), cv, null, null);
  }

  public static long insertMovie(ContentResolver resolver, Movie movie) {
    ContentValues cv = getContentValues(movie);

    Uri uri = resolver.insert(Movies.MOVIES, cv);
    final long movieId = Movies.getId(uri);

    insertGenres(resolver, movieId, movie.getGenres());

    return movieId;
  }

  public static void insertGenres(ContentResolver resolver, long movieId, List<String> genres) {
    try {
      ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
      ContentProviderOperation op;

      op = ContentProviderOperation.newDelete(MovieGenres.fromMovie(movieId)).build();
      ops.add(op);

      for (String genre : genres) {
        op = ContentProviderOperation.newInsert(MovieGenres.fromMovie(movieId))
            .withValue(MovieGenreColumns.MOVIE_ID, movieId)
            .withValue(MovieGenreColumns.GENRE, genre)
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

  private static void insertPeople(ContentResolver resolver, long movieId, People people) {
    // TODO:
  }

  private static ContentValues getContentValues(Movie movie) {
    ContentValues cv = new ContentValues();

    cv.put(MovieColumns.NEEDS_SYNC, 0);

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

    return cv;
  }

  public static void setWatched(ContentResolver resolver, long movieId, boolean isWatched,
      long watchedAt) {
    ContentValues cv = new ContentValues();
    cv.put(MovieColumns.WATCHED, isWatched);
    cv.put(MovieColumns.WATCHED_AT, watchedAt);
    resolver.update(Movies.withId(movieId), cv, null, null);
  }

  public static void setIsInCollection(ContentResolver resolver, long movieId, boolean collected) {
    setIsInCollection(resolver, movieId, collected, 0);
  }

  public static void setIsInCollection(ContentResolver resolver, long movieId, boolean collected,
      long collectedAt) {
    ContentValues cv = new ContentValues();
    cv.put(MovieColumns.IN_COLLECTION, collected);
    cv.put(MovieColumns.COLLECTED_AT, collectedAt);
    resolver.update(Movies.withId(movieId), cv, null, null);
  }

  public static void setIsInWatchlist(ContentResolver resolver, long movieId, boolean inWatchlist) {
    setIsInWatchlist(resolver, movieId, inWatchlist, 0);
  }

  public static void setIsInWatchlist(ContentResolver resolver, long movieId, boolean inWatchlist,
      long listedAt) {
    ContentValues cv = new ContentValues();
    cv.put(MovieColumns.IN_WATCHLIST, inWatchlist);
    cv.put(MovieColumns.LISTED_AT, listedAt);
    resolver.update(Movies.withId(movieId), cv, null, null);
  }

  public static void setRating(ContentResolver resolver, long movieId, int rating) {
    ContentValues cv = new ContentValues();
    cv.put(MovieColumns.RATING, rating);
    resolver.update(Movies.withId(movieId), cv, null, null);
  }
}
