package net.simonvt.trakt.provider;

import net.simonvt.trakt.provider.TraktContract.Episodes;
import net.simonvt.trakt.provider.TraktContract.MovieActors;
import net.simonvt.trakt.provider.TraktContract.MovieDirectors;
import net.simonvt.trakt.provider.TraktContract.MovieGenres;
import net.simonvt.trakt.provider.TraktContract.MovieProducers;
import net.simonvt.trakt.provider.TraktContract.MovieTopWatchers;
import net.simonvt.trakt.provider.TraktContract.MovieWriters;
import net.simonvt.trakt.provider.TraktContract.Movies;
import net.simonvt.trakt.provider.TraktContract.Seasons;
import net.simonvt.trakt.provider.TraktContract.ShowActor;
import net.simonvt.trakt.provider.TraktContract.ShowGenres;
import net.simonvt.trakt.provider.TraktContract.ShowTopWatchers;
import net.simonvt.trakt.provider.TraktContract.Shows;
import net.simonvt.trakt.provider.TraktContract.TopEpisodes;
import net.simonvt.trakt.provider.TraktDatabase.Tables;
import net.simonvt.trakt.util.LogWrapper;
import net.simonvt.trakt.util.SelectionBuilder;

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

import java.util.ArrayList;

public class TraktProvider extends ContentProvider {

    private static final String TAG = "TraktProvider";

    public static final String AUTHORITY = "net.simonvt.trakt.provider.TraktProvider";

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

    private static final int MOVIES = 400;
    private static final int MOVIE_ID = 401;

    private static final int MOVIE_GENRES = 402;
    private static final int MOVIE_TOP_WATCHERS = 403;
    private static final int MOVIE_ACTORS = 404;
    private static final int MOVIE_DIRECTORS = 405;
    private static final int MOVIE_WRITERS = 406;
    private static final int MOVIE_PRODUCERS = 407;

    private TraktDatabase mDatabase;

    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URI_MATCHER.addURI(AUTHORITY, TraktContract.PATH_SHOWS, SHOWS);
        URI_MATCHER.addURI(AUTHORITY, TraktContract.PATH_SHOWS + "/" + TraktContract.PATH_WITHID + "/*", SHOWS_ID);
        URI_MATCHER.addURI(AUTHORITY, TraktContract.PATH_SHOWS + "/" + TraktContract.PATH_WITHNEXT, SHOWS_WITHNEXT);
        URI_MATCHER.addURI(AUTHORITY,
                TraktContract.PATH_SHOWS + "/" + TraktContract.PATH_WITHNEXT + "/" + TraktContract.PATH_IGNOREWATCHED,
                SHOWS_WITHNEXT_IGNOREWATCHED);
        URI_MATCHER.addURI(AUTHORITY,
                TraktContract.PATH_TOPWATCHERS + "/" + TraktContract.PATH_FROMSHOW + "/*", SHOW_TOP_WATCHERS);
        URI_MATCHER.addURI(AUTHORITY,
                TraktContract.PATH_TOPEPISODES + "/" + TraktContract.PATH_FROMSHOW + "/*", SHOW_TOP_EPISODES);
        URI_MATCHER.addURI(AUTHORITY,
                TraktContract.PATH_ACTORS + "/" + TraktContract.PATH_FROMSHOW + "/*", SHOW_ACTORS);
        URI_MATCHER.addURI(AUTHORITY,
                TraktContract.PATH_GENRES + "/" + TraktContract.PATH_FROMSHOW + "/*", SHOW_GENRES);
        URI_MATCHER.addURI(AUTHORITY, TraktContract.PATH_SHOWS + "/" + TraktContract.PATH_WATCHLIST, SHOWS_WATCHLIST);
        URI_MATCHER.addURI(AUTHORITY, TraktContract.PATH_SHOWS + "/" + TraktContract.PATH_COLLECTION,
                SHOWS_COLLECTION);
        URI_MATCHER.addURI(AUTHORITY, TraktContract.PATH_SHOWS + "/" + TraktContract.PATH_WATCHED, SHOWS_WATCHED);

        URI_MATCHER.addURI(AUTHORITY, TraktContract.PATH_SEASONS, SEASONS);
        URI_MATCHER.addURI(AUTHORITY, TraktContract.PATH_SEASONS + "/" + TraktContract.PATH_WITHID + "/*", SEASON_ID);
        URI_MATCHER.addURI(AUTHORITY,
                TraktContract.PATH_SEASONS + "/" + TraktContract.PATH_FROMSHOW + "/*", SEASONS_OFSHOW);

