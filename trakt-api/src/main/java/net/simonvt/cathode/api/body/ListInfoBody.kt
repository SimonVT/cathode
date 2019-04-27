/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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

import net.simonvt.cathode.api.enumeration.Privacy
import net.simonvt.cathode.api.enumeration.SortBy
import net.simonvt.cathode.api.enumeration.SortOrientation

data class ListInfoBody constructor(
  val name: String,
  val description: String? = null,
  val privacy: Privacy? = null,
  val display_numbers: Boolean?,
  val allow_comments: Boolean?,
  val sort_by: SortBy,
  val sort_how: SortOrientation
)
