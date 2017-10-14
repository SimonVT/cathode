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

package net.simonvt.cathode.provider.util;

import android.content.Context;
import android.database.Cursor;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.R;
import net.simonvt.cathode.settings.FirstAiredOffsetPreference;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.schematic.Cursors;

public final class DataHelper {

  private DataHelper() {
  }

  public static String getEpisodeTitle(Context context, Cursor cursor, int season, int episode,
      boolean watched) {
    return getEpisodeTitle(context, cursor, season, episode, watched, false);
  }

  public static String getEpisodeTitle(Context context, String title, int season, int episode,
      boolean watched) {
    return getEpisodeTitle(context, title, season, episode, watched, false);
  }

  public static String getEpisodeTitle(Context context, Cursor cursor, int season, int episode,
      boolean watched, boolean withNumber) {
    String title = Cursors.getString(cursor, EpisodeColumns.TITLE);

    return getEpisodeTitle(context, title, season, episode, watched, withNumber);
  }

  public static String getEpisodeTitle(Context context, String title, int season, int episode,
      boolean watched, boolean withNumber) {
    final boolean avoidSpoilers =
        Settings.get(context).getBoolean(Settings.SHOWS_AVOID_SPOILERS, false);

    if (avoidSpoilers && !watched) {
      return context.getString(R.string.season_x_episode, season, episode);
    }

    if (android.text.TextUtils.isEmpty(title)) {
      if (season == 0) {
        return context.getString(R.string.special_x, episode);
      } else {
        return context.getString(R.string.episode_x, episode);
      }
    }

    if (withNumber) {
      return context.getString(R.string.episode_with_number, season, episode, title);
    }

    return title;
  }

  public static String getEpisodeOverview(Context context, Cursor cursor, boolean watched) {
    final boolean avoidSpoilers =
        Settings.get(context).getBoolean(Settings.SHOWS_AVOID_SPOILERS, false);

    if (avoidSpoilers && !watched) {
      return context.getString(R.string.episode_overview_nospoiler);
    } else {
      return Cursors.getString(cursor, EpisodeColumns.OVERVIEW);
    }
  }

  public static long getFirstAired(Cursor cursor) {
    final long firstAired = Cursors.getLong(cursor, EpisodeColumns.FIRST_AIRED);
    final long offset = FirstAiredOffsetPreference.getInstance().getOffsetMillis();
    return firstAired + offset;
  }
}
