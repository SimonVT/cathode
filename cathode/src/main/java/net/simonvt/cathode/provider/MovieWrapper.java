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
import java.util.List;
import net.simonvt.cathode.api.entity.Images;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.entity.Person;
import net.simonvt.cathode.api.entity.UserProfile;
import net.simonvt.cathode.database.DatabaseUtils;
import net.simonvt.cathode.provider.DatabaseContract.MovieActorColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieDirectoryColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieGenreColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieProducerColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieTopWatcherColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieWriterColumns;
import net.simonvt.cathode.provider.ProviderSchematic.MovieActors;
import net.simonvt.cathode.provider.ProviderSchematic.MovieDirectors;
import net.simonvt.cathode.provider.ProviderSchematic.MovieGenres;
import net.simonvt.cathode.provider.ProviderSchematic.MovieProducers;
import net.simonvt.cathode.provider.ProviderSchematic.MovieTopWatchers;
import net.simonvt.cathode.provider.ProviderSchematic.MovieWriters;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.util.ApiUtils;

public final class MovieWrapper {

  private static final String TAG = "MovieWrapper";

  private MovieWrapper() {
  }

  public static long getTmdbId(ContentResolver resolver, long movieId) {
    Cursor c = resolver.query(Movies.withId(movieId), new String[] {
        MovieColumns.TMDB_ID,
    }, null, null, null);

    int tmdbId = -1;
    if (c.moveToFirst()) {
      tmdbId = c.getInt(c.getColumnIndex(MovieColumns.TMDB_ID));
    }

    c.close();

    return tmdbId;
  }

  public static long getMovieId(ContentResolver resolver, Movie movie) {
    return getMovieId(resolver, movie.getTmdbId());
  }

  public static long getMovieId(ContentResolver resolver, long tmdbId) {
    Cursor c = resolver.query(Movies.MOVIES, new String[] {
        MovieColumns.ID,
    }, MovieColumns.TMDB_ID + "=?", new String[] {
        String.valueOf(tmdbId),
    }, null);

    long id = !c.moveToFirst() ? -1L : c.getLong(c.getColumnIndex(MovieColumns.ID));

    c.close();

    return id;
  }

  public static boolean exists(ContentResolver resolver, long tmdbId) {
    Cursor c = null;
    try {
      c = resolver.query(Movies.MOVIES, new String[] {
          MovieColumns.ID,
      }, MovieColumns.TMDB_ID + "=?", new String[] {
          String.valueOf(tmdbId),
      }, null);

      return c.moveToFirst();
    } finally {
      if (c != null) c.close();
    }
  }

  public static boolean needsUpdate(ContentResolver resolver, long tmdbId, long lastUpdated) {
    if (lastUpdated == 0) return true;

    Cursor c = null;
    try {
      c = resolver.query(Movies.MOVIES, new String[] {
          MovieColumns.LAST_UPDATED,
      }, MovieColumns.TMDB_ID + "=?", new String[] {
          String.valueOf(tmdbId),
      }, null);

      boolean exists = c.moveToFirst();
      if (exists) {
        return lastUpdated > c.getLong(c.getColumnIndex(MovieColumns.LAST_UPDATED));
      }

      return false;
    } finally {
      if (c != null) c.close();
    }
  }

  public static long updateOrInsertMovie(ContentResolver resolver, Movie movie) {
    long movieId = getMovieId(resolver, movie.getTmdbId());

    if (movieId == -1) {
      movieId = insertMovie(resolver, movie);
    } else {
      updateMovie(resolver, movie);
    }

    return movieId;
  }

  public static void updateMovie(ContentResolver resolver, Movie movie) {
    final long movieId = getMovieId(resolver, movie.getTmdbId());
    ContentValues cv = getContentValues(movie);
    resolver.update(Movies.withId(movieId), cv, null, null);

    if (movie.getGenres() != null) insertGenres(resolver, movieId, movie.getGenres());
    if (movie.getPeople() != null) insertPeople(resolver, movieId, movie.getPeople());
    if (movie.getTopWatchers() != null) {
      insertTopWatchers(resolver, movieId, movie.getTopWatchers());
    }
  }

  public static long insertMovie(ContentResolver resolver, Movie movie) {
    ContentValues cv = getContentValues(movie);

    Uri uri = resolver.insert(Movies.MOVIES, cv);
    final long movieId = Long.valueOf(Movies.getId(uri));

    if (movie.getGenres() != null) insertGenres(resolver, movieId, movie.getGenres());
    if (movie.getPeople() != null) insertPeople(resolver, movieId, movie.getPeople());
    if (movie.getTopWatchers() != null) {
      insertTopWatchers(resolver, movieId, movie.getTopWatchers());
    }

    return movieId;
  }

