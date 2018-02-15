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

public final class TraktLinkSettings {

  public static final String TRAKT_LINK_PROMPTED = "traktLinkPrompted";
  public static final String TRAKT_LINKED = "traktLinked";
  public static final String TRAKT_AUTH_FAILED = "traktAuthFailed";

  public static final String TRAKT_ACCESS_TOKEN = "traktToken";
  public static final String TRAKT_REFRESH_TOKEN = "traktRefreshToken";
  public static final String TRAKT_TOKEN_EXPIRATION = "traktTokenExpiration";

  private TraktLinkSettings() {
  }

  public static boolean isLinkPrompted(Context context) {
    return Settings.get(context).getBoolean(TRAKT_LINK_PROMPTED, false);
  }

  public static boolean isLinked(Context context) {
    return Settings.get(context).getBoolean(TRAKT_LINKED, false);
  }

  public static boolean hasAuthFailed(Context context) {
    return Settings.get(context).getBoolean(TRAKT_AUTH_FAILED, false);
  }
}
