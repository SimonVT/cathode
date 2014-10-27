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
import android.net.Uri;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieCastColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieGenreColumns;
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowCharacterColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowGenreColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Joins;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.util.DateUtils;
import net.simonvt.schematic.annotation.ContentProvider;
import net.simonvt.schematic.annotation.ContentUri;
import net.simonvt.schematic.annotation.InexactContentUri;
import net.simonvt.schematic.annotation.InsertUri;
import net.simonvt.schematic.annotation.MapColumns;
import net.simonvt.schematic.annotation.NotificationUri;
import net.simonvt.schematic.annotation.NotifyDelete;
import net.simonvt.schematic.annotation.NotifyInsert;
import net.simonvt.schematic.annotation.NotifyUpdate;
import net.simonvt.schematic.annotation.TableEndpoint;
import net.simonvt.schematic.annotation.Where;

@ContentProvider(name = "CathodeProvider", authority = BuildConfig.PROVIDER_AUTHORITY,
    database = DatabaseSchematic.class)
public final class ProviderSchematic {

  private ProviderSchematic() {
  }

  static final Uri BASE_CONTENT_URI = Uri.parse("content://" + BuildConfig.PROVIDER_AUTHORITY);

  static Uri buildUri(String... paths) {
    Uri.Builder builder = BASE_CONTENT_URI.buildUpon();
    for (String path : paths) {
      builder.appendPath(path);
    }

    return builder.build();
  }

  interface Type {
    String SHOW = "vnd.android.cursor.dir/vnd.simonvt.cathode.show";
    String SHOW_ID = "vnd.android.cursor.item/vnd.simonvt.cathode.show";
    String SHOW_CHARACTERS = "vnd.android.cursor.dir/vnd.simonvt.cathode.showCharacters";
    String SHOW_GENRE = "vnd.android.cursor.dir/vnd.simonvt.cathode.showGenre";
    String SEASON = "vnd.android.cursor.dir/vnd.simonvt.cathode.season";
    String SEASON_ID = "vnd.android.cursor.item/vnd.simonvt.cathode.season";
    String EPISODE = "vnd.android.cursor.dir/vnd.simonvt.cathode.episode";
    String EPISODE_ID = "vnd.android.cursor.item/vnd.simonvt.cathode.episode";

    String MOVIE = "vnd.android.cursor.dir/vnd.simonvt.cathode.movie";
    String MOVIE_ID = "vnd.android.cursor.item/vnd.simonvt.cathode.movie";
    String MOVIE_GENRE = "vnd.android.cursor.dir/vnd.simonvt.cathode.movieGenre";
    String MOVIE_CAST = "vnd.android.cursor.dir/vnd.simonvt.cathode.movieCast";
    String MOVIE_CREW = "vnd.android.cursor.dir/vnd.simonvt.cathode.movieCrew";

    String SHOW_SEARCH = "vnd.android.cursor.dir/vnd.simonvt.cathode.showSearchSuggestions";
    String MOVIE_SEARCH = "vnd.android.cursor.dir/vnd.simonvt.cathode.movieSearchSuggestions";

    String PEOPLE = "vnd.android.cursor.dir/vnd.simonvt.cathode.people";
    String PERSON = "vnd.android.cursor.item/vnd.simonvt.cathode.person";
  }

  interface Path {
    String SHOWS = "shows";
    String UPCOMING = "upcoming";

    String SEASONS = "seasons";
    String EPISODES = "episodes";
    String SHOW_GENRES = "showGenres";
    String SHOW_CHARACTERS = "showCharacters";

    String FROM_SHOW = "fromShow";
    String FROM_SEASON = "fromSeason";

    String MOVIES = "movies";
    String MOVIE_GENRES = "movieGenres";
    String MOVIE_CAST = "movieCast";
    String MOVIE_CREW = "movieCrew";
    String MOVIE_DIRECTORY = "movieDirectory";
    String MOVIE_WRITERS = "movieWriters";
    String MOVIE_PRODUCERS = "movieProducers";
    String MOVIE_TOP_WATCHERS = "movieTopWatchers";

    String FROM_MOVIE = "fromMovie";

    String WATCHED = "watched";
    String COLLECTED = "collected";
    String WATCHLIST = "inWatchlist";

    String TRENDING = "trending";
    String RECOMMENDED = "recommended";
    String WATCHING = "watching";

    String SEARCH_SUGGESTIONS = "searchSuggestions";

    String PEOPLE = "people";
  }

