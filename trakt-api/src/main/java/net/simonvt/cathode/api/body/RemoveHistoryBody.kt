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

class RemoveHistoryBody private constructor(
  val movies: List<TraktIdItem>,
  val shows: List<ShowTraktId>,
  val people: List<TraktIdItem>,
  val ids: List<Long>
) {

  class Builder {

    val movies = mutableListOf<TraktIdItem>()
    val shows = mutableListOf<ShowTraktId>()
    val people = mutableListOf<TraktIdItem>()
    val ids = mutableListOf<Long>()

    fun movie(traktId: Long): Builder {
      val movie = movies.firstOrNull { it.ids.trakt == traktId }
      if (movie == null) {
        movies.add(TraktIdItem.withId(traktId))
      }
      return this
    }

    fun show(traktId: Long): Builder {
      val show = shows.firstOrNull { it.ids.trakt == traktId }
      if (show == null) {
        shows.add(ShowTraktId(TraktId(traktId)))
      }
      return this
    }

    fun season(showTraktId: Long, season: Int): Builder {
      var show: ShowTraktId? = shows.firstOrNull { it.ids.trakt == showTraktId }
      if (show == null) {
        show = ShowTraktId(TraktId(showTraktId))
        shows.add(show)
      }

      val ratingSeason = show.seasons.firstOrNull { it.number == season }
      if (ratingSeason == null) {
        show.seasons.add(SeasonNumber(season))
      }

      return this
    }

    fun episode(showTraktId: Long, season: Int, episode: Int): Builder {
      var show: ShowTraktId? = shows.firstOrNull { it.ids.trakt == showTraktId }
      if (show == null) {
        show = ShowTraktId(TraktId(showTraktId))
        shows.add(show)
      }

      var idSeason = show.seasons.firstOrNull { it.number == season }
      if (idSeason == null) {
        idSeason = SeasonNumber(season)
        show.seasons.add(idSeason)
      }

      val idEpisode = idSeason.episodes.firstOrNull { it.number == episode }
      if (idEpisode == null) {
        idSeason.episodes.add(EpisodeNumber(episode))
      }

      return this
    }

    fun person(traktId: Long): Builder {
      val person = movies.firstOrNull { it.ids.trakt == traktId }
      if (person == null) {
        people.add(TraktIdItem.withId(traktId))
      }
      return this
    }

    fun id(id: Long): Builder {
      ids.firstOrNull { it == id } ?: ids.add(id)
      return this
    }

    fun build(): RemoveHistoryBody {
      return RemoveHistoryBody(movies, shows, people, ids)
    }
  }
}
