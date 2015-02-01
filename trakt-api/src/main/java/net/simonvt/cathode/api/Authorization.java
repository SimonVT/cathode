/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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

package net.simonvt.cathode.api;

public final class Authorization {

  public static final String AUTHORIZATION_URL = "https://trakt.tv/oauth/authorize";

  private Authorization() {
  }

  // TODO: Pass some secret as state?
  public static String getOAuthUri(String applicationId, String redirectUri) {
    return AUTHORIZATION_URL
        + "?response_type=code&client_id="
        + applicationId
        + "&redirect_uri="
        + redirectUri;
  }

  public static String getOAuthUri(String applicationId, String redirectUri, String username) {
    return AUTHORIZATION_URL
        + "?response_type=code&client_id="
        + applicationId
        + "&redirect_uri="
        + redirectUri
        + "&username="
        + username;
  }
}
