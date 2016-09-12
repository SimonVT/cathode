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

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import java.util.Set;
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.HiddenColumns;
import net.simonvt.cathode.provider.DatabaseContract.LastModifiedColumns;
import net.simonvt.cathode.provider.DatabaseContract.ListItemColumns;
import net.simonvt.cathode.provider.DatabaseContract.ListsColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieCastColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieCrewColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieGenreColumns;
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns;
import net.simonvt.cathode.provider.DatabaseContract.RecentQueriesColumns;
import net.simonvt.cathode.provider.DatabaseContract.RelatedMoviesColumns;
import net.simonvt.cathode.provider.DatabaseContract.RelatedShowsColumns;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowCharacterColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowGenreColumns;
import net.simonvt.cathode.provider.DatabaseContract.UserColumns;
import net.simonvt.cathode.provider.generated.CathodeDatabase;
import net.simonvt.cathode.util.SqlIndex;
import net.simonvt.cathode.util.SqlUtils;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.ExecOnCreate;
import net.simonvt.schematic.annotation.IfNotExists;
import net.simonvt.schematic.annotation.OnUpgrade;
import net.simonvt.schematic.annotation.Table;

@Database(className = "CathodeDatabase",
    packageName = "net.simonvt.cathode.provider.generated",
    fileName = "cathode.db",
    version = DatabaseSchematic.DATABASE_VERSION) public final class DatabaseSchematic {

  private DatabaseSchematic() {
  }

  static final int DATABASE_VERSION = 27;

  public interface Joins {
    String SHOWS_UNWATCHED = "LEFT OUTER JOIN episodes ON episodes._id=(SELECT episodes._id FROM"
        + " episodes WHERE episodes.watched=0 AND episodes.showId=shows._id AND episodes.season<>0"
        + " AND episodes.needsSync=0"
        + " ORDER BY episodes.season ASC, episodes.episode ASC LIMIT 1)";

    String SHOWS_UPCOMING =
        "LEFT OUTER JOIN " + Tables.EPISODES + " ON " + Tables.EPISODES + "." + EpisodeColumns.ID
            + "=" + "(" + "SELECT _id " + "FROM episodes " + "JOIN (" + "SELECT season, episode "
            + "FROM episodes " + "WHERE watched=1 AND showId=shows._id "
            + "ORDER BY season DESC, episode DESC LIMIT 1" + ") AS ep2 "
            + "WHERE episodes.watched=0 AND episodes.showId=shows._id" + " AND episodes.needsSync=0"
            + " AND (episodes.season>ep2.season "
            + "OR (episodes.season=ep2.season AND episodes.episode>ep2.episode)) "
            + "ORDER BY episodes.season ASC, episodes.episode ASC LIMIT 1" + ")";

    String SHOWS_UNCOLLECTED = "LEFT OUTER JOIN episodes ON episodes._id=(SELECT episodes._id FROM"
        + " episodes WHERE episodes.inCollection=0 AND episodes.showId=shows._id"
        + " AND episodes.season<>0" + " AND episodes.needsSync=0" + " AND episodes.needsSync=0"
        + " ORDER BY episodes.season ASC, episodes.episode ASC LIMIT 1)";

    String SHOWS_WITH_WATCHING =
        "LEFT OUTER JOIN episodes ON episodes._id=(SELECT episodes._id FROM"
            + " episodes WHERE (episodes.watching=1 OR episodes.checkedIn=1)"
            + " AND episodes.showId=shows._id" + " AND episodes.needsSync=0"
            + " ORDER BY episodes.season ASC, episodes.episode ASC LIMIT 1)";

    String SHOW_RELATED = "JOIN " + Tables.SHOWS + " AS " + Tables.SHOWS + " ON " + Tables.SHOWS
        + "." + ShowColumns.ID + "="
        + Tables.SHOW_RELATED + "." + RelatedShowsColumns.RELATED_SHOW_ID;

    String MOVIE_RELATED = "JOIN " + Tables.MOVIES + " AS " + Tables.MOVIES + " ON " + Tables.MOVIES
        + "." + MovieColumns.ID + "="
        + Tables.MOVIE_RELATED + "." + RelatedMoviesColumns.RELATED_MOVIE_ID;

    String MOVIE_CAST_PERSON =
        "JOIN " + Tables.PEOPLE + " AS " + Tables.PEOPLE + " ON " + Tables.PEOPLE + "."
            + PersonColumns.ID + "=" + Tables.MOVIE_CAST + "." + MovieCastColumns.PERSON_ID;

    String SHOW_CAST_PERSON =
        "JOIN " + Tables.PEOPLE + " AS " + Tables.PEOPLE + " ON " + Tables.PEOPLE + "."
            + PersonColumns.ID + "=" + Tables.SHOW_CHARACTERS + "."
            + ShowCharacterColumns.PERSON_ID;

    String EPISODES_WITH_SHOW_TITLE =
        "JOIN " + Tables.SHOWS + " AS " + Tables.SHOWS + " ON " + Tables.SHOWS + "."
            + ShowColumns.ID + "=" + Tables.EPISODES + "." + EpisodeColumns.SHOW_ID;

    String LIST_SHOWS = "LEFT JOIN " + Tables.SHOWS + " ON " + ListItemColumns.ITEM_TYPE + "="
        + DatabaseContract.ItemType.SHOW + " AND " + Tables.LIST_ITEMS + "."
        + ListItemColumns.ITEM_ID + "=" + Tables.SHOWS + "." + ShowColumns.ID;

    String LIST_SEASONS = "LEFT JOIN " + Tables.SEASONS + " ON " + ListItemColumns.ITEM_TYPE + "="
        + DatabaseContract.ItemType.SEASON + " AND " + Tables.LIST_ITEMS + "."
        + ListItemColumns.ITEM_ID + "=" + Tables.SEASONS + "." + SeasonColumns.ID;

    String LIST_EPISODES = "LEFT JOIN " + Tables.EPISODES + " ON " + ListItemColumns.ITEM_TYPE + "="
        + DatabaseContract.ItemType.EPISODE + " AND " + Tables.LIST_ITEMS + "."
        + ListItemColumns.ITEM_ID + "=" + Tables.EPISODES + "." + EpisodeColumns.ID;

    String LIST_MOVIES = "LEFT JOIN " + Tables.MOVIES + " ON " + ListItemColumns.ITEM_TYPE + "="
        + DatabaseContract.ItemType.MOVIE + " AND " + Tables.LIST_ITEMS + "."
        + ListItemColumns.ITEM_ID + "=" + Tables.MOVIES + "." + MovieColumns.ID;

    String LIST_PEOPLE = "LEFT JOIN " + Tables.PEOPLE + " ON " + ListItemColumns.ITEM_TYPE + "="
        + DatabaseContract.ItemType.PERSON + " AND " + Tables.LIST_ITEMS + "."
        + ListItemColumns.ITEM_ID + "=" + Tables.PEOPLE + "." + PersonColumns.ID;

    String LIST = LIST_SHOWS + " " + LIST_SEASONS + " " + LIST_EPISODES + " " + LIST_MOVIES + " "
        + LIST_PEOPLE;

    String COMMENT_PROFILE =
        "JOIN " + Tables.USERS + " AS " + Tables.USERS + " ON " + Tables.USERS + "."
            + UserColumns.ID + "=" + Tables.COMMENTS + "." + CommentColumns.USER_ID;
  }

  public interface Tables {

    @Table(ShowColumns.class) @IfNotExists String SHOWS = "shows";
    @Table(ShowGenreColumns.class) @IfNotExists String SHOW_GENRES = "showGenres";
    @Table(SeasonColumns.class) @IfNotExists String SEASONS = "seasons";
    @Table(EpisodeColumns.class) @IfNotExists String EPISODES = "episodes";
    @Table(ShowCharacterColumns.class) @IfNotExists String SHOW_CHARACTERS = "showCharacters";
    @Table(RelatedShowsColumns.class) @IfNotExists String SHOW_RELATED =
        "relatedShows";

    @Table(MovieColumns.class) @IfNotExists String MOVIES = "movies";
    @Table(MovieGenreColumns.class) @IfNotExists String MOVIE_GENRES = "movieGenres";
    @Table(MovieCastColumns.class) @IfNotExists String MOVIE_CAST = "movieCast";
    @Table(MovieCrewColumns.class) @IfNotExists String MOVIE_CREW = "movieCrew";
    @Table(RelatedMoviesColumns.class) @IfNotExists String MOVIE_RELATED =
        "relatedMovies";

    @Table(PersonColumns.class) @IfNotExists String PEOPLE = "people";

    @Table(RecentQueriesColumns.class) @IfNotExists String RECENT_QUERIES = "recentQueries";

    @Table(ListsColumns.class) @IfNotExists String LISTS = "lists";
    @Table(ListItemColumns.class) @IfNotExists String LIST_ITEMS = "listItems";

    @Table(UserColumns.class) @IfNotExists String USERS = "users";

    @Table(CommentColumns.class) @IfNotExists String COMMENTS = "comments";
  }

  interface References {

    String SHOW_ID = "REFERENCES " + Tables.SHOWS + "(" + ShowColumns.ID + ")";
    String SEASON_ID = "REFERENCES " + Tables.SEASONS + "(" + SeasonColumns.ID + ")";
    String MOVIE_ID = "REFERENCES " + Tables.MOVIES + "(" + MovieColumns.ID + ")";
  }

  interface TriggerName {

    String EPISODE_UPDATE_AIRED = "episodeUpdateAired";
    String EPISODE_UPDATE_WATCHED = "episodeUpdateWatched";
    String EPISODE_UPDATE_COLLECTED = "episodeUpdateCollected";
    String EPISODE_UPDATE_WATCHING = "episodeUpdateWatching";

    String EPISODE_INSERT = "episodeInsert";
    String EPISODE_UPDATE = "episodeUpdate";
    String EPISODE_DELETE = "episodeDelete";

    String SHOW_UPDATE = "showUpdate";
    String SHOW_DELETE = "showDelete";

    String SEASON_UPDATE = "seasonUpdate";
    String SEASON_DELETE = "seasonDelete";

    String MOVIES_UPDATE = "moviesUpdate";
    String MOVIE_DELETE = "movieDelete";

    String PEOPLE_UPDATE = "peopleUpdate";

    String LIST_UPDATE = "listUpdate";
    String LIST_DELETE = "listDelete";

    String LISTITEM_UPDATE = "listItemUpdate";

    String COMMENT_UPDATE = "commentUpdate";
  }

  interface Trigger {

    String SEASONS_UPDATE_WATCHED =
        "UPDATE " + Tables.SEASONS + " SET " + SeasonColumns.WATCHED_COUNT
            + "=(SELECT COUNT(*) FROM " + Tables.EPISODES + " WHERE " + Tables.EPISODES + "."
            + EpisodeColumns.SEASON_ID + "=NEW." + EpisodeColumns.SEASON_ID + " AND "
            + Tables.EPISODES + "." + EpisodeColumns.WATCHED + "=1" + " AND episodes.needsSync=0"
            + " AND " + Tables.EPISODES + "." + EpisodeColumns.SEASON + ">0)" + " WHERE "
            + Tables.SEASONS + "." + SeasonColumns.ID + "=NEW." + EpisodeColumns.SEASON_ID + ";";

    String SEASONS_UPDATE_COLLECTED =
        "UPDATE " + Tables.SEASONS + " SET " + SeasonColumns.IN_COLLECTION_COUNT
            + "=(SELECT COUNT(*) FROM " + Tables.EPISODES + " WHERE " + Tables.EPISODES + "."
            + EpisodeColumns.SEASON_ID + "=NEW." + EpisodeColumns.SEASON_ID + " AND "
            + Tables.EPISODES + "." + EpisodeColumns.IN_COLLECTION + "=1"
            + " AND episodes.needsSync=0" + " AND " + Tables.EPISODES + "." + EpisodeColumns.SEASON
            + ">0)" + " WHERE " + Tables.SEASONS + "." + SeasonColumns.ID + "=NEW."
            + EpisodeColumns.SEASON_ID + ";";

    String SEASONS_UPDATE_AIRDATE =
        "UPDATE " + Tables.SEASONS + " SET " + SeasonColumns.AIRDATE_COUNT
            + "=(SELECT COUNT(*) FROM " + Tables.EPISODES + " WHERE " + Tables.EPISODES + "."
            + EpisodeColumns.SEASON_ID + "=NEW." + EpisodeColumns.SEASON_ID + " AND "
            + Tables.EPISODES + "." + EpisodeColumns.FIRST_AIRED + " IS NOT NULL "
            + " AND episodes.needsSync=0" + " AND " + Tables.EPISODES + "." + EpisodeColumns.SEASON
            + ">0)" + " WHERE " + Tables.SEASONS + "." + SeasonColumns.ID + "=NEW."
            + EpisodeColumns.SEASON_ID + ";";

    String SHOWS_UPDATE_WATCHED =
        "UPDATE " + Tables.SHOWS + " SET " + ShowColumns.WATCHED_COUNT + "=(SELECT COUNT(*) FROM "
            + Tables.EPISODES + " WHERE " + Tables.EPISODES + "." + EpisodeColumns.SHOW_ID + "=NEW."
            + EpisodeColumns.SHOW_ID + " AND " + Tables.EPISODES + "." + EpisodeColumns.WATCHED
            + "=1" + " AND episodes.needsSync=0" + " AND " + Tables.EPISODES + "."
            + EpisodeColumns.SEASON + ">0)" + " WHERE " + Tables.SHOWS + "." + ShowColumns.ID
            + "=NEW." + EpisodeColumns.SHOW_ID + ";";

    String SHOWS_UPDATE_COLLECTED =
        "UPDATE " + Tables.SHOWS + " SET " + ShowColumns.IN_COLLECTION_COUNT
            + "=(SELECT COUNT(*) FROM " + Tables.EPISODES + " WHERE " + Tables.EPISODES + "."
            + EpisodeColumns.SHOW_ID + "=NEW." + EpisodeColumns.SHOW_ID + " AND " + Tables.EPISODES
            + "." + EpisodeColumns.IN_COLLECTION + "=1" + " AND episodes.needsSync=0" + " AND "
            + Tables.EPISODES + "." + EpisodeColumns.SEASON + ">0)" + " WHERE " + Tables.SHOWS + "."
            + ShowColumns.ID + "=NEW." + EpisodeColumns.SHOW_ID + ";";

    String SHOWS_UPDATE_AIRDATE =
        "UPDATE " + Tables.SHOWS + " SET " + ShowColumns.AIRDATE_COUNT + "=(SELECT COUNT(*) FROM "
            + Tables.EPISODES + " WHERE " + Tables.EPISODES + "." + EpisodeColumns.SHOW_ID + "=NEW."
            + EpisodeColumns.SHOW_ID + " AND " + Tables.EPISODES + "." + EpisodeColumns.FIRST_AIRED
            + " IS NOT NULL " + " AND episodes.needsSync=0" + " AND " + Tables.EPISODES + "."
            + EpisodeColumns.SEASON + ">0)" + " WHERE " + Tables.SHOWS + "." + ShowColumns.ID
            + "=NEW." + EpisodeColumns.SHOW_ID + ";";

    String SHOWS_UPDATE_WATCHING = "UPDATE "
        + Tables.SHOWS
        + " SET "
        + ShowColumns.WATCHING
        + "=(SELECT COUNT(*) FROM "
        + Tables.EPISODES
        + " WHERE "
        + Tables.EPISODES + "." + EpisodeColumns.SHOW_ID + "=NEW." + EpisodeColumns.SHOW_ID
        + " AND "
        + "("
        + Tables.EPISODES + "." + EpisodeColumns.WATCHING + "=1"
        + " OR "
        + Tables.EPISODES + "." + EpisodeColumns.CHECKED_IN + "=1"
        + ")"
        + " AND "
        + Tables.EPISODES + "." + EpisodeColumns.NEEDS_SYNC + "=0"
        + " AND "
        + Tables.EPISODES + "." + EpisodeColumns.SEASON + ">0"
        + ")"
        + " WHERE "
        + Tables.SHOWS + "." + ShowColumns.ID + "=NEW." + EpisodeColumns.SHOW_ID
        + ";";

    String SHOW_DELETE_SEASONS =
        "DELETE FROM " + Tables.SEASONS + " WHERE " + Tables.SEASONS + "." + SeasonColumns.SHOW_ID
            + "=OLD." + ShowColumns.ID + ";";

    String SHOW_DELETE_EPISODES =
        "DELETE FROM " + Tables.EPISODES + " WHERE " + Tables.EPISODES + "."
            + EpisodeColumns.SHOW_ID + "=OLD." + SeasonColumns.ID + ";";

    String SHOW_DELETE_GENRES =
        "DELETE FROM " + Tables.SHOW_GENRES + " WHERE " + Tables.SHOW_GENRES + "."
            + ShowGenreColumns.SHOW_ID + "=OLD." + ShowColumns.ID + ";";

    String SHOW_DELETE_CHARACTERS =
        "DELETE FROM " + Tables.SHOW_CHARACTERS + " WHERE " + Tables.SHOW_CHARACTERS + "."
            + ShowCharacterColumns.SHOW_ID + "=OLD." + ShowColumns.ID + ";";

    String SHOW_DELETE_COMMENTS =
        "DELETE FROM " + Tables.COMMENTS + " WHERE " + Tables.COMMENTS + "."
            + CommentColumns.ITEM_TYPE + "=" + DatabaseContract.ItemType.SHOW + " AND "
            + Tables.COMMENTS + "." + CommentColumns.ITEM_ID + "=OLD." + ShowColumns.ID + ";";

    String SEASON_DELETE_EPISODES =
        "DELETE FROM " + Tables.EPISODES + " WHERE " + Tables.EPISODES + "."
            + EpisodeColumns.SEASON_ID + "=OLD." + SeasonColumns.ID + ";";

    String EPISODE_DELETE_COMMENTS =
        "DELETE FROM " + Tables.COMMENTS + " WHERE " + Tables.COMMENTS + "."
            + CommentColumns.ITEM_TYPE + "=" + DatabaseContract.ItemType.EPISODE + " AND "
            + Tables.COMMENTS + "." + CommentColumns.ITEM_ID + "=OLD." + EpisodeColumns.ID + ";";

    String MOVIE_DELETE_GENRES =
        "DELETE FROM " + Tables.MOVIE_GENRES + " WHERE " + Tables.MOVIE_GENRES + "."
            + MovieGenreColumns.MOVIE_ID + "=OLD." + MovieColumns.ID + ";";

    String MOVIE_DELETE_CAST =
        "DELETE FROM " + Tables.MOVIE_CAST + " WHERE " + Tables.MOVIE_CAST + "."
            + MovieCastColumns.MOVIE_ID + "=OLD." + MovieColumns.ID + ";";

    String MOVIE_DELETE_CREW =
        "DELETE FROM " + Tables.MOVIE_CREW + " WHERE " + Tables.MOVIE_CREW + "."
            + MovieCrewColumns.MOVIE_ID + "=OLD." + MovieColumns.ID + ";";

    String MOVIE_DELETE_COMMENTS =
        "DELETE FROM " + Tables.COMMENTS + " WHERE " + Tables.COMMENTS + "."
            + CommentColumns.ITEM_TYPE + "=" + DatabaseContract.ItemType.MOVIE + " AND "
            + Tables.COMMENTS + "." + CommentColumns.ITEM_ID + "=OLD." + MovieColumns.ID + ";";

    String TIME_MILLIS =
        "(strftime('%s','now') * 1000 + cast(substr(strftime('%f','now'),4,3) AS INTEGER))";

    String SHOW_UPDATE =
        "UPDATE " + Tables.SHOWS + " SET " + ShowColumns.LAST_MODIFIED + "=" + TIME_MILLIS
            + " WHERE " + ShowColumns.ID + "=old." + ShowColumns.ID + ";";

    String SEASON_UPDATE =
        "UPDATE " + Tables.SEASONS + " SET " + SeasonColumns.LAST_MODIFIED + "=" + TIME_MILLIS
            + " WHERE " + SeasonColumns.ID + "=old." + SeasonColumns.ID + ";";

    String EPISODE_UPDATE =
        "UPDATE " + Tables.EPISODES + " SET " + EpisodeColumns.LAST_MODIFIED + "=" + TIME_MILLIS
            + " WHERE " + EpisodeColumns.ID + "=old." + EpisodeColumns.ID + ";";

    String MOVIES_UPDATE =
        "UPDATE " + Tables.MOVIES + " SET " + MovieColumns.LAST_MODIFIED + "=" + TIME_MILLIS
            + " WHERE " + MovieColumns.ID + "=old." + MovieColumns.ID + ";";

    String PEOPLE_UPDATE =
        "UPDATE " + Tables.PEOPLE + " SET " + PersonColumns.LAST_MODIFIED + "=" + TIME_MILLIS
            + " WHERE " + PersonColumns.ID + "=old." + PersonColumns.ID + ";";

    String LIST_DELETE = "DELETE FROM " + Tables.LIST_ITEMS + " WHERE " + Tables.LIST_ITEMS + "."
        + ListItemColumns.LIST_ID + "=OLD." + ListsColumns.ID + ";";

    String LISTS_UPDATE =
        "UPDATE " + Tables.LISTS + " SET " + ListsColumns.LAST_MODIFIED + "=" + TIME_MILLIS
            + " WHERE " + ListsColumns.ID + "=old." + ListsColumns.ID + ";";

    String LISTITEM_UPDATE =
        "UPDATE " + Tables.LIST_ITEMS + " SET " + ListItemColumns.LAST_MODIFIED + "=" + TIME_MILLIS
            + " WHERE " + ListItemColumns.ID + "=old." + ListItemColumns.ID + ";";

    String COMMENT_UPDATE =
        "UPDATE " + Tables.COMMENTS + " SET " + CommentColumns.LAST_MODIFIED + "=" + TIME_MILLIS
            + " WHERE " + CommentColumns.ID + "=old." + CommentColumns.ID + ";";
  }

  @ExecOnCreate public static final String TRIGGER_EPISODE_UPDATE_AIRED =
      "CREATE TRIGGER IF NOT EXISTS " + TriggerName.EPISODE_UPDATE_AIRED + " AFTER UPDATE OF "
          + EpisodeColumns.FIRST_AIRED + "," + EpisodeColumns.NEEDS_SYNC + " ON " + Tables.EPISODES
          + " BEGIN " + Trigger.SEASONS_UPDATE_AIRDATE + Trigger.SHOWS_UPDATE_AIRDATE + " END;";

  @ExecOnCreate public static final String TRIGGER_EPISODE_UPDATE_WATCHED =
      "CREATE TRIGGER IF NOT EXISTS " + TriggerName.EPISODE_UPDATE_WATCHED + " AFTER UPDATE OF "
          + EpisodeColumns.WATCHED + "," + EpisodeColumns.NEEDS_SYNC + " ON " + Tables.EPISODES
          + " BEGIN " + Trigger.SEASONS_UPDATE_WATCHED + Trigger.SHOWS_UPDATE_WATCHED + " END;";

  @ExecOnCreate public static final String TRIGGER_EPISODE_UPDATE_COLLECTED =
      "CREATE TRIGGER IF NOT EXISTS " + TriggerName.EPISODE_UPDATE_COLLECTED + " AFTER UPDATE OF "
          + EpisodeColumns.IN_COLLECTION + "," + EpisodeColumns.NEEDS_SYNC + " ON "
          + Tables.EPISODES + " BEGIN " + Trigger.SEASONS_UPDATE_COLLECTED
          + Trigger.SHOWS_UPDATE_COLLECTED + " END;";

  @ExecOnCreate public static final String TRIGGER_EPISODE_UPDATE_WATCHING =
      "CREATE TRIGGER IF NOT EXISTS " + TriggerName.EPISODE_UPDATE_WATCHING + " AFTER UPDATE OF "
          + EpisodeColumns.WATCHING + "," + EpisodeColumns.CHECKED_IN + " ON " + Tables.EPISODES
          + " BEGIN " + Trigger.SHOWS_UPDATE_WATCHING + " END;";

  @ExecOnCreate public static final String TRIGGER_EPISODE_INSERT =
      "CREATE TRIGGER IF NOT EXISTS " + TriggerName.EPISODE_INSERT + " AFTER INSERT ON "
          + Tables.EPISODES + " BEGIN " + Trigger.SEASONS_UPDATE_WATCHED
          + Trigger.SEASONS_UPDATE_AIRDATE + Trigger.SEASONS_UPDATE_COLLECTED
          + Trigger.SHOWS_UPDATE_WATCHED + Trigger.SHOWS_UPDATE_AIRDATE
          + Trigger.SHOWS_UPDATE_COLLECTED + " END;";

  @ExecOnCreate public static final String TRIGGER_SHOW_DELETE =
      "CREATE TRIGGER IF NOT EXISTS " + TriggerName.SHOW_DELETE + " AFTER DELETE ON " + Tables.SHOWS
          + " BEGIN " + Trigger.SHOW_DELETE_SEASONS + Trigger.SHOW_DELETE_GENRES
          + Trigger.SHOW_DELETE_CHARACTERS + Trigger.SHOW_DELETE_COMMENTS + " END;";

  @ExecOnCreate public static final String TRIGGER_SEASON_DELETE =
      "CREATE TRIGGER IF NOT EXISTS " + TriggerName.SEASON_DELETE + " AFTER DELETE ON "
          + Tables.SEASONS + " BEGIN " + Trigger.SEASON_DELETE_EPISODES + " END";

  @ExecOnCreate public static final String TRIGGER_EPISODE_DELETE =
      "CREATE TRIGGER IF NOT EXISTS " + TriggerName.EPISODE_DELETE + " AFTER DELETE ON "
          + Tables.EPISODES + " BEGIN " + Trigger.EPISODE_DELETE_COMMENTS + " END";

  @ExecOnCreate public static final String TRIGGER_MOVIE_DELETE =
      "CREATE TRIGGER IF NOT EXISTS " + TriggerName.MOVIE_DELETE + " AFTER DELETE ON "
          + Tables.MOVIES + " BEGIN " + Trigger.MOVIE_DELETE_GENRES + Trigger.MOVIE_DELETE_CAST
          + Trigger.MOVIE_DELETE_CREW + Trigger.MOVIE_DELETE_COMMENTS + " END;";

  @ExecOnCreate public static final String TRIGGER_SHOW_UPDATE =
      "CREATE TRIGGER IF NOT EXISTS " + TriggerName.SHOW_UPDATE + " AFTER UPDATE ON " + Tables.SHOWS
          + " FOR EACH ROW BEGIN " + Trigger.SHOW_UPDATE + " END";

  @ExecOnCreate public static final String TRIGGER_SEASON_UPDATE =
      "CREATE TRIGGER IF NOT EXISTS " + TriggerName.SEASON_UPDATE + " AFTER UPDATE ON "
          + Tables.SEASONS + " FOR EACH ROW BEGIN " + Trigger.SEASON_UPDATE + " END";

  @ExecOnCreate public static final String TRIGGER_EPISODE_UPDATE =
      "CREATE TRIGGER IF NOT EXISTS " + TriggerName.EPISODE_UPDATE + " AFTER UPDATE ON "
          + Tables.EPISODES + " FOR EACH ROW BEGIN " + Trigger.EPISODE_UPDATE + " END";

  @ExecOnCreate public static final String TRIGGER_MOVIES_UPDATE =
      "CREATE TRIGGER IF NOT EXISTS " + TriggerName.MOVIES_UPDATE + " AFTER UPDATE ON "
          + Tables.MOVIES + " FOR EACH ROW BEGIN " + Trigger.MOVIES_UPDATE + " END";

  @ExecOnCreate public static final String TRIGGER_PEOPLE_UPDATE =
      "CREATE TRIGGER IF NOT EXISTS " + TriggerName.PEOPLE_UPDATE + " AFTER UPDATE ON "
          + Tables.PEOPLE + " FOR EACH ROW BEGIN " + Trigger.PEOPLE_UPDATE + " END";

  @ExecOnCreate public static final String TRIGGER_LIST_DELETE =
      "CREATE TRIGGER IF NOT EXISTS " + TriggerName.LIST_DELETE + " AFTER DELETE ON " + Tables.LISTS
          + " BEGIN " + Trigger.LIST_DELETE + " END;";

  @ExecOnCreate public static final String TRIGGER_LIST_UPDATE =
      "CREATE TRIGGER IF NOT EXISTS " + TriggerName.LIST_UPDATE + " AFTER UPDATE ON " + Tables.LISTS
          + " FOR EACH ROW BEGIN " + Trigger.LISTS_UPDATE + " END";

  @ExecOnCreate public static final String TRIGGER_LISTITEM_UPDATE =
      "CREATE TRIGGER IF NOT EXISTS " + TriggerName.LISTITEM_UPDATE + " AFTER UPDATE ON "
          + Tables.LIST_ITEMS + " FOR EACH ROW BEGIN " + Trigger.LISTITEM_UPDATE + " END";

  @ExecOnCreate public static final String TRIGGER_COMMENT_UPDATE =
      "CREATE TRIGGER IF NOT EXISTS " + TriggerName.COMMENT_UPDATE + " AFTER UPDATE ON "
          + Tables.COMMENTS + " FOR EACH ROW BEGIN " + Trigger.COMMENT_UPDATE + " END";

  @ExecOnCreate public static final String INDEX_CHARACTERS_SHOW_ID =
      SqlIndex.index("characterShowId")
          .ifNotExists()
          .onTable(Tables.SHOW_CHARACTERS)
          .forColumns(ShowCharacterColumns.SHOW_ID)
          .build();

  @ExecOnCreate public static final String INDEX_SEASON_SHOW_ID = SqlIndex.index("seasonShowId")
      .ifNotExists()
      .onTable(Tables.SEASONS)
      .forColumns(SeasonColumns.SHOW_ID)
      .build();

  @ExecOnCreate public static final String INDEX_GENRE_SHOW_ID = SqlIndex.index("genreShowId")
      .ifNotExists()
      .onTable(Tables.SHOW_GENRES)
      .forColumns(ShowGenreColumns.SHOW_ID)
      .build();

  @ExecOnCreate public static final String INDEX_CAST_MOVIE_ID = SqlIndex.index("castMovieId")
      .ifNotExists()
      .onTable(Tables.MOVIE_CAST)
      .forColumns(MovieCastColumns.MOVIE_ID)
      .build();

  @ExecOnCreate public static final String INDEX_CREW_MOVIE_ID = SqlIndex.index("crewMovieId")
      .ifNotExists()
      .onTable(Tables.MOVIE_CREW)
      .forColumns(MovieCrewColumns.MOVIE_ID)
      .build();

  @ExecOnCreate public static final String INDEX_GENRE_MOVIE_ID = SqlIndex.index("genreMovieId")
      .ifNotExists()
      .onTable(Tables.MOVIE_GENRES)
      .forColumns(MovieGenreColumns.MOVIE_ID)
      .build();

  @ExecOnCreate public static final String INDEX_EPISODES_SHOW_ID = SqlIndex.index("episodesShowId")
      .ifNotExists()
      .onTable(Tables.EPISODES)
      .forColumns(EpisodeColumns.SHOW_ID)
      .build();

  @ExecOnCreate public static final String INDEX_EPISODES_SEASON_ID =
      SqlIndex.index("episodesSeasonId")
          .ifNotExists()
          .onTable(Tables.EPISODES)
          .forColumns(EpisodeColumns.SEASON_ID)
          .build();

  @OnUpgrade
  public static void onUpgrade(Context context, SQLiteDatabase db, int oldVersion, int newVersion) {
    if (oldVersion < 12) {
      db.execSQL("DROP TABLE IF EXISTS shows");
      db.execSQL("DROP TABLE IF EXISTS showTopWatchers");
      db.execSQL("DROP TABLE IF EXISTS topEpisodes");
      db.execSQL("DROP TABLE IF EXISTS showActors");
      db.execSQL("DROP TABLE IF EXISTS showGenres");
      db.execSQL("DROP TABLE IF EXISTS seasons");
      db.execSQL("DROP TABLE IF EXISTS episodes");
      db.execSQL("DROP TABLE IF EXISTS movies");
      db.execSQL("DROP TABLE IF EXISTS movieGenres");
      db.execSQL("DROP TABLE IF EXISTS movieTopWatchers");
      db.execSQL("DROP TABLE IF EXISTS movieActors");
      db.execSQL("DROP TABLE IF EXISTS movieDirectors");
      db.execSQL("DROP TABLE IF EXISTS movieWriters");
      db.execSQL("DROP TABLE IF EXISTS movieProducers");
      db.execSQL("DROP TABLE IF EXISTS showSearchSuggestions");
      db.execSQL("DROP TABLE IF EXISTS movieSearchSuggestions");

      db.execSQL("DROP TRIGGER IF EXISTS episodeInsert");
      db.execSQL("DROP TRIGGER IF EXISTS episodeUpdateAired");
      db.execSQL("DROP TRIGGER IF EXISTS episodeUpdateWatched");
      db.execSQL("DROP TRIGGER IF EXISTS episodeUpdateCollected");

      CathodeDatabase.getInstance(context).onCreate(db);
    }

    if (oldVersion < 13) {
      SqlUtils.createColumnIfNotExists(db, Tables.SHOWS, LastModifiedColumns.LAST_MODIFIED,
          DataType.Type.INTEGER, "0");
      SqlUtils.createColumnIfNotExists(db, Tables.SEASONS, LastModifiedColumns.LAST_MODIFIED,
          DataType.Type.INTEGER, "0");
      SqlUtils.createColumnIfNotExists(db, Tables.EPISODES, LastModifiedColumns.LAST_MODIFIED,
          DataType.Type.INTEGER, "0");
      SqlUtils.createColumnIfNotExists(db, Tables.SHOW_CHARACTERS,
          LastModifiedColumns.LAST_MODIFIED, DataType.Type.INTEGER, "0");

      SqlUtils.createColumnIfNotExists(db, Tables.MOVIES, LastModifiedColumns.LAST_MODIFIED,
          DataType.Type.INTEGER, "0");
      SqlUtils.createColumnIfNotExists(db, Tables.MOVIE_CAST, LastModifiedColumns.LAST_MODIFIED,
          DataType.Type.INTEGER, "0");
      SqlUtils.createColumnIfNotExists(db, Tables.MOVIE_CREW, LastModifiedColumns.LAST_MODIFIED,
          DataType.Type.INTEGER, "0");

      SqlUtils.createColumnIfNotExists(db, Tables.PEOPLE, LastModifiedColumns.LAST_MODIFIED,
          DataType.Type.INTEGER, "0");

      db.execSQL(TRIGGER_SHOW_UPDATE);
      db.execSQL(TRIGGER_SEASON_UPDATE);
      db.execSQL(TRIGGER_EPISODE_UPDATE);
      db.execSQL(TRIGGER_MOVIES_UPDATE);
      db.execSQL(TRIGGER_PEOPLE_UPDATE);
    }

    if (oldVersion < 14) {
      db.execSQL(CathodeDatabase.LISTS);
      db.execSQL(CathodeDatabase.LIST_ITEMS);
      db.execSQL(TRIGGER_LIST_DELETE);
      db.execSQL(TRIGGER_LIST_UPDATE);
      db.execSQL(TRIGGER_LISTITEM_UPDATE);
    }

    if (oldVersion < 15) {
      db.execSQL(INDEX_SEASON_SHOW_ID);
      db.execSQL(INDEX_CHARACTERS_SHOW_ID);
      db.execSQL(INDEX_GENRE_SHOW_ID);
      db.execSQL(INDEX_CAST_MOVIE_ID);
      db.execSQL(INDEX_CREW_MOVIE_ID);
      db.execSQL(INDEX_GENRE_MOVIE_ID);
    }

    if (oldVersion < 16) {
      Set<String> showColumns = SqlUtils.columns(db, Tables.SHOWS);
      if (!showColumns.contains(HiddenColumns.HIDDEN_CALENDAR)) {
        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + HiddenColumns.HIDDEN_CALENDAR
            + " INTEGER DEFAULT 0");
      }
      if (!showColumns.contains(HiddenColumns.HIDDEN_COLLECTED)) {
        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + HiddenColumns.HIDDEN_COLLECTED
            + " INTEGER DEFAULT 0");
      }
      if (!showColumns.contains(HiddenColumns.HIDDEN_WATCHED)) {
        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + HiddenColumns.HIDDEN_WATCHED
            + " INTEGER DEFAULT 0");
      }
      if (!showColumns.contains(HiddenColumns.HIDDEN_RECOMMENDATIONS)) {
        db.execSQL(
            "ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + HiddenColumns.HIDDEN_RECOMMENDATIONS
                + " INTEGER DEFAULT 0");
      }

      Set<String> seasonColumns = SqlUtils.columns(db, Tables.SEASONS);
      if (!seasonColumns.contains(HiddenColumns.HIDDEN_CALENDAR)) {
        db.execSQL("ALTER TABLE " + Tables.SEASONS + " ADD COLUMN " + HiddenColumns.HIDDEN_CALENDAR
            + " INTEGER DEFAULT 0");
      }
      if (!seasonColumns.contains(HiddenColumns.HIDDEN_COLLECTED)) {
        db.execSQL("ALTER TABLE " + Tables.SEASONS + " ADD COLUMN " + HiddenColumns.HIDDEN_COLLECTED
            + " INTEGER DEFAULT 0");
      }
      if (!seasonColumns.contains(HiddenColumns.HIDDEN_WATCHED)) {
        db.execSQL("ALTER TABLE " + Tables.SEASONS + " ADD COLUMN " + HiddenColumns.HIDDEN_WATCHED
            + " INTEGER DEFAULT 0");
      }
      if (!seasonColumns.contains(HiddenColumns.HIDDEN_RECOMMENDATIONS)) {
        db.execSQL(
            "ALTER TABLE " + Tables.SEASONS + " ADD COLUMN " + HiddenColumns.HIDDEN_RECOMMENDATIONS
                + " INTEGER DEFAULT 0");
      }

      Set<String> movieColumns = SqlUtils.columns(db, Tables.MOVIES);
      if (!movieColumns.contains(HiddenColumns.HIDDEN_CALENDAR)) {
        db.execSQL("ALTER TABLE " + Tables.MOVIES + " ADD COLUMN " + HiddenColumns.HIDDEN_CALENDAR
            + " INTEGER DEFAULT 0");
      }
      if (!movieColumns.contains(HiddenColumns.HIDDEN_COLLECTED)) {
        db.execSQL("ALTER TABLE " + Tables.MOVIES + " ADD COLUMN " + HiddenColumns.HIDDEN_COLLECTED
            + " INTEGER DEFAULT 0");
      }
      if (!movieColumns.contains(HiddenColumns.HIDDEN_WATCHED)) {
        db.execSQL("ALTER TABLE " + Tables.MOVIES + " ADD COLUMN " + HiddenColumns.HIDDEN_WATCHED
            + " INTEGER DEFAULT 0");
      }
      if (!movieColumns.contains(HiddenColumns.HIDDEN_RECOMMENDATIONS)) {
        db.execSQL(
            "ALTER TABLE " + Tables.MOVIES + " ADD COLUMN " + HiddenColumns.HIDDEN_RECOMMENDATIONS
                + " INTEGER DEFAULT 0");
      }
    }

    if (oldVersion < 17) {
      db.execSQL(INDEX_EPISODES_SHOW_ID);
      db.execSQL(INDEX_EPISODES_SEASON_ID);
    }

    if (oldVersion < 18) {
      db.execSQL(CathodeDatabase.COMMENTS);
      db.execSQL(CathodeDatabase.USERS);

      db.execSQL("DROP TRIGGER IF EXISTS " + TriggerName.SHOW_DELETE);
      db.execSQL("DROP TRIGGER IF EXISTS " + TriggerName.MOVIE_DELETE);
      db.execSQL(TRIGGER_SHOW_DELETE);
      db.execSQL(TRIGGER_MOVIE_DELETE);

      db.execSQL(TRIGGER_COMMENT_UPDATE);
      db.execSQL(TRIGGER_EPISODE_DELETE);
    }

    if (oldVersion < 19) {
      Set<String> showColumns = SqlUtils.columns(db, Tables.SHOWS);
      if (!showColumns.contains(ShowColumns.LAST_COMMENT_SYNC)) {
        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + ShowColumns.LAST_COMMENT_SYNC
            + " INTEGER DEFAULT 0");
      }
    }

    if (oldVersion < 20) {
      Set<String> movieColumns = SqlUtils.columns(db, Tables.MOVIES);
      if (!movieColumns.contains(MovieColumns.LAST_COMMENT_SYNC)) {
        db.execSQL("ALTER TABLE " + Tables.MOVIES + " ADD COLUMN " + MovieColumns.LAST_COMMENT_SYNC
            + " INTEGER DEFAULT 0");
      }
    }

    if (oldVersion < 21) {
      SqlUtils.createColumnIfNotExists(db, Tables.SHOWS, ShowColumns.LAST_SYNC,
          DataType.Type.INTEGER, "0");
      SqlUtils.createColumnIfNotExists(db, Tables.SHOWS, ShowColumns.LAST_ACTORS_SYNC,
          DataType.Type.INTEGER, "0");
      SqlUtils.createColumnIfNotExists(db, Tables.MOVIES, MovieColumns.LAST_SYNC,
          DataType.Type.INTEGER, "0");
      SqlUtils.createColumnIfNotExists(db, Tables.MOVIES, MovieColumns.LAST_CREW_SYNC,
          DataType.Type.INTEGER, "0");

      final long currentTime = System.currentTimeMillis();

      ContentValues values = new ContentValues();
      values.put(ShowColumns.LAST_SYNC, currentTime);
      values.put(ShowColumns.LAST_ACTORS_SYNC, currentTime);

      db.update(Tables.SHOWS, values, null, null);

      values = new ContentValues();
      values.put(MovieColumns.LAST_SYNC, currentTime);
      values.put(MovieColumns.LAST_CREW_SYNC, currentTime);

      db.update(Tables.MOVIES, values, null, null);
    }

    if (oldVersion < 22) {
      SqlUtils.createColumnIfNotExists(db, Tables.SHOWS, ShowColumns.ANTICIPATED_INDEX,
          DataType.Type.INTEGER, "-1");
      SqlUtils.createColumnIfNotExists(db, Tables.MOVIES, MovieColumns.ANTICIPATED_INDEX,
          DataType.Type.INTEGER, "-1");
    }

    if (oldVersion < 23) {
      SqlUtils.createColumnIfNotExists(db, Tables.EPISODES, EpisodeColumns.LAST_COMMENT_SYNC,
          DataType.Type.INTEGER, "0");
    }

    if (oldVersion < 24) {
      SqlUtils.createColumnIfNotExists(db, Tables.SHOWS, ShowColumns.WATCHING,
          DataType.Type.INTEGER, "0");
      db.execSQL(TRIGGER_EPISODE_UPDATE_WATCHING);
    }

    if (oldVersion < 25) {
      db.execSQL("DROP TABLE IF EXISTS showSearchSuggestions");
      db.execSQL("DROP TABLE IF EXISTS movieSearchSuggestions");

      db.execSQL(CathodeDatabase.RECENT_QUERIES);
    }

    if (oldVersion < 26) {
      db.execSQL(CathodeDatabase.SHOW_RELATED);
      db.execSQL(CathodeDatabase.MOVIE_RELATED);
    }

    if (oldVersion < 27) {
      SqlUtils.createColumnIfNotExists(db, Tables.SHOWS, ShowColumns.LAST_RELATED_SYNC,
          DataType.Type.INTEGER, "0");
      SqlUtils.createColumnIfNotExists(db, Tables.MOVIES, MovieColumns.LAST_RELATED_SYNC,
          DataType.Type.INTEGER, "0");
    }
  }
}
