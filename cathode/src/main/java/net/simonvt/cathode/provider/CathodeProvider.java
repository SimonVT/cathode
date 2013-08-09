package net.simonvt.cathode.provider;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import java.util.ArrayList;
import net.simonvt.cathode.provider.CathodeContract.Episodes;
import net.simonvt.cathode.provider.CathodeContract.MovieActors;
import net.simonvt.cathode.provider.CathodeContract.MovieDirectors;
import net.simonvt.cathode.provider.CathodeContract.MovieGenres;
import net.simonvt.cathode.provider.CathodeContract.MovieProducers;
import net.simonvt.cathode.provider.CathodeContract.MovieTopWatchers;
import net.simonvt.cathode.provider.CathodeContract.MovieWriters;
import net.simonvt.cathode.provider.CathodeContract.Movies;
import net.simonvt.cathode.provider.CathodeContract.Seasons;
import net.simonvt.cathode.provider.CathodeContract.ShowActor;
import net.simonvt.cathode.provider.CathodeContract.ShowGenres;
import net.simonvt.cathode.provider.CathodeContract.ShowTopWatchers;
import net.simonvt.cathode.provider.CathodeContract.Shows;
import net.simonvt.cathode.provider.CathodeContract.TopEpisodes;
import net.simonvt.cathode.provider.CathodeDatabase.Tables;
import net.simonvt.cathode.util.SelectionBuilder;

public class CathodeProvider extends ContentProvider {

  private static final String TAG = "CathodeProvider";

  public static final String AUTHORITY = "net.simonvt.cathode.provider.CathodeProvider";

  private static final int SHOWS = 100;
  private static final int SHOWS_ID = 101;
  private static final int SHOW_TOP_WATCHERS = 102;
  private static final int SHOW_TOP_EPISODES = 103;
  private static final int SHOW_ACTORS = 104;
  private static final int SHOW_GENRES = 105;
  private static final int SHOWS_WITHNEXT = 106;
  private static final int SHOWS_WITHNEXT_IGNOREWATCHED = 107;
  private static final int SHOWS_WATCHLIST = 108;
  private static final int SHOWS_COLLECTION = 109;
  private static final int SHOWS_WATCHED = 110;

  private static final int SEASONS = 200;
  private static final int SEASON_ID = 201;
  private static final int SEASONS_OFSHOW = 202;

  private static final int EPISODES = 300;
  private static final int EPISODE_ID = 301;
  private static final int EPISODES_FROMSHOW = 302;
  private static final int EPISODES_FROMSEASON = 303;
  private static final int EPISODES_WATCHLIST = 304;

  private static final int MOVIES = 400;
  private static final int MOVIE_ID = 401;

  private static final int MOVIE_GENRES = 402;
  private static final int MOVIE_TOP_WATCHERS = 403;
  private static final int MOVIE_ACTORS = 404;
  private static final int MOVIE_DIRECTORS = 405;
  private static final int MOVIE_WRITERS = 406;
  private static final int MOVIE_PRODUCERS = 407;

  private CathodeDatabase database;

  private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

