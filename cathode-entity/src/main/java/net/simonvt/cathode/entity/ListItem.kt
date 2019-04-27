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
  val rank: Int,
  val listedAt: Long,
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
  val titleNoArticle: String,
  val overview: String,
  val firstAired: Long,
  val watchedCount: Int,
  val collectedCount: Int,
  val watchlistCount: Int,
  val rating: Float,
  val votes: Int,
  val airedRuntime: Int,
  val userRating: Int
)

data class ListSeason(
  val id: Long,
  val season: Int,
  val showId: Long,
  val firstAired: Long,
  val votes: Int,
  val rating: Float,
  val userRating: Int,
  val airedRuntime: Int,
  val showTitle: String,
  val showTitleNoArticle: String
)

data class ListEpisode(
  val id: Long,
  val season: Int,
  val episode: Int,
  val title: String,
  val runtime: Int,
  val watched: Boolean,
  val firstAired: Long,
  val votes: Int,
  val rating: Float,
  val userRating: Int,
  val showTitle: String,
  val showTitleNoArticle: String
)

data class ListMovie(
  val id: Long,
  val title: String,
  val titleNoArticle: String,
  val overview: String,
  val releaseDate: Long,
  val watched: Boolean,
  val collected: Boolean,
  val inWatchlist: Boolean,
  val watching: Boolean,
  val checkedIn: Boolean,
  val votes: Int,
  val rating: Float,
  val runtime: Int,
  val userRating: Int
)

data class ListPerson(
  val id: Long,
  val name: String
)
