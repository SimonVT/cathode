/*
 * Copyright (C) 2018 Simon Vig Therkildsen
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

data class Season(
  val id: Long,
  val showId: Long,
  val season: Int,
  val tvdbId: Int?,
  val tmdbId: Int?,
  val tvrageId: Long?,
  val userRating: Int?,
  val ratedAt: Long?,
  val rating: Float?,
  val votes: Int?,
  val hiddenWatched: Boolean?,
  val hiddenCollected: Boolean?,
  val watchedCount: Int?,
  val airdateCount: Int?,
  val inCollectionCount: Int?,
  val inWatchlistCount: Int?,
  val showTitle: String?,
  val airedCount: Int?,
  val unairedCount: Int?,
  val watchedAiredCount: Int?,
  val collectedAiredCount: Int?,
  val episodeCount: Int?
)
