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

package net.simonvt.cathode.api.entity;

import com.google.gson.annotations.SerializedName;
import net.simonvt.cathode.api.enumeration.GrantType;

public class TokenRequest {

  String code;

  @SerializedName("client_id") String clientId;

  @SerializedName("client_secret") String clientSecret;

  @SerializedName("redirect_uri") String redirectUrl;

  @SerializedName("grant_type") GrantType grantType;

  public TokenRequest(String code, String clientId, String clientSecret, String redirectUrl,
      GrantType grantType) {
    this.code = code;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.redirectUrl = redirectUrl;
    this.grantType = grantType;
  }
}
