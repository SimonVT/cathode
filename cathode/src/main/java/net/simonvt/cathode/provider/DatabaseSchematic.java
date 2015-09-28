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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.HiddenColumns;
import net.simonvt.cathode.provider.DatabaseContract.LastModifiedColumns;
import net.simonvt.cathode.provider.DatabaseContract.ListItemColumns;
import net.simonvt.cathode.provider.DatabaseContract.ListsColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieCastColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieCrewColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieGenreColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieSearchSuggestionsColumns;
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowCharacterColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowGenreColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowSearchSuggestionsColumns;
import net.simonvt.cathode.provider.generated.CathodeDatabase;
import net.simonvt.cathode.util.DateUtils;
import net.simonvt.cathode.util.SqlIndex;
import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.ExecOnCreate;
import net.simonvt.schematic.annotation.OnUpgrade;
import net.simonvt.schematic.annotation.Table;

@Database(className = "CathodeDatabase",
    packageName = "net.simonvt.cathode.provider.generated",
    fileName = "cathode.db",
    version = DatabaseSchematic.DATABASE_VERSION)
public final class DatabaseSchematic {

  private DatabaseSchematic() {
  }

  static final int DATABASE_VERSION = 17;

  public interface Joins {
    String SHOWS_UNWATCHED = "LEFT OUTER JOIN episodes ON episodes._id=(SELECT episodes._id FROM"
        + " episodes WHERE episodes.watched=0 AND episodes.showId=shows._id AND episodes.season<>0"
        + " AND episodes.needsSync=0"
        + " AND episodes.episodeFirstAired>"
        + DateUtils.YEAR_IN_MILLIS
        // TODO: Find better solution
        + " ORDER BY episodes.season ASC, episodes.episode ASC LIMIT 1)";

    String SHOWS_UPCOMING = "LEFT OUTER JOIN "
        + TABLE_EPISODES
        + " ON "
        + TABLE_EPISODES
        + "."
        + EpisodeColumns.ID
        + "="
        + "("
        + "SELECT _id "
        + "FROM episodes "
        + "JOIN ("
        + "SELECT season, episode "
        + "FROM episodes "
        + "WHERE watched=1 AND showId=shows._id "
        + "ORDER BY season DESC, episode DESC LIMIT 1"
        + ") AS ep2 "
        + "WHERE episodes.watched=0 AND episodes.showId=shows._id"
        + " AND episodes.needsSync=0"
        + " AND (episodes.season>ep2.season "
        + "OR (episodes.season=ep2.season AND episodes.episode>ep2.episode)) "
        + "ORDER BY episodes.season ASC, episodes.episode ASC LIMIT 1"
        + ")";

    String SHOWS_UNCOLLECTED = "LEFT OUTER JOIN episodes ON episodes._id=(SELECT episodes._id FROM"
        + " episodes WHERE episodes.inCollection=0 AND episodes.showId=shows._id"
        + " AND episodes.season<>0"
        + " AND episodes.needsSync=0"
        // TODO: Find better solution
        + " AND episodes.episodeFirstAired>"
        + DateUtils.YEAR_IN_MILLIS
        + " AND episodes.needsSync=0"
        + " ORDER BY episodes.season ASC, episodes.episode ASC LIMIT 1)";

    String SHOWS_WITH_WATCHING =
        "LEFT OUTER JOIN episodes ON episodes._id=(SELECT episodes._id FROM"
            + " episodes WHERE (episodes.watching=1 OR episodes.checkedIn=1)"
            + " AND episodes.showId=shows._id"
            + " AND episodes.needsSync=0"
            + " AND episodes.episodeFirstAired>"
            + DateUtils.YEAR_IN_MILLIS
            + " ORDER BY episodes.season ASC, episodes.episode ASC LIMIT 1)";

    String MOVIE_CAST_PERSON = "JOIN "
        + Tables.PEOPLE
        + " AS "
        + Tables.PEOPLE
        + " ON "
        + Tables.PEOPLE
        + "."
        + PersonColumns.ID
        + "="
        + Tables.MOVIE_CAST
        + "."
        + MovieCastColumns.PERSON_ID;

