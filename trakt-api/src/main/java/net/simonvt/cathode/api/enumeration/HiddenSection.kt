/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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

enum class HiddenSection constructor(val value: String) {
  CALENDAR("calendar"),
  PROGRESS_WATCHED("progress_watched"),
  PROGRESS_COLLECTED("progress_collected"),
  RECOMMENDATIONS("recommendations");

  override fun toString(): String {
    return value
  }
}

class HiddenSectionAdapter {

  @ToJson
  fun toJson(hiddenSection: HiddenSection): String = hiddenSection.value

  @FromJson
  fun fromJson(value: String): HiddenSection? =
    HiddenSection.values().firstOrNull { it.value == value }
}
