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
package net.simonvt.cathode.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateUtils;

public final class SuggestionsTimestamps {

  private static final String SETTINGS_FILE = "cathode_suggestions";

  public static final String SHOWS_RECOMMENDED = "suggestionsShowsRecommended";
  public static final String SHOWS_TRENDING = "suggestionsShowsTrending";
  public static final String SHOWS_ANTICIPATED = "suggestionsShowsAnticipated";
  public static final String MOVIES_RECOMMENDED = "suggestionsMoviesRecommended";
  public static final String MOVIES_TRENDING = "suggestionsMoviesTrending";
  public static final String MOVIES_ANTICIPATED = "suggestionsMoviesAnticipated";

  private SuggestionsTimestamps() {
  }

  private static SharedPreferences get(Context context) {
    return context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE);
  }

  public static boolean suggestionsNeedsUpdate(Context context, String key) {
    SharedPreferences settings = get(context);
    final long lastActivity = settings.getLong(key, -1);
    return System.currentTimeMillis() > lastActivity + 6 * DateUtils.HOUR_IN_MILLIS;
  }

  public static void updateSuggestions(Context context, String key) {
    final long currentTimeMillis = System.currentTimeMillis();
    get(context).edit().putLong(key, currentTimeMillis).apply();
  }

  public static void clearRecommended(Context context) {
    get(context).edit().remove(SHOWS_RECOMMENDED).remove(MOVIES_RECOMMENDED).apply();
  }
}