    String SHOW_CAST_PERSON = "JOIN "
        + Tables.PEOPLE
        + " AS "
        + Tables.PEOPLE
        + " ON "
        + Tables.PEOPLE
        + "."
        + PersonColumns.ID
        + "="
        + Tables.SHOW_CHARACTERS
        + "."
        + ShowCharacterColumns.PERSON_ID;

    String EPISODES_WITH_SHOW_TITLE = "JOIN "
        + Tables.SHOWS
        + " AS "
        + Tables.SHOWS
        + " ON "
        + Tables.SHOWS
        + "."
        + ShowColumns.ID
        + "="
        + Tables.EPISODES
        + "."
        + EpisodeColumns.SHOW_ID;

    String LIST_SHOWS = "LEFT JOIN " + Tables.SHOWS
        + " ON " + ListItemColumns.ITEM_TYPE + "=" + ListItemColumns.Type.SHOW
        +  " AND " + Tables.LIST_ITEMS + "." + ListItemColumns.ITEM_ID
        + "=" + Tables.SHOWS + "." + ShowColumns.ID;

    String LIST_SEASONS = "LEFT JOIN " + Tables.SEASONS
        + " ON " + ListItemColumns.ITEM_TYPE + "=" + ListItemColumns.Type.SEASON
        +  " AND " + Tables.LIST_ITEMS + "." + ListItemColumns.ITEM_ID
        + "=" + Tables.SEASONS + "." + SeasonColumns.ID;

    String LIST_EPISODES = "LEFT JOIN " + Tables.EPISODES
        + " ON " + ListItemColumns.ITEM_TYPE + "=" + ListItemColumns.Type.EPISODE
        +  " AND " + Tables.LIST_ITEMS + "." + ListItemColumns.ITEM_ID
        + "=" + Tables.EPISODES + "." + EpisodeColumns.ID;

    String LIST_MOVIES = "LEFT JOIN " + Tables.MOVIES
        + " ON " + ListItemColumns.ITEM_TYPE + "=" + ListItemColumns.Type.MOVIE
        +  " AND " + Tables.LIST_ITEMS + "." + ListItemColumns.ITEM_ID
        + "=" + Tables.MOVIES + "." + MovieColumns.ID;

    String LIST_PEOPLE = "LEFT JOIN " + Tables.PEOPLE
        + " ON " + ListItemColumns.ITEM_TYPE + "=" + ListItemColumns.Type.PERSON
        +  " AND " + Tables.LIST_ITEMS + "." + ListItemColumns.ITEM_ID
        + "=" + Tables.PEOPLE + "." + PersonColumns.ID;

    String LIST = LIST_SHOWS
        + " "
        + LIST_SEASONS
        + " "
        + LIST_EPISODES
        + " "
        + LIST_MOVIES
        + " "
        + LIST_PEOPLE;
  }

  public interface Tables {

    String SHOWS = "shows";
    String SHOW_GENRES = "showGenres";
    String SEASONS = "seasons";
    String EPISODES = "episodes";
    String SHOW_CHARACTERS = "showCharacters";

    String MOVIES = "movies";
    String MOVIE_GENRES = "movieGenres";

    String MOVIE_CAST = "movieCast";
    String MOVIE_CREW = "movieCrew";

    String PEOPLE = "people";

    String SHOW_SEARCH_SUGGESTIONS = "showSearchSuggestions";
    String MOVIE_SEARCH_SUGGESTIONS = "movieSearchSuggestions";

    String LISTS = "lists";
    String LIST_ITEMS = "listItems";
  }

  interface References {

    String SHOW_ID = "REFERENCES " + Tables.SHOWS + "(" + ShowColumns.ID + ")";
    String SEASON_ID = "REFERENCES " + Tables.SEASONS + "(" + SeasonColumns.ID + ")";
    String MOVIE_ID = "REFERENCES " + Tables.MOVIES + "(" + MovieColumns.ID + ")";
  }

  interface Trigger {
    String EPISODE_UPDATE_AIRED_NAME = "episodeUpdateAired";
    String EPISODE_UPDATE_WATCHED_NAME = "episodeUpdateWatched";
    String EPISODE_UPDATE_COLLECTED_NAME = "episodeUpdateCollected";
    String EPISODE_INSERT_NAME = "episodeInsert";
    String SHOW_DELETE_NAME = "showDelete";
    String SEASON_DELETE_NAME = "seasonDelete";
    String MOVIE_DELETE_NAME = "movieDelete";

