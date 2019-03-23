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

package net.simonvt.cathode.api.enumeration

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

enum class GrantType constructor(val value: String) {
  AUTHORIZATION_CODE("authorization_code"),
  REFRESH_TOKEN("refresh_token");

  override fun toString(): String {
    return value
  }
}

class GrantTypeAdapter {

  @ToJson
  fun toJson(grantType: GrantType): String = grantType.value

  @FromJson
  fun fromJson(value: String): GrantType? = GrantType.values().firstOrNull { it.value == value }
}