  static Uri getBaseUri(Uri uri) {
    return BASE_CONTENT_URI.buildUpon().appendPath(uri.getPathSegments().get(0)).build();
  }

  @NotifyDelete()
  public static Uri[] defaultNotifyDelete(Uri uri) {
    return new Uri[] {
        getBaseUri(uri),
    };
  }

  @TableEndpoint(table = DatabaseSchematic.TABLE_SHOWS) public static class Shows {

    @MapColumns public static Map<String, String> mapColumns() {
      Map<String, String> map = new HashMap<String, String>();

      map.put(ShowColumns.AIRED_COUNT, getAiredQuery());
      map.put(ShowColumns.UNAIRED_COUNT, getUnairedQuery());
      map.put(ShowColumns.WATCHING, getWatchingQuery());
      map.put(ShowColumns.EPISODE_COUNT, getEpisodeCountQuery());
      map.put(ShowColumns.AIRED_AFTER_WATCHED, getAiredAfterWatched());
      map.put(Tables.SHOWS + "." + ShowColumns.AIRED_COUNT, getAiredQuery());
      map.put(Tables.SHOWS + "." + ShowColumns.UNAIRED_COUNT, getUnairedQuery());
      map.put(Tables.SHOWS + "." + ShowColumns.WATCHING, getWatchingQuery());
      map.put(Tables.SHOWS + "." + ShowColumns.EPISODE_COUNT, getEpisodeCountQuery());
      map.put(Tables.SHOWS + "." + ShowColumns.AIRED_AFTER_WATCHED, getAiredAfterWatched());

      return map;
    }

    @ContentUri(
        path = Path.SHOWS,
        type = Type.SHOW) @NotificationUri(
        paths = {
            Path.SHOWS + "/" + Path.WATCHED, Path.SHOWS + "/" + Path.COLLECTED,
            Path.SHOWS + "/" + Path.WATCHLIST, Path.SHOWS + "/" + Path.UPCOMING,
            Path.SHOWS + "/" + Path.TRENDING, Path.SHOWS + "/" + Path.RECOMMENDED
        })
    public static final Uri SHOWS = buildUri(Path.SHOWS);

    @InexactContentUri(
        path = Path.SHOWS + "/#",
        type = Type.SHOW_ID,
        name = "SHOW_ID",
        whereColumn = ShowColumns.ID,
        pathSegment = 1)
    public static Uri withId(long id) {
      return buildUri(Path.SHOWS, String.valueOf(id));
    }

    public static long getShowId(Uri uri) {
      return Long.valueOf(uri.getPathSegments().get(1));
    }

    @ContentUri(
        path = Path.SHOWS + "/" + Path.WATCHED,
        type = Type.SHOW,
        join = Joins.SHOWS_UNWATCHED,
        where = ShowColumns.WATCHED_COUNT + ">0")
    public static final Uri SHOWS_WATCHED = buildUri(Path.SHOWS, Path.WATCHED);

    @ContentUri(
        path = Path.SHOWS + "/" + Path.COLLECTED,
        type = Type.SHOW,
        join = Joins.SHOWS_UNCOLLECTED,
        where = ShowColumns.IN_COLLECTION_COUNT + ">0")
    public static final Uri SHOWS_COLLECTION = buildUri(Path.SHOWS, Path.COLLECTED);

    @ContentUri(
        path = Path.SHOWS + "/" + Path.WATCHLIST,
        type = Type.SHOW,
        where = ShowColumns.IN_WATCHLIST + "=1")
    public static final Uri SHOWS_WATCHLIST = buildUri(Path.SHOWS, Path.WATCHLIST);

    @ContentUri(
        path = Path.SHOWS + "/" + Path.UPCOMING,
        type = Type.SHOW,
        join = Joins.SHOWS_UPCOMING)
    public static final Uri SHOWS_UPCOMING = buildUri(Path.SHOWS, Path.UPCOMING);

    @Where(path = Path.SHOWS + "/" + Path.UPCOMING)
    public static String[] upcomingWhere() {
      return new String[] {
          ShowColumns.WATCHED_COUNT + ">0", getAiredAfterWatched() + ">0",
      };
    }

    @ContentUri(
        path = Path.SHOWS + "/" + Path.TRENDING,
        type = Type.SHOW,
        where = ShowColumns.TRENDING_INDEX + ">=0",
        defaultSort = ShowColumns.TRENDING_INDEX + " ASC")
    public static final Uri SHOWS_TRENDING = buildUri(Path.SHOWS, Path.TRENDING);