    String SHOW_UPDATE_NAME = "showUpdate";
    String SEASON_UPDATE_NAME = "seasonUpdate";
    String EPISODE_UPDATE_NAME = "episodeUpdate";

    String MOVIES_UPDATE_NAME = "moviesUpdate";

    String PEOPLE_UPDATE_NAME = "peopleUpdate";

    String LIST_DELETE_NAME = "listDelete";
    String LIST_UPDATE_NAME = "listUpdate";
    String LISTITEM_UPDATE_NAME = "listItemUpdate";

    String SEASONS_UPDATE_WATCHED = "UPDATE "
        + Tables.SEASONS
        + " SET "
        + SeasonColumns.WATCHED_COUNT
        + "=(SELECT COUNT(*) FROM "
        + Tables.EPISODES
        + " WHERE "
        + Tables.EPISODES
        + "."
        + EpisodeColumns.SEASON_ID
        + "=NEW."
        + EpisodeColumns.SEASON_ID
        + " AND "
        + Tables.EPISODES
        + "."
        + EpisodeColumns.WATCHED
        + "=1"
        + " AND episodes.needsSync=0"
        + " AND "
        + Tables.EPISODES
        + "."
        + EpisodeColumns.SEASON
        + ">0)"
        + " WHERE "
        + Tables.SEASONS
        + "."
        + SeasonColumns.ID
        + "=NEW."
        + EpisodeColumns.SEASON_ID
        + ";";

    String SEASONS_UPDATE_COLLECTED = "UPDATE "
        + Tables.SEASONS
        + " SET "
        + SeasonColumns.IN_COLLECTION_COUNT
        + "=(SELECT COUNT(*) FROM "
        + Tables.EPISODES
        + " WHERE "
        + Tables.EPISODES
        + "."
        + EpisodeColumns.SEASON_ID
        + "=NEW."
        + EpisodeColumns.SEASON_ID
        + " AND "
        + Tables.EPISODES
        + "."
        + EpisodeColumns.IN_COLLECTION
        + "=1"
        + " AND episodes.needsSync=0"
        + " AND "
        + Tables.EPISODES
        + "."
        + EpisodeColumns.SEASON
        + ">0)"
        + " WHERE "
        + Tables.SEASONS
        + "."
        + SeasonColumns.ID
        + "=NEW."
        + EpisodeColumns.SEASON_ID
        + ";";

    String SEASONS_UPDATE_AIRDATE = "UPDATE "
        + Tables.SEASONS
        + " SET "
        + SeasonColumns.AIRDATE_COUNT
        + "=(SELECT COUNT(*) FROM "
        + Tables.EPISODES
        + " WHERE "
        + Tables.EPISODES
        + "."
        + EpisodeColumns.SEASON_ID
        + "=NEW."
        + EpisodeColumns.SEASON_ID
        + " AND "
        + Tables.EPISODES
        + "."
        + EpisodeColumns.FIRST_AIRED
        + ">"
        + DateUtils.YEAR_IN_MILLIS
        + " AND episodes.needsSync=0"
        + " AND "
        + Tables.EPISODES
        + "."
        + EpisodeColumns.SEASON
        + ">0)"
        + " WHERE "
        + Tables.SEASONS
        + "."
        + SeasonColumns.ID
        + "=NEW."
        + EpisodeColumns.SEASON_ID
        + ";";

    String SHOWS_UPDATE_WATCHED = "UPDATE "
        + Tables.SHOWS
        + " SET "
        + ShowColumns.WATCHED_COUNT
        + "=(SELECT COUNT(*) FROM "
        + Tables.EPISODES
        + " WHERE "
        + Tables.EPISODES
        + "."
        + EpisodeColumns.SHOW_ID
        + "=NEW."
        + EpisodeColumns.SHOW_ID
        + " AND "
        + Tables.EPISODES
        + "."
        + EpisodeColumns.WATCHED
        + "=1"
        + " AND episodes.needsSync=0"
        + " AND "
        + Tables.EPISODES
        + "."
        + EpisodeColumns.SEASON
        + ">0)"
        + " WHERE "
        + Tables.SHOWS
        + "."
        + ShowColumns.ID
        + "=NEW."
        + EpisodeColumns.SHOW_ID
        + ";";

