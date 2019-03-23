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

data class ShowProgress(
  val aired: Int,
  val completed: Int,
  val last_watched_at: IsoTime? = null,
  val last_collected_at: IsoTime? = null,
  val reset_at: IsoTime? = null,
  val seasons: List<ProgressSeason>,
  val hidden_seasons: List<ProgressSeason>,
  val next_episode: LastNextEpisode?,
  val last_episode: LastNextEpisode?
)

data class ProgressSeason(
  val number: Int,
  val aired: Int? = null,
  val completed: Int? = null,
  val episodes: List<ProgressEpisode>
)

data class ProgressEpisode(
  val number: Int,
  val completed: Boolean,
  val last_watched_at: IsoTime? = null,
  val collected_at: IsoTime? = null
)

data class LastNextEpisode(
  val season: Int,
  val number: Int,
  val title: String,
  val ids: Ids
)
