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
package net.simonvt.cathode.service;

import android.database.Cursor;
import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.settings.FirstAiredOffsetPreference;
import net.simonvt.cathode.util.DataHelper;
import net.simonvt.cathode.util.DateUtils;
import net.simonvt.schematic.Cursors;

public class DashClockService extends DashClockExtension {

  @Override protected void onUpdateData(int reason) {
    final long firstAiredOffset = FirstAiredOffsetPreference.getInstance().getOffsetMillis();
    Cursor c = getContentResolver().query(Episodes.EPISODES, null,
        EpisodeColumns.FIRST_AIRED + ">? AND "
        + EpisodeColumns.WATCHED + "=0"
        + " AND "
        + EpisodeColumns.NEEDS_SYNC
        + "=0"
        + " AND ((SELECT " + Tables.SHOWS + "." + ShowColumns.WATCHED_COUNT
        + " FROM " + Tables.SHOWS + " WHERE "
        + Tables.SHOWS + "." + ShowColumns.ID + "="
        + Tables.EPISODES + "." + EpisodeColumns.SHOW_ID
        + ")>0 OR " + EpisodeColumns.IN_WATCHLIST + "=1 OR "
        + "(SELECT " + Tables.SHOWS + "." + ShowColumns.IN_WATCHLIST
        + " FROM " + Tables.SHOWS + " WHERE "
        + Tables.SHOWS + "." + ShowColumns.ID + "="
        + Tables.EPISODES + "." + EpisodeColumns.SHOW_ID
        + ")=1)",
        new String[] {
            String.valueOf(System.currentTimeMillis() - firstAiredOffset),
        }, EpisodeColumns.FIRST_AIRED + " ASC LIMIT 1");

    if (c.moveToFirst()) {
      final int episode = Cursors.getInt(c, EpisodeColumns.EPISODE);
      final int season = Cursors.getInt(c, EpisodeColumns.SEASON);
      final String title = DataHelper.getEpisodeTitle(this, c, season, episode);
      final long showId = Cursors.getLong(c, EpisodeColumns.SHOW_ID);
      final long firstAired = DataHelper.getFirstAired(c);

      final String date = DateUtils.millisToString(this, firstAired, false);

      Cursor show = getContentResolver().query(Shows.withId(showId), null, null, null, null);
      if (!show.moveToFirst()) {
        show.close();
        return; // Wat
      }
      final String showTitle = Cursors.getString(show, ShowColumns.TITLE);
      show.close();

      ExtensionData data = new ExtensionData().visible(true)
          .status(date)
          .expandedTitle(showTitle + " - " + date)
          .expandedBody(season + "x" + episode + " - " + title);

      publishUpdate(data);
    } else {
      publishUpdate(new ExtensionData().visible(false));
    }
    c.close();
  }
}