    String SHOWS_UPDATE_COLLECTED = "UPDATE "
        + Tables.SHOWS
        + " SET "
        + ShowColumns.IN_COLLECTION_COUNT
        + "=(SELECT COUNT(*) FROM "
        + Tables.EPISODES
        + " WHERE "
        + Tables.EPISODES
        + "."
        + EpisodeColumns.SHOW_ID
        + "=NEW."
        + EpisodeColumns.SHOW_ID
        + " AND "
        + Tables.EPISODES
        + "."
        + EpisodeColumns.IN_COLLECTION
        + "=1"
        + " AND episodes.needsSync=0"
        + " AND "
        + Tables.EPISODES
        + "."
        + EpisodeColumns.SEASON
        + ">0)"
        + " WHERE "
        + Tables.SHOWS
        + "."
        + ShowColumns.ID
        + "=NEW."
        + EpisodeColumns.SHOW_ID
        + ";";

    String SHOWS_UPDATE_AIRDATE = "UPDATE "
        + Tables.SHOWS
        + " SET "
        + ShowColumns.AIRDATE_COUNT
        + "=(SELECT COUNT(*) FROM "
        + Tables.EPISODES
        + " WHERE "
        + Tables.EPISODES
        + "."
        + EpisodeColumns.SHOW_ID
        + "=NEW."
        + EpisodeColumns.SHOW_ID
        + " AND "
        + Tables.EPISODES
        + "."
        + EpisodeColumns.FIRST_AIRED
        + ">"
        + DateUtils.YEAR_IN_MILLIS
        + " AND episodes.needsSync=0"
        + " AND "
        + Tables.EPISODES
        + "."
        + EpisodeColumns.SEASON
        + ">0)"
        + " WHERE "
        + Tables.SHOWS
        + "."
        + ShowColumns.ID
        + "=NEW."
        + EpisodeColumns.SHOW_ID
        + ";";

    String SHOW_DELETE_SEASONS = "DELETE FROM "
        + Tables.SEASONS
        + " WHERE "
        + Tables.SEASONS
        + "."
        + SeasonColumns.SHOW_ID
        + "=OLD."
        + ShowColumns.ID
        + ";";

    String SHOW_DELETE_EPISODES = "DELETE FROM "
        + Tables.EPISODES
        + " WHERE "
        + Tables.EPISODES
        + "."
        + EpisodeColumns.SHOW_ID
        + "=OLD."
        + SeasonColumns.ID
        + ";";

    String SHOW_DELETE_GENRES = "DELETE FROM "
        + Tables.SHOW_GENRES
        + " WHERE "
        + Tables.SHOW_GENRES
        + "."
        + ShowGenreColumns.SHOW_ID
        + "=OLD."
        + ShowColumns.ID
        + ";";

    String SHOW_DELETE_CHARACTERS = "DELETE FROM "
        + Tables.SHOW_CHARACTERS
        + " WHERE "
        + Tables.SHOW_CHARACTERS
        + "."
        + ShowCharacterColumns.SHOW_ID
        + "=OLD."
        + ShowColumns.ID
        + ";";

    String SEASON_DELETE_EPISODES = "DELETE FROM "
        + Tables.EPISODES
        + " WHERE "
        + Tables.EPISODES
        + "."
        + EpisodeColumns.SEASON_ID
        + "=OLD."
        + SeasonColumns.ID
        + ";";

    String MOVIE_DELETE_GENRES = "DELETE FROM "
        + Tables.MOVIE_GENRES
        + " WHERE "
        + Tables.MOVIE_GENRES
        + "."
        + MovieGenreColumns.MOVIE_ID
        + "=OLD."
        + MovieColumns.ID
        + ";";

    String MOVIE_DELETE_CAST = "DELETE FROM "
        + Tables.MOVIE_CAST
        + " WHERE "
        + Tables.MOVIE_CAST
        + "."
        + MovieCastColumns.MOVIE_ID
        + "=OLD."
        + MovieColumns.ID
        + ";";

    String MOVIE_DELETE_CREW = "DELETE FROM "
        + Tables.MOVIE_CREW
        + " WHERE "
        + Tables.MOVIE_CREW
        + "."
        + MovieCrewColumns.MOVIE_ID
        + "=OLD."
        + MovieColumns.ID
        + ";";

    String TIME_MILLIS =
        "(strftime('%s','now') * 1000 + cast(substr(strftime('%f','now'),4,3) AS INTEGER))";

