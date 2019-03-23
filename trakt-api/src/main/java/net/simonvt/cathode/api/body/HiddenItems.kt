/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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

class HiddenItems private constructor(
  val movies: List<TraktIdItem>,
  val shows: List<TraktIdItem>,
  val seasons: List<TraktIdItem>
) {

  class Builder {
    private val movies = mutableListOf<TraktIdItem>()
    private val shows = mutableListOf<TraktIdItem>()
    private val seasons = mutableListOf<TraktIdItem>()

    fun movie(traktId: Long): Builder {
      movies.firstOrNull { it.ids.trakt == traktId } ?: movies.add(TraktIdItem.withId(traktId))
      return this
    }

    fun show(traktId: Long): Builder {
      shows.firstOrNull { it.ids.trakt == traktId } ?: shows.add(TraktIdItem.withId(traktId))
      return this
    }

    fun seasons(traktId: Long): Builder {
      seasons.firstOrNull { it.ids.trakt == traktId } ?: seasons.add(TraktIdItem.withId(traktId))
      return this
    }

    fun build(): HiddenItems = HiddenItems(movies, shows, seasons)
  }
}