    @ContentUri(
        path = Path.SHOWS + "/" + Path.RECOMMENDED,
        type = Type.SHOW,
        where = ShowColumns.RECOMMENDATION_INDEX + ">=0",
        defaultSort = ShowColumns.RECOMMENDATION_INDEX + " ASC")
    public static final Uri SHOWS_RECOMMENDED = buildUri(Path.SHOWS, Path.RECOMMENDED);

    @ContentUri(
        path = Path.SHOWS + "/" + Path.WATCHING,
        type = Type.SHOW,
        join = Joins.SHOWS_WITH_WATCHING)
    public static final Uri SHOW_WATCHING = buildUri(Path.SHOWS, Path.WATCHING);

    @Where(path = Path.SHOWS + "/" + Path.WATCHING)
    public static String[] watchingWhere() {
      return new String[] {
          getWatchingQuery() + ">0",
      };
    }

    public static final String SORT_TITLE =
        DatabaseSchematic.Tables.SHOWS + "." + ShowColumns.TITLE_NO_ARTICLE + " ASC";

    public static final String DEFAULT_SORT = SORT_TITLE;

    public static final String SORT_NEXT_EPISODE = DatabaseSchematic.Tables.EPISODES
        + "."
        + EpisodeColumns.FIRST_AIRED
        + " ASC,"
        + DatabaseSchematic.Tables.SHOWS
        + "."
        + ShowColumns.TITLE
        + " DESC";

    public static final String SORT_RATING = DatabaseSchematic.Tables.SHOWS
        + "."
        + ShowColumns.RATING
        + " DESC,"
        + DatabaseSchematic.Tables.SHOWS
        + "."
        + ShowColumns.TITLE
        + " ASC";

    public static final String SORT_VIEWERS =
        DatabaseSchematic.Tables.SHOWS + "." + ShowColumns.TRENDING_INDEX + " ASC";

    public static final String SORT_RECOMMENDED =
        DatabaseSchematic.Tables.SHOWS + "." + ShowColumns.RECOMMENDATION_INDEX + " ASC";

    public static String getAiredQuery() {
      final long currentTime = System.currentTimeMillis();
      return "(SELECT COUNT(*) FROM "
          + DatabaseSchematic.TABLE_EPISODES
          + " WHERE "
          + DatabaseSchematic.TABLE_EPISODES
          + "."
          + EpisodeColumns.SHOW_ID
          + "="
          + DatabaseSchematic.TABLE_SHOWS
          + "."
          + ShowColumns.ID
          + " AND "
          + DatabaseSchematic.TABLE_EPISODES
          + "."
          + EpisodeColumns.FIRST_AIRED
          + "<="
          + (currentTime + DateUtils.DAY_IN_MILLIS)
          + " AND "
          + DatabaseSchematic.TABLE_EPISODES
          + "."
          + EpisodeColumns.FIRST_AIRED
          + ">"
          + DateUtils.YEAR_IN_MILLIS
          + " AND "
          + DatabaseSchematic.TABLE_EPISODES
          + "."
          + EpisodeColumns.SEASON
          + ">0"
          + ")";
    }

    public static String getAiredAfterWatched() {
      final long currentTime = System.currentTimeMillis();
      return "(SELECT COUNT(*) FROM episodes "
          + "JOIN ("
          + "SELECT season, episode "
          + "FROM episodes "
          + "WHERE watched=1 AND showId=shows._id "
          + "ORDER BY season DESC, episode DESC LIMIT 1"
          + ") AS ep2 "
          + "WHERE episodes.showId=shows._id AND episodes.season>0 "
          + "AND (episodes.season>ep2.season OR (episodes.season=ep2.season AND episodes.episode>ep2.episode)) "
          + "AND episodes.episodeFirstAired<="
          + (currentTime + DateUtils.DAY_IN_MILLIS)
          + " AND episodes.episodeFirstAired>"
          + DateUtils.YEAR_IN_MILLIS
          + ")";
    }

    public static String getUnairedQuery() {
      final long currentTime = System.currentTimeMillis();
      return "(SELECT COUNT(*) FROM "
          + DatabaseSchematic.TABLE_EPISODES
          + " WHERE "
          + DatabaseSchematic.TABLE_EPISODES
          + "."
          + EpisodeColumns.SHOW_ID
          + "="
          + DatabaseSchematic.TABLE_SHOWS
          + "."
          + ShowColumns.ID
          + " AND "
          + DatabaseSchematic.TABLE_EPISODES
          + "."
          + EpisodeColumns.FIRST_AIRED
          + ">"
          + currentTime
          + " AND "
          + DatabaseSchematic.TABLE_EPISODES
          + "."
          + EpisodeColumns.SEASON
          + ">0"
          + ")";
    }

