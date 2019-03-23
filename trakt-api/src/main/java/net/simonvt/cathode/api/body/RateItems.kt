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

class RateItems private constructor(val movies: List<RatingMovie>, val shows: List<RatingShow>) {

  class Builder {

    val movies = mutableListOf<RatingMovie>()

    val shows = mutableListOf<RatingShow>()

    fun movie(traktId: Long, rating: Int, ratedAt: String): Builder {
      val movie = movies.firstOrNull { it.ids.trakt == traktId }
      if (movie != null) {
        throw IllegalArgumentException("Movie $traktId already rated in this request")
      }
      movies.add(RatingMovie(TraktId(traktId), rating, ratedAt))
      return this
    }

    fun show(traktId: Long, rating: Int, ratedAt: String): Builder {
      val show = shows.firstOrNull { it.ids.trakt == traktId }
      if (show?.rating != null) {
        throw IllegalArgumentException("Show $traktId already rated in this request")
      }
      shows.add(RatingShow(TraktId(traktId), rating, ratedAt))
      return this
    }

    fun season(showTraktId: Long, season: Int, rating: Int, ratedAt: String): Builder {
      var show: RatingShow? = shows.firstOrNull { it.ids.trakt == showTraktId }
      if (show == null) {
        show = RatingShow(TraktId(showTraktId))
        shows.add(show)
      }

      val ratingSeason = show.seasons.firstOrNull { it.number == season }
      if (ratingSeason?.rating != null) {
        throw IllegalArgumentException("Season $showTraktId-$season already rated in this request")
      }
      show.seasons.add(RatingSeason(season, rating, ratedAt))

      return this
    }

    fun episode(
      showTraktId: Long,
      season: Int,
      episode: Int,
      rating: Int,
      ratedAt: String
    ): Builder {
      var show: RatingShow? = shows.firstOrNull { it.ids.trakt == showTraktId }
      if (show == null) {
        show = RatingShow(TraktId(showTraktId))
        shows.add(show)
      }

      var ratingSeason = show.seasons.firstOrNull { it.number == season }
      if (ratingSeason == null) {
        ratingSeason = RatingSeason(season)
        show.seasons.add(ratingSeason)
      }

      val ratingEpisode = ratingSeason.episodes.firstOrNull { it.number == episode }
      if (ratingEpisode == null) {
        ratingSeason.episodes.add(RatingEpisode(episode, rating, ratedAt))
      } else {
        throw IllegalArgumentException("Episode $showTraktId-$season-$episode already rated in this request")
      }

      return this
    }

    fun build(): RateItems {
      return RateItems(movies, shows)
    }
  }
}

class RatingMovie internal constructor(val ids: TraktId, val rating: Int, val rated_at: String)

class RatingShow internal constructor(
  val ids: TraktId,
  val rating: Int? = null,
  val rated_at: String? = null
) {
  val seasons = mutableListOf<RatingSeason>()
}

class RatingSeason(val number: Int, val rating: Int? = null, val rated_at: String? = null) {

  val episodes = mutableListOf<RatingEpisode>()
}

class RatingEpisode(val number: Int, val rating: Int, val rated_at: String)