    String SHOW_UPDATE = "UPDATE " + Tables.SHOWS
        + " SET " + ShowColumns.LAST_MODIFIED + "=" + TIME_MILLIS
        + " WHERE " + ShowColumns.ID + "=old."   + ShowColumns.ID + ";";

    String SEASON_UPDATE = "UPDATE " + Tables.SEASONS
        + " SET " + SeasonColumns.LAST_MODIFIED + "=" + TIME_MILLIS
        + " WHERE " + SeasonColumns.ID + "=old."   + SeasonColumns.ID + ";";

    String EPISODE_UPDATE = "UPDATE " + Tables.EPISODES
        + " SET " + EpisodeColumns.LAST_MODIFIED + "=" + TIME_MILLIS
        + " WHERE " + EpisodeColumns.ID + "=old."   + EpisodeColumns.ID + ";";

    String MOVIES_UPDATE = "UPDATE " + Tables.MOVIES
        + " SET " + MovieColumns.LAST_MODIFIED + "=" + TIME_MILLIS
        + " WHERE " + MovieColumns.ID + "=old."   + MovieColumns.ID + ";";

    String PEOPLE_UPDATE = "UPDATE " + Tables.PEOPLE
        + " SET " + PersonColumns.LAST_MODIFIED + "=" + TIME_MILLIS
        + " WHERE " + PersonColumns.ID + "=old."   + PersonColumns.ID + ";";

    String LIST_DELETE = "DELETE FROM "
        + Tables.LIST_ITEMS
        + " WHERE "
        + Tables.LIST_ITEMS
        + "."
        + ListItemColumns.LIST_ID
        + "=OLD."
        + ListsColumns.ID
        + ";";

    String LISTS_UPDATE = "UPDATE " + Tables.LISTS
        + " SET " + ListsColumns.LAST_MODIFIED + "=" + TIME_MILLIS
        + " WHERE " + ListsColumns.ID + "=old."   + ListsColumns.ID + ";";

    String LISTITEM_UPDATE = "UPDATE " + Tables.LIST_ITEMS
        + " SET " + ListItemColumns.LAST_MODIFIED + "=" + TIME_MILLIS
        + " WHERE " + ListItemColumns.ID + "=old."   + ListItemColumns.ID + ";";
  }

  @Table(ShowColumns.class) public static final String TABLE_SHOWS = Tables.SHOWS;

  @Table(ShowGenreColumns.class) public static final String TABLE_SHOW_GENRES = Tables.SHOW_GENRES;

  @Table(SeasonColumns.class) public static final String TABLE_SEASONS = Tables.SEASONS;

  @Table(EpisodeColumns.class) public static final String TABLE_EPISODES = Tables.EPISODES;

  @Table(ShowCharacterColumns.class) public static final String TABLE_SHOW_CHARACTERS =
      Tables.SHOW_CHARACTERS;

  @Table(MovieColumns.class) public static final String TABLE_MOVIES = Tables.MOVIES;

  @Table(MovieGenreColumns.class) public static final String TABLE_MOVIE_GENRES =
      Tables.MOVIE_GENRES;

  @Table(MovieCastColumns.class) public static final String TABLE_MOVIE_CAST = Tables.MOVIE_CAST;

  @Table(MovieCrewColumns.class) public static final String TABLE_MOVIE_CREW = Tables.MOVIE_CREW;

  @Table(PersonColumns.class) public static final String TABLE_PEOPLE = Tables.PEOPLE;

  @Table(ShowSearchSuggestionsColumns.class) public static final String
      TABLE_SHOW_SEARCH_SUGGESTIONS = Tables.SHOW_SEARCH_SUGGESTIONS;

  @Table(MovieSearchSuggestionsColumns.class) public static final String
      TABLE_MOVIE_SEARCH_SUGGESTIONS = Tables.MOVIE_SEARCH_SUGGESTIONS;

  @Table(ListsColumns.class) public static final String TABLE_LISTS = Tables.LISTS;

  @Table(ListItemColumns.class) public static final String TABLE_LIST_ITEMS =
      Tables.LIST_ITEMS;

