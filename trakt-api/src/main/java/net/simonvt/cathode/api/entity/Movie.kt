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

data class Movie(
  val title: String? = null,
  val year: Int? = null,
  val ids: Ids,
  val tagline: String? = null,
  val overview: String? = null,
  val released: String? = null,
  val runtime: Int? = null,
  val updated_at: String? = null,
  val trailer: String? = null,
  val homepage: String? = null,
  val rating: Float? = null,
  val votes: Int? = null,
  val language: String? = null,
  val available_translations: List<String>? = null,
  val genres: List<String>? = null,
  val certification: String? = null
)