        URI_MATCHER.addURI(AUTHORITY, TraktContract.PATH_EPISODES, EPISODES);
        URI_MATCHER.addURI(AUTHORITY, TraktContract.PATH_EPISODES + "/" + TraktContract.PATH_WITHID + "/*",
                EPISODE_ID);
        URI_MATCHER.addURI(AUTHORITY,
                TraktContract.PATH_EPISODES + "/" + TraktContract.PATH_FROMSHOW + "/*", EPISODES_FROMSHOW);
        URI_MATCHER.addURI(AUTHORITY,
                TraktContract.PATH_EPISODES + "/" + TraktContract.PATH_FROMSEASON + "/*", EPISODES_FROMSEASON);

        URI_MATCHER.addURI(AUTHORITY, TraktContract.PATH_MOVIES, MOVIES);
        URI_MATCHER.addURI(AUTHORITY, TraktContract.PATH_MOVIES + "/*", MOVIE_ID);
        URI_MATCHER.addURI(AUTHORITY, TraktContract.PATH_GENRES + "/" + TraktContract.PATH_MOVIES + "/*", MOVIE_GENRES);
        URI_MATCHER.addURI(AUTHORITY,
                TraktContract.PATH_TOPWATCHERS + "/" + TraktContract.PATH_MOVIES + "/*", MOVIE_TOP_WATCHERS);
        URI_MATCHER.addURI(AUTHORITY, TraktContract.PATH_ACTORS + "/" + TraktContract.PATH_MOVIES + "/*", MOVIE_ACTORS);
        URI_MATCHER.addURI(AUTHORITY,
                TraktContract.PATH_DIRECTORS + "/" + TraktContract.PATH_MOVIES + "/*", MOVIE_DIRECTORS);
        URI_MATCHER.addURI(AUTHORITY,
                TraktContract.PATH_WRITERS + "/" + TraktContract.PATH_MOVIES + "/*", MOVIE_WRITERS);
        URI_MATCHER.addURI(AUTHORITY,
                TraktContract.PATH_PRODUCERS + "/" + TraktContract.PATH_MOVIES + "/*", MOVIE_PRODUCERS);
    }

    @Override
    public boolean onCreate() {
        mDatabase = new TraktDatabase(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        final SQLiteDatabase db = mDatabase.getReadableDatabase();
        final int match = URI_MATCHER.match(uri);
        switch (match) {
            default: {
                // Most cases are handled with simple SelectionBuilder
                final SelectionBuilder builder = buildExpandedSelection(uri, match);
                return builder.where(selection, selectionArgs).query(db, projection, sortOrder);
            }
        }
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        mDatabase.getWritableDatabase().beginTransaction();
        ContentProviderResult[] results = super.applyBatch(operations);
        mDatabase.getWritableDatabase().setTransactionSuccessful();
        mDatabase.getWritableDatabase().endTransaction();
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
                return TraktContract.Seasons.CONTENT_TYPE;
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
        final SQLiteDatabase db = mDatabase.getWritableDatabase();
        switch (URI_MATCHER.match(uri)) {
            case SHOWS: {
                LogWrapper.v(TAG, "Insert URI: " + uri.toString() + " - Shows URI: " + Shows.CONTENT_URI.toString());
                long rowId = db.insertOrThrow(Tables.SHOWS, null, values);
                getContext().getContentResolver().notifyChange(Shows.CONTENT_URI, null);
                return Shows.buildShowUri(rowId);
            }
            case SHOW_TOP_WATCHERS: {
                long rowId = db.insertOrThrow(Tables.SHOW_TOP_WATCHERS, null, values);
                getContext().getContentResolver().notifyChange(ShowTopWatchers.CONTENT_URI, null);
                return ContentUris.withAppendedId(ShowTopWatchers.CONTENT_URI, rowId);
            }
            case SHOW_TOP_EPISODES: {
                long rowId = db.insertOrThrow(Tables.SHOW_TOP_EPISODES, null, values);
                getContext().getContentResolver().notifyChange(uri, null);
                return ContentUris.withAppendedId(TopEpisodes.CONTENT_URI, rowId);
            }
            case SHOW_ACTORS: {
                long rowId = db.insertOrThrow(Tables.SHOW_ACTORS, null, values);
                getContext().getContentResolver().notifyChange(uri, null);
                return ContentUris.withAppendedId(ShowActor.CONTENT_URI, rowId);
            }
            case SHOW_GENRES: {
                long rowId = db.insertOrThrow(Tables.SHOW_GENRES, null, values);
                getContext().getContentResolver().notifyChange(uri, null);
                return ContentUris.withAppendedId(ShowGenres.CONTENT_URI, rowId);
            }
            case SEASONS: {
                long rowId = db.insertOrThrow(Tables.SEASONS, null, values);
                getContext().getContentResolver().notifyChange(Seasons.CONTENT_URI, null);
                return Seasons.buildFromId(rowId);
            }
            case EPISODES: {
                long rowId = db.insertOrThrow(Tables.EPISODES, null, values);
                getContext().getContentResolver().notifyChange(Episodes.CONTENT_URI, null);
                return Episodes.buildFromId(rowId);
            }
            case MOVIES: {
                long rowId = db.insertOrThrow(Tables.MOVIES, null, values);
                getContext().getContentResolver().notifyChange(uri, null);
                return ContentUris.withAppendedId(Movies.CONTENT_URI, rowId);
            }
            case MOVIE_GENRES: {
                long rowId = db.insertOrThrow(Tables.MOVIE_GENRES, null, values);
                getContext().getContentResolver().notifyChange(uri, null);
                return ContentUris.withAppendedId(MovieGenres.CONTENT_URI, rowId);
            }
            case MOVIE_TOP_WATCHERS: {
                long rowId = db.insertOrThrow(Tables.MOVIE_TOP_WATCHERS, null, values);
                getContext().getContentResolver().notifyChange(uri, null);
                return ContentUris.withAppendedId(MovieTopWatchers.CONTENT_URI, rowId);
            }
            case MOVIE_ACTORS: {
                long rowId = db.insertOrThrow(Tables.MOVIE_ACTORS, null, values);
                getContext().getContentResolver().notifyChange(uri, null);
                return ContentUris.withAppendedId(MovieActors.CONTENT_URI, rowId);
            }
            case MOVIE_DIRECTORS: {
                long rowId = db.insertOrThrow(Tables.MOVIE_DIRECTORS, null, values);
                getContext().getContentResolver().notifyChange(uri, null);
                return ContentUris.withAppendedId(MovieDirectors.CONTENT_URI, rowId);
            }
            case MOVIE_WRITERS: {
                long rowId = db.insertOrThrow(Tables.MOVIE_WRITERS, null, values);
                getContext().getContentResolver().notifyChange(uri, null);
                return ContentUris.withAppendedId(MovieWriters.CONTENT_URI, rowId);
            }
            case MOVIE_PRODUCERS: {
                long rowId = db.insertOrThrow(Tables.MOVIE_PRODUCERS, null, values);
                getContext().getContentResolver().notifyChange(uri, null);
                return ContentUris.withAppendedId(MovieProducers.CONTENT_URI, rowId);
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        final SQLiteDatabase db = mDatabase.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);
        int count = builder.where(where, whereArgs).delete(db);
        getContext().getContentResolver().notifyChange(getBaseUri(uri), null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        final SQLiteDatabase db = mDatabase.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);
        int count = builder.where(where, whereArgs).update(db, values);
        getContext().getContentResolver().notifyChange(getBaseUri(uri), null);
        return count;
    }

    private Uri getBaseUri(Uri uri) {
        return TraktContract.BASE_CONTENT_URI.buildUpon().appendPath(uri.getPathSegments().get(0)).build();
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
                return builder.table(Tables.SHOWS_WITH_UNWATCHED).where(TraktContract.ShowColumns.WATCHED_COUNT + "<"
                        + TraktContract.ShowColumns.AIRED_COUNT, null);
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
                return builder.table(Tables.MOVIE_TOP_WATCHERS).where(MovieTopWatchers.MOVIE_ID + "=?", movieId);
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

    /**
     * Build an advanced {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually only used by {@link #query}, since it
     * performs table joins useful for {@link Cursor} data.
     */
    private SelectionBuilder buildExpandedSelection(Uri uri, int match) {
        final SelectionBuilder builder = new SelectionBuilder();
        switch (match) {
            default: {
                return buildSimpleSelection(uri);
            }
        }
    }
}
