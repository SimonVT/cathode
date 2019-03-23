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

import com.squareup.moshi.Json

data class Rating(val rating: Float, val votes: Int, val distribution: Distribution)

data class Distribution(
  @Json(name = "1") val one: Int,
  @Json(name = "2") val two: Int,
  @Json(name = "3") val three: Int,
  @Json(name = "4") val four: Int,
  @Json(name = "5") val five: Int,
  @Json(name = "6") val six: Int,
  @Json(name = "7") val seven: Int,
  @Json(name = "8") val eight: Int,
  @Json(name = "9") val nine: Int,
  @Json(name = "10") val ten: Int
)