    public static String getWatchingQuery() {
      return "(SELECT COUNT(*) FROM "
          + DatabaseSchematic.Tables.EPISODES
          + " WHERE "
          + DatabaseSchematic.TABLE_EPISODES
          + "."
          + EpisodeColumns.SHOW_ID
          + "="
          + DatabaseSchematic.TABLE_SHOWS
          + "."
          + ShowColumns.ID
          + " AND ("
          + DatabaseSchematic.TABLE_EPISODES
          + "."
          + EpisodeColumns.WATCHING
          + "=1 OR "
          + DatabaseSchematic.TABLE_EPISODES
          + "."
          + EpisodeColumns.CHECKED_IN
          + "=1))";
    }

    public static String getEpisodeCountQuery() {
      return "(SELECT COUNT(*) FROM "
          + DatabaseSchematic.Tables.EPISODES
          + " WHERE "
          + DatabaseSchematic.Tables.EPISODES
          + "."
          + EpisodeColumns.SHOW_ID
          + "="
          + DatabaseSchematic.TABLE_SHOWS
          + "."
          + ShowColumns.ID
          + ")";
    }
  }

  @TableEndpoint(table = DatabaseSchematic.TABLE_SEASONS)
  public static class Seasons {

    @MapColumns public static Map<String, String> mapColumns() {
      Map<String, String> map = new HashMap<String, String>();

      map.put(SeasonColumns.AIRED_COUNT, getAiredQuery());
      map.put(SeasonColumns.UNAIRED_COUNT, getUnairedQuery());
      map.put(Tables.SEASONS + "." + SeasonColumns.AIRED_COUNT, getAiredQuery());
      map.put(Tables.SEASONS + "." + SeasonColumns.UNAIRED_COUNT, getUnairedQuery());

      return map;
    }

    @ContentUri(
        path = Path.SEASONS,
        type = Type.SEASON)
    public static final Uri SEASONS = buildUri(Path.SEASONS);

    @InexactContentUri(
        path = Path.SEASONS + "/#",
        type = Type.SEASON_ID,
        name = "SEASON",
        whereColumn = SeasonColumns.ID,
        pathSegment = 1)
    public static Uri withId(long id) {
      return buildUri(Path.SEASONS, String.valueOf(id));
    }

    public static long getId(Uri seasonUri) {
      return Long.valueOf(seasonUri.getPathSegments().get(1));
    }

    @InexactContentUri(
        path = Path.SEASONS + "/" + Path.FROM_SHOW + "/#",
        type = Type.SEASON,
        name = "SEASONS_FROMSHOW",
        whereColumn = SeasonColumns.SHOW_ID,
        pathSegment = 2)
    public static Uri fromShow(long showId) {
      return buildUri(Path.SEASONS, Path.FROM_SHOW, String.valueOf(showId));
    }

    public static final String DEFAULT_SORT =
        DatabaseSchematic.Tables.SEASONS + "." + SeasonColumns.SEASON + " DESC";

    @NotifyInsert(paths = Path.SEASONS)
    public static Uri[] notifyInsert(ContentValues cv) {
      final long showId = cv.getAsLong(SeasonColumns.SHOW_ID);
      return new Uri[] {
          fromShow(showId),
      };
    }

    @NotifyUpdate(paths = {
        Path.SEASONS, Path.SEASONS + "/#", Path.SEASONS + "/" + Path.FROM_SHOW + "/#"
    })
    public static Uri[] notifyUpdate(Context context, Uri uri, String where, String[] whereArgs) {
      Set<Uri> uris = new HashSet<Uri>();

      Cursor c = context.getContentResolver().query(uri, new String[] {
          SeasonColumns.ID, SeasonColumns.SHOW_ID,
      }, where, whereArgs, null);

      while (c.moveToNext()) {
        final long id = c.getLong(0);
        final long showId = c.getLong(1);

        uris.add(withId(id));
        uris.add(fromShow(showId));
      }
      c.close();

      return uris.toArray(new Uri[uris.size()]);
    }

    @NotifyDelete(
        paths = {
            Path.SEASONS, Path.SEASONS + "/#", Path.SEASONS + "/" + Path.FROM_SHOW + "/#"
        })
    public static Uri[] notifyDelete() {
      return new Uri[] {
          Shows.SHOWS, Seasons.SEASONS,
      };
    }

