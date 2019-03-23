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

data class CheckinResponse(
  var watched_at: IsoTime,
  var sharing: Sharing,
  var movie: CheckInMovie? = null,
  var show: CheckInShow? = null,
  var episode: CheckInEpisode? = null
)

data class CheckInMovie(val title: String, val year: Int, val ids: Ids)
data class CheckInShow(val title: String, val year: Int, val ids: Ids)
data class CheckInEpisode(val season: Int, val number: Int, val title: String, val ids: Ids)
