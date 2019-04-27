/*
 * Copyright (C) 2019 Simon Vig Therkildsen
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

package net.simonvt.cathode.entitymapper

import android.database.Cursor
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.api.enumeration.ItemType.EPISODE
import net.simonvt.cathode.api.enumeration.ItemType.MOVIE
import net.simonvt.cathode.api.enumeration.ItemType.PERSON
import net.simonvt.cathode.api.enumeration.ItemType.SEASON
import net.simonvt.cathode.api.enumeration.ItemType.SHOW
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.common.database.getBoolean
import net.simonvt.cathode.common.database.getFloat
import net.simonvt.cathode.common.database.getInt
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.common.database.getString
import net.simonvt.cathode.common.database.getStringOrNull
import net.simonvt.cathode.entity.ListEpisode
import net.simonvt.cathode.entity.ListItem
import net.simonvt.cathode.entity.ListMovie
import net.simonvt.cathode.entity.ListPerson
import net.simonvt.cathode.entity.ListSeason
import net.simonvt.cathode.entity.ListShow
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.DatabaseContract.ListItemColumns
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns
import net.simonvt.cathode.provider.DatabaseSchematic.Tables
import net.simonvt.cathode.provider.ProviderSchematic.Episodes
import net.simonvt.cathode.provider.ProviderSchematic.Seasons
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.provider.util.DataHelper
import net.simonvt.cathode.provider.util.SqlCoalesce
import net.simonvt.cathode.provider.util.SqlColumn
import timber.log.Timber
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

object ListItemMapper : MappedCursorLiveData.CursorMapper<ListItem> {

  override fun map(cursor: Cursor): ListItem? {
    return if (cursor.moveToFirst()) mapItem(cursor) else null
  }

  fun mapItem(cursor: Cursor): ListItem {
    val listItemId = cursor.getLong(ListItemColumns.ID)
    val rank = cursor.getInt(ListItemColumns.RANK)
    val itemTypeString = cursor.getString(ListItemColumns.ITEM_TYPE)
    val itemType = ItemType.fromValue(itemTypeString)
    val itemId = cursor.getLong(ListItemColumns.ITEM_ID)
    val listId = cursor.getLong(ListItemColumns.LIST_ID)
    val listedAt = cursor.getLong(ListItemColumns.LISTED_AT)

    when (itemType) {
      SHOW -> {
        val title = cursor.getString(ShowColumns.TITLE)
        val titleNoArticle = cursor.getString(ShowColumns.TITLE_NO_ARTICLE)
        val overview = cursor.getString(COLUMN_OVERVIEW)
        val firstAired = cursor.getLong(COLUMN_FIRST_AIRED)
        val airedCount = cursor.getInt(SHOW_AIRED_COUNT)
        val watchedCount = cursor.getInt(ShowColumns.WATCHED_COUNT)
        val collectedCount = cursor.getInt(ShowColumns.IN_COLLECTION_COUNT)
        val watchlistCount = cursor.getInt(ShowColumns.IN_WATCHLIST_COUNT)
        val rating = cursor.getFloat(COLUMN_RATING)
        val votes = cursor.getInt(COLUMN_VOTES)
        val userRating = cursor.getInt(COLUMN_USER_RATING)
        val runtime = cursor.getInt(COLUMN_RUNTIME)

        val airedRuntime = airedCount * runtime

        val show =
          ListShow(
            itemId,
            title,
            titleNoArticle,
            overview,
            firstAired,
            watchedCount,
            collectedCount,
            watchlistCount,
            rating,
            votes,
            airedRuntime,
            userRating
          )
        return ListItem(listItemId, listId, rank, listedAt, SHOW, show = show)
      }

      SEASON -> {
        val seasonNumber = cursor.getInt(COLUMN_SEASON)
        val showId = cursor.getLong(SeasonColumns.SHOW_ID)
        val firstAired = cursor.getLong(COLUMN_FIRST_AIRED)
        val airedCount = cursor.getInt(SEASON_AIRED_COUNT)
        val rating = cursor.getFloat(COLUMN_RATING)
        val votes = cursor.getInt(COLUMN_VOTES)
        val userRating = cursor.getInt(COLUMN_USER_RATING)
        val runtime = cursor.getInt(SEASON_RUNTIME)
        val showTitle = cursor.getString(SeasonColumns.SHOW_TITLE)
        val showTitleNoArticle = cursor.getString(SEASON_SHOW_TITLE_NO_ARTICLE)

        val airedRuntime = airedCount * runtime

        val season = ListSeason(
          itemId,
          seasonNumber,
          showId,
          firstAired,
          votes,
          rating,
          userRating,
          airedRuntime,
          showTitle,
          showTitleNoArticle
        )
        return ListItem(
          listItemId,
          listId,
          rank,
          listedAt,
          SEASON,
          season = season
        )
      }

      EPISODE -> {
        val title = cursor.getString(EpisodeColumns.TITLE)
        val season = cursor.getInt(COLUMN_SEASON)
        val episodeNumber = cursor.getInt(EpisodeColumns.EPISODE)
        val watched = cursor.getBoolean(COLUMN_WATCHED)
        var firstAired = cursor.getLong(COLUMN_FIRST_AIRED)
        if (firstAired != 0L) {
          firstAired = DataHelper.getFirstAired(firstAired)
        }
        val rating = cursor.getFloat(COLUMN_RATING)
        val votes = cursor.getInt(COLUMN_VOTES)
        val userRating = cursor.getInt(COLUMN_USER_RATING)
        val runtime = cursor.getInt(EPISODE_RUNTIME)
        val showTitle = cursor.getString(EpisodeColumns.SHOW_TITLE)
        val showTitleNoArticle = cursor.getString(EPISODE_SHOW_TITLE_NO_ARTICLE)

        val episode =
          ListEpisode(
            itemId,
            season,
            episodeNumber,
            title,
            runtime,
            watched,
            firstAired,
            votes,
            rating,
            userRating,
            showTitle,
            showTitleNoArticle
          )
        return ListItem(
          listItemId,
          listId,
          rank,
          listedAt,
          EPISODE,
          episode = episode
        )
      }

      MOVIE -> {
        val title = cursor.getString(MovieColumns.TITLE)
        val titleNoArticle = cursor.getString(MovieColumns.TITLE_NO_ARTICLE)
        val overview = cursor.getString(COLUMN_OVERVIEW)
        val released = cursor.getStringOrNull(MovieColumns.RELEASED)
        // TODO: Add releasedMillis field to DB
        val releaseDate: Long = if (released.isNullOrEmpty()) {
          0L
        } else {
          val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
          try {
            df.parse(released).time
          } catch (e: ParseException) {
            Timber.e("Parsing release date %s failed", released)
            // Use current date.
            0L
          }
        }
        val runtime = cursor.getInt(COLUMN_RUNTIME)
        val watched = cursor.getBoolean(COLUMN_WATCHED)
        val collected = cursor.getBoolean(MovieColumns.IN_COLLECTION)
        val inWatchlist = cursor.getBoolean(MovieColumns.IN_WATCHLIST)
        val watching = cursor.getBoolean(MovieColumns.WATCHING)
        val checkedIn = cursor.getBoolean(MovieColumns.CHECKED_IN)
        val rating = cursor.getFloat(COLUMN_RATING)
        val votes = cursor.getInt(COLUMN_VOTES)
        val userRating = cursor.getInt(COLUMN_USER_RATING)

        val movie =
          ListMovie(
            itemId,
            title,
            titleNoArticle,
            overview,
            releaseDate,
            watched,
            collected,
            inWatchlist,
            watching,
            checkedIn,
            votes,
            rating,
            runtime,
            userRating
          )
        return ListItem(
          listItemId,
          listId,
          rank,
          listedAt,
          MOVIE,
          movie = movie
        )
      }

      PERSON -> {
        val name = cursor.getString(PersonColumns.NAME)
        val person = ListPerson(itemId, name)
        return ListItem(
          listItemId,
          listId,
          rank,
          listedAt,
          PERSON,
          person = person
        )
      }

      else -> throw IllegalArgumentException("Unknown item type: $itemType")
    }
  }

  private const val COLUMN_TITLE = "title"
  private const val COLUMN_OVERVIEW = "overview"
  private const val COLUMN_RATING = "rating"
  private const val COLUMN_VOTES = "votes"
  private const val COLUMN_USER_RATING = "userRating"
  private const val COLUMN_WATCHED = "watched"
  private const val COLUMN_FIRST_AIRED = "firstAired"
  private const val COLUMN_SEASON = "season"
  private const val COLUMN_RUNTIME = "runtime"

  private const val SHOW_AIRED_COUNT = "showAiredCount"
  private const val SEASON_AIRED_COUNT = "seasonAiredCount"
  private const val SEASON_SHOW_TITLE_NO_ARTICLE = "seasonShowTitleNoArticle"
  private const val SEASON_SHOW_POSTER = "seasonShowPoster"
  private const val SEASON_RUNTIME = "seasonRuntime"
  private const val EPISODE_RUNTIME = "episodeRuntime"
  private const val EPISODE_SHOW_TITLE_NO_ARTICLE = "episodeShowTitleNoArticle"

  val projection = arrayOf(
    SqlColumn.table(Tables.LIST_ITEMS).column(ListItemColumns.ID),
    ListItemColumns.LIST_ID,
    ListItemColumns.ITEM_TYPE,
    ListItemColumns.ITEM_ID,
    ListItemColumns.RANK,
    SqlColumn.table(Tables.LIST_ITEMS).column(ListItemColumns.LISTED_AT),

    SqlCoalesce.coaloesce(
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.OVERVIEW),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.OVERVIEW)
    ).`as`(
      COLUMN_OVERVIEW
    ),

    SqlCoalesce.coaloesce(
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.RATING),
      SqlColumn.table(Tables.SEASONS).column(SeasonColumns.RATING),
      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.RATING),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.RATING)
    ).`as`(
      COLUMN_RATING
    ),

    SqlCoalesce.coaloesce(
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.VOTES),
      SqlColumn.table(Tables.SEASONS).column(SeasonColumns.VOTES),
      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.VOTES),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.VOTES)
    ).`as`(
      COLUMN_VOTES
    ),

    SqlCoalesce.coaloesce(
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.USER_RATING),
      SqlColumn.table(Tables.SEASONS).column(SeasonColumns.USER_RATING),
      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.USER_RATING),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.USER_RATING)
    ).`as`(
      COLUMN_USER_RATING
    ),

    SqlCoalesce.coaloesce(
      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.WATCHED),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.WATCHED)
    ).`as`(
      COLUMN_WATCHED
    ),

    SqlCoalesce.coaloesce(
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.FIRST_AIRED),
      SqlColumn.table(Tables.SEASONS).column(SeasonColumns.FIRST_AIRED),
      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.FIRST_AIRED)
    ).`as`(
      COLUMN_FIRST_AIRED
    ),

    SqlCoalesce.coaloesce(
      SqlColumn.table(Tables.SEASONS).column(SeasonColumns.SEASON),
      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.SEASON)
    ).`as`(
      COLUMN_SEASON
    ),

    SqlCoalesce.coaloesce(
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.RUNTIME),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.RUNTIME)
    ).`as`(
      COLUMN_RUNTIME
    ),

    SqlColumn.table(Tables.SHOWS).column(ShowColumns.TITLE),
    SqlColumn.table(Tables.SHOWS).column(ShowColumns.TITLE_NO_ARTICLE),
    SqlColumn.table(Tables.SHOWS).column(ShowColumns.WATCHED_COUNT),
    SqlColumn.table(Tables.SHOWS).column(ShowColumns.IN_COLLECTION_COUNT),
    SqlColumn.table(Tables.SHOWS).column(ShowColumns.IN_WATCHLIST),
    SqlColumn.table(Tables.SHOWS).column(ShowColumns.IN_WATCHLIST_COUNT),
    Shows.getAiredQuery() + " AS " + SHOW_AIRED_COUNT,

    SqlColumn.table(Tables.SEASONS).column(SeasonColumns.SHOW_ID),
    Seasons.getAiredQuery() + " AS " + SEASON_AIRED_COUNT,
    Seasons.getShowTitleQuery() + " AS " + SeasonColumns.SHOW_TITLE,
    "(SELECT " + ShowColumns.TITLE_NO_ARTICLE + " FROM " + Tables.SHOWS + " WHERE " +
        Tables.SHOWS + "." + ShowColumns.ID + "=" + Tables.SEASONS + "." + SeasonColumns.SHOW_ID +
        ") AS " + SEASON_SHOW_TITLE_NO_ARTICLE,
    "(SELECT " + ShowColumns.RUNTIME + " FROM " + Tables.SHOWS + " WHERE " +
        Tables.SHOWS + "." + ShowColumns.ID + "=" + Tables.SEASONS + "." + SeasonColumns.SHOW_ID +
        ") AS " + SEASON_RUNTIME,

    SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.TITLE),
    SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.EPISODE),
    Episodes.getShowTitleQuery() + " AS " + EpisodeColumns.SHOW_TITLE,
    "(SELECT " + ShowColumns.TITLE_NO_ARTICLE + " FROM " + Tables.SHOWS + " WHERE " +
        Tables.SHOWS + "." + ShowColumns.ID + "=" + Tables.EPISODES + "." + EpisodeColumns.SHOW_ID +
        ") AS " + EPISODE_SHOW_TITLE_NO_ARTICLE,
    "(SELECT " + ShowColumns.RUNTIME + " FROM " + Tables.SHOWS + " WHERE " +
        Tables.SHOWS + "." + ShowColumns.ID + "=" + Tables.EPISODES + "." + EpisodeColumns.SHOW_ID +
        ") AS " + EPISODE_RUNTIME,

    SqlColumn.table(Tables.MOVIES).column(MovieColumns.TITLE),
    SqlColumn.table(Tables.MOVIES).column(MovieColumns.TITLE_NO_ARTICLE),
    SqlColumn.table(Tables.MOVIES).column(MovieColumns.RELEASED),
    SqlColumn.table(Tables.MOVIES).column(MovieColumns.IN_COLLECTION),
    SqlColumn.table(Tables.MOVIES).column(MovieColumns.IN_WATCHLIST),
    SqlColumn.table(Tables.MOVIES).column(MovieColumns.WATCHING),
    SqlColumn.table(Tables.MOVIES).column(MovieColumns.CHECKED_IN),

    SqlColumn.table(Tables.PEOPLE).column(PersonColumns.NAME)
  )
}
