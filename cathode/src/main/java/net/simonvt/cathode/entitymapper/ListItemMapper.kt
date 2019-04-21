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
import net.simonvt.cathode.api.enumeration.ItemType.EPISODE
import net.simonvt.cathode.api.enumeration.ItemType.MOVIE
import net.simonvt.cathode.api.enumeration.ItemType.PERSON
import net.simonvt.cathode.api.enumeration.ItemType.SEASON
import net.simonvt.cathode.api.enumeration.ItemType.SHOW
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.common.database.Cursors
import net.simonvt.cathode.entity.ListEpisode
import net.simonvt.cathode.entity.ListItem
import net.simonvt.cathode.entity.ListMovie
import net.simonvt.cathode.entity.ListPerson
import net.simonvt.cathode.entity.ListSeason
import net.simonvt.cathode.entity.ListShow
import net.simonvt.cathode.provider.DatabaseContract
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.DatabaseContract.LastModifiedColumns
import net.simonvt.cathode.provider.DatabaseContract.ListItemColumns
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns
import net.simonvt.cathode.provider.DatabaseSchematic.Tables
import net.simonvt.cathode.provider.util.DataHelper
import net.simonvt.cathode.provider.util.SqlCoalesce
import net.simonvt.cathode.provider.util.SqlColumn

object ListItemMapper : MappedCursorLiveData.CursorMapper<ListItem> {

  override fun map(cursor: Cursor): ListItem? {
    return if (cursor.moveToFirst()) mapItem(cursor) else null
  }

  fun mapItem(cursor: Cursor): ListItem {
    val listItemId = Cursors.getLong(cursor, ListItemColumns.ID)
    val itemType = Cursors.getInt(cursor, ListItemColumns.ITEM_TYPE)
    val itemId = Cursors.getLong(cursor, ListItemColumns.ITEM_ID)
    val listId = Cursors.getLong(cursor, ListItemColumns.LIST_ID)

    when (itemType) {
      DatabaseContract.ItemType.SHOW -> {
        val title = Cursors.getString(cursor, ShowColumns.TITLE)
        val overview = Cursors.getString(cursor, ListItemColumns.OVERVIEW)
        val watchedCount = Cursors.getInt(cursor, ShowColumns.WATCHED_COUNT)
        val collectedCount = Cursors.getInt(cursor, ShowColumns.IN_COLLECTION_COUNT)
        val watchlistCount = Cursors.getInt(cursor, ShowColumns.IN_WATCHLIST_COUNT)
        val rating = Cursors.getFloat(cursor, ShowColumns.RATING)

        val show =
          ListShow(
            itemId,
            title,
            overview,
            watchedCount,
            collectedCount,
            watchlistCount,
            rating
          )
        return ListItem(listItemId, listId, SHOW, show = show)
      }

      DatabaseContract.ItemType.SEASON -> {
        val seasonNumber = Cursors.getInt(cursor, SeasonColumns.SEASON)
        val showId = Cursors.getLong(cursor, SeasonColumns.SHOW_ID)
        val showTitle = Cursors.getString(cursor, "seasonShowTitle")

        val season = ListSeason(itemId, seasonNumber, showId, showTitle)
        return ListItem(
          listItemId,
          listId,
          SEASON,
          season = season
        )
      }

      DatabaseContract.ItemType.EPISODE -> {
        val title = Cursors.getString(cursor, EpisodeColumns.TITLE)
        val season = Cursors.getInt(cursor, EpisodeColumns.SEASON)
        val episodeNumber = Cursors.getInt(cursor, EpisodeColumns.EPISODE)
        val watched = Cursors.getBoolean(cursor, EpisodeColumns.WATCHED)
        var firstAired = Cursors.getLong(cursor, EpisodeColumns.FIRST_AIRED)
        if (firstAired != 0L) {
          firstAired = DataHelper.getFirstAired(firstAired)
        }
        val showTitle = Cursors.getString(cursor, "episodeShowTitle")

        val episode =
          ListEpisode(
            itemId,
            season,
            episodeNumber,
            title,
            watched,
            firstAired,
            showTitle
          )
        return ListItem(
          listItemId,
          listId,
          EPISODE,
          episode = episode
        )
      }

      DatabaseContract.ItemType.MOVIE -> {
        val title = Cursors.getString(cursor, MovieColumns.TITLE)
        val overview = Cursors.getString(cursor, ListItemColumns.OVERVIEW)
        val watched = Cursors.getBoolean(cursor, MovieColumns.WATCHED)
        val collected = Cursors.getBoolean(cursor, MovieColumns.IN_COLLECTION)
        val inWatchlist = Cursors.getBoolean(cursor, MovieColumns.IN_WATCHLIST)
        val watching = Cursors.getBoolean(cursor, MovieColumns.WATCHING)
        val checkedIn = Cursors.getBoolean(cursor, MovieColumns.CHECKED_IN)

        val movie =
          ListMovie(
            itemId,
            title,
            overview,
            watched,
            collected,
            inWatchlist,
            watching,
            checkedIn
          )
        return ListItem(
          listItemId,
          listId,
          MOVIE,
          movie = movie
        )
      }

      DatabaseContract.ItemType.PERSON -> {
        val name = Cursors.getString(cursor, PersonColumns.NAME)
        val person = ListPerson(itemId, name)
        return ListItem(
          listItemId,
          listId,
          PERSON,
          person = person
        )
      }

      else -> throw IllegalArgumentException("Unknown item type: $itemType")
    }
  }

