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
import net.simonvt.cathode.provider.DatabaseContract.ShowCastColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowCrewColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowGenreColumns;
import net.simonvt.cathode.provider.DatabaseContract.UserColumns;
import net.simonvt.cathode.provider.entity.ItemTypeString;
import net.simonvt.cathode.provider.generated.CathodeDatabase;
import net.simonvt.cathode.provider.util.SqlIndex;
import net.simonvt.cathode.provider.util.SqlUtils;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.ExecOnCreate;
import net.simonvt.schematic.annotation.IfNotExists;
import net.simonvt.schematic.annotation.OnUpgrade;
import net.simonvt.schematic.annotation.Table;

@Database(className = "CathodeDatabase",
    packageName = "net.simonvt.cathode.provider.generated",
    fileName = "cathode.db",
    version = DatabaseSchematic.DATABASE_VERSION)
public final class DatabaseSchematic {

  private DatabaseSchematic() {
  }

  static final int DATABASE_VERSION = 46;

  public interface Tables {

    @Table(ShowColumns.class) @IfNotExists String SHOWS = "shows";
    @Table(ShowGenreColumns.class) @IfNotExists String SHOW_GENRES = "showGenres";
    @Table(SeasonColumns.class) @IfNotExists String SEASONS = "seasons";
    @Table(EpisodeColumns.class) @IfNotExists String EPISODES = "episodes";
    @Table(ShowCastColumns.class) @IfNotExists String SHOW_CAST = "showCast";
    @Table(ShowCrewColumns.class) @IfNotExists String SHOW_CREW = "showCrew";
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
            + Tables.EPISODES + "." + EpisodeColumns.WATCHED + "=1" + " AND " + Tables.EPISODES
            + "." + EpisodeColumns.SEASON + ">0)" + " WHERE " + Tables.SEASONS + "."
            + SeasonColumns.ID + "=NEW." + EpisodeColumns.SEASON_ID + ";";

    String SEASONS_UPDATE_COLLECTED =
        "UPDATE " + Tables.SEASONS + " SET " + SeasonColumns.IN_COLLECTION_COUNT
            + "=(SELECT COUNT(*) FROM " + Tables.EPISODES + " WHERE " + Tables.EPISODES + "."
            + EpisodeColumns.SEASON_ID + "=NEW." + EpisodeColumns.SEASON_ID + " AND "
            + Tables.EPISODES + "." + EpisodeColumns.IN_COLLECTION + "=1"
            + " AND " + Tables.EPISODES + "." + EpisodeColumns.SEASON + ">0)" + " WHERE "
            + Tables.SEASONS + "." + SeasonColumns.ID + "=NEW." + EpisodeColumns.SEASON_ID + ";";

    String SEASONS_UPDATE_AIRDATE =
        "UPDATE " + Tables.SEASONS + " SET " + SeasonColumns.AIRDATE_COUNT
            + "=(SELECT COUNT(*) FROM " + Tables.EPISODES + " WHERE " + Tables.EPISODES + "."
            + EpisodeColumns.SEASON_ID + "=NEW." + EpisodeColumns.SEASON_ID + " AND "
            + Tables.EPISODES + "." + EpisodeColumns.FIRST_AIRED + " IS NOT NULL "
            + " AND " + Tables.EPISODES + "." + EpisodeColumns.SEASON + ">0)" + " WHERE "
            + Tables.SEASONS + "." + SeasonColumns.ID + "=NEW." + EpisodeColumns.SEASON_ID + ";";

    String SHOWS_UPDATE_WATCHED =
        "UPDATE " + Tables.SHOWS + " SET " + ShowColumns.WATCHED_COUNT + "=(SELECT COUNT(*) FROM "
            + Tables.EPISODES + " WHERE " + Tables.EPISODES + "." + EpisodeColumns.SHOW_ID + "=NEW."
            + EpisodeColumns.SHOW_ID + " AND " + Tables.EPISODES + "." + EpisodeColumns.WATCHED
            + "=1" + " AND " + Tables.EPISODES + "." + EpisodeColumns.SEASON + ">0)" + " WHERE "
            + Tables.SHOWS + "." + ShowColumns.ID + "=NEW." + EpisodeColumns.SHOW_ID + ";";

