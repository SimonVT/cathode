/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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

package net.simonvt.cathode.appwidget;

import android.content.Context;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import net.simonvt.cathode.common.util.DateUtils;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.util.DataHelper;
import net.simonvt.cathode.util.SqlColumn;
import net.simonvt.schematic.Cursors;

public class ItemModel {

  private static final String COLUMN_EPISODE_ID = "episodeId";

  public static final String[] PROJECTION = new String[] {
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.ID),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.TITLE),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.OVERVIEW),
      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.ID) + " AS " + COLUMN_EPISODE_ID,
      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.TITLE),
      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.SEASON),
      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.EPISODE),
      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.FIRST_AIRED),
      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.WATCHED),
  };

  private List<WidgetItem> widgetItems;

  private ItemModel(List<WidgetItem> widgetItems) {
    this.widgetItems = widgetItems;
  }

  public int getItemCount() {
    return widgetItems == null ? 0 : widgetItems.size();
  }

  public int getItemType(int position) {
    return widgetItems.get(position).getItemType();
  }

  public DayInfo getDay(int position) {
    return (DayInfo) widgetItems.get(position);
  }

  public ItemInfo getItem(int position) {
    return (ItemInfo) widgetItems.get(position);
  }

  public long getNextUpdateTime() {
    // Update at least every six hours
    long updateTime = System.currentTimeMillis() + 6 * DateUtils.HOUR_IN_MILLIS;

    if (getItemCount() > 0) {
      ItemInfo info = getItem(1);
      final long airTime = info.getFirstAired() + DateUtils.HOUR_IN_MILLIS;
      if (airTime < updateTime) {
        updateTime = airTime;
      }
    }

    // Update at most once an hour
    updateTime = Math.max(updateTime, DateUtils.HOUR_IN_MILLIS);

    return updateTime;
  }

  public static ItemModel fromCursor(Context context, Cursor cursor) {
    List<WidgetItem> items = new ArrayList<>();

    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);

    final long currentDay = calendar.getTimeInMillis();

    DayInfo lastDay = null;

    cursor.moveToPosition(-1);
    while (cursor.moveToNext()) {
      final long showId = Cursors.getLong(cursor, ShowColumns.ID);
      final String showTitle = Cursors.getString(cursor, ShowColumns.TITLE);
      final String showOverview = Cursors.getString(cursor, ShowColumns.OVERVIEW);

      final long episodeId = Cursors.getLong(cursor, COLUMN_EPISODE_ID);
      final int season = Cursors.getInt(cursor, EpisodeColumns.SEASON);
      final int episode = Cursors.getInt(cursor, EpisodeColumns.EPISODE);
      final boolean watched = Cursors.getBoolean(cursor, EpisodeColumns.WATCHED);
      final long firstAired = DataHelper.getFirstAired(cursor);

      final String episodeTitle = DataHelper.getEpisodeTitle(context, cursor, season, episode,
          watched);

      String airTime = DateUtils.getTimeString(context, firstAired);

      ItemInfo item =
          new ItemInfo(showId, showTitle, showOverview, episodeId, episodeTitle, season, episode,
              firstAired, airTime);

      calendar.setTimeInMillis(firstAired);
      calendar.set(Calendar.HOUR_OF_DAY, 0);
      calendar.set(Calendar.MINUTE, 0);

      final long itemDay = calendar.getTimeInMillis();

      if (lastDay == null || itemDay != lastDay.dayStart) {
        lastDay = new DayInfo(itemDay, DateUtils.getDateString(itemDay));
        items.add(lastDay);
      }

      items.add(item);
    }

    return new ItemModel(items);
  }
}
