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

import android.provider.BaseColumns;
import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.DefaultValue;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;
import net.simonvt.schematic.annotation.References;

import static net.simonvt.schematic.annotation.DataType.Type.INTEGER;
import static net.simonvt.schematic.annotation.DataType.Type.TEXT;

public final class DatabaseContract {

  private DatabaseContract() {
  }

  public interface ShowColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(TEXT) @NotNull String TITLE = "showTitle";
    @DataType(TEXT) @NotNull String TITLE_NO_ARTICLE = "showTitleNoArticle";
    @DataType(INTEGER) String YEAR = "year";
    @DataType(TEXT) String URL = "url";
    @DataType(INTEGER) String FIRST_AIRED = "firstAired";
    @DataType(TEXT) String COUNTRY = "country";
    @DataType(TEXT) String OVERVIEW = "overview";
    @DataType(INTEGER) @DefaultValue("0") String RUNTIME = "runtime";
    @DataType(TEXT) String NETWORK = "network";
    @DataType(TEXT) String AIR_DAY = "airDay";
    @DataType(TEXT) String AIR_TIME = "airTime";
    @DataType(TEXT) String CERTIFICATION = "certification";
    @DataType(TEXT) String IMDB_ID = "imdbId";
    @DataType(TEXT) String TVDB_ID = "tvdbId";
    @DataType(TEXT) String TVRAGE_ID = "tvrageId";
    @DataType(INTEGER) @DefaultValue("0") String LAST_UPDATED = "lastUpdated";
    @DataType(TEXT) String POSTER = "poster";
    @DataType(TEXT) String FANART = "fanart";
    @DataType(TEXT) String BANNER = "banner";
    @DataType(INTEGER) @DefaultValue("0") String RATING_PERCENTAGE = "ratingPercentage";
    @DataType(INTEGER) @DefaultValue("0") String RATING_VOTES = "ratingVotes";
    @DataType(INTEGER) @DefaultValue("0") String RATING_LOVED = "ratingLoved";
    @DataType(INTEGER) @DefaultValue("0") String RATING_HATED = "ratingHated";
    @DataType(INTEGER) @DefaultValue("0") String WATCHERS = "watchers";
    @DataType(INTEGER) @DefaultValue("0") String PLAYS = "plays";
    @DataType(INTEGER) @DefaultValue("0") String SCROBBLES = "scrobbles";
    @DataType(INTEGER) @DefaultValue("0") String CHECKINS = "checkins";
    @DataType(INTEGER) @DefaultValue("0") String RATING = "rating";
    @DataType(INTEGER) @DefaultValue("0") String IN_WATCHLIST = "showInWatchlist";
    @DataType(INTEGER) @DefaultValue("0") String WATCHED_COUNT = "watchedCount";
    @DataType(INTEGER) @DefaultValue("0") String AIRDATE_COUNT = "airdateCount";
    @DataType(TEXT) String STATUS = "status";
    @DataType(INTEGER) @DefaultValue("0") String IN_COLLECTION_COUNT = "inCollectionCount";
    @DataType(INTEGER) @DefaultValue("0") String IN_WATCHLIST_COUNT = "inWatchlistCount";
    @DataType(INTEGER) @DefaultValue("-1") String TRENDING_INDEX = "trendingIndex";
    @DataType(INTEGER) @DefaultValue("-1") String RECOMMENDATION_INDEX = "recommendationIndex";
    @DataType(INTEGER) @DefaultValue("0") String HIDDEN = "hidden";
    @DataType(INTEGER) @DefaultValue("0") String FULL_SYNC_REQUESTED = "fullSyncRequested";

