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

import net.simonvt.cathode.api.enumeration.ItemType

data class ListItem(
  val rank: Int,
  val id: Long,
  val listed_at: IsoTime,
  val type: ItemType,
  val movie: Movie? = null,
  val show: Show? = null,
  val season: Season? = null,
  val episode: Episode? = null,
  val person: Person? = null
)
