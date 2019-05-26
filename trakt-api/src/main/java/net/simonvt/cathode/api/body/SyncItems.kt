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

class SyncItems private constructor(val movies: List<SyncMovie>, val shows: List<SyncShow>) {

  class Builder {

    val movies = mutableListOf<SyncMovie>()

    val shows = mutableListOf<SyncShow>()

    fun movie(
      traktId: Long,
      watchedAt: String? = null,
      collectedAt: String? = null,
      listedAt: String? = null
    ): Builder {
      val movie = movies.firstOrNull { it.ids.trakt == traktId }
      if (movie != null) {
        throw IllegalArgumentException("Movie $traktId already added to request")
      }
      movies.add(SyncMovie(TraktId(traktId), watchedAt, collectedAt, listedAt))
      return this
    }

    fun show(
      traktId: Long,
      watchedAt: String? = null,
      collectedAt: String? = null,
      listedAt: String? = null
    ): Builder {
      val show = shows.firstOrNull { it.ids.trakt == traktId }
      if (show?.watched_at != null || show?.collected_at != null || show?.listed_at != null) {
        throw IllegalArgumentException("Show $traktId already added to request")
      }
      shows.add(SyncShow(TraktId(traktId), watchedAt, collectedAt, listedAt))
      return this
    }

    fun season(
      showTraktId: Long,
      season: Int,
      watchedAt: String? = null,
      collectedAt: String? = null,
      listedAt: String? = null
    ): Builder {
      var show = shows.firstOrNull { it.ids.trakt == showTraktId }
      if (show == null) {
        show = SyncShow(TraktId(showTraktId))
        shows.add(show)
      }

      val syncSeason = show.seasons.firstOrNull { it.number == season }
      if (syncSeason?.watched_at != null || syncSeason?.collected_at != null || syncSeason?.listed_at != null) {
        throw IllegalArgumentException("Season $showTraktId-$season already added to request")
      }
      show.seasons.add(SyncSeason(season, watchedAt, collectedAt, listedAt))

      return this
    }

    fun episode(
      showTraktId: Long,
      season: Int,
      episode: Int,
      watchedAt: String? = null,
      collectedAt: String? = null,
      listedAt: String? = null
    ): Builder {
      var show: SyncShow? = shows.firstOrNull { it.ids.trakt == showTraktId }
      if (show == null) {
        show = SyncShow(TraktId(showTraktId))
        shows.add(show)
      }

      var syncSeason = show.seasons.firstOrNull { it.number == season }
      if (syncSeason == null) {
        syncSeason = SyncSeason(season)
        show.seasons.add(syncSeason)
      }

      val syncEpisode = syncSeason.episodes.firstOrNull { it.number == episode }
      if (syncEpisode == null) {
        syncSeason.episodes.add(SyncEpisode(episode, watchedAt, collectedAt, listedAt))
      } else {
        throw IllegalArgumentException("Episode $showTraktId-$season-$episode already added to request")
      }

      return this
    }

    fun build(): SyncItems {
      return SyncItems(movies, shows)
    }
  }

  companion object {
    const val TIME_RELEASED = "released"
  }
}

class SyncMovie internal constructor(
  val ids: TraktId,
  val watched_at: String? = null,
  val collected_at: String? = null,
  val listed_at: String? = null
)

class SyncShow internal constructor(
  val ids: TraktId,
  val watched_at: String? = null,
  val collected_at: String? = null,
  val listed_at: String? = null,
  val seasons: MutableList<SyncSeason> = mutableListOf()
)

class SyncSeason(
  val number: Int,
  val watched_at: String? = null,
  val collected_at: String? = null,
  val listed_at: String? = null,
  val episodes: MutableList<SyncEpisode> = mutableListOf()
)

class SyncEpisode(
  val number: Int,
  val watched_at: String? = null,
  val collected_at: String? = null,
  val listed_at: String? = null
)