    public static String getAiredQuery() {
      Calendar cal = Calendar.getInstance();
      final long currentTime = cal.getTimeInMillis();
      return "(SELECT COUNT(*) FROM "
          + DatabaseSchematic.TABLE_EPISODES
          + " WHERE "
          + DatabaseSchematic.TABLE_EPISODES
          + "."
          + EpisodeColumns.SEASON_ID
          + "="
          + DatabaseSchematic.TABLE_SEASONS
          + "."
          + SeasonColumns.ID
          + " AND "
          + DatabaseSchematic.TABLE_EPISODES
          + "."
          + EpisodeColumns.FIRST_AIRED
          + "<="
          + currentTime
          + " AND "
          + DatabaseSchematic.TABLE_EPISODES
          + "."
          + EpisodeColumns.FIRST_AIRED
          + ">"
          + DateUtils.YEAR_IN_MILLIS
          + " AND "
          + DatabaseSchematic.TABLE_EPISODES
          + "."
          + EpisodeColumns.SEASON
          + ">0"
          + ")";
    }

    public static String getUnairedQuery() {
      Calendar cal = Calendar.getInstance();
      final long currentTime = cal.getTimeInMillis();
      return "(SELECT COUNT(*) FROM "
          + DatabaseSchematic.TABLE_EPISODES
          + " WHERE "
          + DatabaseSchematic.TABLE_EPISODES
          + "."
          + EpisodeColumns.SEASON_ID
          + "="
          + DatabaseSchematic.TABLE_SEASONS
          + "."
          + SeasonColumns.ID
          + " AND "
          + DatabaseSchematic.TABLE_EPISODES
          + "."
          + EpisodeColumns.FIRST_AIRED
          + ">"
          + currentTime
          + " AND "
          + DatabaseSchematic.TABLE_EPISODES
          + "."
          + EpisodeColumns.SEASON
          + ">0"
          + ")";
    }
  }

  @TableEndpoint(table = DatabaseSchematic.TABLE_EPISODES)
  public static class Episodes {

    @ContentUri(
        path = Path.EPISODES,
        type = Type.EPISODE) @NotificationUri(
        paths = {
            Path.SHOWS + "/" + Path.WATCHING, Path.EPISODES + "/" + Path.WATCHING,
            Path.EPISODES + "/" + Path.WATCHLIST
        })
    public static final Uri EPISODES = buildUri(Path.EPISODES);

    @ContentUri(
        path = Path.EPISODES + "/" + Path.WATCHLIST,
        type = Type.EPISODE,
        join = Joins.EPISODES_WITH_SHOW_TITLE,
        where = EpisodeColumns.IN_WATCHLIST + "=1")
    public static final Uri EPISODES_IN_WATCHLIST = buildUri(Path.EPISODES, Path.WATCHLIST);

    @ContentUri(
        path = Path.EPISODES + "/" + Path.WATCHING,
        type = Type.EPISODE,
        where = EpisodeColumns.CHECKED_IN + "=1 OR " + EpisodeColumns.WATCHING + "=1")
    public static final Uri EPISODE_WATCHING = buildUri(Path.EPISODES, Path.WATCHING);

    @InexactContentUri(
        path = Path.EPISODES + "/#",
        type = Type.EPISODE_ID,
        name = "EPISODE_WITHID",
        whereColumn = EpisodeColumns.ID,
        pathSegment = 1)
    public static Uri withId(long id) {
      return buildUri(Path.EPISODES, String.valueOf(id));
    }

    public static long getId(Uri episodeUri) {
      return Long.valueOf(episodeUri.getPathSegments().get(1));
    }

    @InexactContentUri(
        path = Path.EPISODES + "/" + Path.FROM_SEASON + "/#",
        type = Type.EPISODE,
        name = "EPISODES_FROMSEASON",
        whereColumn = EpisodeColumns.SEASON_ID,
        pathSegment = 2)
    public static Uri fromSeason(long seasonId) {
      return buildUri(Path.EPISODES, Path.FROM_SEASON, String.valueOf(seasonId));
    }

    @InexactContentUri(
        path = Path.EPISODES + "/" + Path.FROM_SHOW + "/#",
        type = Type.EPISODE,
        name = "EPISODES_FROMSHOW",
        whereColumn = EpisodeColumns.SHOW_ID,
        pathSegment = 2)
    public static Uri fromShow(long showId) {
      return buildUri(Path.EPISODES, Path.FROM_SHOW, String.valueOf(showId));
    }

