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

enum class Action(val value: String) {
  SCROBBLE("scrobble"), CHECKIN("checkin"), WATCH("watch");

  override fun toString(): String {
    return value
  }
}

class ActionAdapter {

  @ToJson
  fun toJson(action: Action): String = action.value

  @FromJson
  fun fromJson(value: String): Action? = Action.values().firstOrNull { it.value == value }
}
