package net.simonvt.trakt.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public final class TraktContract {

    private static final String TAG = "ShowContract";

    static final Uri BASE_CONTENT_URI = Uri.parse("content://" + TraktProvider.AUTHORITY);

    private TraktContract() {
    }

    interface ShowColumns {

        String TITLE = "showTitle";
        String YEAR = "year";
        String URL = "url";
        String FIRST_AIRED = "firstAired";
        String COUNTRY = "country";
        String OVERVIEW = "overview";
        String RUNTIME = "runtime";
        String NETWORK = "network";
        String AIR_DAY = "airDay";
        String AIR_TIME = "airTime";
        String CERTIFICATION = "certification";
        String IMDB_ID = "imdbId";
        String TVDB_ID = "tvdbId";
        String TVRAGE_ID = "tvrageId";
        String LAST_UPDATED = "lastUpdated";
        String RATING_PERCENTAGE = "ratingPercentage";
        String RATING_VOTES = "ratingVotes";
        String RATING_LOVED = "ratingLoved";
        String RATING_HATED = "ratingHated";
        String WATCHERS = "watchers";
        String PLAYS = "plays";
        String SCROBBLES = "scrobbles";
        String CHECKINS = "checkins";
        String RATING = "rating";
        String IN_WATCHLIST = "showInWatchlist";
        String WATCHED_COUNT = "watchedCount";
        String UNAIRED_COUNT = "unairedCount";
        String AIRDATE_COUNT = "airdateCount";
        String AIRED_COUNT = "airedCount";
        String STATUS = "status";
        String IN_COLLECTION_COUNT = "inCollectionCount";
        String IN_WATCHLIST_COUNT = "inWatchlistCount";
    }

    interface TopWatcherColumns {

        String PLAYS = "plays";
        String USERNAME = "username";
        String PROTECTED = "protected";
        String FULL_NAME = "fullName";
        String GENDER = "gender";
        String AGE = "age";
        String LOCATION = "location";
        String ABOUT = "about";
        String JOINED = "joined";
        String AVATAR = "avatar";
        String URL = "url";
    }

    interface TopEpisodeColumns {

        String SHOW_ID = "showId";
        String SEASON_ID = "seasonId";
        String NUMBER = "number";
        String PLAYS = "plays";
        String TITLE = "title";
        String URL = "url";
        String FIRST_AIRED = "firstAired";
    }

    interface ActorColumns {

        String NAME = "name";
        String CHARACTER = "character";
        String HEADSHOT = "headshot";
    }

    interface SeasonColumns {

        String SHOW_ID = "showId";
        String SEASON = "season";
        String EPISODES = "episodes";
        String URL = "url";
        String WATCHED_COUNT = "watchedCount";
        String UNAIRED_COUNT = "unairedCount";
        String AIRDATE_COUNT = "airdateCount";
        String AIRED_COUNT = "airedCount";
        String IN_COLLECTION_COUNT = "inCollectionCount";
        String IN_WATCHLIST_COUNT = "inWatchlistCount";
    }

    interface EpisodeColumns {

        String SHOW_ID = "showId";
        String SEASON_ID = "seasonId";
        String SEASON = "season";
        String EPISODE = "episode";
        String TITLE = "episodeTitle";
        String OVERVIEW = "overview";
        String URL = "url";
        String FIRST_AIRED = "episodeFirstAired";
        String RATING_PERCENTAGE = "ratingPercentage";
        String RATING_VOTES = "ratingVotes";
        String RATING_LOVED = "ratingLoved";
        String RATING_HATED = "ratingHated";
        String WATCHED = "watched";
        String PLAYS = "plays";
        String RATING = "rating";
        String IN_WATCHLIST = "inWatchlist";
        String IN_COLLECTION = "inCollection";
    }

    interface MovieColumns {

        String TITLE = "title";
        String YEAR = "year";
        String RELEASED = "released";
        String URL = "url";
        String TRAILER = "trailer";
        String RUNTIME = "runtime";
        String TAGLINE = "tagline";
        String OVERVIEW = "overview";
        String CERTIFICATION = "certification";
        String IMDB_ID = "imdbId";
        String TMDB_ID = "tmdbId";
        String RT_ID = "rtId";
        String RATING_PERCENTAGE = "ratingPercentage";
        String RATING_VOTES = "ratingVotes";
        String RATING_LOVED = "ratingLoved";
        String RATING_HATED = "ratingHated";
        String RATING = "rating";
        String WATCHERS = "watchers";
        String PLAYS = "plays";
        String SCROBBLES = "scrobbles";
        String CHECKINS = "checkins";
        String WATCHED = "watched";
        String IN_COLLECTION = "inCollection";
        String IN_WATCHLIST = "inWatchlist";
        String LAST_UPDATED = "lastUpdated";
    }

    interface ImageColumns {

        String POSTER = "poster";
        String FANART = "fanart";
        String HEADSHOT = "headshot";
        String SCREEN = "screen";
        String BANNER = "banner";
    }

    interface UserActivityColumns {

        String ALL = "allActivity";

        String EPISODE_WATCHED = "episodeWatched";
        String EPISODE_SCROBBLE = "episodeScrobble";
        String EPISODE_SEEN = "episodeSeen";
        String EPISODE_CHECKIN = "episodeCheckin";
        String EPISODE_COLLECTION = "episodeCollection";

        String MOVIE_WATCHED = "movieWatched";
        String MOVIE_SCROBBLE = "movieScrobble";
        String MOVIE_SEEN = "movieSeen";
        String MOVIE_CHECKIN = "movieCheckin";
        String MOVIE_COLLECTION = "movieCollection";
    }

    public static final String PATH_WATCHLIST = "watchlist";

    public static final String PATH_SHOWS = "shows";
    public static final String PATH_WITHNEXT = "withNext";
    public static final String PATH_IGNOREWATCHED = "ignoreWatched";
    public static final String PATH_SEASONS = "seasons";
    public static final String PATH_EPISODES = "episodes";
    public static final String PATH_FROMSHOW = "fromshow";
    public static final String PATH_FROMSEASON = "fromSeason";
    public static final String PATH_FROMEPISODE = "fromEpisode";
    public static final String PATH_TOPEPISODES = "topEpisodes";
    public static final String PATH_WITHID = "withId";
    public static final String PATH_COLLECTION = "inCollection";
    public static final String PATH_WATCHED = "watched";

    public static final String PATH_MOVIES = "movies";
    public static final String PATH_FROMMOVIE = "fromMovie";

    public static final String PATH_TOPWATCHERS = "topWatchers";
    public static final String PATH_GENRES = "genres";
    public static final String PATH_ACTORS = "actors";
    public static final String PATH_DIRECTORS = "directors";
    public static final String PATH_PRODUCERS = "producers";
    public static final String PATH_WRITERS = "writers";

    private static final String PATH_ACTIVITY = "userActivity";

    public static class Shows implements ShowColumns, ImageColumns, BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_SHOWS).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.simonvt.trakt.show";

        public static final String DEFAULT_SORT = TraktDatabase.Tables.SHOWS + "." + ShowColumns.TITLE + " ASC";

        public static final Uri SHOWS_WITHNEXT = CONTENT_URI.buildUpon().appendPath(PATH_WITHNEXT).build();

        public static final Uri SHOWS_WATCHED = CONTENT_URI.buildUpon().appendEncodedPath(PATH_WATCHED).build();

        public static final Uri SHOWS_COLLECTION = CONTENT_URI.buildUpon().appendEncodedPath(PATH_COLLECTION).build();

        public static final Uri SHOWS_WATCHLIST = CONTENT_URI.buildUpon().appendEncodedPath(PATH_WATCHLIST).build();

        public static final Uri SHOWS_WITHNEXT_IGNOREWATCHED =
                SHOWS_WITHNEXT.buildUpon().appendPath(PATH_IGNOREWATCHED).build();

        public static Uri buildShowUri(long showId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_WITHID).appendPath(String.valueOf(showId)).build();
        }

        public static String getShowId(Uri uri) {
            return uri.getPathSegments().get(2);
        }
    }

    public static class ShowTopWatchers implements TopWatcherColumns, BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_TOPWATCHERS).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.simonvt.trakt.showTopWatcher";

        public static final String SHOW_ID = "showId";

        public static Uri buildFromShowId(long showId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_FROMSHOW).appendPath(String.valueOf(showId)).build();
        }

        public static String getShowId(Uri uri) {
            return uri.getPathSegments().get(2);
        }
    }

    public static class TopEpisodes implements TopEpisodeColumns, BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_TOPEPISODES).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.simonvt.trakt.showTopEpisode";

        public static Uri buildFromShowId(long showId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_FROMSHOW).appendPath(String.valueOf(showId)).build();
        }

        public static String getShowId(Uri uri) {
            return uri.getPathSegments().get(2);
        }
    }

    public static class ShowActor implements ActorColumns, BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_ACTORS).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.simonvt.trakt.showActor";

        public static final String SHOW_ID = "showId";

        public static Uri buildFromShowId(long showId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_FROMSHOW).appendPath(String.valueOf(showId)).build();
        }

        public static String getShowId(Uri uri) {
            return uri.getPathSegments().get(2);
        }
    }

    public static class ShowGenres implements BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_GENRES).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.simonvt.trakt.showGenre";

        public static final String SHOW_ID = "showId";
        public static final String GENRE = "genre";

        public static final String DEFAULT_SORT = GENRE + " ASC";

        public static Uri buildFromShowId(long showId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_FROMSHOW).appendPath(String.valueOf(showId)).build();
        }

        public static String getShowId(Uri uri) {
            return uri.getPathSegments().get(2);
        }
    }

    public static class Seasons implements SeasonColumns, ImageColumns, BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_SEASONS).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.simonvt.trakt.season";

        public static final String DEFAULT_SORT = TraktDatabase.Tables.SEASONS + "." + SeasonColumns.SEASON + " DESC";

        public static Uri buildFromId(long seasonId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_WITHID).appendPath(String.valueOf(seasonId)).build();
        }

        public static Uri buildFromShowId(long showId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_FROMSHOW).appendPath(String.valueOf(showId)).build();
        }

        public static String getSeasonId(Uri uri) {
            return uri.getPathSegments().get(2);
        }

        public static String getShowId(Uri uri) {
            return uri.getPathSegments().get(2);
        }
    }

    public static class Episodes implements EpisodeColumns, ImageColumns, BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_EPISODES).build();

        public static final Uri WATCHLIST_URI = CONTENT_URI.buildUpon().appendPath(PATH_WATCHLIST).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.simonvt.trakt.episode";

        public static final String DEFAULT_SORT = EpisodeColumns.EPISODE + " ASC";

        public static Uri buildFromId(long episodeId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_WITHID).appendPath(String.valueOf(episodeId)).build();
        }

        public static Uri buildFromShowId(long showId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_FROMSHOW).appendPath(String.valueOf(showId)).build();
        }

        public static Uri buildFromSeasonId(long seasonId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_FROMSEASON).appendPath(String.valueOf(seasonId)).build();
        }

        public static String getEpisodeId(Uri uri) {
            return uri.getPathSegments().get(2);
        }

        public static String getShowId(Uri uri) {
            return uri.getPathSegments().get(2);
        }

        public static String getSeasonId(Uri uri) {
            return uri.getPathSegments().get(2);
        }
    }

    public static class Movies implements MovieColumns, ImageColumns, BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_MOVIES).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.simonvt.trakt.movie";

        public static Uri buildMovieUri(long movieId) {
            return CONTENT_URI.buildUpon().appendPath(String.valueOf(movieId)).build();
        }

        public static String getMovieId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static class MovieGenres implements BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_GENRES).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.simonvt.trakt.movieGenre";

        public static final String MOVIE_ID = "movieId";
        public static final String GENRE = "genre";

        public static Uri buildFromMovieId(long movieId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_FROMMOVIE).appendPath(String.valueOf(movieId)).build();
        }

        public static String getMovieId(Uri uri) {
            return uri.getPathSegments().get(2);
        }
    }

    public static class MovieTopWatchers implements TopWatcherColumns, BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_TOPWATCHERS).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.simonvt.trakt.movieTopWatcher";

        public static final String MOVIE_ID = "movieId";

        public static Uri buildFromMovieId(long movieId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_FROMMOVIE).appendPath(String.valueOf(movieId)).build();
        }

        public static String getMovieId(Uri uri) {
            return uri.getPathSegments().get(2);
        }
    }

    public static class MovieActors implements ActorColumns, BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_ACTORS).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.simonvt.trakt.movieActor";

        public static final String MOVIE_ID = "movieId";

        public static Uri buildFromMovieId(long movieId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_FROMMOVIE).appendPath(String.valueOf(movieId)).build();
        }

        public static String getMovieId(Uri uri) {
            return uri.getPathSegments().get(2);
        }
    }

    public static class MovieDirectors implements BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_DIRECTORS).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.simonvt.trakt.movieDirector";

        public static final String MOVIE_ID = "movieId";
        public static final String NAME = "name";
        public static final String HEADSHOT = "headshot";

        public static Uri buildFromMovieId(long movieId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_FROMMOVIE).appendPath(String.valueOf(movieId)).build();
        }

        public static String getMovieId(Uri uri) {
            return uri.getPathSegments().get(2);
        }
    }

    public static class MovieWriters implements BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_WRITERS).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.simonvt.trakt.movieWriter";

        public static final String MOVIE_ID = "movieId";
        public static final String NAME = "name";
        public static final String HEADSHOT = "headshot";
        public static final String JOB = "job";

        public static Uri buildFromMovieId(long movieId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_FROMMOVIE).appendPath(String.valueOf(movieId)).build();
        }

        public static String getMovieId(Uri uri) {
            return uri.getPathSegments().get(2);
        }
    }

    public static class MovieProducers implements BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_PRODUCERS).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.simonvt.trakt.movieProducer";

        public static final String MOVIE_ID = "movieId";
        public static final String NAME = "name";
        public static final String EXECUTIVE = "executive";
        public static final String HEADSHOT = "headshot";

        public static Uri buildFromMovieId(long movieId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_FROMMOVIE).appendPath(String.valueOf(movieId)).build();
        }

        public static String getMovieId(Uri uri) {
            return uri.getPathSegments().get(2);
        }
    }

    public static class UserActivity implements UserActivityColumns, BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_ACTIVITY).build();
    }
}
