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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import net.simonvt.cathode.database.DatabaseUtils;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieActorColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieDirectoryColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieGenreColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieProducerColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieSearchSuggestionsColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieTopWatcherColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieWriterColumns;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowActorColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowGenreColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowSearchSuggestionsColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowTopWatcherColumns;
import net.simonvt.cathode.provider.DatabaseContract.TopEpisodeColumns;
import net.simonvt.cathode.provider.generated.CathodeDatabase;
import net.simonvt.cathode.util.DateUtils;
import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.ExecOnCreate;
import net.simonvt.schematic.annotation.OnUpgrade;
import net.simonvt.schematic.annotation.Table;

@Database(className = DatabaseSchematic.DATABASE_NAME, fileName = "cathode.db",
    version = DatabaseSchematic.DATABASE_VERSION)
public final class DatabaseSchematic {

  private DatabaseSchematic() {
  }

  static final String DATABASE_NAME = "CathodeDatabase";

  static final int DATABASE_VERSION = 6;

  public interface Joins {
    String SHOWS_UNWATCHED = "LEFT OUTER JOIN episodes ON episodes._id=(SELECT episodes._id FROM"
        + " episodes WHERE episodes.watched=0 AND episodes.showId=shows._id AND episodes.season<>0"
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
        + "WHERE episodes.watched=0 AND episodes.showId=shows._id AND (episodes.season>ep2.season "
        + "OR (episodes.season=ep2.season AND episodes.episode>ep2.episode)) "
        + "ORDER BY episodes.season ASC, episodes.episode ASC LIMIT 1"
        + ")";

    String SHOWS_UNCOLLECTED = "LEFT OUTER JOIN episodes ON episodes._id=(SELECT episodes._id FROM"
        + " episodes WHERE episodes.inCollection=0 AND episodes.showId=shows._id"
        + " AND episodes.season<>0"
        + " AND episodes.episodeFirstAired>"
        + DateUtils.YEAR_IN_MILLIS
        // TODO: Find better solution
        + " ORDER BY episodes.season ASC, episodes.episode ASC LIMIT 1)";

    String SHOWS_WITH_WATCHING =
        "LEFT OUTER JOIN episodes ON episodes._id=(SELECT episodes._id FROM"
            + " episodes WHERE (episodes.watching=1 OR episodes.checkedIn=1)"
            + " AND episodes.showId=shows._id"
            + " AND episodes.episodeFirstAired>"
            + DateUtils.YEAR_IN_MILLIS
            + " ORDER BY episodes.season ASC, episodes.episode ASC LIMIT 1)";

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
  }

  public interface Tables {

    String SHOWS = "shows";

    String SHOW_TOP_WATCHERS = "showTopWatchers";
    String SHOW_TOP_EPISODES = "topEpisodes";
    String SHOW_ACTORS = "showActors";
    String SHOW_GENRES = "showGenres";
    String SEASONS = "seasons";
    String EPISODES = "episodes";
    String MOVIES = "movies";
    String MOVIE_GENRES = "movieGenres";
    String MOVIE_TOP_WATCHERS = "movieTopWatchers";
    String MOVIE_ACTORS = "movieActors";
    String MOVIE_DIRECTORS = "movieDirectors";
    String MOVIE_WRITERS = "movieWriters";
    String MOVIE_PRODUCERS = "movieProducers";

    String SHOW_SEARCH_SUGGESTIONS = "showSearchSuggestions";
    String MOVIE_SEARCH_SUGGESTIONS = "movieSearchSuggestions";
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
        + "=1 AND "
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
        + "=1 AND "
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
        + "=1 AND "
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
        + "=1 AND "
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
  }

  @Table(ShowColumns.class) public static final String TABLE_SHOWS = "shows";

  @Table(ShowTopWatcherColumns.class) public static final String TABLE_SHOW_TOP_WATCHERS =
      "showTopWatchers";

  @Table(TopEpisodeColumns.class) public static final String TABLE_SHOW_TOP_EPISODES =
      "topEpisodes";

  @Table(ShowActorColumns.class) public static final String TABLE_SHOW_ACTORS = "showActors";

  @Table(ShowGenreColumns.class) public static final String TABLE_SHOW_GENRES = "showGenres";

  @Table(SeasonColumns.class) public static final String TABLE_SEASONS = "seasons";

  @Table(EpisodeColumns.class) public static final String TABLE_EPISODES = "episodes";

  @Table(MovieColumns.class) public static final String TABLE_MOVIES = "movies";

  @Table(MovieGenreColumns.class) public static final String TABLE_MOVIE_GENRES = "movieGenres";

  @Table(MovieTopWatcherColumns.class) public static final String TABLE_MOVIE_TOP_WATCHERS =
      "movieTopWatchers";

  @Table(MovieActorColumns.class) public static final String TABLE_MOVIE_ACTORS = "movieActors";

  @Table(MovieDirectoryColumns.class) public static final String TABLE_MOVIE_DIRECTORS =
      "movieDirectors";

  @Table(MovieWriterColumns.class) public static final String TABLE_MOVIE_WRITERS = "movieWriters";

  @Table(MovieProducerColumns.class) public static final String TABLE_MOVIE_PRODUCERS =
      "movieProducers";

  @Table(ShowSearchSuggestionsColumns.class) public static final String
      TABLE_SHOW_SEARCH_SUGGESTIONS = "showSearchSuggestions";

  @Table(MovieSearchSuggestionsColumns.class) public static final String
      TABLE_MOVIE_SEARCH_SUGGESTIONS = "movieSearchSuggestions";