  static {
    URI_MATCHER.addURI(AUTHORITY, CathodeContract.PATH_SHOWS, SHOWS);
    URI_MATCHER.addURI(AUTHORITY, CathodeContract.PATH_SHOWS + "/" + CathodeContract.PATH_WITHID + "/*",
        SHOWS_ID);
    URI_MATCHER.addURI(AUTHORITY, CathodeContract.PATH_SHOWS + "/" + CathodeContract.PATH_WITHNEXT,
        SHOWS_WITHNEXT);
    URI_MATCHER.addURI(AUTHORITY, CathodeContract.PATH_SHOWS
        + "/"
        + CathodeContract.PATH_WITHNEXT
        + "/"
        + CathodeContract.PATH_IGNOREWATCHED, SHOWS_WITHNEXT_IGNOREWATCHED);
    URI_MATCHER.addURI(AUTHORITY,
        CathodeContract.PATH_TOPWATCHERS + "/" + CathodeContract.PATH_FROMSHOW + "/*",
        SHOW_TOP_WATCHERS);
    URI_MATCHER.addURI(AUTHORITY,
        CathodeContract.PATH_TOPEPISODES + "/" + CathodeContract.PATH_FROMSHOW + "/*",
        SHOW_TOP_EPISODES);
    URI_MATCHER.addURI(AUTHORITY,
        CathodeContract.PATH_ACTORS + "/" + CathodeContract.PATH_FROMSHOW + "/*", SHOW_ACTORS);
    URI_MATCHER.addURI(AUTHORITY,
        CathodeContract.PATH_GENRES + "/" + CathodeContract.PATH_FROMSHOW + "/*", SHOW_GENRES);
    URI_MATCHER.addURI(AUTHORITY, CathodeContract.PATH_SHOWS + "/" + CathodeContract.PATH_WATCHLIST,
        SHOWS_WATCHLIST);
    URI_MATCHER.addURI(AUTHORITY, CathodeContract.PATH_SHOWS + "/" + CathodeContract.PATH_COLLECTION,
        SHOWS_COLLECTION);
    URI_MATCHER.addURI(AUTHORITY, CathodeContract.PATH_SHOWS + "/" + CathodeContract.PATH_WATCHED,
        SHOWS_WATCHED);

    URI_MATCHER.addURI(AUTHORITY, CathodeContract.PATH_SEASONS, SEASONS);
    URI_MATCHER.addURI(AUTHORITY,
        CathodeContract.PATH_SEASONS + "/" + CathodeContract.PATH_WITHID + "/*", SEASON_ID);
    URI_MATCHER.addURI(AUTHORITY,
        CathodeContract.PATH_SEASONS + "/" + CathodeContract.PATH_FROMSHOW + "/*", SEASONS_OFSHOW);

    URI_MATCHER.addURI(AUTHORITY, CathodeContract.PATH_EPISODES, EPISODES);
    URI_MATCHER.addURI(AUTHORITY,
        CathodeContract.PATH_EPISODES + "/" + CathodeContract.PATH_WITHID + "/*", EPISODE_ID);
    URI_MATCHER.addURI(AUTHORITY,
        CathodeContract.PATH_EPISODES + "/" + CathodeContract.PATH_FROMSHOW + "/*", EPISODES_FROMSHOW);
    URI_MATCHER.addURI(AUTHORITY,
        CathodeContract.PATH_EPISODES + "/" + CathodeContract.PATH_FROMSEASON + "/*",
        EPISODES_FROMSEASON);
    URI_MATCHER.addURI(AUTHORITY, CathodeContract.PATH_EPISODES + "/" + CathodeContract.PATH_WATCHLIST,
        EPISODES_WATCHLIST);

    URI_MATCHER.addURI(AUTHORITY, CathodeContract.PATH_MOVIES, MOVIES);
    URI_MATCHER.addURI(AUTHORITY, CathodeContract.PATH_MOVIES + "/*", MOVIE_ID);
    URI_MATCHER.addURI(AUTHORITY,
        CathodeContract.PATH_GENRES + "/" + CathodeContract.PATH_FROMMOVIE + "/*", MOVIE_GENRES);
    URI_MATCHER.addURI(AUTHORITY,
        CathodeContract.PATH_TOPWATCHERS + "/" + CathodeContract.PATH_MOVIES + "/*",
        MOVIE_TOP_WATCHERS);
    URI_MATCHER.addURI(AUTHORITY,
        CathodeContract.PATH_ACTORS + "/" + CathodeContract.PATH_FROMMOVIE + "/*", MOVIE_ACTORS);
    URI_MATCHER.addURI(AUTHORITY,
        CathodeContract.PATH_DIRECTORS + "/" + CathodeContract.PATH_FROMMOVIE + "/*", MOVIE_DIRECTORS);
    URI_MATCHER.addURI(AUTHORITY,
        CathodeContract.PATH_WRITERS + "/" + CathodeContract.PATH_FROMMOVIE + "/*", MOVIE_WRITERS);
    URI_MATCHER.addURI(AUTHORITY,
        CathodeContract.PATH_PRODUCERS + "/" + CathodeContract.PATH_FROMMOVIE + "/*", MOVIE_PRODUCERS);
  }

  @Override
  public boolean onCreate() {
    database = new CathodeDatabase(getContext());
    return true;
  }

  private SelectionBuilder getBuilder() {
    return new SelectionBuilder();
  }

  private SelectionBuilder getShowsBuilder() {
    SelectionBuilder builder = new SelectionBuilder();
    builder.map(CathodeContract.ShowColumns.AIRED_COUNT, Shows.getAiredQuery());
    builder.map(CathodeContract.ShowColumns.UNAIRED_COUNT, Shows.getUnairedQuery());
    builder.map(Tables.SHOWS + "." + CathodeContract.ShowColumns.AIRED_COUNT, Shows.getAiredQuery());
    builder.map(Tables.SHOWS + "." + CathodeContract.ShowColumns.UNAIRED_COUNT,
        Shows.getUnairedQuery());
    return builder;
  }

