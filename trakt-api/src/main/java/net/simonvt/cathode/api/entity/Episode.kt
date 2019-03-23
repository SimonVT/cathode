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

data class Episode(
  var season: Int? = null,
  var number: Int? = null,
  var title: String? = null,
  var ids: Ids,
  var number_abs: Int? = null,
  var overview: String? = null,
  var first_aired: IsoTime? = null,
  var updated_at: IsoTime? = null,
  var rating: Float? = null,
  var votes: Int? = null,
  var available_translations: List<String>? = null
)
