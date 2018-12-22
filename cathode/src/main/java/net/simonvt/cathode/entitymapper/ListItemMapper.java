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

package net.simonvt.cathode.entitymapper;

import android.database.Cursor;
import net.simonvt.cathode.common.data.MappedCursorLiveData;
import net.simonvt.cathode.common.database.Cursors;
import net.simonvt.cathode.common.entity.Episode;
import net.simonvt.cathode.common.entity.ListItem;
import net.simonvt.cathode.common.entity.Movie;
import net.simonvt.cathode.common.entity.Person;
import net.simonvt.cathode.common.entity.Season;
import net.simonvt.cathode.common.entity.Show;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.LastModifiedColumns;
import net.simonvt.cathode.provider.DatabaseContract.ListItemColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.util.DataHelper;
import net.simonvt.cathode.provider.util.SqlCoalesce;
import net.simonvt.cathode.provider.util.SqlColumn;

public class ListItemMapper implements MappedCursorLiveData.CursorMapper<ListItem> {

  public static final String[] PROJECTION = {
      SqlColumn.table(Tables.LIST_ITEMS).column(ListItemColumns.ID), ListItemColumns.LIST_ID,
      ListItemColumns.ITEM_TYPE, ListItemColumns.ITEM_ID,

      SqlCoalesce.coaloesce(SqlColumn.table(Tables.SHOWS).column(ShowColumns.OVERVIEW),
          SqlColumn.table(Tables.MOVIES).column(MovieColumns.OVERVIEW)).as(
          ListItemColumns.OVERVIEW),

      SqlColumn.table(Tables.SHOWS).column(ShowColumns.TITLE),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.WATCHED_COUNT),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.IN_COLLECTION_COUNT),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.IN_WATCHLIST),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.IN_WATCHLIST_COUNT),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.RATING),
      SqlColumn.table(Tables.SHOWS).column(LastModifiedColumns.LAST_MODIFIED),

      SqlColumn.table(Tables.SEASONS).column(SeasonColumns.SEASON),
      SqlColumn.table(Tables.SEASONS).column(SeasonColumns.SHOW_ID),
      SqlColumn.table(Tables.SEASONS).column(LastModifiedColumns.LAST_MODIFIED), "(SELECT "
      + ShowColumns.TITLE
      + " FROM "
      + Tables.SHOWS
      + " WHERE "
      + Tables.SHOWS
      + "."
      + ShowColumns.ID
      + "="
      + Tables.SEASONS
      + "."
      + SeasonColumns.SHOW_ID
      + ") AS seasonShowTitle", "(SELECT "
      + ShowColumns.POSTER
      + " FROM "
      + Tables.SHOWS
      + " WHERE "
      + Tables.SHOWS
      + "."
      + ShowColumns.ID
      + "="
      + Tables.SEASONS
      + "."
      + SeasonColumns.SHOW_ID
      + ") AS seasonShowPoster",

      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.TITLE),
      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.SEASON),
      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.EPISODE),
      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.WATCHED),
      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.FIRST_AIRED),
      SqlColumn.table(Tables.EPISODES).column(LastModifiedColumns.LAST_MODIFIED), "(SELECT "
      + ShowColumns.TITLE
      + " FROM "
      + Tables.SHOWS
      + " WHERE "
      + Tables.SHOWS
      + "."
      + ShowColumns.ID
      + "="
      + Tables.EPISODES
      + "."
      + EpisodeColumns.SHOW_ID
      + ") AS episodeShowTitle",

      SqlColumn.table(Tables.MOVIES).column(MovieColumns.TITLE),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.WATCHED),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.IN_COLLECTION),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.IN_WATCHLIST),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.WATCHING),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.CHECKED_IN),
      SqlColumn.table(Tables.MOVIES).column(LastModifiedColumns.LAST_MODIFIED),

      SqlColumn.table(Tables.PEOPLE).column(PersonColumns.NAME),
      SqlColumn.table(Tables.PEOPLE).column(LastModifiedColumns.LAST_MODIFIED),
  };

  @Override public ListItem map(Cursor cursor) {
    if (cursor.moveToFirst()) {
      return mapItem(cursor);
    }

    return null;
  }

  static ListItem mapItem(Cursor cursor) {
    long listItemId = Cursors.getLong(cursor, ListItemColumns.ID);
    int itemType = Cursors.getInt(cursor, ListItemColumns.ITEM_TYPE);
    long itemId = Cursors.getLong(cursor, ListItemColumns.ITEM_ID);
    long listId = Cursors.getLong(cursor, ListItemColumns.LIST_ID);

    switch (itemType) {
      case DatabaseContract.ItemType.SHOW: {
        String title = Cursors.getString(cursor, ShowColumns.TITLE);
        String overview = Cursors.getString(cursor, ListItemColumns.OVERVIEW);
        int watchedCount = Cursors.getInt(cursor, ShowColumns.WATCHED_COUNT);
        int collectedCount = Cursors.getInt(cursor, ShowColumns.IN_COLLECTION_COUNT);
        int watchlistCount = Cursors.getInt(cursor, ShowColumns.IN_WATCHLIST_COUNT);
        float rating = Cursors.getFloat(cursor, ShowColumns.RATING);
        Show show =
            new Show(itemId, title, null, null, null, null, overview, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                rating, null, null, null, null, null, null, null, null, null, null, null, null,
                null, watchedCount, null, collectedCount, watchlistCount, null, null, null, null,
                null, null, null, null, null, null);
        return new ListItem(listItemId, listId, show);
      }

      case DatabaseContract.ItemType.SEASON: {
        int seasonNumber = Cursors.getInt(cursor, SeasonColumns.SEASON);
        long showId = Cursors.getLong(cursor, SeasonColumns.SHOW_ID);
        String showTitle = Cursors.getString(cursor, "seasonShowTitle");
        Season season =
            new Season(itemId, showId, seasonNumber, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, showTitle, null, null, null, null, null);
        return new ListItem(listItemId, listId, season);
      }

      case DatabaseContract.ItemType.EPISODE: {
        String title = Cursors.getString(cursor, EpisodeColumns.TITLE);
        int season = Cursors.getInt(cursor, EpisodeColumns.SEASON);
        int episodeNumber = Cursors.getInt(cursor, EpisodeColumns.EPISODE);
        boolean watched = Cursors.getBoolean(cursor, EpisodeColumns.WATCHED);
        long firstAired = Cursors.getLong(cursor, EpisodeColumns.FIRST_AIRED);
        if (firstAired != 0L) {
          firstAired = DataHelper.getFirstAired(firstAired);
        }
        String showTitle = Cursors.getString(cursor, "episodeShowTitle");
        Episode episode =
            new Episode(itemId, null, null, season, episodeNumber, null, title, null, null, null,
                null, null, null, firstAired, null, null, null, null, null, null, watched, null,
                null, null, null, null, null, null, null, null, null, null, showTitle);
        return new ListItem(listItemId, listId, episode);
      }

      case DatabaseContract.ItemType.MOVIE: {
        String title = Cursors.getString(cursor, MovieColumns.TITLE);
        String overview = Cursors.getString(cursor, ListItemColumns.OVERVIEW);
        Boolean watched = Cursors.getBoolean(cursor, MovieColumns.WATCHED);
        Boolean collected = Cursors.getBoolean(cursor, MovieColumns.IN_COLLECTION);
        Boolean inWatchlist = Cursors.getBoolean(cursor, MovieColumns.IN_WATCHLIST);
        Boolean watching = Cursors.getBoolean(cursor, MovieColumns.WATCHING);
        Boolean checkedIn = Cursors.getBoolean(cursor, MovieColumns.CHECKED_IN);
        Movie movie =
            new Movie(itemId, null, null, null, title, null, null, null, null, null, overview, null,
                null, null, null, null, null, null, watched, null, collected, null, inWatchlist,
                null, watching, checkedIn, null, null, null, null, null, null, null);
        return new ListItem(listItemId, listId, movie);
      }

      case DatabaseContract.ItemType.PERSON: {
        String name = Cursors.getString(cursor, PersonColumns.NAME);
        Person person = new Person(itemId, name, null, null, null, null, null);
        return new ListItem(listItemId, listId, person);
      }

      default:
        throw new IllegalArgumentException("Unknown item type: " + itemType);
    }
  }
}