    // Don't create a columns in database
    String AIRED_COUNT = "airedCount";
    String UNAIRED_COUNT = "unairedCount";
    String WATCHING = "watchingShow";
    String EPISODE_COUNT = "episodeCount";
    String AIRED_AFTER_WATCHED = "airedAfterWatched";
  }

  public interface ShowTopWatcherColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(INTEGER) @References(table = DatabaseSchematic.TABLE_SHOWS, column = ShowColumns.ID)
    String SHOW_ID = "showId";
    @DataType(INTEGER) @DefaultValue("0") String PLAYS = "plays";
    @DataType(TEXT) String USERNAME = "username";
    @DataType(INTEGER) @DefaultValue("0") String PROTECTED = "protected";
    @DataType(TEXT) String FULL_NAME = "fullName";
    @DataType(TEXT) String GENDER = "gender";
    @DataType(INTEGER) String AGE = "age";
    @DataType(TEXT) String LOCATION = "location";
    @DataType(TEXT) String ABOUT = "about";
    @DataType(INTEGER) @DefaultValue("0") String JOINED = "joined";
    @DataType(TEXT) String AVATAR = "avatar";
    @DataType(TEXT) String URL = "url";
  }

  public interface TopEpisodeColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(INTEGER) @References(table = DatabaseSchematic.TABLE_SHOWS, column = ShowColumns.ID)
    String SHOW_ID = "showId";
    @DataType(INTEGER)
    @References(table = DatabaseSchematic.TABLE_SEASONS, column = SeasonColumns.ID) String
        SEASON_ID = "seasonId";
    @DataType(INTEGER) @NotNull String NUMBER = "number";
    @DataType(INTEGER) @DefaultValue("0") String PLAYS = "plays";
    @DataType(TEXT) @NotNull String TITLE = "title";
    @DataType(TEXT) String URL = "url";
    @DataType(INTEGER) @DefaultValue("0") String FIRST_AIRED = "firstAired";
  }

  public interface ShowActorColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(INTEGER) @References(table = DatabaseSchematic.TABLE_SHOWS, column = ShowColumns.ID)
    String SHOW_ID = "showId";
    @DataType(TEXT) String NAME = "name";
    @DataType(TEXT) String CHARACTER = "character";
    @DataType(TEXT) String HEADSHOT = "headshot";
  }

  public interface ShowGenreColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(INTEGER) @References(table = DatabaseSchematic.TABLE_SHOWS, column = ShowColumns.ID)
    String SHOW_ID = "showId";
    @DataType(TEXT) String GENRE = "genre";
  }

  public interface SeasonColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(INTEGER) @References(table = DatabaseSchematic.TABLE_SHOWS, column = ShowColumns.ID)
    String SHOW_ID = "showId";
    @DataType(INTEGER) @NotNull String SEASON = "season";
    @DataType(INTEGER) @DefaultValue("0") String EPISODES = "episodes";
    @DataType(TEXT) String URL = "url";
    @DataType(INTEGER) @DefaultValue("0") String WATCHED_COUNT = "watchedCount";
    @DataType(INTEGER) @DefaultValue("0") String AIRDATE_COUNT = "airdateCount";
    @DataType(INTEGER) @DefaultValue("0") String IN_COLLECTION_COUNT = "inCollectionCount";
    @DataType(INTEGER) @DefaultValue("0") String IN_WATCHLIST_COUNT = "inWatchlistCount";
    @DataType(TEXT) String POSTER = "poster";

    String AIRED_COUNT = "airedCount";
    String UNAIRED_COUNT = "unairedCount";
  }

  public interface EpisodeColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(INTEGER) @References(table = DatabaseSchematic.TABLE_SHOWS, column = ShowColumns.ID)
    String SHOW_ID = "showId";
    @DataType(INTEGER)
    @References(table = DatabaseSchematic.TABLE_SEASONS, column = SeasonColumns.ID) String
        SEASON_ID = "seasonId";
    @DataType(INTEGER) String SEASON = "season";
    @DataType(INTEGER) String EPISODE = "episode";
    @DataType(TEXT) @NotNull String TITLE = "episodeTitle";
    @DataType(TEXT) String OVERVIEW = "overview";
    @DataType(TEXT) String URL = "url";
    @DataType(INTEGER) String TVDB_ID = "tvdbId";
    @DataType(TEXT) String IMDB_ID = "imdbId";
    @DataType(INTEGER) String FIRST_AIRED = "episodeFirstAired";
    @DataType(TEXT) String SCREEN = "screen";
    @DataType(INTEGER) @DefaultValue("0") String RATING_PERCENTAGE = "ratingPercentage";
    @DataType(INTEGER) @DefaultValue("0") String RATING_VOTES = "ratingVotes";
    @DataType(INTEGER) @DefaultValue("0") String RATING_LOVED = "ratingLoved";
    @DataType(INTEGER) @DefaultValue("0") String RATING_HATED = "ratingHated";
    @DataType(INTEGER) @DefaultValue("0") String WATCHED = "watched";
    @DataType(INTEGER) @DefaultValue("0") String PLAYS = "plays";
    @DataType(TEXT) String RATING = "rating";
    @DataType(INTEGER) @DefaultValue("0") String IN_WATCHLIST = "inWatchlist";
    @DataType(INTEGER) @DefaultValue("0") String IN_COLLECTION = "inCollection";
    @DataType(INTEGER) @DefaultValue("0") String WATCHING = "watching";
    @DataType(INTEGER) @DefaultValue("0") String CHECKED_IN = "checkedIn";
  }

  public interface MovieColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(TEXT) @NotNull String TITLE = "title";
    @DataType(INTEGER) String YEAR = "year";
    @DataType(INTEGER) String RELEASED = "released";
    @DataType(TEXT) String URL = "url";
    @DataType(TEXT) String TRAILER = "trailer";
    @DataType(INTEGER) String RUNTIME = "runtime";
    @DataType(TEXT) String TAGLINE = "tagline";
    @DataType(TEXT) String OVERVIEW = "overview";
    @DataType(TEXT) String CERTIFICATION = "certification";
    @DataType(TEXT) String IMDB_ID = "imdbId";
    @DataType(INTEGER) String TMDB_ID = "tmdbId";
    @DataType(INTEGER) @DefaultValue("0") String RT_ID = "rtId";
    @DataType(TEXT) String POSTER = "poster";
    @DataType(TEXT) String FANART = "fanart";
    @DataType(INTEGER) @DefaultValue("0") String RATING_PERCENTAGE = "ratingPercentage";
    @DataType(INTEGER) @DefaultValue("0") String RATING_VOTES = "ratingVotes";
    @DataType(INTEGER) @DefaultValue("0") String RATING_LOVED = "ratingLoved";
    @DataType(INTEGER) @DefaultValue("0") String RATING_HATED = "ratingHated";
    @DataType(INTEGER) @DefaultValue("0") String RATING = "rating";
    @DataType(INTEGER) @DefaultValue("0") String WATCHERS = "watchers";
    @DataType(INTEGER) @DefaultValue("0") String PLAYS = "plays";
    @DataType(INTEGER) @DefaultValue("0") String SCROBBLES = "scrobbles";
    @DataType(INTEGER) @DefaultValue("0") String CHECKINS = "checkins";
    @DataType(INTEGER) @DefaultValue("0") String WATCHED = "watched";
    @DataType(INTEGER) @DefaultValue("0") String IN_COLLECTION = "inCollection";
    @DataType(INTEGER) @DefaultValue("0") String IN_WATCHLIST = "inWatchlist";
    @DataType(INTEGER) @DefaultValue("0") String LAST_UPDATED = "lastUpdated";
    @DataType(INTEGER) @DefaultValue("0") String TRENDING_INDEX = "trendingIndex";
    @DataType(INTEGER) @DefaultValue("0") String RECOMMENDATION_INDEX = "recommendationIndex";
    @DataType(INTEGER) @DefaultValue("0") String WATCHING = "watching";
    @DataType(INTEGER) @DefaultValue("0") String CHECKED_IN = "checkedIn";
  }

  public interface MovieGenreColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(INTEGER) @References(table = DatabaseSchematic.TABLE_MOVIES, column = MovieColumns.ID)
    String MOVIE_ID = "movieId";
    @DataType(TEXT) String GENRE = "genre";
  }

  public interface MovieTopWatcherColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(INTEGER) @References(table = DatabaseSchematic.TABLE_MOVIES, column = MovieColumns.ID)
    String MOVIE_ID = "movieId";
    @DataType(INTEGER) @DefaultValue("0") String PLAYS = "plays";
    @DataType(TEXT) @NotNull String USERNAME = "username";
    @DataType(INTEGER) @DefaultValue("0") String PROTECTED = "protected";
    @DataType(TEXT) String FULL_NAME = "fullName";
    @DataType(TEXT) String GENDER = "gender";
    @DataType(INTEGER) String AGE = "age";
    @DataType(TEXT) String LOCATION = "location";
    @DataType(TEXT) String ABOUT = "about";
    @DataType(INTEGER) @DefaultValue("0") String JOINED = "joined";
    @DataType(TEXT) String AVATAR = "avatar";
    @DataType(TEXT) @NotNull String URL = "url";
  }

  public interface MovieActorColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(INTEGER) @References(table = DatabaseSchematic.TABLE_MOVIES, column = MovieColumns.ID)
    String MOVIE_ID = "movieId";
    @DataType(TEXT) @NotNull String NAME = "name";
    @DataType(TEXT) String CHARACTER = "character";
    @DataType(TEXT) String HEADSHOT = "headshot";
  }

  public interface MovieDirectoryColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(INTEGER) @References(table = DatabaseSchematic.TABLE_MOVIES, column = MovieColumns.ID)
    String MOVIE_ID = "movieId";
    @DataType(TEXT) @NotNull String NAME = "name";
    @DataType(TEXT) String HEADSHOT = "headshot";
  }

  public interface MovieWriterColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(INTEGER) @References(table = DatabaseSchematic.TABLE_MOVIES, column = MovieColumns.ID)
    String MOVIE_ID = "movieId";
    @DataType(TEXT) @NotNull String NAME = "name";
    @DataType(TEXT) String HEADSHOT = "headshot";
    @DataType(TEXT) String JOB = "job";
  }

  public interface MovieProducerColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(INTEGER) @References(table = DatabaseSchematic.TABLE_MOVIES, column = MovieColumns.ID)
    String MOVIE_ID = "movieId";
    @DataType(TEXT) @NotNull String NAME = "name";
    @DataType(INTEGER) @NotNull String EXECUTIVE = "executive";
    @DataType(TEXT) String HEADSHOT = "headshot";
  }

  public interface ShowSearchSuggestionsColumns {
    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(TEXT) @NotNull String QUERY = "query";
    @DataType(INTEGER) @DefaultValue("0") String COUNT = "queryCount";
  }

  public interface MovieSearchSuggestionsColumns {
    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(TEXT) @NotNull String QUERY = "query";
    @DataType(INTEGER) @DefaultValue("0") String COUNT = "queryCount";
  }
}