    String SHOWS_UPDATE_COLLECTED =
        "UPDATE " + Tables.SHOWS + " SET " + ShowColumns.IN_COLLECTION_COUNT
            + "=(SELECT COUNT(*) FROM " + Tables.EPISODES + " WHERE " + Tables.EPISODES + "."
            + EpisodeColumns.SHOW_ID + "=NEW." + EpisodeColumns.SHOW_ID + " AND " + Tables.EPISODES
            + "." + EpisodeColumns.IN_COLLECTION + "=1" + " AND " + Tables.EPISODES + "."
            + EpisodeColumns.SEASON + ">0)" + " WHERE " + Tables.SHOWS + "." + ShowColumns.ID
            + "=NEW." + EpisodeColumns.SHOW_ID + ";";

    String SHOWS_UPDATE_AIRDATE =
        "UPDATE " + Tables.SHOWS + " SET " + ShowColumns.AIRDATE_COUNT + "=(SELECT COUNT(*) FROM "
            + Tables.EPISODES + " WHERE " + Tables.EPISODES + "." + EpisodeColumns.SHOW_ID + "=NEW."
            + EpisodeColumns.SHOW_ID + " AND " + Tables.EPISODES + "." + EpisodeColumns.FIRST_AIRED
            + " IS NOT NULL " + " AND " + Tables.EPISODES + "." + EpisodeColumns.SEASON + ">0)"
            + " WHERE " + Tables.SHOWS + "." + ShowColumns.ID + "=NEW." + EpisodeColumns.SHOW_ID
            + ";";

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
        + ")"
        + " WHERE "
        + Tables.SHOWS + "." + ShowColumns.ID + "=NEW." + EpisodeColumns.SHOW_ID
        + ";";

    String SHOW_DELETE_SEASONS =
        "DELETE FROM " + Tables.SEASONS + " WHERE " + Tables.SEASONS + "." + SeasonColumns.SHOW_ID
            + "=OLD." + ShowColumns.ID + ";";

    String SHOW_DELETE_GENRES =
        "DELETE FROM " + Tables.SHOW_GENRES + " WHERE " + Tables.SHOW_GENRES + "."
            + ShowGenreColumns.SHOW_ID + "=OLD." + ShowColumns.ID + ";";

    String SHOW_DELETE_CAST =
        "DELETE FROM " + Tables.SHOW_CAST + " WHERE " + Tables.SHOW_CAST + "."
            + ShowCastColumns.SHOW_ID + "=OLD." + ShowColumns.ID + ";";

    String SHOW_DELETE_CREW =
        "DELETE FROM " + Tables.SHOW_CREW + " WHERE " + Tables.SHOW_CREW + "."
            + ShowCrewColumns.SHOW_ID + "=OLD." + ShowColumns.ID + ";";

    String SHOW_DELETE_COMMENTS =
        "DELETE FROM " + Tables.COMMENTS + " WHERE " + Tables.COMMENTS + "."
            + CommentColumns.ITEM_TYPE + "='" + ItemTypeString.SHOW + "' AND "
            + Tables.COMMENTS + "." + CommentColumns.ITEM_ID + "=OLD." + ShowColumns.ID + ";";

    String SHOW_DELETE_RELATED =
        "DELETE FROM " + Tables.SHOW_RELATED + " WHERE " + Tables.SHOW_RELATED + "."
            + RelatedShowsColumns.SHOW_ID + "=OLD." + ShowColumns.ID + ";";

    String SEASON_DELETE_EPISODES =
        "DELETE FROM " + Tables.EPISODES + " WHERE " + Tables.EPISODES + "."
            + EpisodeColumns.SEASON_ID + "=OLD." + SeasonColumns.ID + ";";

    String EPISODE_DELETE_COMMENTS =
        "DELETE FROM " + Tables.COMMENTS + " WHERE " + Tables.COMMENTS + "."
            + CommentColumns.ITEM_TYPE + "='" + ItemTypeString.EPISODE + "' AND "
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
            + CommentColumns.ITEM_TYPE + "='" + ItemTypeString.MOVIE + "' AND "
            + Tables.COMMENTS + "." + CommentColumns.ITEM_ID + "=OLD." + MovieColumns.ID + ";";

    String MOVIE_DELETE_RELATED =
        "DELETE FROM " + Tables.MOVIE_RELATED + " WHERE " + Tables.MOVIE_RELATED + "."
            + RelatedMoviesColumns.MOVIE_ID + "=OLD." + MovieColumns.ID + ";";

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
          + EpisodeColumns.FIRST_AIRED + " ON " + Tables.EPISODES + " BEGIN "
          + Trigger.SEASONS_UPDATE_AIRDATE + Trigger.SHOWS_UPDATE_AIRDATE + " END;";

