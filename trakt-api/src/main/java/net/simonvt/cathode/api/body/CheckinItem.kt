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

package net.simonvt.cathode.api.body

import net.simonvt.cathode.api.entity.Sharing

class CheckinItem private constructor(
  val movie: TraktIdItem?,
  val episode: TraktIdItem?,
  val sharing: Sharing?,
  val message: String?,
  val venue_id: String?,
  val venue_name: String?,
  val app_version: String?,
  val app_date: String?
) {

  data class Builder(
    var movie: TraktIdItem? = null,
    var episode: TraktIdItem? = null,
    var sharing: Sharing? = null,
    var message: String? = null,
    var venue_id: String? = null,
    var venue_name: String? = null,
    var app_version: String? = null,
    var app_date: String? = null
  ) {

    fun movie(traktId: Long) = apply { this.movie = TraktIdItem.withId(traktId) }
    fun episode(traktId: Long) = apply { this.episode = TraktIdItem.withId(traktId) }
    fun sharing(sharing: Sharing) = apply { this.sharing = sharing }
    fun message(message: String?) = apply { this.message = message }
    fun venueId(venueId: String?) = apply { this.venue_id = venueId }
    fun venueName(venueName: String?) = apply { this.venue_name = venueName }
    fun appVersion(appVersion: String) = apply { this.app_version = appVersion }
    fun appDate(appDate: String) = apply { this.app_date = appDate }
    fun build() =
      CheckinItem(movie, episode, sharing, message, venue_id, venue_name, app_version, app_date)
  }
}
