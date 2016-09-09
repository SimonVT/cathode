/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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

package net.simonvt.cathode.util;

import android.content.Context;
import android.database.Cursor;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.schematic.Cursors;

public final class DataHelper {

  private DataHelper() {
  }

  public static String getEpisodeTitle(Context context, Cursor cursor, int season, int episode) {
    String title = Cursors.getString(cursor, EpisodeColumns.TITLE);

    if (android.text.TextUtils.isEmpty(title)) {
      if (season == 0) {
        title = context.getString(R.string.special_x, episode);
      } else {
        title = context.getString(R.string.episode_x, episode);
      }
    }

    return title;
  }
}