  @ExecOnCreate public static final String TRIGGER_EPISODE_UPDATE_WATCHED =
      "CREATE TRIGGER IF NOT EXISTS " + TriggerName.EPISODE_UPDATE_WATCHED + " AFTER UPDATE OF "
          + EpisodeColumns.WATCHED + " ON " + Tables.EPISODES + " BEGIN "
          + Trigger.SEASONS_UPDATE_WATCHED + Trigger.SHOWS_UPDATE_WATCHED + " END;";

  @ExecOnCreate public static final String TRIGGER_EPISODE_UPDATE_COLLECTED =
      "CREATE TRIGGER IF NOT EXISTS " + TriggerName.EPISODE_UPDATE_COLLECTED + " AFTER UPDATE OF "
          + EpisodeColumns.IN_COLLECTION + " ON " + Tables.EPISODES + " BEGIN "
          + Trigger.SEASONS_UPDATE_COLLECTED + Trigger.SHOWS_UPDATE_COLLECTED + " END;";

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
          + Trigger.SHOW_DELETE_CAST + Trigger.SHOW_DELETE_CREW + Trigger.SHOW_DELETE_COMMENTS
          + Trigger.SHOW_DELETE_RELATED + " END;";

  @ExecOnCreate public static final String TRIGGER_SEASON_DELETE =
      "CREATE TRIGGER IF NOT EXISTS " + TriggerName.SEASON_DELETE + " AFTER DELETE ON "
          + Tables.SEASONS + " BEGIN " + Trigger.SEASON_DELETE_EPISODES + " END";

  @ExecOnCreate public static final String TRIGGER_EPISODE_DELETE =
      "CREATE TRIGGER IF NOT EXISTS " + TriggerName.EPISODE_DELETE + " AFTER DELETE ON "
          + Tables.EPISODES + " BEGIN " + Trigger.EPISODE_DELETE_COMMENTS + " END";

  @ExecOnCreate public static final String TRIGGER_MOVIE_DELETE =
      "CREATE TRIGGER IF NOT EXISTS " + TriggerName.MOVIE_DELETE + " AFTER DELETE ON "
          + Tables.MOVIES + " BEGIN " + Trigger.MOVIE_DELETE_GENRES + Trigger.MOVIE_DELETE_CAST
          + Trigger.MOVIE_DELETE_CREW + Trigger.MOVIE_DELETE_COMMENTS + Trigger.MOVIE_DELETE_RELATED
          + " END;";

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

  @ExecOnCreate public static final String INDEX_CAST_SHOW_ID =
      SqlIndex.index("castShowId")
          .ifNotExists()
          .onTable(Tables.SHOW_CAST)
          .forColumns(ShowCastColumns.SHOW_ID)
          .build();

  @ExecOnCreate public static final String INDEX_CREW_SHOW_ID =
      SqlIndex.index("crewShowId")
          .ifNotExists()
          .onTable(Tables.SHOW_CREW)
          .forColumns(ShowCrewColumns.SHOW_ID)
          .build();

  @ExecOnCreate public static final String INDEX_RELATED_SHOW_ID =
      SqlIndex.index("relatedShowId")
          .ifNotExists()
          .onTable(Tables.SHOW_RELATED)
          .forColumns(RelatedShowsColumns.SHOW_ID)
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

  @ExecOnCreate public static final String INDEX_RELATED_MOVIE_ID = SqlIndex.index("relatedMovieId")
      .ifNotExists()
      .onTable(Tables.MOVIE_RELATED)
      .forColumns(RelatedMoviesColumns.MOVIE_ID)
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
    }

    if (oldVersion < 13) {
      CathodeDatabase.getInstance(context).onCreate(db);

      SqlUtils.createColumnIfNotExists(db, Tables.SHOWS, LastModifiedColumns.LAST_MODIFIED,
          DataType.Type.INTEGER, "0");
      SqlUtils.createColumnIfNotExists(db, Tables.SEASONS, LastModifiedColumns.LAST_MODIFIED,
          DataType.Type.INTEGER, "0");
      SqlUtils.createColumnIfNotExists(db, Tables.EPISODES, LastModifiedColumns.LAST_MODIFIED,
          DataType.Type.INTEGER, "0");
      SqlUtils.createColumnIfNotExists(db, Tables.SHOW_CAST,
          LastModifiedColumns.LAST_MODIFIED, DataType.Type.INTEGER, "0");

      SqlUtils.createColumnIfNotExists(db, Tables.MOVIES, LastModifiedColumns.LAST_MODIFIED,
          DataType.Type.INTEGER, "0");
      SqlUtils.createColumnIfNotExists(db, Tables.MOVIE_CAST, LastModifiedColumns.LAST_MODIFIED,
          DataType.Type.INTEGER, "0");
      SqlUtils.createColumnIfNotExists(db, Tables.MOVIE_CREW, LastModifiedColumns.LAST_MODIFIED,
          DataType.Type.INTEGER, "0");

      SqlUtils.createColumnIfNotExists(db, Tables.PEOPLE, LastModifiedColumns.LAST_MODIFIED,
          DataType.Type.INTEGER, "0");
    }

