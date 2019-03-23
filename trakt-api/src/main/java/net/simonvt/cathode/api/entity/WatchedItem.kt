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

data class WatchedItem(
  val plays: Int? = null,
  val last_watched_at: IsoTime,
  val last_updated_at: IsoTime,
  val show: Show? = null,
  val seasons: List<WatchedSeason>? = null,
  val movie: Movie? = null
)

data class WatchedSeason(
  val number: Int,
  val episodes: List<WatchedEpisode>
)

data class WatchedEpisode(
  val number: Int,
  val plays: Int,
  val last_watched_at: IsoTime
)