  public static void insertGenres(ContentResolver resolver, long movieId, List<String> genres) {
    resolver.delete(MovieGenres.fromMovie(movieId), null, null);

    for (String genre : genres) {
      ContentValues cv = new ContentValues();

      cv.put(MovieGenreColumns.MOVIE_ID, movieId);
      cv.put(MovieGenreColumns.GENRE, genre);

      resolver.insert(MovieGenres.fromMovie(movieId), cv);
    }
  }

  public static void insertTopWatchers(ContentResolver resolver, long movieId,
      List<UserProfile> topWatchers) {
    resolver.delete(MovieTopWatchers.fromMovie(movieId), null, null);

    for (UserProfile profile : topWatchers) {
      ContentValues cv = new ContentValues();

      cv.put(MovieTopWatcherColumns.MOVIE_ID, movieId);
      cv.put(MovieTopWatcherColumns.PLAYS, profile.getPlays());
      cv.put(MovieTopWatcherColumns.USERNAME, profile.getUsername());
      cv.put(MovieTopWatcherColumns.PROTECTED, profile.isProtected());
      cv.put(MovieTopWatcherColumns.FULL_NAME, profile.getFullName());
      cv.put(MovieTopWatcherColumns.GENDER,
          profile.getGender() != null ? profile.getGender().toString() : null);
      cv.put(MovieTopWatcherColumns.AGE, profile.getAge());
      cv.put(MovieTopWatcherColumns.LOCATION, profile.getLocation());
      cv.put(MovieTopWatcherColumns.ABOUT, profile.getAbout());
      cv.put(MovieTopWatcherColumns.JOINED, profile.getJoined());
      cv.put(MovieTopWatcherColumns.AVATAR, profile.getAvatar());
      cv.put(MovieTopWatcherColumns.URL, profile.getUrl());

      resolver.insert(MovieTopWatchers.fromMovie(movieId), cv);
    }
  }

  private static void insertPeople(ContentResolver resolver, long movieId, Movie.People people) {
    resolver.delete(MovieDirectors.fromMovie(movieId), null, null);
    List<Person> directors = people.getDirectors();
    for (Person person : directors) {
      if (person.getName() == null) {
        continue;
      }
      ContentValues cv = new ContentValues();
      cv.put(MovieDirectoryColumns.MOVIE_ID, movieId);
      cv.put(MovieDirectoryColumns.NAME, person.getName());
      if (!ApiUtils.isPlaceholder(person.getImages().getHeadshot())) {
        cv.put(MovieDirectoryColumns.HEADSHOT, person.getImages().getHeadshot());
      }

      resolver.insert(MovieDirectors.fromMovie(movieId), cv);
    }

    resolver.delete(MovieWriters.fromMovie(movieId), null, null);
    List<Person> writers = people.getWriters();
    for (Person person : writers) {
      if (person.getName() == null) {
        continue;
      }
      ContentValues cv = new ContentValues();
      cv.put(MovieWriterColumns.MOVIE_ID, movieId);
      cv.put(MovieWriterColumns.NAME, person.getName());
      if (!ApiUtils.isPlaceholder(person.getImages().getHeadshot())) {
        cv.put(MovieWriterColumns.HEADSHOT, person.getImages().getHeadshot());
      }
      cv.put(MovieWriterColumns.JOB, person.getJob());

      resolver.insert(MovieWriters.fromMovie(movieId), cv);
    }

    resolver.delete(MovieProducers.fromMovie(movieId), null, null);
    List<Person> producers = people.getProducers();
    for (Person person : producers) {
      if (person.getName() == null) {
        continue;
      }
      ContentValues cv = new ContentValues();
      cv.put(MovieProducerColumns.MOVIE_ID, movieId);
      cv.put(MovieProducerColumns.NAME, person.getName());
      if (!ApiUtils.isPlaceholder(person.getImages().getHeadshot())) {
        cv.put(MovieProducerColumns.HEADSHOT, person.getImages().getHeadshot());
      }
      cv.put(MovieProducerColumns.EXECUTIVE, person.isExecutive());

      resolver.insert(MovieProducers.fromMovie(movieId), cv);
    }

    resolver.delete(MovieActors.fromMovie(movieId), null, null);
    List<Person> actors = people.getActors();
    for (Person person : actors) {
      if (person.getName() == null) {
        continue;
      }
      ContentValues cv = new ContentValues();
      cv.put(MovieActorColumns.MOVIE_ID, movieId);
      cv.put(MovieActorColumns.NAME, person.getName());
      if (!ApiUtils.isPlaceholder(person.getImages().getHeadshot())) {
        cv.put(MovieActorColumns.HEADSHOT, person.getImages().getHeadshot());
      }
      cv.put(MovieActorColumns.CHARACTER, person.getCharacter());

      resolver.insert(MovieActors.fromMovie(movieId), cv);
    }
  }