  @ExecOnCreate
  public static final String TRIGGER_EPISODE_UPDATE_AIRED = "CREATE TRIGGER "
      + Trigger.EPISODE_UPDATE_AIRED_NAME
      + " AFTER UPDATE OF "
      + EpisodeColumns.FIRST_AIRED
      + ","
      + EpisodeColumns.NEEDS_SYNC
      + " ON "
      + TABLE_EPISODES
      + " BEGIN "
      + Trigger.SEASONS_UPDATE_AIRDATE
      + Trigger.SHOWS_UPDATE_AIRDATE
      + " END;";

  @ExecOnCreate
  public static final String TRIGGER_EPISODE_UPDATE_WATCHED = "CREATE TRIGGER "
      + Trigger.EPISODE_UPDATE_WATCHED_NAME
      + " AFTER UPDATE OF "
      + EpisodeColumns.WATCHED
      + ","
      + EpisodeColumns.NEEDS_SYNC
      + " ON "
      + TABLE_EPISODES
      + " BEGIN "
      + Trigger.SEASONS_UPDATE_WATCHED
      + Trigger.SHOWS_UPDATE_WATCHED
      + " END;";

  @ExecOnCreate
  public static final String TRIGGER_EPISODE_UPDATE_COLLECTED = "CREATE TRIGGER "
      + Trigger.EPISODE_UPDATE_COLLECTED_NAME
      + " AFTER UPDATE OF "
      + EpisodeColumns.IN_COLLECTION
      + ","
      + EpisodeColumns.NEEDS_SYNC
      + " ON "
      + TABLE_EPISODES
      + " BEGIN "
      + Trigger.SEASONS_UPDATE_COLLECTED
      + Trigger.SHOWS_UPDATE_COLLECTED
      + " END;";

  @ExecOnCreate
  public static final String TRIGGER_EPISODE_INSERT = "CREATE TRIGGER "
      + Trigger.EPISODE_INSERT_NAME
      + " AFTER INSERT ON "
      + TABLE_EPISODES
      + " BEGIN "
      + Trigger.SEASONS_UPDATE_WATCHED
      + Trigger.SEASONS_UPDATE_AIRDATE
      + Trigger.SEASONS_UPDATE_COLLECTED
      + Trigger.SHOWS_UPDATE_WATCHED
      + Trigger.SHOWS_UPDATE_AIRDATE
      + Trigger.SHOWS_UPDATE_COLLECTED
      + " END;";

  @ExecOnCreate public static final String TRIGGER_SHOW_DELETE = "CREATE TRIGGER "
      + Trigger.SHOW_DELETE_NAME
      + " AFTER DELETE ON "
      + Tables.SHOWS
      + " BEGIN "
      + Trigger.SHOW_DELETE_SEASONS
      + Trigger.SHOW_DELETE_GENRES
      + Trigger.SHOW_DELETE_CHARACTERS
      + " END;";

  @ExecOnCreate public static final String TRIGGER_SEASON_DELETE = "CREATE TRIGGER "
      + Trigger.SEASON_DELETE_NAME
      + " AFTER DELETE ON "
      + Tables.SEASONS
      + " BEGIN "
      + Trigger.SEASON_DELETE_EPISODES
      + " END";

  @ExecOnCreate public static final String TRIGGER_MOVIE_DELETE = "CREATE TRIGGER "
      + Trigger.MOVIE_DELETE_NAME
      + " AFTER DELETE ON "
      + Tables.MOVIES
      + " BEGIN "
      + Trigger.MOVIE_DELETE_GENRES
      + Trigger.MOVIE_DELETE_CAST
      + Trigger.MOVIE_DELETE_CREW
      + " END;";

  @ExecOnCreate public static final String TRIGGER_SHOW_UPDATE = "CREATE TRIGGER "
      + Trigger.SHOW_UPDATE_NAME
      + " AFTER UPDATE ON "
      + Tables.SHOWS
      + " FOR EACH ROW BEGIN "
      + Trigger.SHOW_UPDATE
      + " END";

  @ExecOnCreate public static final String TRIGGER_SEASON_UPDATE = "CREATE TRIGGER "
      + Trigger.SEASON_UPDATE_NAME
      + " AFTER UPDATE ON "
      + Tables.SEASONS
      + " FOR EACH ROW BEGIN "
      + Trigger.SEASON_UPDATE
      + " END";

  @ExecOnCreate public static final String TRIGGER_EPISODE_UPDATE = "CREATE TRIGGER "
      + Trigger.EPISODE_UPDATE_NAME
      + " AFTER UPDATE ON "
      + Tables.EPISODES
      + " FOR EACH ROW BEGIN "
      + Trigger.EPISODE_UPDATE
      + " END";

