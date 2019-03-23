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

data class SyncResponse(
  val added: Success? = null,
  val existing: Success? = null,
  val deleted: Success? = null,
  val not_found: Errors? = null
)

data class Success(
  val movies: Int? = null,
  val shows: Int? = null,
  val seasons: Int? = null,
  val episodes: Int? = null
)

data class Errors(
  val movies: List<ErrorMovie>? = null,
  val shows: List<ErrorShow>? = null,
  val seasons: List<ErrorSeason>? = null,
  val episodes: List<ErrorEpisode>? = null,
  val ids: List<Int>? = null
)

data class ErrorShow(val ids: Ids)
data class ErrorSeason(val ids: Ids)
data class ErrorEpisode(val ids: Ids)
data class ErrorMovie(val ids: Ids)
