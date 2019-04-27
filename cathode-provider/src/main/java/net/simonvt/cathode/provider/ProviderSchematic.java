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
import android.text.format.DateUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.simonvt.cathode.common.database.Cursors;
import net.simonvt.cathode.common.util.Joiner;
import net.simonvt.cathode.provider.entity.ItemTypeString;
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.ListItemColumns;
import net.simonvt.cathode.provider.DatabaseContract.ListsColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieCastColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieCrewColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieGenreColumns;
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns;
import net.simonvt.cathode.provider.DatabaseContract.RelatedMoviesColumns;
import net.simonvt.cathode.provider.DatabaseContract.RelatedShowsColumns;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowCastColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowCrewColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowGenreColumns;
import net.simonvt.cathode.provider.DatabaseContract.UserColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.util.SqlColumn;
import net.simonvt.cathode.settings.FirstAiredOffsetPreference;
import net.simonvt.cathode.settings.UpcomingTimePreference;
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

@ContentProvider(name = "CathodeProvider",
    packageName = "net.simonvt.cathode.provider.generated",
    authority = BuildConfig.PROVIDER_AUTHORITY,
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
    String SHOW_CAST = "vnd.android.cursor.dir/vnd.simonvt.cathode.showCast";
    String SHOW_CREW = "vnd.android.cursor.dir/vnd.simonvt.cathode.showCrew";
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

    String RECENT_QUERIES = "vnd.android.cursor.dir/vnd.simonvt.cathode.recentQueries";

    String PEOPLE = "vnd.android.cursor.dir/vnd.simonvt.cathode.people";
    String PERSON = "vnd.android.cursor.item/vnd.simonvt.cathode.person";

    String LISTS = "vnd.android.cursor.dir/vnd.simonvt.cathode.lists";
    String LIST_ID = "vnd.android.cursor.item/vnd.simonvt.cathode.list";
    String LIST_ITEMS = "vnd.android.cursor.dir/vnd.simonvt.cathode.listItem";

    String USERS = "vnd.android.cursor.dir/vnd.simonvt.cathode.users";
    String USER_ID = "vnd.android.cursor.item/vnd.simonvt.cathode.userId";

    String COMMENTS = "vnd.android.cursor.dir/vnd.simonvt.cathode.comments";
    String COMMENT_ID = "vnd.android.cursor.item/vnd.simonvt.cathode.userComment";
  }

  interface Path {
    String SHOWS = "shows";

    String UPCOMING = "upcoming";
    String WITH_NEXT = "withNext";
    String WITH_SHOW = "withShow";

    String SEASONS = "seasons";
    String EPISODES = "episodes";
    String SHOW_GENRES = "showGenres";
    String SHOW_CAST = "showCast";
    String SHOW_CREW = "showCrew";
    String SHOW_RELATED = "showRelated";

    String FROM_SHOW = "fromShow";
    String FROM_SEASON = "fromSeason";
    String FROM_EPISODE = "fromEpisode";

    String MOVIES = "movies";
    String MOVIE_GENRES = "movieGenres";
    String MOVIE_CAST = "movieCast";
    String MOVIE_CREW = "movieCrew";
    String MOVIE_DIRECTORY = "movieDirectory";
    String MOVIE_WRITERS = "movieWriters";
    String MOVIE_PRODUCERS = "movieProducers";
    String MOVIE_TOP_WATCHERS = "movieTopWatchers";
    String MOVIE_RELATED = "movieRelated";

    String FROM_MOVIE = "fromMovie";

    String WATCHED = "watched";
    String COLLECTED = "collected";
    String WATCHLIST = "inWatchlist";

    String TRENDING = "trending";
    String RECOMMENDED = "recommended";
    String ANTICIPATED = "anticipated";
    String WATCHING = "watching";

    String RECENT_QUERIES = "recentQueries";

    String PEOPLE = "people";
    String FROM_PERSON = "withPerson";

    String LISTS = "lists";
    String LIST_ITEMS = "listItems";

    String IN_LIST = "inList";

    String USERS = "users";

    String COMMENTS = "comments";
    String FROM_USER = "fromUser";
    String WITH_PARENT = "withParent";
    String WITH_USER = "withUser";
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

  @TableEndpoint(table = Tables.SHOWS) public static class Shows {

    @MapColumns public static Map<String, String> mapColumns() {
      Map<String, String> map = new HashMap<>();

      map.put(ShowColumns.AIRED_COUNT, getAiredQuery());
      map.put(ShowColumns.UNAIRED_COUNT, getUnairedQuery());
      map.put(ShowColumns.EPISODE_COUNT, getEpisodeCountQuery());
      map.put(ShowColumns.WATCHING_EPISODE_ID, getWatchingIdQuery());
      map.put(Tables.SHOWS + "." + ShowColumns.AIRED_COUNT, getAiredQuery());
      map.put(Tables.SHOWS + "." + ShowColumns.UNAIRED_COUNT, getUnairedQuery());
      map.put(Tables.SHOWS + "." + ShowColumns.EPISODE_COUNT, getEpisodeCountQuery());
      map.put(Tables.SHOWS + "." + ShowColumns.WATCHING_EPISODE_ID, getWatchingIdQuery());

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
        where = ShowColumns.WATCHED_COUNT + ">0 AND "
            + Tables.SHOWS + "." + ShowColumns.LAST_SYNC + ">0 AND "
            + Tables.SHOWS + "." + ShowColumns.HIDDEN_WATCHED + "=0")
    public static final Uri SHOWS_WATCHED = buildUri(Path.SHOWS, Path.WATCHED);

    @ContentUri(
        path = Path.SHOWS + "/" + Path.COLLECTED,
        type = Type.SHOW,
        join = Joins.SHOWS_UNCOLLECTED,
        where = ShowColumns.IN_COLLECTION_COUNT + ">0 AND "
            + Tables.SHOWS + "." + ShowColumns.LAST_SYNC + ">0 AND "
            + Tables.SHOWS + "." + ShowColumns.HIDDEN_COLLECTED + "=0")
    public static final Uri SHOWS_COLLECTION = buildUri(Path.SHOWS, Path.COLLECTED);

    @ContentUri(
        path = Path.SHOWS + "/" + Path.WATCHLIST,
        type = Type.SHOW,
        where = ShowColumns.IN_WATCHLIST + "=1 AND "
            + Tables.SHOWS + "." + ShowColumns.LAST_SYNC + ">0")
    public static final Uri SHOWS_WATCHLIST = buildUri(Path.SHOWS, Path.WATCHLIST);

    @ContentUri(
        path = Path.SHOWS + "/" + Path.UPCOMING,
        type = Type.SHOW,
        join = Joins.SHOWS_UPCOMING,
        where = ShowColumns.HIDDEN_CALENDAR + "=0"
    )
    public static final Uri SHOWS_UPCOMING = buildUri(Path.SHOWS, Path.UPCOMING);

    @Where(path = Path.SHOWS + "/" + Path.UPCOMING)
    public static String[] upcomingWhere() {
      final long upcomingTime = UpcomingTimePreference.getInstance().get().getCacheTime();
      return new String[] {
          ShowColumns.WATCHED_COUNT + ">0", getUpcomingQuery(upcomingTime) + ">0",
          Tables.SHOWS + "." + ShowColumns.LAST_SYNC + ">0"
      };
    }

    public static String getUpcomingQuery(long upcomingTime) {
      final long offset = FirstAiredOffsetPreference.getInstance().getOffsetMillis();
      final long currentTime = System.currentTimeMillis();
      String query = "(SELECT COUNT(*) FROM "
          + Tables.EPISODES
          + " JOIN ("
          + "SELECT "
          + EpisodeColumns.SEASON
          + ", "
          + EpisodeColumns.EPISODE
          + " FROM "
          + Tables.EPISODES
          + " WHERE "
          + EpisodeColumns.SHOW_ID
          + "="
          + SqlColumn.table(Tables.SHOWS).column(ShowColumns.ID)
          + " AND "
          + EpisodeColumns.WATCHED
          + "=1"
          + " ORDER BY "
          + EpisodeColumns.SEASON
          + " DESC, "
          + EpisodeColumns.EPISODE
          + " DESC LIMIT 1"
          + ") AS ep2 "
          + "WHERE "
          + SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.SHOW_ID)
          + "="
          + SqlColumn.table(Tables.SHOWS).column(ShowColumns.ID)
          + " AND "
          + SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.SEASON)
          + ">0"
          + " AND ("
          + SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.SEASON)
          + ">"
          + SqlColumn.table("ep2").column(EpisodeColumns.SEASON)
          + " OR ("
          + SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.SEASON)
          + "="
          + SqlColumn.table("ep2").column(EpisodeColumns.SEASON)
          + " AND "
          + SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.EPISODE)
          + ">"
          + SqlColumn.table("ep2").column(EpisodeColumns.EPISODE)
          + "))"
          + " AND "
          + SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.FIRST_AIRED)
          + " NOT NULL";

      if (upcomingTime > 0L) {
        query += " AND (" + SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.FIRST_AIRED);
        query += " + " + offset + ")";
        query += "<=" + (currentTime + upcomingTime);
      }

      query += ")";

      return query;
    }

    @ContentUri(
        path = Path.SHOWS + "/" + Path.WITH_NEXT,
        type = Type.SHOW,
        join = Joins.SHOWS_WITH_NEXT,
        where = ShowColumns.HIDDEN_CALENDAR + "=0"
    )
    public static final Uri WITH_NEXT = buildUri(Path.SHOWS, Path.WITH_NEXT);

    @Where(path = Path.SHOWS + "/" + Path.WITH_NEXT)
    public static String[] withNextWhere() {
      return new String[] {
          ShowColumns.WATCHED_COUNT + ">0", withNextQuery() + ">0",
          Tables.SHOWS + "." + ShowColumns.LAST_UPDATED + ">0"
      };
    }

    static String withNextQuery() {
      String query = "(SELECT COUNT(*) FROM "
          + Tables.EPISODES
          + " JOIN ("
          + "SELECT "
          + EpisodeColumns.SEASON
          + ", "
          + EpisodeColumns.EPISODE
          + " FROM "
          + Tables.EPISODES
          + " WHERE "
          + EpisodeColumns.SHOW_ID
          + "="
          + SqlColumn.table(Tables.SHOWS).column(ShowColumns.ID)
          + " AND "
          + EpisodeColumns.WATCHED
          + "=1 "
          + "ORDER BY "
          + EpisodeColumns.SEASON
          + " DESC, "
          + EpisodeColumns.EPISODE
          + " DESC LIMIT 1"
          + ") AS ep2 "
          + "WHERE "
          + SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.SHOW_ID)
          + "="
          + SqlColumn.table(Tables.SHOWS).column(ShowColumns.ID)
          + " AND "
          + SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.SEASON)
          + ">0"
          + " AND ("
          + SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.SEASON)
          + ">"
          + SqlColumn.table("ep2").column(EpisodeColumns.SEASON)
          + " OR ("
          + SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.SEASON)
          + "="
          + SqlColumn.table("ep2").column(EpisodeColumns.SEASON)
          + " AND "
          + SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.EPISODE)
          + ">"
          + SqlColumn.table("ep2").column(EpisodeColumns.EPISODE)
          + "))"
          + " AND "
          + SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.FIRST_AIRED)
          + " NOT NULL)";

      return query;
    }

    @ContentUri(
        path = Path.SHOWS + "/" + Path.TRENDING,
        type = Type.SHOW,
        where = {
            ShowColumns.TRENDING_INDEX + ">=0"
        },
        defaultSort = ShowColumns.TRENDING_INDEX + " ASC")
    public static final Uri SHOWS_TRENDING = buildUri(Path.SHOWS, Path.TRENDING);

    @ContentUri(
        path = Path.SHOWS + "/" + Path.RECOMMENDED,
        type = Type.SHOW,
        where = {
            ShowColumns.RECOMMENDATION_INDEX + ">=0"
        },
        defaultSort = ShowColumns.RECOMMENDATION_INDEX + " ASC")
    public static final Uri SHOWS_RECOMMENDED = buildUri(Path.SHOWS, Path.RECOMMENDED);

    @ContentUri(
        path = Path.SHOWS + "/" + Path.ANTICIPATED,
        type = Type.SHOW,
        where = {
            ShowColumns.ANTICIPATED_INDEX + ">=0"
        },
        defaultSort = ShowColumns.ANTICIPATED_INDEX + " ASC")
    public static final Uri SHOWS_ANTICIPATED = buildUri(Path.SHOWS, Path.ANTICIPATED);

    @ContentUri(
        path = Path.SHOWS + "/" + Path.WATCHING,
        type = Type.SHOW,
        join = Joins.SHOWS_WITH_WATCHING,
        where = {
            Tables.SHOWS + "." + ShowColumns.WATCHING + "=1",
            Tables.SHOWS + "." + ShowColumns.LAST_UPDATED + ">0"
        }
    )
    public static final Uri SHOW_WATCHING = buildUri(Path.SHOWS, Path.WATCHING);

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

    public static final String SORT_LAST_WATCHED = DatabaseSchematic.Tables.SHOWS
        + "."
        + ShowColumns.LAST_WATCHED_AT
        + " DESC,"
        + DatabaseSchematic.Tables.SHOWS
        + "."
        + ShowColumns.TITLE
        + " ASC";

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

    public static final String SORT_ANTICIPATED =
        DatabaseSchematic.Tables.SHOWS + "." + ShowColumns.ANTICIPATED_INDEX + " ASC";

    public static final String SORT_WATCHED =
        Tables.SHOWS + "." + ShowColumns.LAST_WATCHED_AT + " DESC";

    public static final String SORT_COLLECTED =
        Tables.SHOWS + "." + ShowColumns.LAST_COLLECTED_AT + " DESC";

    public static String getAiredQuery() {
      final long firstAiredOffset = FirstAiredOffsetPreference.getInstance().getOffsetMillis();
      final long currentTime = System.currentTimeMillis();
      return "(SELECT COUNT(*) FROM "
          + Tables.EPISODES
          + " WHERE "
          + Tables.EPISODES
          + "."
          + EpisodeColumns.SHOW_ID
          + "="
          + Tables.SHOWS
          + "."
          + ShowColumns.ID
          + " AND "
          + Tables.EPISODES
          + "."
          + EpisodeColumns.FIRST_AIRED
          + "<="
          + (currentTime - firstAiredOffset + DateUtils.DAY_IN_MILLIS)
          + " AND "
          + Tables.EPISODES
          + "."
          + EpisodeColumns.SEASON
          + ">0"
          + ")";
    }

    public static String getUnairedQuery() {
      final long firstAiredOffset = FirstAiredOffsetPreference.getInstance().getOffsetMillis();
      final long currentTime = System.currentTimeMillis();
      return "(SELECT COUNT(*) FROM "
          + Tables.EPISODES
          + " WHERE "
          + Tables.EPISODES
          + "."
          + EpisodeColumns.SHOW_ID
          + "="
          + Tables.SHOWS
          + "."
          + ShowColumns.ID
          + " AND "
          + Tables.EPISODES
          + "."
          + EpisodeColumns.FIRST_AIRED
          + ">"
          + (currentTime - firstAiredOffset)
          + " AND "
          + Tables.EPISODES
          + "."
          + EpisodeColumns.SEASON
          + ">0"
          + ")";
    }

    public static String getEpisodeCountQuery() {
      return "(SELECT COUNT(*) FROM "
          + Tables.EPISODES
          + " WHERE "
          + Tables.EPISODES
          + "."
          + EpisodeColumns.SHOW_ID
          + "="
          + Tables.SHOWS
          + "."
          + ShowColumns.ID
          + ")";
    }

    public static String getWatchingIdQuery() {
      return "(SELECT " + Tables.EPISODES + "." + EpisodeColumns.ID + " FROM "
          + Tables.EPISODES
          + " WHERE "
          + Tables.EPISODES + "." + EpisodeColumns.SHOW_ID
          + "="
          + Tables.SHOWS + "." + ShowColumns.ID
          + " AND ("
          + Tables.EPISODES + "." + EpisodeColumns.WATCHING
          + "=1"
          + " OR "
          + Tables.EPISODES + "." + EpisodeColumns.CHECKED_IN
          + "=1"
          + ")"
          + ")";
    }
  }

  @TableEndpoint(table = Tables.SEASONS)
  public static class Seasons {

    @MapColumns public static Map<String, String> mapColumns() {
      Map<String, String> map = new HashMap<>();

      map.put(SeasonColumns.SHOW_TITLE, getShowTitleQuery());
      map.put(SeasonColumns.AIRED_COUNT, getAiredQuery());
      map.put(SeasonColumns.UNAIRED_COUNT, getUnairedQuery());
      map.put(SeasonColumns.WATCHED_AIRED_COUNT, getWatchedAiredCount());
      map.put(SeasonColumns.COLLECTED_AIRED_COUNT, getCollectedAiredCount());
      map.put(SeasonColumns.EPISODE_COUNT, getEpisodeCountQuery());
      map.put(Tables.SEASONS + "." + SeasonColumns.SHOW_TITLE, getShowTitleQuery());
      map.put(Tables.SEASONS + "." + SeasonColumns.AIRED_COUNT, getAiredQuery());
      map.put(Tables.SEASONS + "." + SeasonColumns.UNAIRED_COUNT, getUnairedQuery());
      map.put(Tables.SEASONS + "." + SeasonColumns.WATCHED_AIRED_COUNT, getWatchedAiredCount());
      map.put(Tables.SEASONS + "." + SeasonColumns.COLLECTED_AIRED_COUNT, getCollectedAiredCount());
      map.put(Tables.SEASONS + "." + SeasonColumns.EPISODE_COUNT, getEpisodeCountQuery());

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

    @Where(path = Path.SEASONS + "/" + Path.FROM_SHOW + "/#")
    public static String[] fromShowWhere() {
      return new String[] {
          getEpisodeCountQuery() + ">0",
      };
    }

    public static final String DEFAULT_SORT =
        DatabaseSchematic.Tables.SEASONS + "." + SeasonColumns.SEASON + " DESC";

    @NotifyInsert(paths = Path.SEASONS)
    public static Uri[] notifyInsert(ContentValues values) {
      final long showId = values.getAsLong(SeasonColumns.SHOW_ID);
      return new Uri[] {
          fromShow(showId),
      };
    }

    @NotifyUpdate(paths = {
        Path.SEASONS, Path.SEASONS + "/#", Path.SEASONS + "/" + Path.FROM_SHOW + "/#"
    })
    public static Uri[] notifyUpdate(Context context, Uri uri, String where, String[] whereArgs) {
      Set<Uri> uris = new HashSet<>();

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

    public static String getShowTitleQuery() {
      return "(SELECT " + ShowColumns.TITLE + " FROM "
          + Tables.SHOWS
          + " WHERE "
          + Tables.SEASONS
          + "."
          + SeasonColumns.SHOW_ID
          + "="
          + Tables.SHOWS
          + "."
          + ShowColumns.ID
          + ")";
    }

    public static String getAiredQuery() {
      final long firstAiredOffset = FirstAiredOffsetPreference.getInstance().getOffsetMillis();
      final long currentTime = System.currentTimeMillis();
      return "(SELECT COUNT(*) FROM "
          + Tables.EPISODES
          + " WHERE "
          + Tables.EPISODES
          + "."
          + EpisodeColumns.SEASON_ID
          + "="
          + Tables.SEASONS
          + "."
          + SeasonColumns.ID
          + " AND "
          + Tables.EPISODES
          + "."
          + EpisodeColumns.FIRST_AIRED
          + "<="
          + (currentTime + firstAiredOffset)
          + " AND "
          + Tables.EPISODES
          + "."
          + EpisodeColumns.SEASON
          + ">0"
          + ")";
    }

    public static String getUnairedQuery() {
      final long firstAiredOffset = FirstAiredOffsetPreference.getInstance().getOffsetMillis();
      final long currentTime = System.currentTimeMillis();
      return "(SELECT COUNT(*) FROM "
          + Tables.EPISODES
          + " WHERE "
          + Tables.EPISODES
          + "."
          + EpisodeColumns.SEASON_ID
          + "="
          + Tables.SEASONS
          + "."
          + SeasonColumns.ID
          + " AND "
          + Tables.EPISODES
          + "."
          + EpisodeColumns.FIRST_AIRED
          + ">"
          + (currentTime - firstAiredOffset)
          + " AND "
          + Tables.EPISODES
          + "."
          + EpisodeColumns.SEASON
          + ">0"
          + ")";
    }

    public static String getWatchedAiredCount() {
      final long firstAiredOffset = FirstAiredOffsetPreference.getInstance().getOffsetMillis();
      final long currentTime = System.currentTimeMillis();
      return "(SELECT COUNT(*) FROM "
          + Tables.EPISODES
          + " WHERE "
          + Tables.EPISODES
          + "."
          + EpisodeColumns.SEASON_ID
          + "="
          + Tables.SEASONS
          + "."
          + SeasonColumns.ID
          + " AND "
          + Tables.EPISODES
          + "."
          + EpisodeColumns.FIRST_AIRED
          + "<="
          + (currentTime - firstAiredOffset)
          + " AND "
          + Tables.EPISODES
          + "."
          + EpisodeColumns.SEASON
          + ">0"
          + " AND "
          + Tables.EPISODES + "." + EpisodeColumns.WATCHED + "=1"
          + ")";
    }

    public static String getCollectedAiredCount() {
      final long firstAiredOffset = FirstAiredOffsetPreference.getInstance().getOffsetMillis();
      final long currentTime = System.currentTimeMillis();
      return "(SELECT COUNT(*) FROM "
          + Tables.EPISODES
          + " WHERE "
          + Tables.EPISODES
          + "."
          + EpisodeColumns.SEASON_ID
          + "="
          + Tables.SEASONS
          + "."
          + SeasonColumns.ID
          + " AND "
          + Tables.EPISODES
          + "."
          + EpisodeColumns.FIRST_AIRED
          + "<="
          + (currentTime - firstAiredOffset)
          + " AND "
          + Tables.EPISODES
          + "."
          + EpisodeColumns.SEASON
          + ">0"
          + " AND "
          + Tables.EPISODES + "." + EpisodeColumns.IN_COLLECTION + "=1"
          + ")";
    }

    public static String getEpisodeCountQuery() {
      return "(SELECT COUNT(*) FROM "
          + Tables.EPISODES
          + " WHERE "
          + Tables.EPISODES
          + "."
          + EpisodeColumns.SEASON_ID
          + "="
          + Tables.SEASONS
          + "."
          + SeasonColumns.ID
          + ")";
    }
  }

  @TableEndpoint(table = Tables.EPISODES)
  public static class Episodes {

    @MapColumns public static Map<String, String> mapColumns() {
      Map<String, String> map = new HashMap<>();

      map.put(EpisodeColumns.SHOW_TITLE, getShowTitleQuery());

      return map;
    }

    @ContentUri(
        path = Path.EPISODES,
        type = Type.EPISODE) @NotificationUri(
        paths = {
            Path.SHOWS + "/" + Path.WATCHING, Path.EPISODES + "/" + Path.WATCHING,
            Path.EPISODES + "/" + Path.WATCHLIST
        })
    public static final Uri EPISODES = buildUri(Path.EPISODES);

    @ContentUri(
        path = Path.EPISODES + "/" + Path.WITH_SHOW,
        type = Type.EPISODE,
        join = Joins.EPISODES_WITH_SHOW
    )
    public static final Uri EPISODES_WITH_SHOW = buildUri(Path.EPISODES, Path.WITH_SHOW);

    @ContentUri(
        path = Path.EPISODES + "/" + Path.WATCHLIST,
        type = Type.EPISODE,
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
    public static Uri[] notifyInsert(ContentValues values) {
      final long showId = values.getAsLong(EpisodeColumns.SHOW_ID);
      final long seasonId = values.getAsLong(EpisodeColumns.SEASON_ID);
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
      Set<Uri> uris = new HashSet<>();

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

    public static String getShowTitleQuery() {
      return "(SELECT " + ShowColumns.TITLE + " FROM "
          + Tables.SHOWS
          + " WHERE "
          + Tables.EPISODES
          + "."
          + EpisodeColumns.SHOW_ID
          + "="
          + Tables.SHOWS
          + "."
          + ShowColumns.ID
          + ")";
    }
  }

  @TableEndpoint(table = Tables.SHOW_GENRES)
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
    public static Uri insertReturnUri(ContentValues values) {
      final long showId = values.getAsLong(ShowGenreColumns.SHOW_ID);
      return fromShow(showId);
    }

    @NotifyInsert(paths = Path.SHOW_GENRES)
    public static Uri[] notifyInsert(ContentValues values) {
      final long showId = values.getAsLong(ShowGenreColumns.SHOW_ID);
      return new Uri[] {
          fromShow(showId),
      };
    }
  }

  @TableEndpoint(table = Tables.SHOW_CAST)
  public static class ShowCast {

    @ContentUri(
        path = Path.SHOW_CAST,
        type = Type.SHOW_CAST,
        join = Joins.SHOW_CAST)
    public static final Uri SHOW_CAST = buildUri(Path.SHOW_CAST);

    @InexactContentUri(
        path = Path.SHOW_CAST + "/#",
        type = Type.SHOW_CAST,
        name = "CAST_WITHID",
        whereColumn = ShowCastColumns.ID,
        pathSegment = 1,
        join = Joins.SHOW_CAST)
    public static Uri withId(long id) {
      return buildUri(Path.SHOW_CAST, String.valueOf(id));
    }

    @InexactContentUri(
        path = Path.SHOW_CAST + "/" + Path.FROM_SHOW + "/#",
        type = Type.SHOW_CAST,
        name = "CAST_FROMSHOW",
        whereColumn = ShowCastColumns.SHOW_ID,
        pathSegment = 2,
        join = Joins.SHOW_CAST)
    public static Uri fromShow(long showId) {
      return buildUri(Path.SHOW_CAST, Path.FROM_SHOW, String.valueOf(showId));
    }

    @InexactContentUri(
        path = Path.SHOW_CAST + "/" + Path.FROM_PERSON + "/#",
        type = Type.SHOW_CAST,
        name = "CAST_WITHPERSON",
        whereColumn = ShowCastColumns.PERSON_ID,
        pathSegment = 2,
        join = Joins.SHOW_CAST)
    public static Uri withPerson(long personId) {
      return buildUri(Path.SHOW_CAST, Path.FROM_PERSON, String.valueOf(personId));
    }

    @InsertUri(paths = Path.SHOW_CAST)
    public static Uri insertReturnUri(ContentValues values) {
      final long showId = values.getAsLong(ShowCastColumns.SHOW_ID);
      return fromShow(showId);
    }

    @NotifyInsert(paths = Path.SHOW_CAST)
    public static Uri[] notifyInsert(ContentValues values) {
      final long showId = values.getAsLong(ShowCastColumns.SHOW_ID);
      final long personId = values.getAsLong(ShowCastColumns.PERSON_ID);
      return new Uri[] {
          fromShow(showId), withPerson(personId),
      };
    }
  }

  @TableEndpoint(table = Tables.SHOW_CREW)
  public static class ShowCrew {

    @ContentUri(
        path = Path.SHOW_CREW,
        type = Type.SHOW_CREW,
        join = Joins.SHOW_CREW)
    public static final Uri SHOW_CREW = buildUri(Path.SHOW_CREW);

    @InexactContentUri(
        path = Path.SHOW_CREW + "/#",
        type = Type.SHOW_CREW,
        name = "CREW_WITHID",
        whereColumn = ShowCrewColumns.ID,
        pathSegment = 1,
        join = Joins.SHOW_CREW)
    public static Uri withId(long id) {
      return buildUri(Path.SHOW_CREW, String.valueOf(id));
    }

    @InexactContentUri(
        path = Path.SHOW_CREW + "/" + Path.FROM_SHOW + "/#",
        type = Type.SHOW_CREW,
        name = "CREW_FROMSHOW",
        whereColumn = ShowCrewColumns.SHOW_ID,
        pathSegment = 2,
        join = Joins.SHOW_CREW)
    public static Uri fromShow(long showId) {
      return buildUri(Path.SHOW_CREW, Path.FROM_SHOW, String.valueOf(showId));
    }

    @InexactContentUri(
        path = Path.SHOW_CREW + "/" + Path.FROM_PERSON + "/#",
        type = Type.SHOW_CREW,
        name = "CREW_WITHPERSON",
        whereColumn = ShowCrewColumns.PERSON_ID,
        pathSegment = 2,
        join = Joins.SHOW_CREW)
    public static Uri withPerson(long personId) {
      return buildUri(Path.SHOW_CREW, Path.FROM_PERSON, String.valueOf(personId));
    }

    @NotifyInsert(paths = Path.SHOW_CREW)
    public static Uri[] notifyInsert(ContentValues values) {
      final long showId = values.getAsLong(ShowCrewColumns.SHOW_ID);
      final long personId = values.getAsLong(ShowCrewColumns.PERSON_ID);
      return new Uri[] {
          fromShow(showId), withPerson(personId),
      };
    }

    @InsertUri(paths = Path.SHOW_CREW)
    public static Uri insertUri(ContentValues values) {
      final long showId = values.getAsLong(ShowCrewColumns.SHOW_ID);
      return fromShow(showId);
    }
  }

  @TableEndpoint(table = Tables.MOVIES)
  public static class Movies {

    @ContentUri(
        path = Path.MOVIES,
        type = Type.MOVIE)
    @NotificationUri(
        paths = {
            Path.MOVIES + "/" + Path.WATCHED, Path.MOVIES + "/" + Path.COLLECTED,
            Path.MOVIES + "/" + Path.WATCHLIST, Path.MOVIES + "/" + Path.WATCHING,
            Path.MOVIES + "/" + Path.TRENDING, Path.MOVIES + "/" + Path.RECOMMENDED
        })
    public static final Uri MOVIES = buildUri(Path.MOVIES);

    @ContentUri(
        path = Path.MOVIES + "/" + Path.WATCHED,
        type = Type.MOVIE,
        where = {
            MovieColumns.WATCHED + "=1", MovieColumns.LAST_SYNC + ">0"
        }
    )
    public static final Uri MOVIES_WATCHED = buildUri(Path.MOVIES, Path.WATCHED);

    @ContentUri(
        path = Path.MOVIES + "/" + Path.COLLECTED,
        type = Type.MOVIE,
        where = {
            MovieColumns.IN_COLLECTION + "=1", MovieColumns.LAST_SYNC + ">0"
        }
    )
    public static final Uri MOVIES_COLLECTED = buildUri(Path.MOVIES, Path.COLLECTED);

    @ContentUri(
        path = Path.MOVIES + "/" + Path.WATCHLIST,
        type = Type.MOVIE,
        where = {
            MovieColumns.IN_WATCHLIST + "=1", MovieColumns.LAST_SYNC + ">0"
        }
    )
    public static final Uri MOVIES_WATCHLIST = buildUri(Path.MOVIES, Path.WATCHLIST);

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
        where = MovieColumns.WATCHING + "=1 OR " + MovieColumns.CHECKED_IN + "=1")
    public static final Uri WATCHING = buildUri(Path.MOVIES, Path.WATCHING);

    @NotifyUpdate(paths = {
        Path.MOVIES + "/" + Path.WATCHING
    })
    public static Uri[] notifyUpdate(Context context, String where, String[] whereArgs) {
      Set<Uri> uris = new HashSet<>();

      Cursor c = context.getContentResolver().query(WATCHING, new String[] {
          MovieColumns.ID,
      }, where, whereArgs, null);

      while (c.moveToNext()) {
        final long id = Cursors.getLong(c, MovieColumns.ID);
        uris.add(withId(id));
      }

      c.close();

      return uris.toArray(new Uri[uris.size()]);
    }

    @ContentUri(
        path = Path.MOVIES + "/" + Path.TRENDING,
        type = Type.MOVIE,
        where = {
            MovieColumns.TRENDING_INDEX + ">=0"
        },
        defaultSort = MovieColumns.TRENDING_INDEX + " ASC")
    public static final Uri TRENDING = buildUri(Path.MOVIES, Path.TRENDING);

    @ContentUri(
        path = Path.MOVIES + "/" + Path.RECOMMENDED,
        type = Type.MOVIE,
        where = {
            MovieColumns.RECOMMENDATION_INDEX + ">=0"
        },
        defaultSort = MovieColumns.RECOMMENDATION_INDEX + " ASC")
    public static final Uri RECOMMENDED = buildUri(Path.MOVIES, Path.RECOMMENDED);

    @ContentUri(
        path = Path.MOVIES + "/" + Path.ANTICIPATED,
        type = Type.MOVIE,
        where = {
            MovieColumns.ANTICIPATED_INDEX + ">=0"
        },
        defaultSort = MovieColumns.ANTICIPATED_INDEX + " ASC")
    public static final Uri ANTICIPATED = buildUri(Path.MOVIES, Path.ANTICIPATED);

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

    public static final String SORT_ANTICIPATED =
        DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.ANTICIPATED_INDEX + " ASC";

    public static final String SORT_WATCHED =
        DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.WATCHED_AT + " DESC";

    public static final String SORT_COLLECTED =
        DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.COLLECTED_AT + " DESC";
  }

  @TableEndpoint(table = Tables.MOVIE_GENRES)
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
    public static Uri[] notifyInsert(ContentValues values) {
      final long movieId = values.getAsLong(MovieGenreColumns.MOVIE_ID);
      return new Uri[] {
          fromMovie(movieId),
      };
    }

    @InsertUri(paths = Path.MOVIE_GENRES)
    public static Uri insertUri(ContentValues values) {
      final long movieId = values.getAsLong(MovieGenreColumns.MOVIE_ID);
      return fromMovie(movieId);
    }
  }

  @TableEndpoint(table = Tables.MOVIE_CAST)
  public static class MovieCast {

    @ContentUri(
        path = Path.MOVIE_CAST,
        type = Type.MOVIE_CAST,
        join = Joins.MOVIE_CAST)
    public static final Uri MOVIE_CAST = buildUri(Path.MOVIE_CAST);

    @InexactContentUri(
        path = Path.MOVIE_CAST + "/#",
        type = Type.MOVIE_CAST,
        name = "CAST_WITHID",
        whereColumn = MovieCastColumns.ID,
        pathSegment = 1,
        join = Joins.MOVIE_CAST)
    public static Uri withId(long id) {
      return buildUri(Path.MOVIE_CAST, String.valueOf(id));
    }

    @InexactContentUri(
        path = Path.MOVIE_CAST + "/" + Path.FROM_MOVIE + "/#",
        type = Type.MOVIE_CAST,
        name = "CAST_FROMMOVIE",
        whereColumn = MovieCastColumns.MOVIE_ID,
        pathSegment = 2,
        join = Joins.MOVIE_CAST)
    public static Uri fromMovie(long movieId) {
      return buildUri(Path.MOVIE_CAST, Path.FROM_MOVIE, String.valueOf(movieId));
    }

    @InexactContentUri(
        path = Path.MOVIE_CAST + "/" + Path.FROM_PERSON + "/#",
        type = Type.MOVIE_CAST,
        name = "CAST_WITHPERSON",
        whereColumn = MovieCastColumns.PERSON_ID,
        pathSegment = 2,
        join = Joins.MOVIE_CAST)
    public static Uri withPerson(long personId) {
      return buildUri(Path.MOVIE_CAST, Path.FROM_PERSON, String.valueOf(personId));
    }

    @NotifyInsert(paths = Path.MOVIE_CAST)
    public static Uri[] notifyInsert(ContentValues values) {
      final long movieId = values.getAsLong(MovieCastColumns.MOVIE_ID);
      final long personId = values.getAsLong(MovieCastColumns.PERSON_ID);
      return new Uri[] {
          fromMovie(movieId), withPerson(personId),
      };
    }

    @InsertUri(paths = Path.MOVIE_CAST)
    public static Uri insertUri(ContentValues values) {
      final long movieId = values.getAsLong(MovieCastColumns.MOVIE_ID);
      return fromMovie(movieId);
    }
  }

  @TableEndpoint(table = Tables.MOVIE_CREW)
  public static class MovieCrew {

    @ContentUri(
        path = Path.MOVIE_CREW,
        type = Type.MOVIE_CREW,
        join = Joins.MOVIE_CREW)
    public static final Uri MOVIE_CREW = buildUri(Path.MOVIE_CREW);

    @InexactContentUri(
        path = Path.MOVIE_CREW + "/#",
        type = Type.MOVIE_CREW,
        name = "CREW_WITHID",
        whereColumn = MovieCrewColumns.ID,
        pathSegment = 1,
        join = Joins.MOVIE_CREW)
    public static Uri withId(long id) {
      return buildUri(Path.MOVIE_CREW, String.valueOf(id));
    }

    @InexactContentUri(
        path = Path.MOVIE_CREW + "/" + Path.FROM_MOVIE + "/#",
        type = Type.MOVIE_CREW,
        name = "CREW_FROMMOVIE",
        whereColumn = MovieCrewColumns.MOVIE_ID,
        pathSegment = 2,
        join = Joins.MOVIE_CREW)
    public static Uri fromMovie(long movieId) {
      return buildUri(Path.MOVIE_CREW, Path.FROM_MOVIE, String.valueOf(movieId));
    }

    @InexactContentUri(
        path = Path.MOVIE_CREW + "/" + Path.FROM_PERSON + "/#",
        type = Type.MOVIE_CREW,
        name = "CREW_WITHPERSON",
        whereColumn = MovieCrewColumns.PERSON_ID,
        pathSegment = 2,
        join = Joins.MOVIE_CREW)
    public static Uri withPerson(long personId) {
      return buildUri(Path.MOVIE_CREW, Path.FROM_PERSON, String.valueOf(personId));
    }

    @NotifyInsert(paths = Path.MOVIE_CREW)
    public static Uri[] notifyInsert(ContentValues values) {
      final long movieId = values.getAsLong(MovieCrewColumns.MOVIE_ID);
      final long personId = values.getAsLong(MovieCrewColumns.PERSON_ID);
      return new Uri[] {
          fromMovie(movieId), withPerson(personId),
      };
    }

    @InsertUri(paths = Path.MOVIE_CREW)
    public static Uri insertUri(ContentValues values) {
      final long movieId = values.getAsLong(MovieCrewColumns.MOVIE_ID);
      return fromMovie(movieId);
    }
  }

  @TableEndpoint(table = Tables.RECENT_QUERIES)
  public static class RecentQueries {

    @ContentUri(
        path = Path.RECENT_QUERIES,
        type = Type.RECENT_QUERIES,
        defaultSort = DatabaseContract.RecentQueriesColumns.QUERIED_AT + " DESC")
    public static final Uri RECENT_QUERIES = buildUri(Path.RECENT_QUERIES);
  }

  @TableEndpoint(table = Tables.PEOPLE)
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

    @NotifyUpdate(paths = {
        Path.PEOPLE, Path.PEOPLE + "/#"
    })
    public static Uri[] notifyUpdate(Context context, Uri uri, String where, String[] whereArgs) {
      Set<Uri> uris = new HashSet<>();

      Cursor people = context.getContentResolver().query(uri, new String[] {
          PersonColumns.ID,
      }, where, whereArgs, null);

      List<Long> peopleIds = new ArrayList<>();
      while (people.moveToNext()) {
        final long personId = Cursors.getLong(people, PersonColumns.ID);
        peopleIds.add(personId);
      }

      people.close();

      String joined = Joiner.on(", ").join(peopleIds);

      Cursor c = context.getContentResolver().query(ShowCast.SHOW_CAST, new String[] {
          ShowCastColumns.SHOW_ID,
      }, ShowCastColumns.PERSON_ID + " IN (?)", new String[] {
          joined
      }, null);

      while (c.moveToNext()) {
        final long showId = Cursors.getLong(c, ShowCastColumns.SHOW_ID);

        uris.add(ShowCast.fromShow(showId));
      }
      c.close();

      c = context.getContentResolver().query(MovieCast.MOVIE_CAST, new String[] {
          MovieCastColumns.MOVIE_ID,
      }, MovieCastColumns.PERSON_ID + " IN (?)", new String[] {
          joined
      }, null);

      while (c.moveToNext()) {
        final long showId = Cursors.getLong(c, MovieCastColumns.MOVIE_ID);

        uris.add(MovieCast.fromMovie(showId));
      }
      c.close();

      return uris.toArray(new Uri[uris.size()]);
    }
  }

  @TableEndpoint(table = Tables.LISTS)
  public static class Lists {

    @ContentUri(
        path = Path.LISTS,
        type = Type.LISTS)
    public static final Uri LISTS = buildUri(Path.LISTS);

    @InexactContentUri(
        path = Path.LISTS + "/#",
        type = Type.LIST_ID,
        name = "LIST_WITHID",
        whereColumn = ListsColumns.ID,
        pathSegment = 1)
    public static Uri withId(long id) {
      return buildUri(Path.LISTS, String.valueOf(id));
    }

    public static long getId(Uri uri) {
      return Long.valueOf(uri.getPathSegments().get(1));
    }
  }

  @TableEndpoint(table = Tables.LIST_ITEMS) public static class ListItems {

    @ContentUri(
        path = Path.LIST_ITEMS,
        type = Type.LIST_ITEMS)
    public static final Uri LIST_ITEMS = buildUri(Path.LIST_ITEMS);

    @InexactContentUri(
        path = Path.LIST_ITEMS + "/" + Path.IN_LIST + "/#",
        type = Type.LIST_ITEMS,
        name = "LISTITEMS_INLIST",
        join = Joins.LIST,
        whereColumn = ListItemColumns.LIST_ID,
        pathSegment = 2)
    public static Uri inList(long id) {
      return buildUri(Path.LIST_ITEMS, Path.IN_LIST, String.valueOf(id));
    }
  }

  @TableEndpoint(table = Tables.USERS) public static class Users {

    @ContentUri(
        path = Path.USERS,
        type = Type.USERS)
    public static final Uri USERS = buildUri(Path.USERS);

    @InexactContentUri(
        path = Path.USERS + "/#",
        type = Type.USER_ID,
        name = "USER_WITHID",
        whereColumn = UserColumns.ID,
        pathSegment = 1)
    public static Uri withId(long id) {
      return buildUri(Path.USERS, String.valueOf(id));
    }

    public static long getUserId(Uri uri) {
      return Long.valueOf(uri.getPathSegments().get(1));
    }
  }

  @TableEndpoint(table = Tables.COMMENTS) public static class Comments {

    @ContentUri(
        path = Path.COMMENTS,
        type = Type.COMMENTS)
    @NotificationUri(paths = {
        Path.COMMENTS + "/" + Path.WITH_USER,
        Path.COMMENTS + "/" + Path.WITH_PARENT + "/#",
        Path.COMMENTS + "/" + Path.FROM_SHOW + "/#",
        Path.COMMENTS + "/" + Path.FROM_MOVIE + "/#",
        Path.COMMENTS + "/" + Path.FROM_EPISODE + "/#",
    })
    public static final Uri COMMENTS = buildUri(Path.COMMENTS);

    @ContentUri(
        path = Path.COMMENTS + "/" + Path.WITH_USER,
        type = Type.COMMENTS,
        join = Joins.COMMENT_PROFILE)
    public static final Uri COMMENTS_WITH_PROFILE = buildUri(Path.COMMENTS, Path.WITH_USER);

    @InexactContentUri(
        path = Path.COMMENTS + "/#",
        type = Type.COMMENT_ID,
        name = "COMMENT_WITH_ID",
        whereColumn = CommentColumns.ID,
        pathSegment = 1)
    public static Uri withId(long id) {
      return buildUri(Path.COMMENTS, String.valueOf(id));
    }

    @InexactContentUri(
        path = Path.COMMENTS + "/" + Path.WITH_PARENT + "/#",
        type = Type.COMMENTS,
        name = "COMMENTS_WITH_PARENT",
        join = Joins.COMMENT_PROFILE,
        whereColumn = CommentColumns.PARENT_ID,
        pathSegment = 2)
    public static Uri withParent(long parentId) {
      return buildUri(Path.COMMENTS, Path.WITH_PARENT, String.valueOf(parentId));
    }

    @InexactContentUri(
        path = Path.COMMENTS + "/" + Path.FROM_SHOW + "/#",
        type = Type.COMMENTS,
        name = "COMMENTS_FROM_SHOW",
        join = Joins.COMMENT_PROFILE,
        where = CommentColumns.ITEM_TYPE + "='" + ItemTypeString.SHOW + "'",
        whereColumn = CommentColumns.ITEM_ID,
        pathSegment = 2)
    public static Uri fromShow(long showId) {
      return buildUri(Path.COMMENTS, Path.FROM_SHOW, String.valueOf(showId));
    }

    @InexactContentUri(
        path = Path.COMMENTS + "/" + Path.FROM_MOVIE + "/#",
        type = Type.COMMENTS,
        name = "COMMENTS_FROM_MOVIE",
        join = Joins.COMMENT_PROFILE,
        where = CommentColumns.ITEM_TYPE + "='" + ItemTypeString.MOVIE + "'",
        whereColumn = CommentColumns.ITEM_ID,
        pathSegment = 2)
    public static Uri fromMovie(long movieId) {
      return buildUri(Path.COMMENTS, Path.FROM_MOVIE, String.valueOf(movieId));
    }

    @InexactContentUri(
        path = Path.COMMENTS + "/" + Path.FROM_EPISODE + "/#",
        type = Type.COMMENTS,
        name = "COMMENTS_FROM_EPISODE",
        join = Joins.COMMENT_PROFILE,
        where = CommentColumns.ITEM_TYPE + "='" + ItemTypeString.EPISODE + "'",
        whereColumn = CommentColumns.ITEM_ID,
        pathSegment = 2)
    public static Uri fromEpisode(long episodeId) {
      return buildUri(Path.COMMENTS, Path.FROM_EPISODE, String.valueOf(episodeId));
    }
  }

  @TableEndpoint(table = Tables.SHOW_RELATED)
  public static class RelatedShows {

    @ContentUri(
        path = Path.SHOW_RELATED,
        type = Type.SHOW,
        join = Joins.SHOW_RELATED)
    public static final Uri RELATED = buildUri(Path.SHOW_RELATED);

    @InexactContentUri(
        path = Path.SHOW_RELATED + "/" + Path.FROM_SHOW + "/#",
        type = Type.SHOW,
        name = "RELATED_FROMSHOW",
        whereColumn = RelatedShowsColumns.SHOW_ID,
        pathSegment = 2,
        join = Joins.SHOW_RELATED,
        defaultSort = RelatedShowsColumns.RELATED_INDEX + " ASC")
    public static Uri fromShow(long showId) {
      return buildUri(Path.SHOW_RELATED, Path.FROM_SHOW, String.valueOf(showId));
    }

    @InexactContentUri(
        path = Path.SHOW_RELATED + "/#",
        type = Type.SHOW,
        name = "RELATED_WITHID",
        whereColumn = RelatedShowsColumns.ID,
        pathSegment = 1,
        join = Joins.SHOW_RELATED)
    public static Uri withId(long id) {
      return buildUri(Path.SHOW_RELATED, String.valueOf(id));
    }
  }

  @TableEndpoint(table = Tables.MOVIE_RELATED)
  public static class RelatedMovies {

    @ContentUri(
        path = Path.MOVIE_RELATED,
        type = Type.MOVIE,
        join = Joins.MOVIE_RELATED)
    public static final Uri RELATED = buildUri(Path.MOVIE_RELATED);

    @InexactContentUri(
        path = Path.MOVIE_RELATED + "/" + Path.FROM_MOVIE + "/#",
        type = Type.MOVIE,
        name = "RELATED_FROMMOVIE",
        whereColumn = RelatedMoviesColumns.MOVIE_ID,
        pathSegment = 2,
        join = Joins.MOVIE_RELATED,
        defaultSort = RelatedMoviesColumns.RELATED_INDEX + " ASC")
    public static Uri fromMovie(long movieId) {
      return buildUri(Path.MOVIE_RELATED, Path.FROM_MOVIE, String.valueOf(movieId));
    }

    @InexactContentUri(
        path = Path.MOVIE_RELATED + "/#",
        type = Type.MOVIE,
        name = "RELATED_WITHID",
        whereColumn = RelatedMoviesColumns.ID,
        pathSegment = 1,
        join = Joins.MOVIE_RELATED)
    public static Uri withId(long id) {
      return buildUri(Path.MOVIE_RELATED, String.valueOf(id));
    }
  }
}
