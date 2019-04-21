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

data class Episode(
  val id: Long,
  val showId: Long,
  val seasonId: Long,
  val season: Int,
  val episode: Int,
  val numberAbs: Int,
  val title: String?,
  val overview: String?,
  val traktId: Long,
  val imdbId: String?,
  val tvdbId: Int,
  val tmdbId: Int,
  val tvrageId: Long,
  val firstAired: Long,
  val updatedAt: Long,
  val userRating: Int,
  val ratedAt: Long,
  val rating: Float,
  val votes: Int,
  val plays: Int,
  val watched: Boolean,
  val watchedAt: Long,
  val inCollection: Boolean,
  val collectedAt: Long,
  val inWatchlist: Boolean,
  val watchedlistedAt: Long,
  val watching: Boolean,
  val checkedIn: Boolean,
  val checkinStartedAt: Long,
  val checkinExpiresAt: Long,
  val lastCommentSync: Long,
  val notificationDismissed: Boolean,
  val showTitle: String?
)
