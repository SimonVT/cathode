package net.simonvt.trakt.provider;

import net.simonvt.trakt.provider.TraktContract.ActorColumns;
import net.simonvt.trakt.provider.TraktContract.EpisodeColumns;
import net.simonvt.trakt.provider.TraktContract.ImageColumns;
import net.simonvt.trakt.provider.TraktContract.MovieActors;
import net.simonvt.trakt.provider.TraktContract.MovieColumns;
import net.simonvt.trakt.provider.TraktContract.MovieDirectors;
import net.simonvt.trakt.provider.TraktContract.MovieGenres;
import net.simonvt.trakt.provider.TraktContract.MovieProducers;
import net.simonvt.trakt.provider.TraktContract.MovieTopWatchers;
import net.simonvt.trakt.provider.TraktContract.MovieWriters;
import net.simonvt.trakt.provider.TraktContract.SeasonColumns;
import net.simonvt.trakt.provider.TraktContract.ShowActor;
import net.simonvt.trakt.provider.TraktContract.ShowColumns;
import net.simonvt.trakt.provider.TraktContract.ShowGenres;
import net.simonvt.trakt.provider.TraktContract.ShowTopWatchers;
import net.simonvt.trakt.provider.TraktContract.TopEpisodeColumns;
import net.simonvt.trakt.provider.TraktContract.TopWatcherColumns;
import net.simonvt.trakt.provider.TraktContract.UserActivity;
import net.simonvt.trakt.util.DateUtils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class TraktDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "trakt.db";
    private static final int DATABASE_VERSION = 1;

    public interface Tables {

        String SHOWS = "shows";

        String SHOWS_WITH_UNWATCHED = SHOWS + " LEFT OUTER JOIN episodes ON episodes._id=(SELECT episodes._id FROM"
                + " episodes WHERE episodes.watched=0 AND episodes.showId=shows._id AND episodes.season<>0"
                + " AND episodes.episodeFirstAired>" + DateUtils.YEAR_IN_SECONDS // TODO: Find better solution
                + " ORDER BY episodes.season ASC, episodes.episode ASC LIMIT 1)";

        String SHOWS_WITH_UNCOLLECTED = SHOWS + " LEFT OUTER JOIN episodes ON episodes._id=(SELECT episodes._id FROM"
                + " episodes WHERE episodes.inCollection=0 AND episodes.showId=shows._id AND episodes.season<>0"
                + " AND episodes.episodeFirstAired>" + DateUtils.YEAR_IN_SECONDS // TODO: Find better solution
                + " ORDER BY episodes.season ASC, episodes.episode ASC LIMIT 1)";

        String SHOW_TOP_WATCHERS = "showTopWatchers";
        String SHOW_TOP_EPISODES = "topEpisodes";
        String SHOW_ACTORS = "showActors";
        String SHOW_GENRES = "showGenres";
        String SEASONS = "seasons";
        String EPISODES = "episodes";
        String EPISODES_WITH_SHOW_TITLE = EPISODES + " JOIN " + SHOWS + " AS shows ON " + SHOWS + "." + TraktContract.Shows._ID
                + "=" + EPISODES + "." + TraktContract.Episodes.SHOW_ID;
        String MOVIES = "movies";
        String MOVIE_GENRES = "movieGenres";
        String MOVIE_TOP_WATCHERS = "movieTopWatchers";
        String MOVIE_ACTORS = "movieActors";
        String MOVIE_DIRECTORS = "movieDirectors";
        String MOVIE_WRITERS = "movieWriters";
        String MOVIE_PRODUCERS = "movieProducers";
        String USER_ACTIVITY = "userActivity";
    }

    interface References {

        String SHOW_ID = "REFERENCES " + Tables.SHOWS + "(" + BaseColumns._ID + ")";
        String SEASON_ID = "REFERENCES " + Tables.SEASONS + "(" + BaseColumns._ID + ")";
        String MOVIE_ID = "REFERENCES " + Tables.MOVIES + "(" + BaseColumns._ID + ")";
    }

    public TraktDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.SHOWS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ShowColumns.TITLE + " TEXT NOT NULL,"
                + ShowColumns.YEAR + " INTEGER,"
                + ShowColumns.URL + " TEXT,"
                + ShowColumns.FIRST_AIRED + " INTEGER,"
                + ShowColumns.COUNTRY + " TEXT,"
                + ShowColumns.OVERVIEW + " TEXT,"
                + ShowColumns.RUNTIME + " INTEGER DEFAULT 0,"
                + ShowColumns.NETWORK + " TEXT,"
                + ShowColumns.STATUS + " TEXT,"
                + ShowColumns.AIR_DAY + " TEXT,"
                + ShowColumns.AIR_TIME + " TEXT,"
                + ShowColumns.CERTIFICATION + " TEXT,"
                + ShowColumns.IMDB_ID + " TEXT,"
                + ShowColumns.TVDB_ID + " TEXT,"
                + ShowColumns.TVRAGE_ID + " TEXT,"
                + ShowColumns.LAST_UPDATED + " INTEGER DEFAULT 0,"
                + ImageColumns.POSTER + " TEXT,"
                + ImageColumns.FANART + " TEXT,"
                + ImageColumns.HEADSHOT + " TEXT,"
                + ImageColumns.SCREEN + " TEXT,"
                + ImageColumns.BANNER + " TEXT,"
                + ShowColumns.RATING_PERCENTAGE + " INTEGER DEFAULT 0,"
                + ShowColumns.RATING_VOTES + " INTEGER DEFAULT 0,"
                + ShowColumns.RATING_LOVED + " INTEGER DEFAULT 0,"
                + ShowColumns.RATING_HATED + " INTEGER DEFAULT 0,"
                + ShowColumns.WATCHERS + " INTEGER DEFAULT 0,"
                + ShowColumns.PLAYS + " INTEGER DEFAULT 0,"
                + ShowColumns.SCROBBLES + " INTEGER DEFAULT 0,"
                + ShowColumns.CHECKINS + " INTEGER DEFAULT 0,"
                + ShowColumns.RATING + " INTEGER DEFAULT 0,"
                + ShowColumns.IN_WATCHLIST + " INTEGER DEFAULT 0,"
                + ShowColumns.WATCHED_COUNT + " INTEGER DEFAULT 0,"
                + ShowColumns.UNAIRED_COUNT + " INTEGER DEFAULT 0,"
                + ShowColumns.AIRDATE_COUNT + " INTEGER DEFAULT 0,"
                + ShowColumns.AIRED_COUNT + " INTEGER DEFAULT 0,"
                + ShowColumns.IN_WATCHLIST_COUNT + " INTEGER DEFAULT 0,"
                + ShowColumns.IN_COLLECTION_COUNT + " INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + Tables.SHOW_TOP_WATCHERS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ShowTopWatchers.SHOW_ID + " INTEGER " + References.SHOW_ID + ","
                + TopWatcherColumns.PLAYS + " INTEGER DEFAULT 0,"
                + TopWatcherColumns.USERNAME + " TEXT NOT NULL,"
                + TopWatcherColumns.PROTECTED + " INTEGER DEFAULT 0,"
                + TopWatcherColumns.FULL_NAME + " TEXT,"
                + TopWatcherColumns.GENDER + " TEXT,"
                + TopWatcherColumns.AGE + " INTEGER,"
                + TopWatcherColumns.LOCATION + "TEXT,"
                + TopWatcherColumns.ABOUT + " TEXT,"
                + TopWatcherColumns.JOINED + " INTEGER DEFAULT 0,"
                + TopWatcherColumns.AVATAR + " TEXT,"
                + TopWatcherColumns.URL + " TEXT NOT NULL)");

        db.execSQL("CREATE TABLE " + Tables.SHOW_TOP_EPISODES + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + TopEpisodeColumns.SHOW_ID + " INTEGER " + References.SHOW_ID + ","
                + TopEpisodeColumns.SEASON_ID + " INTEGER " + References.SEASON_ID + ","
                + TopEpisodeColumns.NUMBER + " INTEGER NOT NULL,"
                + TopEpisodeColumns.PLAYS + " INTEGER DEFAULT 0,"
                + TopEpisodeColumns.TITLE + " TEXT NOT NULL,"
                + TopEpisodeColumns.URL + " TEXT,"
                + TopEpisodeColumns.FIRST_AIRED + " INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + Tables.SHOW_ACTORS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ShowActor.SHOW_ID + " INTEGER " + References.SHOW_ID + ","
                + ActorColumns.NAME + " TEXT NOT NULL,"
                + ActorColumns.CHARACTER + " TEXT)");

        db.execSQL("CREATE TABLE " + Tables.SHOW_GENRES + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ShowGenres.SHOW_ID + " INTEGER " + References.SHOW_ID + ","
                + ShowGenres.GENRE + " TEXT)");

        db.execSQL("CREATE TABLE " + Tables.SEASONS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SeasonColumns.SHOW_ID + " INTEGER " + References.SHOW_ID + ","
                + SeasonColumns.SEASON + " INTEGER NOT NULL,"
                + SeasonColumns.EPISODES + " INTEGER DEFAULT 0,"
                + SeasonColumns.URL + " TEXT,"
                + SeasonColumns.WATCHED_COUNT + " INTEGER DEFAULT 0,"
                + SeasonColumns.UNAIRED_COUNT + " INTEGER DEFAULT 0,"
                + SeasonColumns.AIRDATE_COUNT + " INTEGER DEFAULT 0,"
                + SeasonColumns.AIRED_COUNT + " INTEGER DEFAULT 0,"
                + SeasonColumns.IN_COLLECTION_COUNT + " INTEGER DEFAULT 0,"
                + SeasonColumns.IN_WATCHLIST_COUNT + " INTEGER DEFAULT 0,"
                + ImageColumns.POSTER + " TEXT,"
                + ImageColumns.FANART + " TEXT,"
                + ImageColumns.HEADSHOT + " TEXT,"
                + ImageColumns.SCREEN + " TEXT,"
                + ImageColumns.BANNER + " TEXT)");

        db.execSQL("CREATE TABLE " + Tables.EPISODES + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + EpisodeColumns.SHOW_ID + " INTEGER " + References.SHOW_ID + ","
                + EpisodeColumns.SEASON_ID + " INTEGER " + References.SEASON_ID + ","
                + EpisodeColumns.SEASON + " INTEGER NOT NULL,"
                + EpisodeColumns.EPISODE + " INTEGER NOT NULL,"
                + EpisodeColumns.TITLE + " TEXT NOT NULL,"
                + EpisodeColumns.OVERVIEW + " TEXT,"
                + EpisodeColumns.URL + " TEXT,"
                + EpisodeColumns.FIRST_AIRED + " INTEGER,"
                + ImageColumns.POSTER + " TEXT,"
                + ImageColumns.FANART + " TEXT,"
                + ImageColumns.HEADSHOT + " TEXT,"
                + ImageColumns.SCREEN + " TEXT,"
                + ImageColumns.BANNER + " TEXT,"
                + EpisodeColumns.RATING_PERCENTAGE + " INTEGER DEFAULT 0,"
                + EpisodeColumns.RATING_VOTES + " INTEGER DEFAULT 0,"
                + EpisodeColumns.RATING_LOVED + " INTEGER DEFAULT 0,"
                + EpisodeColumns.RATING_HATED + " INTEGER DEFAULT 0,"
                + EpisodeColumns.WATCHED + " INTEGER DEFAULT 0,"
                + EpisodeColumns.PLAYS + " INTEGER DEFAULT 0,"
                + EpisodeColumns.RATING + " TEXT,"
                + EpisodeColumns.IN_WATCHLIST + " INTEGER DEFAULT 0,"
                + EpisodeColumns.IN_COLLECTION + " INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + Tables.MOVIES + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + MovieColumns.TITLE + " TEXT NOT NULL,"
                + MovieColumns.YEAR + " INTEGER,"
                + MovieColumns.RELEASED + " INTEGER,"
                + MovieColumns.URL + " TEXT,"
                + MovieColumns.TRAILER + " TEXT,"
                + MovieColumns.RUNTIME + " INTEGER,"
                + MovieColumns.TAGLINE + " TEXT,"
                + MovieColumns.OVERVIEW + " TEXT,"
                + MovieColumns.CERTIFICATION + " TEXT,"
                + MovieColumns.IMDB_ID + " TEXT,"
                + MovieColumns.TMDB_ID + " INTEGER,"
                + MovieColumns.RT_ID + " INTEGER DEFAULT 0,"
                + ImageColumns.POSTER + " TEXT,"
                + ImageColumns.FANART + " TEXT,"
                + ImageColumns.SCREEN + " TEXT,"
                + ImageColumns.BANNER + " TEXT,"
                + MovieColumns.RATING_PERCENTAGE + " INTEGER,"
                + MovieColumns.RATING_VOTES + " INTEGER DEFAULT 0,"
                + MovieColumns.RATING_LOVED + " INTEGER DEFAULT 0,"
                + MovieColumns.RATING_HATED + " INTEGER DEFAULT 0,"
                + MovieColumns.RATING + " INTEGER DEFAULT 0,"
                + MovieColumns.WATCHERS + " INTEGER DEFAULT 0,"
                + MovieColumns.PLAYS + " INTEGER DEFAULT 0,"
                + MovieColumns.SCROBBLES + " INTEGER DEFAULT 0,"
                + MovieColumns.CHECKINS + " INTEGER DEFAULT 0,"
                + MovieColumns.LAST_UPDATED + " INTEGER DEFAULT 0,"
                + MovieColumns.WATCHED + " INTEGER DEFAULT 0,"
                + MovieColumns.IN_WATCHLIST + " INTEGER DEFAULT 0,"
                + MovieColumns.IN_COLLECTION + " INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + Tables.MOVIE_GENRES + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + MovieGenres.MOVIE_ID + " INTEGER " + References.MOVIE_ID + ","
                + MovieGenres.GENRE + " TEXT)");

        db.execSQL("CREATE TABLE " + Tables.MOVIE_TOP_WATCHERS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + MovieTopWatchers.MOVIE_ID + " INTEGER " + References.MOVIE_ID + ","
                + TopWatcherColumns.PLAYS + " INTEGER DEFAULT 0,"
                + TopWatcherColumns.USERNAME + " TEXT NOT NULL,"
                + TopWatcherColumns.PROTECTED + " INTEGER DEFAULT 0,"
                + TopWatcherColumns.FULL_NAME + " TEXT,"
                + TopWatcherColumns.GENDER + " TEXT,"
                + TopWatcherColumns.AGE + " INTEGER,"
                + TopWatcherColumns.LOCATION + "TEXT,"
                + TopWatcherColumns.ABOUT + " TEXT,"
                + TopWatcherColumns.JOINED + " INTEGER DEFAULT 0,"
                + TopWatcherColumns.AVATAR + " TEXT,"
                + TopWatcherColumns.URL + " TEXT NOT NULL)");

        db.execSQL("CREATE TABLE " + Tables.MOVIE_ACTORS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + MovieActors.MOVIE_ID + " INTEGER " + References.MOVIE_ID + ","
                + ActorColumns.NAME + " TEXT NOT NULL,"
                + ActorColumns.CHARACTER + " TEXT,"
                + ActorColumns.HEADSHOT + " TEXT)");

        db.execSQL("CREATE TABLE " + Tables.MOVIE_DIRECTORS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + MovieDirectors.MOVIE_ID + " INTEGER " + References.MOVIE_ID + ","
                + MovieDirectors.NAME + " TEXT NOT NULL,"
                + MovieDirectors.HEADSHOT + " TEXT)");

        db.execSQL("CREATE TABLE " + Tables.MOVIE_WRITERS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + MovieWriters.MOVIE_ID + " INTEGER " + References.MOVIE_ID + ","
                + MovieWriters.NAME + " TEXT NOT NULL,"
                + MovieWriters.JOB + " TEXT,"
                + MovieWriters.HEADSHOT + " TEXT)");

        db.execSQL("CREATE TABLE " + Tables.MOVIE_PRODUCERS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + MovieProducers.MOVIE_ID + " INTEGER " + References.MOVIE_ID + ","
                + MovieProducers.NAME + " TEXT NOT NULL,"
                + MovieProducers.EXECUTIVE + " INTEGER NOT NULL,"
                + MovieProducers.HEADSHOT + " TEXT)");

        db.execSQL("CREATE TABLE " + Tables.USER_ACTIVITY + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + UserActivity.ALL + " INTEGER NOT NULL,"
                + UserActivity.EPISODE_WATCHED + " INTEGER NOT NULL,"
                + UserActivity.EPISODE_SCROBBLE + " INTEGER NOT NULL,"
                + UserActivity.EPISODE_SEEN + " INTEGER NOT NULL,"
                + UserActivity.EPISODE_CHECKIN + " INTEGER NOT NULL,"
                + UserActivity.EPISODE_COLLECTION + " INTEGER NOT NULL,"
                + UserActivity.MOVIE_WATCHED + " INTEGER NOT NULL,"
                + UserActivity.MOVIE_SCROBBLE + " INTEGER NOT NULL,"
                + UserActivity.MOVIE_SEEN + " INTEGER NOT NULL,"
                + UserActivity.MOVIE_CHECKIN + " INTEGER NOT NULL,"
                + UserActivity.MOVIE_COLLECTION + " INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
