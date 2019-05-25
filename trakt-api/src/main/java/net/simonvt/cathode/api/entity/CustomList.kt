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

package net.simonvt.cathode.api.entity

import net.simonvt.cathode.api.enumeration.Privacy

data class CustomList(
  val name: String,
  val description: String,
  val privacy: Privacy,
  val display_numbers: Boolean,
  val allow_comments: Boolean,
  val updated_at: IsoTime? = null,
  val item_count: Int,
  val comment_count: Int,
  val likes: Int,
  val ids: Ids,
  val user: Profile? = null
)