  @ExecOnCreate public static final String TRIGGER_MOVIES_UPDATE = "CREATE TRIGGER "
      + Trigger.MOVIES_UPDATE_NAME
      + " AFTER UPDATE ON "
      + Tables.MOVIES
      + " FOR EACH ROW BEGIN "
      + Trigger.MOVIES_UPDATE
      + " END";

  @ExecOnCreate public static final String TRIGGER_PEOPLE_UPDATE = "CREATE TRIGGER "
      + Trigger.PEOPLE_UPDATE_NAME
      + " AFTER UPDATE ON "
      + Tables.PEOPLE
      + " FOR EACH ROW BEGIN "
      + Trigger.PEOPLE_UPDATE
      + " END";

  @ExecOnCreate public static final String TRIGGER_LIST_DELETE = "CREATE TRIGGER "
      + Trigger.LIST_DELETE_NAME
      + " AFTER DELETE ON "
      + Tables.LISTS
      + " BEGIN "
      + Trigger.LIST_DELETE
      + " END;";

  @ExecOnCreate public static final String TRIGGER_LIST_UPDATE = "CREATE TRIGGER "
      + Trigger.LIST_UPDATE_NAME
      + " AFTER UPDATE ON "
      + Tables.LISTS
      + " FOR EACH ROW BEGIN "
      + Trigger.LISTS_UPDATE
      + " END";

  @ExecOnCreate public static final String TRIGGER_LISTITEM_UPDATE = "CREATE TRIGGER "
      + Trigger.LISTITEM_UPDATE_NAME
      + " AFTER UPDATE ON "
      + Tables.LIST_ITEMS
      + " FOR EACH ROW BEGIN "
      + Trigger.LISTITEM_UPDATE
      + " END";

  @ExecOnCreate public static final String INDEX_CHARACTERS_SHOW_ID =
      SqlIndex.index("characterShowId")
          .ifNotExists()
          .onTable(Tables.SHOW_CHARACTERS)
          .forColumns(ShowCharacterColumns.SHOW_ID)
          .build();

  @ExecOnCreate public static final String INDEX_SEASON_SHOW_ID =
      SqlIndex.index("seasonShowId")
          .ifNotExists()
          .onTable(Tables.SEASONS)
          .forColumns(SeasonColumns.SHOW_ID)
          .build();

  @ExecOnCreate public static final String INDEX_GENRE_SHOW_ID =
      SqlIndex.index("genreShowId")
          .ifNotExists()
          .onTable(Tables.SHOW_GENRES)
          .forColumns(ShowGenreColumns.SHOW_ID)
          .build();

  @ExecOnCreate public static final String INDEX_CAST_MOVIE_ID =
      SqlIndex.index("castMovieId")
          .ifNotExists()
          .onTable(Tables.MOVIE_CAST)
          .forColumns(MovieCastColumns.MOVIE_ID)
          .build();

  @ExecOnCreate public static final String INDEX_CREW_MOVIE_ID =
      SqlIndex.index("crewMovieId")
          .ifNotExists()
          .onTable(Tables.MOVIE_CREW)
          .forColumns(MovieCrewColumns.MOVIE_ID)
          .build();

  @ExecOnCreate public static final String INDEX_GENRE_MOVIE_ID =
      SqlIndex.index("genreMovieId")
          .ifNotExists()
          .onTable(Tables.MOVIE_GENRES)
          .forColumns(MovieGenreColumns.MOVIE_ID)
          .build();

  @ExecOnCreate public static final String INDEX_EPISODES_SHOW_ID =
      SqlIndex.index("episodesShowId")
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

