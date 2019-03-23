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

import net.simonvt.cathode.api.enumeration.ShowStatus

data class Show(
  var title: String? = null,
  var year: Int? = null,
  var ids: Ids,
  var overview: String? = null,
  var first_aired: String? = null,
  var airs: Airs? = null,
  var runtime: Int? = null,
  var certification: String? = null,
  var network: String? = null,
  var country: String? = null,
  var updated_at: IsoTime? = null,
  var trailer: String? = null,
  var homepage: String? = null,
  var status: ShowStatus? = null,
  var rating: Float? = null,
  var votes: Int? = null,
  var language: String? = null,
  var available_translations: List<String>? = null,
  var genres: List<String>? = null
)
