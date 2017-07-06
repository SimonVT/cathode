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
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.ui.adapter.LastModifiedColumn;
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

  public interface LastModifiedColumns {

    @DataType(INTEGER) @DefaultValue("0") String LAST_MODIFIED = LastModifiedColumn.LAST_MODIFIED;
  }

  public interface ShowColumns extends LastModifiedColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(TEXT) String TITLE = "showTitle";
    @DataType(TEXT) String TITLE_NO_ARTICLE = "showTitleNoArticle";
    @DataType(INTEGER) String YEAR = "year";
    @DataType(INTEGER) String FIRST_AIRED = "firstAired";
    @DataType(TEXT) String COUNTRY = "country";
    @DataType(TEXT) String OVERVIEW = "overview";
    @DataType(INTEGER) @DefaultValue("0") String RUNTIME = "runtime";
    @DataType(TEXT) String NETWORK = "network";

    @DataType(TEXT) String AIR_DAY = "airDay";
    @DataType(TEXT) String AIR_TIME = "airTime";
    @DataType(TEXT) String AIR_TIMEZONE = "airTimezone";

    @DataType(TEXT) String CERTIFICATION = "certification";
    @DataType(TEXT) String SLUG = "slug";
    @DataType(INTEGER) String TRAKT_ID = "traktId";
    @DataType(TEXT) String IMDB_ID = "imdbId";
    @DataType(INTEGER) String TVDB_ID = "tvdbId";
    @DataType(INTEGER) String TMDB_ID = "tmdbId";
    @DataType(INTEGER) String TVRAGE_ID = "tvrageId";
    @DataType(INTEGER) @DefaultValue("0") String LAST_UPDATED = "lastUpdated";
    @DataType(TEXT) String TRAILER = "trailer";
    @DataType(TEXT) String HOMEPAGE = "homepage";
    @DataType(TEXT) String STATUS = "status";

    @DataType(INTEGER) String IMAGES_LAST_SYNC = "imagesLastSync";
    @DataType(TEXT) String BACKDROP = "backdrop";
    @DataType(TEXT) String POSTER = "poster";

    @DataType(INTEGER) @DefaultValue("0") String USER_RATING = "userRating";
    @DataType(INTEGER) @DefaultValue("0") String RATED_AT = "ratedAt";

    @DataType(INTEGER) @DefaultValue("0") String RATING = "rating";
    @DataType(INTEGER) @DefaultValue("0") String VOTES = "votes";

    @DataType(INTEGER) @DefaultValue("0") String WATCHERS = "watchers";
    @DataType(INTEGER) @DefaultValue("0") String PLAYS = "plays";
    @DataType(INTEGER) @DefaultValue("0") String SCROBBLES = "scrobbles";
    @DataType(INTEGER) @DefaultValue("0") String CHECKINS = "checkins";

    @DataType(INTEGER) @DefaultValue("0") String IN_WATCHLIST = "showInWatchlist";
    @DataType(INTEGER) @DefaultValue("0") String LISTED_AT = "watchlistListedAt";

    @DataType(INTEGER) @DefaultValue("0") String LAST_WATCHED_AT = "lastWatchedAt";
    @DataType(INTEGER) @DefaultValue("0") String LAST_COLLECTED_AT = "lastCollectedAt";

    @DataType(INTEGER) @DefaultValue("0") String HIDDEN_CALENDAR = "hiddenCalendar";
    @DataType(INTEGER) @DefaultValue("0") String HIDDEN_WATCHED = "hiddenWatched";
    @DataType(INTEGER) @DefaultValue("0") String HIDDEN_COLLECTED = "hiddenCollected";
    @DataType(INTEGER) @DefaultValue("0") String HIDDEN_RECOMMENDATIONS = "hiddenRecommendations";

    @DataType(INTEGER) @DefaultValue("0") String WATCHED_COUNT = "watchedCount";
    @DataType(INTEGER) @DefaultValue("0") String AIRDATE_COUNT = "airdateCount";

    @DataType(INTEGER) @DefaultValue("0") String IN_COLLECTION_COUNT = "inCollectionCount";
    @DataType(INTEGER) @DefaultValue("0") String IN_WATCHLIST_COUNT = "inWatchlistCount";
    @DataType(INTEGER) @DefaultValue("-1") String TRENDING_INDEX = "trendingIndex";
    @DataType(INTEGER) @DefaultValue("-1") String RECOMMENDATION_INDEX = "recommendationIndex";
    @DataType(INTEGER) @DefaultValue("-1") String ANTICIPATED_INDEX = "anticipatedIndex";
    @DataType(INTEGER) @DefaultValue("0") String FULL_SYNC_REQUESTED = "fullSyncRequested";

    @DataType(INTEGER) @DefaultValue("0") String NEEDS_SYNC = "needsSync";
    @DataType(INTEGER) @DefaultValue("0") String LAST_SYNC = "lastSync";
    @DataType(INTEGER) @DefaultValue("0") String LAST_COMMENT_SYNC = "lastCommentSync";
    @DataType(INTEGER) @DefaultValue("0") String LAST_CREDITS_SYNC = "lastCreditsSync";
    @DataType(INTEGER) @DefaultValue("0") String LAST_RELATED_SYNC = "lastRelatedSync";

    @DataType(INTEGER) @DefaultValue("0") String WATCHING = "watchingShow";

    // Don't create a columns in database
    String AIRED_COUNT = "airedCount";
    String UNAIRED_COUNT = "unairedCount";
    String EPISODE_COUNT = "episodeCount";
    String WATCHING_EPISODE_ID = "watchingEpisodeId";
  }

  public interface ShowGenreColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(INTEGER) @References(table = Tables.SHOWS, column = ShowColumns.ID)
    String SHOW_ID = "showId";
    @DataType(TEXT) String GENRE = "genre";
  }

  public interface ShowCastColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(INTEGER) @References(table = Tables.SHOWS, column = ShowColumns.ID)
    String SHOW_ID = "showId";

    @DataType(TEXT) String CHARACTER = "character";
    @DataType(TEXT) @References(table = Tables.PEOPLE, column = PersonColumns.ID)
    String PERSON_ID = "personId";
  }

  public interface ShowCrewColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(INTEGER) @References(table = Tables.SHOWS, column = ShowColumns.ID)
    String SHOW_ID = "showId";

    @DataType(TEXT) String CATEGORY = "category";
    @DataType(TEXT) String JOB = "job";
    @DataType(TEXT) @References(table = Tables.PEOPLE, column = PersonColumns.ID)
    String PERSON_ID = "personId";
  }

  public interface SeasonColumns extends LastModifiedColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(INTEGER) @References(table = Tables.SHOWS, column = ShowColumns.ID)
    String SHOW_ID = "showId";
    @DataType(INTEGER) @NotNull String SEASON = "season";

    @DataType(INTEGER) String TVDB_ID = "tvdbId";
    @DataType(INTEGER) String TMDB_ID = "tmdbId";
    @DataType(INTEGER) String TVRAGE_ID = "tvrageId";

    @DataType(INTEGER) @DefaultValue("0") String USER_RATING = "userRating";
    @DataType(INTEGER) @DefaultValue("0") String RATED_AT = "ratedAt";

    @DataType(INTEGER) @DefaultValue("0") String RATING = "rating";
    @DataType(INTEGER) @DefaultValue("0") String VOTES = "votes";

    @DataType(INTEGER) @DefaultValue("0") String HIDDEN_WATCHED = "hiddenWatched";
    @DataType(INTEGER) @DefaultValue("0") String HIDDEN_COLLECTED = "hiddenCollected";

    @DataType(INTEGER) @DefaultValue("0") String WATCHED_COUNT = "watchedCount";
    @DataType(INTEGER) @DefaultValue("0") String AIRDATE_COUNT = "airdateCount";
    @DataType(INTEGER) @DefaultValue("0") String IN_COLLECTION_COUNT = "inCollectionCount";
    @DataType(INTEGER) @DefaultValue("0") String IN_WATCHLIST_COUNT = "inWatchlistCount";

    @DataType(INTEGER) @DefaultValue("0") String NEEDS_SYNC = "needsSync";

    String AIRED_COUNT = "airedCount";
    String UNAIRED_COUNT = "unairedCount";
    String WATCHED_AIRED_COUNT = "watchedAiredCount";
    String COLLECTED_AIRED_COUNT = "collectedAiredCount";
    String EPISODE_COUNT = "episodeCount";
  }

  public interface EpisodeColumns extends LastModifiedColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(INTEGER) @References(table = Tables.SHOWS, column = ShowColumns.ID) String SHOW_ID =
        "showId";
    @DataType(INTEGER) @References(table = Tables.SEASONS, column = SeasonColumns.ID) String
        SEASON_ID = "seasonId";

    @DataType(INTEGER) String SEASON = "season";
    @DataType(INTEGER) String EPISODE = "episode";
    @DataType(INTEGER) String NUMBER_ABS = "numberAbs";

    @DataType(TEXT) String TITLE = "episodeTitle";
    @DataType(TEXT) String OVERVIEW = "overview";

    @DataType(INTEGER) String TRAKT_ID = "traktId";
    @DataType(TEXT) String IMDB_ID = "imdbId";
    @DataType(INTEGER) String TVDB_ID = "tvdbId";
    @DataType(INTEGER) String TMDB_ID = "tmdbId";
    @DataType(INTEGER) String TVRAGE_ID = "tvrageId";

    @DataType(INTEGER) String IMAGES_LAST_SYNC = "imagesLastSync";
    @DataType(TEXT) String SCREENSHOT = "screenshot";

    @DataType(INTEGER) String FIRST_AIRED = "episodeFirstAired";
    @DataType(INTEGER) String UPDATED_AT = "updatedAt";

    @DataType(INTEGER) @DefaultValue("0") String USER_RATING = "userRating";
    @DataType(INTEGER) @DefaultValue("0") String RATED_AT = "ratedAt";

    @DataType(INTEGER) @DefaultValue("0") String RATING = "rating";
    @DataType(INTEGER) @DefaultValue("0") String VOTES = "votes";

    @DataType(INTEGER) @DefaultValue("0") String PLAYS = "plays";

    @DataType(INTEGER) @DefaultValue("0") String WATCHED = "watched";
    @DataType(INTEGER) @DefaultValue("0") String LAST_WATCHED_AT = "lastWatchedAt";
    @DataType(INTEGER) @DefaultValue("0") String IN_WATCHLIST = "inWatchlist";
    @DataType(INTEGER) @DefaultValue("0") String IN_COLLECTION = "inCollection";
    @DataType(INTEGER) @DefaultValue("0") String COLLECTED_AT = "collectedAt";
    @DataType(INTEGER) @DefaultValue("0") String LISTED_AT = "listedAt";

    @DataType(INTEGER) @DefaultValue("0") String WATCHING = "watching";
    @DataType(INTEGER) @DefaultValue("0") String CHECKED_IN = "checkedIn";
    @DataType(INTEGER) @DefaultValue("0") String STARTED_AT = "startedAt";
    @DataType(INTEGER) @DefaultValue("0") String EXPIRES_AT = "expiresAt";

    @DataType(INTEGER) @DefaultValue("0") String LAST_COMMENT_SYNC = "lastCommentSync";

    @DataType(INTEGER) @DefaultValue("0") String NOTIFICATION_DISMISSED = "notificationDismissed";
    @DataType(INTEGER) @DefaultValue("0") String NEEDS_SYNC = "needsSync";

    String SHOW_TITLE = "episodeShowTitle";
  }

  public interface MovieColumns extends LastModifiedColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(TEXT) String TITLE = "title";
    @DataType(TEXT) String TITLE_NO_ARTICLE = "titleNoArticle";
    @DataType(INTEGER) String YEAR = "year";
    @DataType(INTEGER) String RELEASED = "released";
    @DataType(TEXT) String TRAILER = "trailer";
    @DataType(INTEGER) String RUNTIME = "runtime";
    @DataType(TEXT) String TAGLINE = "tagline";
    @DataType(TEXT) String OVERVIEW = "overview";
    @DataType(TEXT) String CERTIFICATION = "certification";
    @DataType(TEXT) String LANGUAGE = "language";
    @DataType(TEXT) String HOMEPAGE = "homepage";

    @DataType(INTEGER) String TRAKT_ID = "traktId";
    @DataType(TEXT) String SLUG = "slug";
    @DataType(TEXT) String IMDB_ID = "imdbId";
    @DataType(INTEGER) String TMDB_ID = "tmdbId";

    @DataType(INTEGER) String IMAGES_LAST_SYNC = "imagesLastSync";
    @DataType(TEXT) String BACKDROP = "backdrop";
    @DataType(TEXT) String POSTER = "poster";

    @DataType(INTEGER) @DefaultValue("0") String USER_RATING = "userRating";
    @DataType(INTEGER) @DefaultValue("0") String RATED_AT = "ratedAt";

    @DataType(INTEGER) @DefaultValue("0") String RATING = "rating";
    @DataType(INTEGER) @DefaultValue("0") String VOTES = "votes";

    @DataType(INTEGER) @DefaultValue("0") String WATCHED = "watched";
    @DataType(INTEGER) @DefaultValue("0") String IN_COLLECTION = "inCollection";
    @DataType(INTEGER) @DefaultValue("0") String IN_WATCHLIST = "inWatchlist";
    @DataType(INTEGER) @DefaultValue("0") String WATCHED_AT = "watchedAt";
    @DataType(INTEGER) @DefaultValue("0") String COLLECTED_AT = "collectedAt";
    @DataType(INTEGER) @DefaultValue("0") String LISTED_AT = "watchlistListedAt";

    @DataType(INTEGER) @DefaultValue("0") String HIDDEN_CALENDAR = "hiddenCalendar";
    @DataType(INTEGER) @DefaultValue("0") String HIDDEN_RECOMMENDATIONS = "hiddenRecommendations";

    @DataType(INTEGER) @DefaultValue("0") String LAST_UPDATED = "lastUpdated";

    @DataType(INTEGER) @DefaultValue("0") String TRENDING_INDEX = "trendingIndex";
    @DataType(INTEGER) @DefaultValue("0") String RECOMMENDATION_INDEX = "recommendationIndex";
    @DataType(INTEGER) @DefaultValue("-1") String ANTICIPATED_INDEX = "anticipatedIndex";

    @DataType(INTEGER) @DefaultValue("0") String WATCHING = "watching";
    @DataType(INTEGER) @DefaultValue("0") String CHECKED_IN = "checkedIn";
    @DataType(INTEGER) @DefaultValue("0") String STARTED_AT = "startedAt";
    @DataType(INTEGER) @DefaultValue("0") String EXPIRES_AT = "expiresAt";

    @DataType(INTEGER) @DefaultValue("0") String NEEDS_SYNC = "needsSync";
    @DataType(INTEGER) @DefaultValue("0") String LAST_SYNC = "lastSync";
    @DataType(INTEGER) @DefaultValue("0") String LAST_COMMENT_SYNC = "lastCommentSync";
    @DataType(INTEGER) @DefaultValue("0") String LAST_CREDITS_SYNC = "lastCreditsSync";
    @DataType(INTEGER) @DefaultValue("0") String LAST_RELATED_SYNC = "lastRelatedSync";
  }

  public interface MovieGenreColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(INTEGER) @References(table = Tables.MOVIES, column = MovieColumns.ID)
    String MOVIE_ID = "movieId";
    @DataType(TEXT) String GENRE = "genre";
  }

  public interface MovieCastColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(INTEGER) @References(table = Tables.MOVIES, column = MovieColumns.ID)
    String MOVIE_ID = "movieId";

    @DataType(TEXT) String CHARACTER = "character";
    @DataType(TEXT) @References(table = Tables.PEOPLE, column = PersonColumns.ID)
    String PERSON_ID = "personId";
  }

  public interface MovieCrewColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(INTEGER) @References(table = Tables.MOVIES, column = MovieColumns.ID)
    String MOVIE_ID = "movieId";

    @DataType(TEXT) String CATEGORY = "category";
    @DataType(TEXT) String JOB = "job";
    @DataType(TEXT) @References(table = Tables.PEOPLE, column = PersonColumns.ID)
    String PERSON_ID = "personId";
  }

  public interface PersonColumns extends LastModifiedColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(TEXT) String NAME = "name";
    @DataType(INTEGER) String TRAKT_ID = "traktId";
    @DataType(INTEGER) String SLUG = "slug";
    @DataType(INTEGER) String IMDB_ID = "imdbId";
    @DataType(INTEGER) String TMDB_ID = "tmdbId";
    @DataType(INTEGER) String TVRAGE_ID = "tvrageId";

    @DataType(INTEGER) String IMAGES_LAST_SYNC = "imagesLastSync";
    @DataType(TEXT) String HEADSHOT = "headshot";
    @DataType(TEXT) String SCREENSHOT = "screenshot";

    @DataType(TEXT) String BIOGRAPHY = "biography";
    @DataType(TEXT) String BIRTHDAY = "birthday";
    @DataType(TEXT) String DEATH = "death";
    @DataType(TEXT) String BIRTHPLACE = "birthplace";
    @DataType(TEXT) String HOMEPAGE = "homepage";
    @DataType(INTEGER) @DefaultValue("0") String LAST_SYNC = "lastSync";

    @DataType(INTEGER) String NEEDS_SYNC = "needsSync";
  }

  public interface RecentQueriesColumns {
    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(TEXT) @NotNull String QUERY = "query";
    @DataType(INTEGER) @NotNull String QUERIED_AT = "queriedAt";
  }

  public interface ListsColumns extends LastModifiedColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;

    @DataType(TEXT) String NAME = "name";
    @DataType(TEXT) String DESCRIPTION = "description";

    @DataType(TEXT) String PRIVACY = "privacy";
    @DataType(INTEGER) String DISPLAY_NUMBERS = "displayNumbers";
    @DataType(INTEGER) String ALLOW_COMMENTS = "allowComments";

    @DataType(INTEGER) String UPDATED_AT = "updatedAt";

    @DataType(INTEGER) String LIKES = "likes";

    @DataType(TEXT) String SLUG = "slug";
    @DataType(INTEGER) String TRAKT_ID = "traktId";
  }

  public interface ListItemColumns extends LastModifiedColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(INTEGER) @References(table = Tables.LISTS, column = ListsColumns.ID)
    String LIST_ID = "listId";
    @DataType(INTEGER) String LISTED_AT = "listedAt";
    @DataType(INTEGER) String ITEM_ID = "itemId";
    @DataType(INTEGER) String ITEM_TYPE = "itemType";

    String TITLE = "title";
    String OVERVIEW = "overview";
  }

  public interface ItemType {

    int SHOW = 1;
    int SEASON = 2;
    int EPISODE = 3;
    int MOVIE = 4;
    int PERSON = 5;
    int LIST = 6;
  }

  public interface UserColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(TEXT) String USERNAME = "username";
    @DataType(INTEGER) String IS_PRIVATE = "isPrivate";
    @DataType(TEXT) String NAME = "name";
    @DataType(INTEGER) String VIP = "vip";
    @DataType(INTEGER) String VIP_EP = "vipEp";
    @DataType(INTEGER) String JOINED_AT = "joinedAt";
    @DataType(TEXT) String LOCATION = "location";
    @DataType(TEXT) String ABOUT = "about";
    @DataType(TEXT) String GENDER = "gender";
    @DataType(INTEGER) String AGE = "age";
    @DataType(TEXT) String AVATAR = "avatar";
  }

  public interface CommentColumns extends LastModifiedColumns {

    @DataType(INTEGER) @PrimaryKey String ID = BaseColumns._ID;
    @DataType(TEXT) @NotNull String COMMENT = "comment";
    @DataType(INTEGER) @DefaultValue("0") String SPOILER = "spoiler";
    @DataType(INTEGER) @DefaultValue("0") String REVIEW = "review";
    @DataType(INTEGER) @DefaultValue("0")  String PARENT_ID = "parentId";
    @DataType(INTEGER) @DefaultValue("0") String CREATED_AT = "createdAt";
    @DataType(INTEGER) @DefaultValue("0") String REPLIES = "replies";
    @DataType(INTEGER) @DefaultValue("0") String LIKES = "likes";
    @DataType(INTEGER) @DefaultValue("0") String USER_RATING = "userRating";

    @DataType(INTEGER) String ITEM_ID = "itemId";
    @DataType(INTEGER) String ITEM_TYPE = "itemType";

    @DataType(INTEGER) @References(table = Tables.USERS, column = UserColumns.ID)
    String USER_ID = "userId";
    @DataType(INTEGER) @DefaultValue("0") String IS_USER_COMMENT = "isUserComment";
    @DataType(INTEGER) @DefaultValue("0") String LIKED = "liked";
    @DataType(INTEGER) @DefaultValue("0") String LIKED_AT = "likedAt";
  }

  public interface RelatedShowsColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(INTEGER) @References(table = Tables.SHOWS, column = ShowColumns.ID) String SHOW_ID =
        "showId";
    @DataType(INTEGER) @References(table = Tables.SHOWS, column = ShowColumns.ID) String
        RELATED_SHOW_ID = "relatedShowId";
    @DataType(INTEGER) String RELATED_INDEX = "relatedIndex";
  }

  public interface RelatedMoviesColumns {

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String ID = BaseColumns._ID;
    @DataType(INTEGER) @References(table = Tables.MOVIES, column = MovieColumns.ID) String
        MOVIE_ID = "movieId";
    @DataType(INTEGER) @References(table = Tables.MOVIES, column = MovieColumns.ID) String
        RELATED_MOVIE_ID = "relatedMovieId";
    @DataType(INTEGER) String RELATED_INDEX = "relatedIndex";
  }
}