  @OnUpgrade public static void onUpgrade(Context context, SQLiteDatabase db, int oldVersion,
      int newVersion) {
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
      db.execSQL("ALTER TABLE " + Tables.SHOWS
          + " ADD COLUMN " + LastModifiedColumns.LAST_MODIFIED + " INTEGER DEFAULT 0");
      db.execSQL("ALTER TABLE " + Tables.SEASONS
          + " ADD COLUMN " + LastModifiedColumns.LAST_MODIFIED + " INTEGER DEFAULT 0");
      db.execSQL("ALTER TABLE " + Tables.EPISODES
          + " ADD COLUMN " + LastModifiedColumns.LAST_MODIFIED + " INTEGER DEFAULT 0");
      db.execSQL("ALTER TABLE " + Tables.SHOW_CHARACTERS
          + " ADD COLUMN " + LastModifiedColumns.LAST_MODIFIED + " INTEGER DEFAULT 0");

      db.execSQL("ALTER TABLE " + Tables.MOVIES
          + " ADD COLUMN " + LastModifiedColumns.LAST_MODIFIED + " INTEGER DEFAULT 0");
      db.execSQL("ALTER TABLE " + Tables.MOVIE_CAST
          + " ADD COLUMN " + LastModifiedColumns.LAST_MODIFIED + " INTEGER DEFAULT 0");
      db.execSQL("ALTER TABLE " + Tables.MOVIE_CREW
          + " ADD COLUMN " + LastModifiedColumns.LAST_MODIFIED + " INTEGER DEFAULT 0");

      db.execSQL("ALTER TABLE " + Tables.PEOPLE
          + " ADD COLUMN " + LastModifiedColumns.LAST_MODIFIED + " INTEGER DEFAULT 0");

      db.execSQL(TRIGGER_SHOW_UPDATE);
      db.execSQL(TRIGGER_SEASON_UPDATE);
      db.execSQL(TRIGGER_EPISODE_UPDATE);
      db.execSQL(TRIGGER_MOVIES_UPDATE);
      db.execSQL(TRIGGER_PEOPLE_UPDATE);
    }

    if (oldVersion < 14) {
      db.execSQL(CathodeDatabase.TABLE_LISTS);
      db.execSQL(CathodeDatabase.TABLE_LIST_ITEMS);
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
      db.execSQL("ALTER TABLE " + Tables.SHOWS
          + " ADD COLUMN " + HiddenColumns.HIDDEN_CALENDAR + " INTEGER DEFAULT 0");
      db.execSQL("ALTER TABLE " + Tables.SHOWS
          + " ADD COLUMN " + HiddenColumns.HIDDEN_COLLECTED + " INTEGER DEFAULT 0");
      db.execSQL("ALTER TABLE " + Tables.SHOWS
          + " ADD COLUMN " + HiddenColumns.HIDDEN_WATCHED + " INTEGER DEFAULT 0");
      db.execSQL("ALTER TABLE " + Tables.SHOWS
          + " ADD COLUMN " + HiddenColumns.HIDDEN_RECOMMENDATIONS + " INTEGER DEFAULT 0");

      db.execSQL("ALTER TABLE " + Tables.SEASONS
          + " ADD COLUMN " + HiddenColumns.HIDDEN_CALENDAR + " INTEGER DEFAULT 0");
      db.execSQL("ALTER TABLE " + Tables.SEASONS
          + " ADD COLUMN " + HiddenColumns.HIDDEN_COLLECTED + " INTEGER DEFAULT 0");
      db.execSQL("ALTER TABLE " + Tables.SEASONS
          + " ADD COLUMN " + HiddenColumns.HIDDEN_WATCHED + " INTEGER DEFAULT 0");
      db.execSQL("ALTER TABLE " + Tables.SEASONS
          + " ADD COLUMN " + HiddenColumns.HIDDEN_RECOMMENDATIONS + " INTEGER DEFAULT 0");

      db.execSQL("ALTER TABLE " + Tables.MOVIES
          + " ADD COLUMN " + HiddenColumns.HIDDEN_CALENDAR + " INTEGER DEFAULT 0");
      db.execSQL("ALTER TABLE " + Tables.MOVIES
          + " ADD COLUMN " + HiddenColumns.HIDDEN_COLLECTED + " INTEGER DEFAULT 0");
      db.execSQL("ALTER TABLE " + Tables.MOVIES
          + " ADD COLUMN " + HiddenColumns.HIDDEN_WATCHED + " INTEGER DEFAULT 0");
      db.execSQL("ALTER TABLE " + Tables.MOVIES
          + " ADD COLUMN " + HiddenColumns.HIDDEN_RECOMMENDATIONS + " INTEGER DEFAULT 0");
    }

    if (oldVersion < 17) {
      db.execSQL(INDEX_EPISODES_SHOW_ID);
      db.execSQL(INDEX_EPISODES_SEASON_ID);
    }
  }
}