  @ExecOnCreate
  public static final String TRIGGER_EPISODE_UPDATE_AIRED = "CREATE TRIGGER "
      + Trigger.EPISODE_UPDATE_AIRED_NAME
      + " AFTER UPDATE OF "
      + EpisodeColumns.FIRST_AIRED
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

  @OnUpgrade public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    switch (oldVersion) {
      case 1:
        db.execSQL("ALTER TABLE "
            + Tables.SHOWS
            + " ADD COLUMN "
            + ShowColumns.HIDDEN
            + " INTEGER DEFAULT 0");
      case 2:
        db.execSQL("ALTER TABLE "
            + Tables.EPISODES
            + " ADD COLUMN "
            + EpisodeColumns.WATCHING
            + " INTEGER DEFAULT 0");
        db.execSQL("ALTER TABLE "
            + Tables.EPISODES
            + " ADD COLUMN "
            + EpisodeColumns.CHECKED_IN
            + " INTEGER DEFAULT 0");
        db.execSQL("ALTER TABLE "
            + Tables.MOVIES
            + " ADD COLUMN "
            + MovieColumns.WATCHING
            + " INTEGER DEFAULT 0");
        db.execSQL("ALTER TABLE "
            + Tables.MOVIES
            + " ADD COLUMN "
            + MovieColumns.CHECKED_IN
            + " INTEGER DEFAULT 0");
      case 3:
        db.execSQL("ALTER TABLE "
            + Tables.SHOWS
            + " ADD COLUMN "
            + ShowColumns.FULL_SYNC_REQUESTED
            + " INTEGER DEFAULT 0");
      case 4:
        db.execSQL("DROP TABLE IF EXISTS " + Tables.SHOW_ACTORS);
        db.execSQL(CathodeDatabase.TABLE_SHOW_ACTORS);
      case 5:
        db.execSQL("ALTER TABLE "
            + Tables.SHOWS
            + " ADD COLUMN "
            + ShowColumns.TITLE_NO_ARTICLE
            + " TEXT");

        Cursor shows = db.query(Tables.SHOWS, new String[] {
            ShowColumns.ID, ShowColumns.TITLE
        }, null, null, null, null, null);
        while (shows.moveToNext()) {
          final long showId = shows.getLong(shows.getColumnIndex(ShowColumns.ID));
          final String showTitle = shows.getString(shows.getColumnIndex(ShowColumns.TITLE));

          final String titleNoArticle = DatabaseUtils.removeLeadingArticle(showTitle);

          ContentValues cv = new ContentValues();
          cv.put(ShowColumns.TITLE_NO_ARTICLE, titleNoArticle);
          db.update(Tables.SHOWS, cv, ShowColumns.ID + "=?", new String[] {
              String.valueOf(showId),
          });
        }
        shows.close();
    }
  }

  private static void createTriggers(SQLiteDatabase db) {
    db.execSQL(TRIGGER_EPISODE_INSERT);
    db.execSQL(TRIGGER_EPISODE_UPDATE_AIRED);
    db.execSQL(TRIGGER_EPISODE_UPDATE_WATCHED);
    db.execSQL(TRIGGER_EPISODE_UPDATE_COLLECTED);
  }

  private static void dropTriggers(SQLiteDatabase db) {
    db.execSQL("DROP TRIGGER " + Trigger.EPISODE_UPDATE_AIRED_NAME);
    db.execSQL("DROP TRIGGER " + Trigger.EPISODE_UPDATE_WATCHED_NAME);
    db.execSQL("DROP TRIGGER " + Trigger.EPISODE_UPDATE_COLLECTED_NAME);
    db.execSQL("DROP TRIGGER " + Trigger.EPISODE_INSERT_NAME);
  }

  public static void clearUserData(Context context) {
    SQLiteOpenHelper oh = CathodeDatabase.getInstance(context);
    SQLiteDatabase db = oh.getWritableDatabase();
    db.beginTransaction();
    dropTriggers(db);

    ContentValues cv;

    cv = new ContentValues();
    cv.put(ShowColumns.WATCHED_COUNT, 0);
    cv.put(ShowColumns.IN_COLLECTION_COUNT, 0);
    cv.put(ShowColumns.IN_WATCHLIST_COUNT, 0);
    cv.put(ShowColumns.IN_WATCHLIST, false);
    cv.put(ShowColumns.HIDDEN, false);
    db.update(Tables.SHOWS, cv, null, null);

    cv = new ContentValues();
    cv.put(SeasonColumns.WATCHED_COUNT, 0);
    cv.put(SeasonColumns.IN_COLLECTION_COUNT, 0);
    cv.put(SeasonColumns.IN_WATCHLIST_COUNT, 0);
    db.update(Tables.SEASONS, cv, null, null);

    cv = new ContentValues();
    cv.put(EpisodeColumns.WATCHED, 0);
    cv.put(EpisodeColumns.PLAYS, 0);
    cv.put(EpisodeColumns.IN_WATCHLIST, 0);
    cv.put(EpisodeColumns.IN_COLLECTION, 0);
    db.update(Tables.EPISODES, cv, null, null);

    cv = new ContentValues();
    cv.put(MovieColumns.WATCHED, 0);
    cv.put(MovieColumns.IN_COLLECTION, 0);
    cv.put(MovieColumns.IN_WATCHLIST, 0);
    db.update(Tables.MOVIES, cv, null, null);

    createTriggers(db);

    db.setTransactionSuccessful();
    db.endTransaction();
  }
}