    if (oldVersion < 14) {
      db.execSQL(CathodeDatabase.LISTS);
      db.execSQL(CathodeDatabase.LIST_ITEMS);
    }

    if (oldVersion < 16) {
      Set<String> showColumns = SqlUtils.columns(db, Tables.SHOWS);
      if (!showColumns.contains(ShowColumns.HIDDEN_CALENDAR)) {
        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + ShowColumns.HIDDEN_CALENDAR
            + " INTEGER DEFAULT 0");
      }
      if (!showColumns.contains(ShowColumns.HIDDEN_COLLECTED)) {
        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + ShowColumns.HIDDEN_COLLECTED
            + " INTEGER DEFAULT 0");
      }
      if (!showColumns.contains(ShowColumns.HIDDEN_WATCHED)) {
        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + ShowColumns.HIDDEN_WATCHED
            + " INTEGER DEFAULT 0");
      }
      if (!showColumns.contains(ShowColumns.HIDDEN_RECOMMENDATIONS)) {
        db.execSQL(
            "ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + ShowColumns.HIDDEN_RECOMMENDATIONS
                + " INTEGER DEFAULT 0");
      }

      Set<String> seasonColumns = SqlUtils.columns(db, Tables.SEASONS);
      if (!seasonColumns.contains(SeasonColumns.HIDDEN_COLLECTED)) {
        db.execSQL("ALTER TABLE " + Tables.SEASONS + " ADD COLUMN " + SeasonColumns.HIDDEN_COLLECTED
            + " INTEGER DEFAULT 0");
      }
      if (!seasonColumns.contains(SeasonColumns.HIDDEN_WATCHED)) {
        db.execSQL("ALTER TABLE " + Tables.SEASONS + " ADD COLUMN " + SeasonColumns.HIDDEN_WATCHED
            + " INTEGER DEFAULT 0");
      }

      Set<String> movieColumns = SqlUtils.columns(db, Tables.MOVIES);
      if (!movieColumns.contains(MovieColumns.HIDDEN_CALENDAR)) {
        db.execSQL("ALTER TABLE " + Tables.MOVIES + " ADD COLUMN " + MovieColumns.HIDDEN_CALENDAR
            + " INTEGER DEFAULT 0");
      }
      if (!movieColumns.contains(MovieColumns.HIDDEN_RECOMMENDATIONS)) {
        db.execSQL(
            "ALTER TABLE " + Tables.MOVIES + " ADD COLUMN " + MovieColumns.HIDDEN_RECOMMENDATIONS
                + " INTEGER DEFAULT 0");
      }
    }

    if (oldVersion < 18) {
      db.execSQL(CathodeDatabase.COMMENTS);
      db.execSQL(CathodeDatabase.USERS);
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
      SqlUtils.createColumnIfNotExists(db, Tables.SHOWS, ShowColumns.LAST_CREDITS_SYNC,
          DataType.Type.INTEGER, "0");
      SqlUtils.createColumnIfNotExists(db, Tables.MOVIES, MovieColumns.LAST_SYNC,
          DataType.Type.INTEGER, "0");
      SqlUtils.createColumnIfNotExists(db, Tables.MOVIES, MovieColumns.LAST_CREDITS_SYNC,
          DataType.Type.INTEGER, "0");

      final long currentTime = System.currentTimeMillis();

      ContentValues values = new ContentValues();
      values.put(ShowColumns.LAST_SYNC, currentTime);
      values.put(ShowColumns.LAST_CREDITS_SYNC, currentTime);

      db.update(Tables.SHOWS, values, null, null);

      values = new ContentValues();
      values.put(MovieColumns.LAST_SYNC, currentTime);
      values.put(MovieColumns.LAST_CREDITS_SYNC, currentTime);

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

    if (oldVersion < 28) {
      db.execSQL("DROP TABLE IF EXISTS showCharacters");
      db.execSQL(CathodeDatabase.SHOW_CAST);
      db.execSQL(CathodeDatabase.SHOW_CREW);
    }

    if (oldVersion < 29) {
      SqlUtils.createColumnIfNotExists(db, Tables.PEOPLE, PersonColumns.SCREENSHOT, DataType.Type.TEXT,
          null);
    }

    if (oldVersion < 30) {
      SqlUtils.createColumnIfNotExists(db, Tables.PEOPLE, PersonColumns.LAST_SYNC,
          DataType.Type.INTEGER, "0");
    }

    if (oldVersion < 31) {
      SqlUtils.createColumnIfNotExists(db, Tables.SHOWS, ShowColumns.LAST_CREDITS_SYNC,
          DataType.Type.INTEGER, "0");
      SqlUtils.createColumnIfNotExists(db, Tables.MOVIES, MovieColumns.LAST_CREDITS_SYNC,
          DataType.Type.INTEGER, "0");
    }

    if (oldVersion < 34) {
      SqlUtils.createColumnIfNotExists(db, Tables.SHOWS, ShowColumns.BACKDROP,
          DataType.Type.TEXT, null);
      SqlUtils.createColumnIfNotExists(db, Tables.MOVIES, MovieColumns.BACKDROP,
          DataType.Type.TEXT, null);
      SqlUtils.createColumnIfNotExists(db, Tables.PEOPLE, PersonColumns.SCREENSHOT,
          DataType.Type.TEXT, null);
    }

    if (oldVersion < 35) {
      ContentValues showValues = new ContentValues();
      showValues.putNull(ShowColumns.BACKDROP);
      showValues.putNull(ShowColumns.POSTER);
      db.update(Tables.SHOWS, showValues, null, null);

      ContentValues episodeValues = new ContentValues();
      episodeValues.putNull(EpisodeColumns.SCREENSHOT);
      db.update(Tables.EPISODES, episodeValues, null, null);

      ContentValues movieValues = new ContentValues();
      movieValues.putNull(MovieColumns.BACKDROP);
      movieValues.putNull(MovieColumns.POSTER);
      db.update(Tables.MOVIES, movieValues, null, null);

      ContentValues personValues = new ContentValues();
      personValues.putNull(PersonColumns.SCREENSHOT);
      personValues.putNull(PersonColumns.HEADSHOT);
      db.update(Tables.PEOPLE, personValues, null, null);
    }

    if (oldVersion < 36) {
      db.execSQL("DROP TRIGGER IF EXISTS showDelete");
      db.execSQL("DROP TRIGGER IF EXISTS movieDelete");

      db.execSQL(TRIGGER_EPISODE_UPDATE_AIRED);
      db.execSQL(TRIGGER_EPISODE_UPDATE_WATCHED);
      db.execSQL(TRIGGER_EPISODE_UPDATE_COLLECTED);
      db.execSQL(TRIGGER_EPISODE_UPDATE_WATCHING);
      db.execSQL(TRIGGER_EPISODE_INSERT);
      db.execSQL(TRIGGER_SHOW_DELETE);
      db.execSQL(TRIGGER_SEASON_DELETE);
      db.execSQL(TRIGGER_EPISODE_DELETE);
      db.execSQL(TRIGGER_MOVIE_DELETE);
      db.execSQL(TRIGGER_SHOW_UPDATE);
      db.execSQL(TRIGGER_SEASON_UPDATE);
      db.execSQL(TRIGGER_EPISODE_UPDATE);
      db.execSQL(TRIGGER_MOVIES_UPDATE);
      db.execSQL(TRIGGER_PEOPLE_UPDATE);
      db.execSQL(TRIGGER_LIST_DELETE);
      db.execSQL(TRIGGER_LIST_UPDATE);
      db.execSQL(TRIGGER_LISTITEM_UPDATE);
      db.execSQL(TRIGGER_COMMENT_UPDATE);

      db.execSQL("DROP INDEX IF EXISTS characterShowId");

      db.execSQL(INDEX_CAST_SHOW_ID);
      db.execSQL(INDEX_CREW_SHOW_ID);
      db.execSQL(INDEX_RELATED_SHOW_ID);
      db.execSQL(INDEX_SEASON_SHOW_ID);
      db.execSQL(INDEX_GENRE_SHOW_ID);
      db.execSQL(INDEX_CAST_MOVIE_ID);
      db.execSQL(INDEX_CREW_MOVIE_ID);
      db.execSQL(INDEX_RELATED_MOVIE_ID);
      db.execSQL(INDEX_GENRE_MOVIE_ID);
      db.execSQL(INDEX_EPISODES_SHOW_ID);
      db.execSQL(INDEX_EPISODES_SEASON_ID);
    }

    if (oldVersion < 37) {
      SqlUtils.createColumnIfNotExists(db, Tables.EPISODES, EpisodeColumns.NOTIFICATION_DISMISSED,
          DataType.Type.INTEGER, "0");
    }

    if (oldVersion < 38) {
      SqlUtils.createColumnIfNotExists(db, Tables.EPISODES, EpisodeColumns.LAST_WATCHED_AT,
          DataType.Type.INTEGER, "0");
    }

    if (oldVersion < 39) {
      db.delete(Tables.LISTS, ListsColumns.TRAKT_ID + "<0", null);
    }

    if (oldVersion < 40) {
      SqlUtils.createColumnIfNotExists(db, Tables.SHOWS, ShowColumns.IMAGES_LAST_UPDATE,
          DataType.Type.INTEGER, "0");
      SqlUtils.createColumnIfNotExists(db, Tables.EPISODES, EpisodeColumns.IMAGES_LAST_UPDATE,
          DataType.Type.INTEGER, "0");
      SqlUtils.createColumnIfNotExists(db, Tables.MOVIES, MovieColumns.IMAGES_LAST_UPDATE,
          DataType.Type.INTEGER, "0");
      SqlUtils.createColumnIfNotExists(db, Tables.PEOPLE, PersonColumns.IMAGES_LAST_UPDATE,
          DataType.Type.INTEGER, "0");
    }

    if (oldVersion < 41) {
      SqlUtils.createColumnIfNotExists(db, Tables.SEASONS, SeasonColumns.IMAGES_LAST_UPDATE,
          DataType.Type.INTEGER, "0");
      SqlUtils.createColumnIfNotExists(db, Tables.SEASONS, SeasonColumns.POSTER, DataType.Type.TEXT,
          null);
    }

    if (oldVersion < 42) {
      db.execSQL("DROP TRIGGER IF EXISTS " + TriggerName.EPISODE_UPDATE_WATCHED);
      db.execSQL("DROP TRIGGER IF EXISTS " + TriggerName.EPISODE_UPDATE_COLLECTED);
      db.execSQL("DROP TRIGGER IF EXISTS " + TriggerName.EPISODE_UPDATE_AIRED);
      db.execSQL(TRIGGER_EPISODE_UPDATE_WATCHED);
      db.execSQL(TRIGGER_EPISODE_UPDATE_COLLECTED);
      db.execSQL(TRIGGER_EPISODE_UPDATE_AIRED);
    }

    if (oldVersion < 43) {
      SqlUtils.createColumnIfNotExists(db, Tables.COMMENTS, CommentColumns.LAST_SYNC,
          DataType.Type.INTEGER, "0");
    }

    if (oldVersion < 44) {
      db.execSQL("DROP TABLE IF EXISTS " + Tables.LIST_ITEMS);
      db.execSQL("DROP TABLE IF EXISTS " + Tables.COMMENTS);
      db.execSQL(CathodeDatabase.LIST_ITEMS);
      db.execSQL(CathodeDatabase.COMMENTS);

      db.execSQL("DROP TRIGGER IF EXISTS " + TriggerName.SHOW_DELETE);
      db.execSQL("DROP TRIGGER IF EXISTS " + TriggerName.EPISODE_DELETE);
      db.execSQL("DROP TRIGGER IF EXISTS " + TriggerName.MOVIE_DELETE);
      db.execSQL(TRIGGER_SHOW_DELETE);
      db.execSQL(TRIGGER_EPISODE_DELETE);
      db.execSQL(TRIGGER_MOVIE_DELETE);
    }

    if (oldVersion < 45) {
      SqlUtils.createColumnIfNotExists(db, Tables.SEASONS, SeasonColumns.FIRST_AIRED,
          DataType.Type.INTEGER, null);
    }

    if (oldVersion < 46) {
      db.execSQL("DROP TRIGGER IF EXISTS " + TriggerName.EPISODE_UPDATE_WATCHING);
      db.execSQL(TRIGGER_EPISODE_UPDATE_WATCHING);
    }
  }
}