  private SelectionBuilder getSeasonBuilder() {
    SelectionBuilder builder = new SelectionBuilder();
    builder.map(CathodeContract.SeasonColumns.AIRED_COUNT, Seasons.getAiredQuery());
    builder.map(CathodeContract.SeasonColumns.UNAIRED_COUNT, Seasons.getUnairedQuery());
    builder.map(Tables.SEASONS + "." + CathodeContract.SeasonColumns.AIRED_COUNT,
        Seasons.getAiredQuery());
    builder.map(Tables.SEASONS + "." + CathodeContract.SeasonColumns.UNAIRED_COUNT,
        Seasons.getUnairedQuery());
    return builder;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
      String sortOrder) {
    final SQLiteDatabase db = database.getReadableDatabase();
    final int match = URI_MATCHER.match(uri);
    switch (match) {
      case SHOWS: {
        Cursor c = getShowsBuilder().table(Tables.SHOWS)
            .where(selection, selectionArgs)
            .query(db, projection, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), Shows.CONTENT_URI);
        return c;
      }
      case SHOWS_WITHNEXT: {
        Cursor c = getShowsBuilder().table(Tables.SHOWS_WITH_UNWATCHED)
            .where(selection, selectionArgs)
            .query(db, projection, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), Shows.CONTENT_URI);
        return c;
      }
      case SHOWS_WITHNEXT_IGNOREWATCHED: {
        Cursor c = getShowsBuilder().table(Tables.SHOWS_WITH_UNWATCHED)
            .where(CathodeContract.ShowColumns.WATCHED_COUNT + "<" + Shows.getAiredQuery(), null)
            .where(selection, selectionArgs)
            .query(db, projection, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), Shows.CONTENT_URI);
        return c;
      }
      case SHOWS_ID: {
        final String showId = Shows.getShowId(uri);
        Cursor c = getShowsBuilder().table(Tables.SHOWS)
            .where(Shows._ID + "=?", showId)
            .where(selection, selectionArgs)
            .query(db, projection, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
      }
      case SHOW_TOP_WATCHERS: {
        final String showId = ShowTopWatchers.getShowId(uri);
        Cursor c = getBuilder().table(Tables.SHOW_TOP_WATCHERS)
            .where(Shows._ID + "=?", showId)
            .where(selection, selectionArgs)
            .query(db, projection, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
      }
      case SHOW_TOP_EPISODES: {
        final String showId = TopEpisodes.getShowId(uri);
        Cursor c = getBuilder().table(Tables.SHOW_TOP_EPISODES)
            .where(Shows._ID + "=?", showId)
            .where(selection, selectionArgs)
            .query(db, projection, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
      }
      case SHOW_ACTORS: {
        final String showId = ShowActor.getShowId(uri);
        Cursor c = getBuilder().table(Tables.SHOW_ACTORS)
            .where(Shows._ID + "=?", showId)
            .where(selection, selectionArgs)
            .query(db, projection, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
      }
      case SHOW_GENRES: {
        final String showId = ShowGenres.getShowId(uri);
        Cursor c = getBuilder().table(Tables.SHOW_GENRES)
            .where(Shows._ID + "=?", showId)
            .where(selection, selectionArgs)
            .query(db, projection, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
      }
      case SHOWS_WATCHLIST: {
        Cursor c = getShowsBuilder().table(Tables.SHOWS_WITH_UNWATCHED)
            .where(Shows.IN_WATCHLIST + "=1")
            .where(selection, selectionArgs)
            .query(db, projection, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), Shows.CONTENT_URI);
        return c;
      }
      case SHOWS_COLLECTION: {
        Cursor c = getShowsBuilder().table(Tables.SHOWS_WITH_UNCOLLECTED)
            .where(Shows.IN_COLLECTION_COUNT + ">0")
            .where(selection, selectionArgs)
            .query(db, projection, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), Shows.CONTENT_URI);
        return c;
      }
      case SHOWS_WATCHED: {
        Cursor c = getShowsBuilder().table(Tables.SHOWS_WITH_UNWATCHED)
            .where(Shows.WATCHED_COUNT + ">0")
            .where(selection, selectionArgs)
            .query(db, projection, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), Shows.CONTENT_URI);
        return c;
      }
      case SEASONS: {
        Cursor c = getSeasonBuilder().table(Tables.SEASONS)
            .where(selection, selectionArgs)
            .query(db, projection, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), Seasons.CONTENT_URI);
        return c;
      }
      case SEASON_ID: {
        final String seasonId = Seasons.getSeasonId(uri);
        Cursor c = getSeasonBuilder().table(Tables.SEASONS)
            .where(Seasons._ID + "=?", seasonId)
            .where(selection, selectionArgs)
            .query(db, projection, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
      }
      case SEASONS_OFSHOW: {
        final String showId = Seasons.getShowId(uri);
        Cursor c = getSeasonBuilder().table(Tables.SEASONS)
            .where(Seasons.SHOW_ID + "=?", showId)
            .where(selection, selectionArgs)
            .query(db, projection, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
      }
      case EPISODES: {
        Cursor c = getBuilder().table(Tables.EPISODES)
            .where(selection, selectionArgs)
            .query(db, projection, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), Episodes.CONTENT_URI);
        return c;
      }
      case EPISODE_ID: {
        final String episodeId = Episodes.getEpisodeId(uri);
        Cursor c = getBuilder().table(Tables.EPISODES)
            .where(Episodes._ID + "=?", episodeId)
            .where(selection, selectionArgs)
            .query(db, projection, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
      }
      case EPISODES_FROMSHOW: {
        final String showId = Episodes.getShowId(uri);
        Cursor c = getBuilder().table(Tables.EPISODES)
            .where(Episodes.SHOW_ID + "=?", showId)
            .where(selection, selectionArgs)
            .query(db, projection, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
      }
      case EPISODES_FROMSEASON: {
        final String seasonId = Episodes.getSeasonId(uri);
        Cursor c = getBuilder().table(Tables.EPISODES)
            .where(Episodes.SEASON_ID + "=?", seasonId)
            .where(selection, selectionArgs)
            .query(db, projection, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
      }
      case EPISODES_WATCHLIST: {
        Cursor c = getBuilder().table(Tables.EPISODES_WITH_SHOW_TITLE)
            .where(Episodes.IN_WATCHLIST + "=1")
            .query(db, projection, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), Episodes.CONTENT_URI);
        return c;
      }
      case MOVIES: {
        Cursor c = getBuilder().table(Tables.MOVIES)
            .where(selection, selectionArgs)
            .query(db, projection, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), Movies.CONTENT_URI);
        return c;
      }
      case MOVIE_ID: {
        final String movieId = Movies.getMovieId(uri);
        Cursor c = getBuilder().table(Tables.MOVIES)
            .where(Movies._ID + "=?", movieId)
            .where(selection, selectionArgs)
            .query(db, projection, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
      }
      case MOVIE_GENRES: {
        final String movieId = MovieGenres.getMovieId(uri);
        Cursor c = getBuilder().table(Tables.MOVIE_GENRES)
            .where(MovieGenres.MOVIE_ID + "=?", movieId)
            .where(selection, selectionArgs)
            .query(db, projection, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
      }
      case MOVIE_TOP_WATCHERS: {
        final String movieId = MovieTopWatchers.getMovieId(uri);
        Cursor c = getBuilder().table(Tables.MOVIE_TOP_WATCHERS)
            .where(MovieTopWatchers.MOVIE_ID + "=?", movieId)
            .where(selection, selectionArgs)
            .query(db, projection, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
      }
      case MOVIE_ACTORS: {
        final String movieId = MovieActors.getMovieId(uri);
        Cursor c = getBuilder().table(Tables.MOVIE_ACTORS)
            .where(MovieActors.MOVIE_ID + "=?", movieId)
            .where(selection, selectionArgs)
            .query(db, projection, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
      }
      case MOVIE_DIRECTORS: {
        final String movieId = MovieDirectors.getMovieId(uri);
        Cursor c = getBuilder().table(Tables.MOVIE_DIRECTORS)
            .where(MovieDirectors.MOVIE_ID + "=?", movieId)
            .where(selection, selectionArgs)
            .query(db, projection, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
      }
      case MOVIE_WRITERS: {
        final String movieId = MovieWriters.getMovieId(uri);
        Cursor c = getBuilder().table(Tables.MOVIE_WRITERS)
            .where(MovieWriters.MOVIE_ID + "=?", movieId)
            .where(selection, selectionArgs)
            .query(db, projection, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
      }
      case MOVIE_PRODUCERS: {
        final String movieId = MovieProducers.getMovieId(uri);
        Cursor c = getBuilder().table(Tables.MOVIE_PRODUCERS)
            .where(MovieProducers.MOVIE_ID + "=?", movieId)
            .where(selection, selectionArgs)
            .query(db, projection, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
      }
      default: {
        throw new UnsupportedOperationException("Unknown uri: " + uri);
      }
    }
  }

  @Override
  public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
      throws OperationApplicationException {
    database.getWritableDatabase().beginTransaction();
    ContentProviderResult[] results = super.applyBatch(operations);
    database.getWritableDatabase().setTransactionSuccessful();
    database.getWritableDatabase().endTransaction();
    return results;
  }

  @Override
  public String getType(Uri uri) {
    switch (URI_MATCHER.match(uri)) {
      case SHOWS:
      case SHOWS_ID:
      case SHOWS_WITHNEXT:
      case SHOWS_WITHNEXT_IGNOREWATCHED:
      case SHOWS_WATCHLIST:
      case SHOWS_COLLECTION:
      case SHOWS_WATCHED:
        return Shows.CONTENT_TYPE;
      case SHOW_TOP_WATCHERS:
        return ShowTopWatchers.CONTENT_TYPE;
      case SHOW_TOP_EPISODES:
        return TopEpisodes.CONTENT_TYPE;
      case SHOW_ACTORS:
        return ShowActor.CONTENT_TYPE;
      case SHOW_GENRES:
        return ShowGenres.CONTENT_TYPE;
      case SEASONS:
      case SEASON_ID:
        return CathodeContract.Seasons.CONTENT_TYPE;
      case EPISODES:
      case EPISODE_ID:
      case EPISODES_FROMSHOW:
      case EPISODES_FROMSEASON:
        return Episodes.CONTENT_TYPE;
      case MOVIES:
      case MOVIE_ID:
        return Movies.CONTENT_TYPE;
      case MOVIE_GENRES:
        return MovieGenres.CONTENT_TYPE;
      case MOVIE_TOP_WATCHERS:
        return MovieTopWatchers.CONTENT_TYPE;
      case MOVIE_ACTORS:
        return MovieActors.CONTENT_TYPE;
      case MOVIE_DIRECTORS:
        return MovieDirectors.CONTENT_TYPE;
      case MOVIE_WRITERS:
        return MovieWriters.CONTENT_TYPE;
      case MOVIE_PRODUCERS:
        return MovieProducers.CONTENT_TYPE;
      default:
        throw new IllegalArgumentException("Unknown URI " + uri);
    }
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    final SQLiteDatabase db = database.getWritableDatabase();
    switch (URI_MATCHER.match(uri)) {
      case SHOWS: {
        final long showId = db.insertOrThrow(Tables.SHOWS, null, values);
        getContext().getContentResolver().notifyChange(Shows.buildShowUri(showId), null);
        return Shows.buildShowUri(showId);
      }
      case SHOW_TOP_WATCHERS: {
        final long rowId = db.insertOrThrow(Tables.SHOW_TOP_WATCHERS, null, values);
        final long showId = values.getAsLong(ShowTopWatchers.SHOW_ID);
        getContext().getContentResolver()
            .notifyChange(ShowTopWatchers.buildFromShowId(showId), null);
        return ContentUris.withAppendedId(ShowTopWatchers.CONTENT_URI, rowId);
      }
      case SHOW_TOP_EPISODES: {
        final long rowId = db.insertOrThrow(Tables.SHOW_TOP_EPISODES, null, values);
        final long showId = values.getAsLong(TopEpisodes.SHOW_ID);
        getContext().getContentResolver().notifyChange(TopEpisodes.buildFromShowId(showId), null);
        return ContentUris.withAppendedId(TopEpisodes.CONTENT_URI, rowId);
      }
      case SHOW_ACTORS: {
        final long rowId = db.insertOrThrow(Tables.SHOW_ACTORS, null, values);
        final long showId = values.getAsLong(ShowActor.SHOW_ID);
        getContext().getContentResolver().notifyChange(ShowActor.buildFromShowId(showId), null);
        return ContentUris.withAppendedId(ShowActor.CONTENT_URI, rowId);
      }
      case SHOW_GENRES: {
        final long rowId = db.insertOrThrow(Tables.SHOW_GENRES, null, values);
        final long showId = values.getAsLong(ShowGenres.SHOW_ID);
        getContext().getContentResolver().notifyChange(ShowGenres.buildFromShowId(showId), null);
        return ContentUris.withAppendedId(ShowGenres.CONTENT_URI, rowId);
      }
      case SEASONS: {
        final long seasonId = db.insertOrThrow(Tables.SEASONS, null, values);
        final long showId = values.getAsLong(CathodeContract.SeasonColumns.SHOW_ID);
        getContext().getContentResolver().notifyChange(Seasons.buildFromShowId(showId), null);
        return Seasons.buildFromId(seasonId);
      }
      case EPISODES: {
        final long episodeId = db.insertOrThrow(Tables.EPISODES, null, values);
        final long showId = values.getAsLong(CathodeContract.EpisodeColumns.SHOW_ID);
        final long seasonId = values.getAsLong(CathodeContract.EpisodeColumns.SEASON_ID);
        getContext().getContentResolver().notifyChange(Episodes.buildFromShowId(showId), null);
        getContext().getContentResolver().notifyChange(Episodes.buildFromSeasonId(seasonId), null);
        return Episodes.buildFromId(episodeId);
      }
      case MOVIES: {
        final long movieId = db.insertOrThrow(Tables.MOVIES, null, values);
        getContext().getContentResolver().notifyChange(Movies.buildMovieUri(movieId), null);
        return ContentUris.withAppendedId(Movies.CONTENT_URI, movieId);
      }
      case MOVIE_GENRES: {
        final long rowId = db.insertOrThrow(Tables.MOVIE_GENRES, null, values);
        final long movieId = values.getAsLong(MovieGenres.MOVIE_ID);
        getContext().getContentResolver().notifyChange(MovieGenres.buildFromMovieId(movieId), null);
        return ContentUris.withAppendedId(MovieGenres.CONTENT_URI, rowId);
      }
      case MOVIE_TOP_WATCHERS: {
        final long rowId = db.insertOrThrow(Tables.MOVIE_TOP_WATCHERS, null, values);
        final long movieId = values.getAsLong(MovieTopWatchers.MOVIE_ID);
        getContext().getContentResolver()
            .notifyChange(MovieTopWatchers.buildFromMovieId(movieId), null);
        return ContentUris.withAppendedId(MovieTopWatchers.CONTENT_URI, rowId);
      }
      case MOVIE_ACTORS: {
        final long rowId = db.insertOrThrow(Tables.MOVIE_ACTORS, null, values);
        final long movieId = values.getAsLong(MovieActors.MOVIE_ID);
        getContext().getContentResolver().notifyChange(MovieActors.buildFromMovieId(movieId), null);
        return ContentUris.withAppendedId(MovieActors.CONTENT_URI, rowId);
      }
      case MOVIE_DIRECTORS: {
        final long rowId = db.insertOrThrow(Tables.MOVIE_DIRECTORS, null, values);
        final long movieId = values.getAsLong(MovieDirectors.MOVIE_ID);
        getContext().getContentResolver()
            .notifyChange(MovieDirectors.buildFromMovieId(movieId), null);
        return ContentUris.withAppendedId(MovieDirectors.CONTENT_URI, rowId);
      }
      case MOVIE_WRITERS: {
        final long rowId = db.insertOrThrow(Tables.MOVIE_WRITERS, null, values);
        final long movieId = values.getAsLong(MovieWriters.MOVIE_ID);
        getContext().getContentResolver()
            .notifyChange(MovieWriters.buildFromMovieId(movieId), null);
        return ContentUris.withAppendedId(MovieWriters.CONTENT_URI, rowId);
      }
      case MOVIE_PRODUCERS: {
        final long rowId = db.insertOrThrow(Tables.MOVIE_PRODUCERS, null, values);
        final long movieId = values.getAsLong(MovieProducers.MOVIE_ID);
        getContext().getContentResolver()
            .notifyChange(MovieProducers.buildFromMovieId(movieId), null);
        return ContentUris.withAppendedId(MovieProducers.CONTENT_URI, rowId);
      }
      default:
        throw new UnsupportedOperationException("Unknown uri: " + uri);
    }
  }

  @Override
  public int delete(Uri uri, String where, String[] whereArgs) {
    final SQLiteDatabase db = database.getWritableDatabase();
    final SelectionBuilder builder = buildSimpleSelection(uri);
    int count = builder.where(where, whereArgs).delete(db);
    getContext().getContentResolver().notifyChange(getBaseUri(uri), null);
    return count;
  }

  @Override
  public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
    final SQLiteDatabase db = database.getWritableDatabase();

    final SelectionBuilder builder = new SelectionBuilder();
    final int match = URI_MATCHER.match(uri);
    switch (match) {
      case SHOWS: {
        final int count = builder.table(Tables.SHOWS).where(where, whereArgs).update(db, values);
        if (count > 0) {
          Cursor c = db.query(Tables.SHOWS, new String[] {
              BaseColumns._ID,
          }, where, whereArgs, null, null, null);
          while (c.moveToNext()) {
            final long id = c.getLong(0);
            getContext().getContentResolver().notifyChange(Shows.buildShowUri(id), null);
          }
          c.close();
        }
        return count;
      }

      case SHOWS_ID: {
        final String showId = Shows.getShowId(uri);
        final int count = builder.table(Tables.SHOWS)
            .where(Shows._ID + "=?", showId)
            .where(where, whereArgs)
            .update(db, values);
        if (count > 0) {
          getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
      }
      case SHOW_TOP_WATCHERS: {
        final String showId = ShowTopWatchers.getShowId(uri);
        final int count = builder.table(Tables.SHOW_TOP_WATCHERS)
            .where(Shows._ID + "=?", showId)
            .where(where, whereArgs)
            .update(db, values);

        if (count > 0) {
          getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
      }
      case SHOW_TOP_EPISODES: {
        final String showId = TopEpisodes.getShowId(uri);
        final int count = builder.table(Tables.SHOW_TOP_EPISODES)
            .where(Shows._ID + "=?", showId)
            .where(where, whereArgs)
            .update(db, values);

        if (count > 0) {
          getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
      }
      case SHOW_ACTORS: {
        final String showId = ShowActor.getShowId(uri);
        final int count = builder.table(Tables.SHOW_ACTORS)
            .where(Shows._ID + "=?", showId)
            .where(where, whereArgs)
            .update(db, values);

        if (count > 0) {
          getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
      }
      case SHOW_GENRES: {
        final String showId = ShowGenres.getShowId(uri);
        final int count = builder.table(Tables.SHOW_GENRES)
            .where(Shows._ID + "=?", showId)
            .where(where, whereArgs)
            .update(db, values);

        if (count > 0) {
          getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
      }
      case SEASONS_OFSHOW: {
        final String showId = Seasons.getShowId(uri);
        builder.where(Seasons.SHOW_ID + "=?", showId);
      }
      case SEASONS: {
        final int count = builder.table(Tables.SEASONS).where(where, whereArgs).update(db, values);
        if (count > 0) {
          Cursor c = db.query(Tables.SEASONS, new String[] {
              BaseColumns._ID, Seasons.SHOW_ID,
          }, where, whereArgs, null, null, null);
          while (c.moveToNext()) {
            final long id = c.getLong(0);
            final long showId = c.getLong(1);
            getContext().getContentResolver().notifyChange(Seasons.buildFromId(id), null);
            getContext().getContentResolver().notifyChange(Seasons.buildFromShowId(showId), null);
          }
          c.close();
        }
        return count;
      }
      case SEASON_ID: {
        final String seasonId = Seasons.getSeasonId(uri);
        final int count = builder.table(Tables.SEASONS)
            .where(Seasons._ID + "=?", seasonId)
            .where(where, whereArgs)
            .update(db, values);
        if (count > 0) {
          final long showId =
              SeasonWrapper.getShowId(getContext().getContentResolver(), Long.valueOf(seasonId));
          getContext().getContentResolver().notifyChange(Seasons.buildFromShowId(showId), null);
          getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
      }
      case EPISODES: {
        final int count = builder.table(Tables.EPISODES).where(where, whereArgs).update(db, values);
        if (count > 0) {
          Cursor c = db.query(Tables.EPISODES, new String[] {
              BaseColumns._ID, Episodes.SHOW_ID, Episodes.SEASON_ID,
          }, where, whereArgs, null, null, null);
          while (c.moveToNext()) {
            final long id = c.getLong(0);
            final long showId = c.getLong(1);
            final long seasonId = c.getLong(2);
            getContext().getContentResolver().notifyChange(Episodes.buildFromId(id), null);
            getContext().getContentResolver().notifyChange(Episodes.buildFromShowId(showId), null);
            getContext().getContentResolver()
                .notifyChange(Episodes.buildFromSeasonId(seasonId), null);
            getContext().getContentResolver().notifyChange(Shows.buildShowUri(showId), null);
            getContext().getContentResolver().notifyChange(Seasons.buildFromId(seasonId), null);
            getContext().getContentResolver().notifyChange(Seasons.buildFromShowId(seasonId), null);
          }
          c.close();
        }
        return count;
      }
      case EPISODE_ID: {
        final String episodeId = Episodes.getEpisodeId(uri);
        final int count = builder.table(Tables.EPISODES)
            .where(Episodes._ID + "=?", episodeId)
            .where(where, whereArgs)
            .update(db, values);
        if (count > 0) {
          final long id = Long.valueOf(episodeId);
          final long showId = EpisodeWrapper.getShowId(getContext().getContentResolver(), id);
          final long seasonId = EpisodeWrapper.getSeasonId(getContext().getContentResolver(), id);
          getContext().getContentResolver().notifyChange(Episodes.buildFromId(id), null);
          getContext().getContentResolver().notifyChange(Episodes.buildFromShowId(showId), null);
          getContext().getContentResolver()
              .notifyChange(Episodes.buildFromSeasonId(seasonId), null);
          getContext().getContentResolver().notifyChange(Shows.buildShowUri(showId), null);
          getContext().getContentResolver().notifyChange(Seasons.buildFromId(seasonId), null);
          getContext().getContentResolver().notifyChange(Seasons.buildFromShowId(seasonId), null);
        }
        return count;
      }
      case EPISODES_FROMSHOW: {
        final String showIdStr = Episodes.getShowId(uri);
        final int count = builder.table(Tables.EPISODES)
            .where(Episodes.SHOW_ID + "=?", showIdStr)
            .where(where, whereArgs)
            .update(db, values);
        if (count > 0) {
          Cursor c = builder.query(db, new String[] {
              BaseColumns._ID, Episodes.SHOW_ID, Episodes.SEASON_ID,
          }, null);
          while (c.moveToNext()) {
            final long id = c.getLong(0);
            final long showId = c.getLong(1);
            final long seasonId = c.getLong(2);
            getContext().getContentResolver().notifyChange(Episodes.buildFromId(id), null);
            getContext().getContentResolver().notifyChange(Episodes.buildFromShowId(showId), null);
            getContext().getContentResolver()
                .notifyChange(Episodes.buildFromSeasonId(seasonId), null);
            getContext().getContentResolver().notifyChange(Shows.buildShowUri(showId), null);
            getContext().getContentResolver().notifyChange(Seasons.buildFromId(seasonId), null);
            getContext().getContentResolver().notifyChange(Seasons.buildFromShowId(seasonId), null);
          }
          c.close();
        }
        return count;
      }
      case EPISODES_FROMSEASON: {
        final String seasonIdStr = Episodes.getSeasonId(uri);
        final int count = builder.table(Tables.EPISODES)
            .where(Episodes.SEASON_ID + "=?", seasonIdStr)
            .where(where, whereArgs)
            .update(db, values);
        if (count > 0) {
          Cursor c = builder.query(db, new String[] {
              BaseColumns._ID, Episodes.SHOW_ID, Episodes.SEASON_ID,
          }, null);
          while (c.moveToNext()) {
            final long id = c.getLong(0);
            final long showId = c.getLong(1);
            final long seasonId = c.getLong(2);
            getContext().getContentResolver().notifyChange(Episodes.buildFromId(id), null);
            getContext().getContentResolver().notifyChange(Episodes.buildFromShowId(showId), null);
            getContext().getContentResolver()
                .notifyChange(Episodes.buildFromSeasonId(seasonId), null);
            getContext().getContentResolver().notifyChange(Shows.buildShowUri(showId), null);
            getContext().getContentResolver().notifyChange(Seasons.buildFromId(seasonId), null);
            getContext().getContentResolver().notifyChange(Seasons.buildFromShowId(seasonId), null);
          }
          c.close();
        }
        return count;
      }
      case MOVIES: {
        final int count = builder.table(Tables.MOVIES).where(where, whereArgs).update(db, values);
        if (count > 0) {
          Cursor c = db.query(Tables.MOVIES, new String[] {
              BaseColumns._ID,
          }, where, whereArgs, null, null, null);
          while (c.moveToNext()) {
            final long id = c.getLong(0);
            getContext().getContentResolver().notifyChange(Movies.buildMovieUri(id), null);
          }
          c.close();
        }
        return count;
      }
      case MOVIE_ID: {
        final String movieId = Movies.getMovieId(uri);
        final int count = builder.table(Tables.MOVIES)
            .where(Movies._ID + "=?", movieId)
            .where(where, whereArgs)
            .update(db, values);
        if (count > 0) {
          getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
      }
      case MOVIE_GENRES: {
        final String movieId = MovieGenres.getMovieId(uri);
        final int count = builder.table(Tables.MOVIE_GENRES)
            .where(MovieGenres.MOVIE_ID + "=?", movieId)
            .where(where, whereArgs)
            .update(db, values);
        if (count > 0) {
          getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
      }
      case MOVIE_TOP_WATCHERS: {
        final String movieId = MovieTopWatchers.getMovieId(uri);
        final int count = builder.table(Tables.MOVIE_TOP_WATCHERS)
            .where(MovieTopWatchers.MOVIE_ID + "=?", movieId)
            .where(where, whereArgs)
            .update(db, values);
        if (count > 0) {
          getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
      }
      case MOVIE_ACTORS: {
        final String movieId = MovieActors.getMovieId(uri);
        final int count = builder.table(Tables.MOVIE_ACTORS)
            .where(MovieActors.MOVIE_ID + "=?", movieId)
            .where(where, whereArgs)
            .update(db, values);
        if (count > 0) {
          getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
      }
      case MOVIE_DIRECTORS: {
        final String movieId = MovieDirectors.getMovieId(uri);
        final int count = builder.table(Tables.MOVIE_DIRECTORS)
            .where(MovieDirectors.MOVIE_ID + "=?", movieId)
            .where(where, whereArgs)
            .update(db, values);
        if (count > 0) {
          getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
      }
      case MOVIE_WRITERS: {
        final String movieId = MovieWriters.getMovieId(uri);
        final int count = builder.table(Tables.MOVIE_WRITERS)
            .where(MovieWriters.MOVIE_ID + "=?", movieId)
            .where(where, whereArgs)
            .update(db, values);
        if (count > 0) {
          getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
      }
      case MOVIE_PRODUCERS: {
        final String movieId = MovieProducers.getMovieId(uri);
        final int count = builder.table(Tables.MOVIE_PRODUCERS)
            .where(MovieProducers.MOVIE_ID + "=?", movieId)
            .where(where, whereArgs)
            .update(db, values);
        if (count > 0) {
          getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
      }

      default: {
        throw new UnsupportedOperationException("Unknown uri: " + uri);
      }
    }
  }

  private Uri getBaseUri(Uri uri) {
    return CathodeContract.BASE_CONTENT_URI
        .buildUpon()
        .appendPath(uri.getPathSegments().get(0))
        .build();
  }

  private SelectionBuilder buildSimpleSelection(Uri uri) {
    final SelectionBuilder builder = new SelectionBuilder();
    final int match = URI_MATCHER.match(uri);
    switch (match) {
      case SHOWS: {
        return builder.table(Tables.SHOWS);
      }
      case SHOWS_WITHNEXT: {
        return builder.table(Tables.SHOWS_WITH_UNWATCHED);
      }
      case SHOWS_WITHNEXT_IGNOREWATCHED: {
        return builder.table(Tables.SHOWS_WITH_UNWATCHED)
            .where(CathodeContract.ShowColumns.WATCHED_COUNT + "<" + Shows.getAiredQuery(), null);
      }
      case SHOWS_ID: {
        final String showId = Shows.getShowId(uri);
        return builder.table(Tables.SHOWS).where(Shows._ID + "=?", showId);
      }
      case SHOW_TOP_WATCHERS: {
        final String showId = ShowTopWatchers.getShowId(uri);
        return builder.table(Tables.SHOW_TOP_WATCHERS).where(Shows._ID + "=?", showId);
      }
      case SHOW_TOP_EPISODES: {
        final String showId = TopEpisodes.getShowId(uri);
        return builder.table(Tables.SHOW_TOP_EPISODES).where(Shows._ID + "=?", showId);
      }
      case SHOW_ACTORS: {
        final String showId = ShowActor.getShowId(uri);
        return builder.table(Tables.SHOW_ACTORS).where(Shows._ID + "=?", showId);
      }
      case SHOW_GENRES: {
        final String showId = ShowGenres.getShowId(uri);
        return builder.table(Tables.SHOW_GENRES).where(Shows._ID + "=?", showId);
      }
      case SHOWS_WATCHLIST: {
        return builder.table(Tables.SHOWS_WITH_UNWATCHED).where(Shows.IN_WATCHLIST + "=1");
      }
      case SHOWS_COLLECTION: {
        return builder.table(Tables.SHOWS_WITH_UNCOLLECTED).where(Shows.IN_COLLECTION_COUNT + ">0");
      }
      case SHOWS_WATCHED: {
        return builder.table(Tables.SHOWS_WITH_UNWATCHED).where(Shows.WATCHED_COUNT + ">0");
      }
      case SEASONS: {
        return builder.table(Tables.SEASONS);
      }
      case SEASON_ID: {
        final String seasonId = Seasons.getSeasonId(uri);
        return builder.table(Tables.SEASONS).where(Seasons._ID + "=?", seasonId);
      }
      case SEASONS_OFSHOW: {
        final String showId = Seasons.getShowId(uri);
        return builder.table(Tables.SEASONS).where(Seasons.SHOW_ID + "=?", showId);
      }
      case EPISODES: {
        return builder.table(Tables.EPISODES);
      }
      case EPISODE_ID: {
        final String episodeId = Episodes.getEpisodeId(uri);
        return builder.table(Tables.EPISODES).where(Episodes._ID + "=?", episodeId);
      }
      case EPISODES_FROMSHOW: {
        final String showId = Episodes.getShowId(uri);
        return builder.table(Tables.EPISODES).where(Episodes.SHOW_ID + "=?", showId);
      }
      case EPISODES_FROMSEASON: {
        final String seasonId = Episodes.getSeasonId(uri);
        return builder.table(Tables.EPISODES).where(Episodes.SEASON_ID + "=?", seasonId);
      }
      case MOVIES: {
        return builder.table(Tables.MOVIES);
      }
      case MOVIE_ID: {
        final String movieId = Movies.getMovieId(uri);
        return builder.table(Tables.MOVIES).where(Movies._ID + "=?", movieId);
      }
      case MOVIE_GENRES: {
        final String movieId = MovieGenres.getMovieId(uri);
        return builder.table(Tables.MOVIE_GENRES).where(MovieGenres.MOVIE_ID + "=?", movieId);
      }
      case MOVIE_TOP_WATCHERS: {
        final String movieId = MovieTopWatchers.getMovieId(uri);
        return builder.table(Tables.MOVIE_TOP_WATCHERS)
            .where(MovieTopWatchers.MOVIE_ID + "=?", movieId);
      }
      case MOVIE_ACTORS: {
        final String movieId = MovieActors.getMovieId(uri);
        return builder.table(Tables.MOVIE_ACTORS).where(MovieActors.MOVIE_ID + "=?", movieId);
      }
      case MOVIE_DIRECTORS: {
        final String movieId = MovieDirectors.getMovieId(uri);
        return builder.table(Tables.MOVIE_DIRECTORS).where(MovieDirectors.MOVIE_ID + "=?", movieId);
      }
      case MOVIE_WRITERS: {
        final String movieId = MovieWriters.getMovieId(uri);
        return builder.table(Tables.MOVIE_WRITERS).where(MovieWriters.MOVIE_ID + "=?", movieId);
      }
      case MOVIE_PRODUCERS: {
        final String movieId = MovieProducers.getMovieId(uri);
        return builder.table(Tables.MOVIE_PRODUCERS).where(MovieProducers.MOVIE_ID + "=?", movieId);
      }
      default: {
        throw new UnsupportedOperationException("Unknown uri: " + uri);
      }
    }
  }
}