  val projection = arrayOf(
    SqlColumn.table(Tables.LIST_ITEMS).column(ListItemColumns.ID),
    ListItemColumns.LIST_ID,
    ListItemColumns.ITEM_TYPE,
    ListItemColumns.ITEM_ID,

    SqlCoalesce.coaloesce(
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.OVERVIEW),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.OVERVIEW)
    ).`as`(
      ListItemColumns.OVERVIEW
    ),

    SqlColumn.table(Tables.SHOWS).column(ShowColumns.TITLE),
    SqlColumn.table(Tables.SHOWS).column(ShowColumns.WATCHED_COUNT),
    SqlColumn.table(Tables.SHOWS).column(ShowColumns.IN_COLLECTION_COUNT),
    SqlColumn.table(Tables.SHOWS).column(ShowColumns.IN_WATCHLIST),
    SqlColumn.table(Tables.SHOWS).column(ShowColumns.IN_WATCHLIST_COUNT),
    SqlColumn.table(Tables.SHOWS).column(ShowColumns.RATING),
    SqlColumn.table(Tables.SHOWS).column(LastModifiedColumns.LAST_MODIFIED),

    SqlColumn.table(Tables.SEASONS).column(SeasonColumns.SEASON),
    SqlColumn.table(Tables.SEASONS).column(SeasonColumns.SHOW_ID),
    SqlColumn.table(Tables.SEASONS).column(LastModifiedColumns.LAST_MODIFIED),
    "(SELECT " + ShowColumns.TITLE + " FROM " + Tables.SHOWS + " WHERE " +
        Tables.SHOWS + "." + ShowColumns.ID + "=" + Tables.SEASONS + "." + SeasonColumns.SHOW_ID +
        ") AS seasonShowTitle",
    "(SELECT " + ShowColumns.POSTER + " FROM " + Tables.SHOWS + " WHERE " +
        Tables.SHOWS + "." + ShowColumns.ID + "=" + Tables.SEASONS + "." + SeasonColumns.SHOW_ID +
        ") AS seasonShowPoster",

    SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.TITLE),
    SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.SEASON),
    SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.EPISODE),
    SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.WATCHED),
    SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.FIRST_AIRED),
    SqlColumn.table(Tables.EPISODES).column(LastModifiedColumns.LAST_MODIFIED),
    "(SELECT " + ShowColumns.TITLE + " FROM " + Tables.SHOWS + " WHERE " +
        Tables.SHOWS + "." + ShowColumns.ID + "=" + Tables.EPISODES + "." + EpisodeColumns.SHOW_ID +
        ") AS episodeShowTitle",

    SqlColumn.table(Tables.MOVIES).column(MovieColumns.TITLE),
    SqlColumn.table(Tables.MOVIES).column(MovieColumns.WATCHED),
    SqlColumn.table(Tables.MOVIES).column(MovieColumns.IN_COLLECTION),
    SqlColumn.table(Tables.MOVIES).column(MovieColumns.IN_WATCHLIST),
    SqlColumn.table(Tables.MOVIES).column(MovieColumns.WATCHING),
    SqlColumn.table(Tables.MOVIES).column(MovieColumns.CHECKED_IN),
    SqlColumn.table(Tables.MOVIES).column(LastModifiedColumns.LAST_MODIFIED),

    SqlColumn.table(Tables.PEOPLE).column(PersonColumns.NAME),
    SqlColumn.table(Tables.PEOPLE).column(LastModifiedColumns.LAST_MODIFIED)
  )
}
