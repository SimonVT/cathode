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

package net.simonvt.cathode.entity

import net.simonvt.cathode.api.enumeration.ItemType

data class ListItem(
  val listItemId: Long,
  val listId: Long,
  val type: ItemType,
  val show: ListShow? = null,
  val season: ListSeason? = null,
  val episode: ListEpisode? = null,
  val movie: ListMovie? = null,
  val person: ListPerson? = null
)

data class ListShow(
  val id: Long,
  val title: String,
  val overview: String,
  val watchedCount: Int,
  val collectedCount: Int,
  val watchlistCount: Int,
  val rating: Float
)

data class ListSeason(
  val id: Long,
  val season: Int,
  val showId: Long,
  val showTitle: String
)

data class ListEpisode(
  val id: Long,
  val season: Int,
  val episode: Int,
  val title: String,
  val watched: Boolean,
  val firstAired: Long,
  val showTitle: String
)

data class ListMovie(
  val id: Long,
  val title: String,
  val overview: String,
  val watched: Boolean,
  val collected: Boolean,
  val inWatchlist: Boolean,
  val watching: Boolean,
  val checkedIn: Boolean
)

data class ListPerson(
  val id: Long,
  val name: String
)
