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
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.provider.CathodeDatabase;
import net.simonvt.cathode.util.DateUtils;

public class DashClockService extends DashClockExtension {

  @Override protected void onUpdateData(int reason) {
    Cursor c = getContentResolver().query(CathodeContract.Episodes.CONTENT_URI, null,
        CathodeContract.Episodes.FIRST_AIRED + ">? AND "
        + CathodeContract.Episodes.WATCHED + "=0"
        + " AND ((SELECT " + CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.WATCHED_COUNT
        + " FROM " + CathodeDatabase.Tables.SHOWS + " WHERE "
        + CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows._ID + "="
        + CathodeDatabase.Tables.EPISODES + "." + CathodeContract.Episodes.SHOW_ID
        + ")>0 OR " + CathodeContract.Episodes.IN_WATCHLIST + "=1 OR "
        + "(SELECT " + CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.IN_WATCHLIST
        + " FROM " + CathodeDatabase.Tables.SHOWS + " WHERE "
        + CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows._ID + "="
        + CathodeDatabase.Tables.EPISODES + "." + CathodeContract.Episodes.SHOW_ID
        + ")=1)",
        new String[] {
            String.valueOf(System.currentTimeMillis()),
        }, CathodeContract.Episodes.FIRST_AIRED + " ASC LIMIT 1");

    if (c.moveToFirst()) {
      final int episode = c.getInt(c.getColumnIndex(CathodeContract.Episodes.EPISODE));
      final int season = c.getInt(c.getColumnIndex(CathodeContract.Episodes.SEASON));
      final String title = c.getString(c.getColumnIndex(CathodeContract.Episodes.TITLE));
      final long showId = c.getLong(c.getColumnIndex(CathodeContract.Episodes.SHOW_ID));
      final long firstAired = c.getLong(c.getColumnIndex(CathodeContract.Episodes.FIRST_AIRED));

      final String date = DateUtils.millisToString(this, firstAired, false);

      Cursor show =
          getContentResolver().query(CathodeContract.Shows.buildFromId(showId), null, null, null,
              null);
      if (!show.moveToFirst()) {
        show.close();
        return; // Wat
      }
      final String showTitle = show.getString(show.getColumnIndex(CathodeContract.Shows.TITLE));
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