    @NotifyInsert(paths = Path.EPISODES)
    public static Uri[] notifyInsert(ContentValues cv) {
      // TODO Is episode watchlist notified?
      final long showId = cv.getAsLong(EpisodeColumns.SHOW_ID);
      final long seasonId = cv.getAsLong(EpisodeColumns.SEASON_ID);
      return new Uri[] {
          fromShow(showId), fromSeason(seasonId), Shows.withId(showId), Seasons.withId(seasonId),
          Seasons.fromShow(showId),
      };
    }

    @NotifyUpdate(paths = {
        Path.EPISODES, Path.EPISODES + "/#", Path.EPISODES + "/" + Path.WATCHLIST,
        Path.EPISODES + "/" + Path.WATCHING, Path.EPISODES + "/" + Path.FROM_SEASON + "/#",
        Path.EPISODES + "/" + Path.FROM_SHOW + "/#"
    })
    public static Uri[] notifyUpdate(Context context, String where, String[] whereArgs) {
      Set<Uri> uris = new HashSet<Uri>();

      Cursor c = context.getContentResolver().query(EPISODES, new String[] {
          EpisodeColumns.ID, EpisodeColumns.SHOW_ID, EpisodeColumns.SEASON_ID,
      }, where, whereArgs, null);

      while (c.moveToNext()) {
        final long id = c.getLong(0);
        final long showId = c.getLong(1);
        final long seasonId = c.getLong(2);

        uris.add(withId(id));
        uris.add(fromShow(showId));
        uris.add(fromSeason(seasonId));
        uris.add(Shows.withId(id));
        uris.add(Seasons.withId(seasonId));
        uris.add(Seasons.fromShow(showId));
      }

      c.close();

      return uris.toArray(new Uri[uris.size()]);
    }

    @NotifyDelete(paths = {
        Path.EPISODES, Path.EPISODES + "/#", Path.EPISODES + "/" + Path.WATCHLIST,
        Path.EPISODES + "/" + Path.WATCHING, Path.EPISODES + "/" + Path.FROM_SEASON + "/#",
        Path.EPISODES + "/" + Path.FROM_SHOW + "/#"
    })
    public static Uri[] notifyDelete() {
      return new Uri[] {
          Shows.SHOWS, Seasons.SEASONS, EPISODES,
      };
    }
  }

  @TableEndpoint(table = DatabaseSchematic.TABLE_SHOW_GENRES)
  public static class ShowGenres {

    @ContentUri(
        path = Path.SHOW_GENRES,
        type = Type.SHOW_GENRE)
    public static final Uri SHOW_GENRES = buildUri(Path.SHOW_GENRES);

    @InexactContentUri(
        path = Path.SHOW_GENRES + "/" + Path.FROM_SHOW + "/#",
        type = Type.SHOW_GENRE,
        name = "GENRES_FROMSHOW",
        whereColumn = ShowGenreColumns.SHOW_ID,
        pathSegment = 2)
    public static Uri fromShow(long id) {
      return buildUri(Path.SHOW_GENRES, Path.FROM_SHOW, String.valueOf(id));
    }

    public static final String DEFAULT_SORT = ShowGenreColumns.GENRE + " ASC";

    @InsertUri(paths = Path.SHOW_GENRES)
    public static Uri insertReturnUri(ContentValues cv) {
      final long showId = cv.getAsLong(ShowGenreColumns.SHOW_ID);
      return fromShow(showId);
    }

    @NotifyInsert(paths = Path.SHOW_GENRES)
    public static Uri[] notifyInsert(ContentValues cv) {
      final long showId = cv.getAsLong(ShowGenreColumns.SHOW_ID);
      return new Uri[] {
          fromShow(showId),
      };
    }
  }

  @TableEndpoint(table = DatabaseSchematic.TABLE_SHOW_CHARACTERS)
  public static class ShowCharacters {

    @ContentUri(
        path = Path.SHOW_CHARACTERS,
        type = Type.SHOW_CHARACTERS,
        join = Joins.SHOW_CAST_PERSON)
    public static final Uri SHOW_CHARACTERS = buildUri(Path.SHOW_CHARACTERS);

    @InexactContentUri(
        path = Path.SHOW_CHARACTERS + "/" + Path.FROM_SHOW + "/#",
        type = Type.SHOW_CHARACTERS,
        name = "CHARACTERS_FROMSHOW",
        whereColumn = ShowCharacterColumns.SHOW_ID,
        pathSegment = 2,
        join = Joins.SHOW_CAST_PERSON)
    public static Uri fromShow(long showId) {
      return buildUri(Path.SHOW_CHARACTERS, Path.FROM_SHOW, String.valueOf(showId));
    }

