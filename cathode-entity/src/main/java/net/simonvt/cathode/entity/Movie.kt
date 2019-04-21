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

data class Movie(
  val id: Long,
  val traktId: Long,
  val imdbId: String?,
  val tmdbId: Int,
  val title: String,
  val titleNoArticle: String?,
  val year: Int,
  val released: String?,
  val runtime: Int,
  val tagline: String?,
  val overview: String?,
  val certification: String?,
  val language: String?,
  val homepage: String?,
  val trailer: String?,
  val userRating: Int,
  val rating: Float,
  val votes: Int,
  val watched: Boolean,
  val watchedAt: Long,
  val inCollection: Boolean,
  val collectedAt: Long,
  val inWatchlist: Boolean,
  val watchlistedAt: Long,
  val watching: Boolean,
  val checkedIn: Boolean,
  val checkinStartedAt: Long,
  val checkinExpiresAt: Long,
  val needsSync: Boolean,
  val lastSync: Long,
  val lastCommentSync: Long,
  val lastCreditsSync: Long,
  val lastRelatedSync: Long
)