  private static ContentValues getContentValues(Movie movie) {
    ContentValues cv = new ContentValues();

    cv.put(MovieColumns.TITLE, movie.getTitle());
    cv.put(MovieColumns.TITLE_NO_ARTICLE, DatabaseUtils.removeLeadingArticle(movie.getTitle()));
    cv.put(MovieColumns.YEAR, movie.getYear());
    cv.put(MovieColumns.RELEASED, movie.getReleased());
    cv.put(MovieColumns.URL, movie.getUrl());
    cv.put(MovieColumns.TRAILER, movie.getTrailer());
    cv.put(MovieColumns.RUNTIME, movie.getRuntime());
    cv.put(MovieColumns.TAGLINE, movie.getTagline());
    cv.put(MovieColumns.OVERVIEW, movie.getOverview());
    cv.put(MovieColumns.CERTIFICATION, movie.getCertification());
    cv.put(MovieColumns.IMDB_ID, movie.getImdbId());
    cv.put(MovieColumns.TMDB_ID, movie.getTmdbId());
    cv.put(MovieColumns.RT_ID, movie.getRtId());
    cv.put(MovieColumns.LAST_UPDATED, movie.getLastUpdated());
    if (movie.getImages() != null) {
      Images images = movie.getImages();
      if (!ApiUtils.isPlaceholder(images.getPoster())) {
        cv.put(MovieColumns.POSTER, images.getPoster());
      }
      if (!ApiUtils.isPlaceholder(images.getFanart())) {
        cv.put(MovieColumns.FANART, images.getFanart());
      }
    }
    if (movie.getRatings() != null) {
      cv.put(MovieColumns.RATING_PERCENTAGE, movie.getRatings().getPercentage());
      cv.put(MovieColumns.RATING_VOTES, movie.getRatings().getVotes());
      cv.put(MovieColumns.RATING_LOVED, movie.getRatings().getLoved());
      cv.put(MovieColumns.RATING_HATED, movie.getRatings().getHated());
    }
    if (movie.getStats() != null) {
      cv.put(MovieColumns.WATCHERS, movie.getStats().getWatchers());
      cv.put(MovieColumns.PLAYS, movie.getStats().getPlays());
      cv.put(MovieColumns.SCROBBLES, movie.getStats().getScrobbles());
      cv.put(MovieColumns.CHECKINS, movie.getStats().getCheckins());
    }
    if (movie.isWatched() != null) cv.put(MovieColumns.WATCHED, movie.isWatched());
    cv.put(MovieColumns.PLAYS, movie.getPlays());
    if (movie.getRatingAdvanced() != null) cv.put(MovieColumns.RATING, movie.getRatingAdvanced());
    cv.put(MovieColumns.IN_WATCHLIST, movie.isInWatchlist());
    cv.put(MovieColumns.IN_COLLECTION, movie.isInCollection());

    return cv;
  }

  public static void setWatched(ContentResolver resolver, long movieId, boolean isWatched) {
    ContentValues cv = new ContentValues();
    cv.put(MovieColumns.WATCHED, isWatched);
    resolver.update(Movies.withId(movieId), cv, null, null);
  }

  public static void setIsInCollection(ContentResolver resolver, long movieId,
      boolean inCollection) {
    ContentValues cv = new ContentValues();
    cv.put(MovieColumns.IN_COLLECTION, inCollection);
    resolver.update(Movies.withId(movieId), cv, null, null);
  }

  public static void setIsInWatchlist(ContentResolver resolver, long movieId, boolean inWatchlist) {
    ContentValues cv = new ContentValues();
    cv.put(MovieColumns.IN_WATCHLIST, inWatchlist);
    resolver.update(Movies.withId(movieId), cv, null, null);
  }

  public static void setRating(ContentResolver resolver, long movieId, int rating) {
    ContentValues cv = new ContentValues();
    cv.put(MovieColumns.RATING, rating);
    resolver.update(Movies.withId(movieId), cv, null, null);
  }
}