    @InsertUri(paths = Path.SHOW_CHARACTERS)
    public static Uri insertReturnUri(ContentValues cv) {
      final long showId = cv.getAsLong(ShowCharacterColumns.SHOW_ID);
      return fromShow(showId);
    }

    @NotifyInsert(paths = Path.SHOW_CHARACTERS)
    public static Uri[] notifyInsert(ContentValues cv) {
      final long showId = cv.getAsLong(ShowCharacterColumns.SHOW_ID);
      return new Uri[] {
          fromShow(showId),
      };
    }
  }

  @TableEndpoint(table = DatabaseSchematic.TABLE_MOVIES)
  public static class Movies {

    @ContentUri(
        path = Path.MOVIES,
        type = Type.MOVIE) @NotificationUri(
        paths = {
            Path.MOVIES + "/" + Path.WATCHING, Path.MOVIES + "/" + Path.TRENDING,
            Path.MOVIES + "/" + Path.RECOMMENDED
        })
    public static final Uri MOVIES = buildUri(Path.MOVIES);

    @InexactContentUri(
        path = Path.MOVIES + "/#",
        type = Type.MOVIE_ID,
        name = "MOVIE_WITHID",
        whereColumn = MovieColumns.ID,
        pathSegment = 1)
    public static Uri withId(long id) {
      return buildUri(Path.MOVIES, String.valueOf(id));
    }

    public static long getId(Uri movieUri) {
      return Long.valueOf(movieUri.getPathSegments().get(1));
    }

    @ContentUri(
        path = Path.MOVIES + "/" + Path.WATCHING,
        type = Type.MOVIE,
        where = MovieColumns.WATCHING + "=1")
    public static final Uri WATCHING = buildUri(Path.MOVIES, Path.WATCHING);

    @ContentUri(
        path = Path.MOVIES + "/" + Path.TRENDING,
        type = Type.MOVIE,
        where = MovieColumns.TRENDING_INDEX + ">=0",
        defaultSort = MovieColumns.TRENDING_INDEX + " ASC")
    public static final Uri TRENDING = buildUri(Path.MOVIES, Path.TRENDING);

    @ContentUri(
        path = Path.MOVIES + "/" + Path.RECOMMENDED,
        type = Type.MOVIE,
        where = MovieColumns.RECOMMENDATION_INDEX + ">=0",
        defaultSort = MovieColumns.RECOMMENDATION_INDEX + " ASC")
    public static final Uri RECOMMENDED = buildUri(Path.MOVIES, Path.RECOMMENDED);

    public static final String SORT_TITLE =
        DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.TITLE_NO_ARTICLE + " ASC";

    public static final String DEFAULT_SORT = SORT_TITLE;

    public static final String SORT_RATING = DatabaseSchematic.Tables.MOVIES
        + "."
        + MovieColumns.RATING
        + " DESC,"
        + DatabaseSchematic.Tables.MOVIES
        + "."
        + MovieColumns.TITLE_NO_ARTICLE
        + " ASC";

    public static final String SORT_VIEWERS =
        DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.TRENDING_INDEX + " ASC";

    public static final String SORT_RECOMMENDED =
        DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.RECOMMENDATION_INDEX + " ASC";
  }

  @TableEndpoint(table = DatabaseSchematic.TABLE_MOVIE_GENRES)
  public static class MovieGenres {

    @ContentUri(
        path = Path.MOVIE_GENRES,
        type = Type.MOVIE_GENRE)
    public static final Uri MOVIE_GENRES = buildUri(Path.MOVIE_GENRES);

    @InexactContentUri(
        path = Path.MOVIE_GENRES + "/" + Path.FROM_MOVIE + "/#",
        type = Type.MOVIE_GENRE,
        name = "GENRES_FROMMOVIE",
        whereColumn = MovieGenreColumns.MOVIE_ID,
        pathSegment = 2)
    public static Uri fromMovie(long movieId) {
      return buildUri(Path.MOVIE_GENRES, Path.FROM_MOVIE, String.valueOf(movieId));
    }

    @NotifyInsert(paths = Path.MOVIE_GENRES)
    public static Uri[] notifyInsert(ContentValues cv) {
      final long movieId = cv.getAsLong(MovieGenreColumns.MOVIE_ID);
      return new Uri[] {
          fromMovie(movieId),
      };
    }

    @InsertUri(paths = Path.MOVIE_GENRES)
    public static Uri insertUri(ContentValues cv) {
      final long movieId = cv.getAsLong(MovieGenreColumns.MOVIE_ID);
      return fromMovie(movieId);
    }
  }

