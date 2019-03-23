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

package net.simonvt.cathode.api.entity

import com.squareup.moshi.Json
import net.simonvt.cathode.api.enumeration.Gender

data class Profile(
  val username: String,
  @Json(name = "private") val isPrivate: Boolean? = null,
  val name: String? = null,
  val vip: Boolean? = null,
  val vip_ep: Boolean? = null,
  val vip_og: Boolean? = null,
  val vip_years: Int? = null,
  val joined_at: IsoTime? = null,
  val location: String? = null,
  val about: String? = null,
  val gender: Gender? = null,
  val age: Int? = null,
  val images: Images? = null
)