  @TableEndpoint(table = DatabaseSchematic.TABLE_MOVIE_CAST)
  public static class MovieCast {

    @ContentUri(
        path = Path.MOVIE_CAST,
        type = Type.MOVIE_CAST)
    public static final Uri MOVIE_CAST = buildUri(Path.MOVIE_CAST);

    @InexactContentUri(
        path = Path.MOVIE_CAST + "/" + Path.FROM_MOVIE + "/#",
        type = Type.MOVIE_CAST,
        name = "CAST_FROMMOVIE",
        whereColumn = MovieCastColumns.MOVIE_ID,
        pathSegment = 2,
        join = Joins.MOVIE_CAST_PERSON)
    public static Uri fromMovie(long movieId) {
      return buildUri(Path.MOVIE_CAST, Path.FROM_MOVIE, String.valueOf(movieId));
    }

    @NotifyInsert(paths = Path.MOVIE_CAST)
    public static Uri[] notifyInsert(ContentValues cv) {
      final long movieId = cv.getAsLong(MovieCastColumns.MOVIE_ID);
      return new Uri[] {
          fromMovie(movieId),
      };
    }

    @InsertUri(paths = Path.MOVIE_CAST)
    public static Uri insertUri(ContentValues cv) {
      final long movieId = cv.getAsLong(MovieCastColumns.MOVIE_ID);
      return fromMovie(movieId);
    }
  }

  @TableEndpoint(table = DatabaseSchematic.TABLE_MOVIE_CREW)
  public static class MovieCrew {

    @ContentUri(
        path = Path.MOVIE_CREW,
        type = Type.MOVIE_CREW)
    public static final Uri MOVIE_CREW = buildUri(Path.MOVIE_CREW);

    @InexactContentUri(
        path = Path.MOVIE_CREW + "/" + Path.FROM_MOVIE + "/#",
        type = Type.MOVIE_CREW,
        name = "CREW_FROMMOVIE",
        whereColumn = MovieCastColumns.MOVIE_ID,
        pathSegment = 2)
    public static Uri fromMovie(long movieId) {
      return buildUri(Path.MOVIE_CREW, Path.FROM_MOVIE, String.valueOf(movieId));
    }

    @NotifyInsert(paths = Path.MOVIE_CREW)
    public static Uri[] notifyInsert(ContentValues cv) {
      final long movieId = cv.getAsLong(MovieCastColumns.MOVIE_ID);
      return new Uri[] {
          fromMovie(movieId),
      };
    }

    @InsertUri(paths = Path.MOVIE_CREW)
    public static Uri insertUri(ContentValues cv) {
      final long movieId = cv.getAsLong(MovieCastColumns.MOVIE_ID);
      return fromMovie(movieId);
    }
  }

  @TableEndpoint(table = DatabaseSchematic.TABLE_SHOW_SEARCH_SUGGESTIONS)
  public static class ShowSearchSuggestions {

    @ContentUri(
        path = Path.SEARCH_SUGGESTIONS + "/" + Path.SHOWS,
        type = Type.SHOW_SEARCH)
    public static final Uri SHOW_SUGGESTIONS = buildUri(Path.SEARCH_SUGGESTIONS, Path.SHOWS);
  }

  @TableEndpoint(table = DatabaseSchematic.TABLE_MOVIE_SEARCH_SUGGESTIONS)
  public static class MovieSearchSuggestions {

    @ContentUri(
        path = Path.SEARCH_SUGGESTIONS + "/" + Path.MOVIES,
        type = Type.MOVIE_SEARCH)
    public static final Uri MOVIE_SUGGESTIONS = buildUri(Path.SEARCH_SUGGESTIONS, Path.MOVIES);
  }

  @TableEndpoint(table = DatabaseSchematic.TABLE_PEOPLE)
  public static class People {

    @ContentUri(
        path = Path.PEOPLE,
        type = Type.PEOPLE)
    public static final Uri PEOPLE = buildUri(Path.PEOPLE);

    @InexactContentUri(
        path = Path.PEOPLE + "/#",
        type = Type.PERSON,
        name = "PERSON_ID",
        whereColumn = PersonColumns.ID,
        pathSegment = 1)
    public static Uri withId(long id) {
      return buildUri(Path.PEOPLE, String.valueOf(id));
    }

    public static long getId(Uri uri) {
      return Long.valueOf(uri.getPathSegments().get(1));
    }
  }
}
