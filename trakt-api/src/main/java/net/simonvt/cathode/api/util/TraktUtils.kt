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

package net.simonvt.cathode.api.util

object TraktUtils {

  @JvmStatic
  fun getTraktShowUrl(traktId: Long): String {
    return "https://trakt.tv/search/trakt/$traktId?id_type=show"
  }

  @JvmStatic
  fun getTraktSeasonUrl(traktId: Long): String {
    return "https://trakt.tv/search/trakt/$traktId?id_type=season"
  }

  @JvmStatic
  fun getTraktEpisodeUrl(traktId: Long): String {
    return "https://trakt.tv/search/trakt/$traktId?id_type=episode"
  }

  @JvmStatic
  fun getTraktMovieUrl(traktId: Long): String {
    return "https://trakt.tv/search/trakt/$traktId?id_type=movie"
  }

  @JvmStatic
  fun getTraktPersonUrl(traktId: Long): String {
    return "https://trakt.tv/search/trakt/$traktId?id_type=person"
  }

  @JvmStatic
  fun getImdbUrl(imdbId: String): String {
    return "http://www.imdb.com/title/$imdbId"
  }

  @JvmStatic
  fun getTvdbUrl(tvdbId: Int): String {
    return "http://thetvdb.com/?tab=series&id=$tvdbId"
  }

  @JvmStatic
  fun getTmdbTvUrl(tmdbId: Int): String {
    return "https://www.themoviedb.org/tv/$tmdbId"
  }

  @JvmStatic
  fun getTmdbMovieUrl(tmdbId: Int): String {
    return "https://www.themoviedb.org/movie/$tmdbId"
  }

  @JvmStatic
  fun getYoutubeId(url: String): String? {
    if (url.contains("youtube.com")) {
      var id = url.substring(url.indexOf("?v=") + 3)
      val index = id.indexOf("&")
      if (index >= 0) {
        id = id.substring(0, index)
      }
      return id
    } else if (url.contains("youtu.be")) {
      var id = url.substring(url.lastIndexOf("/") + 1)
      val index = id.indexOf("&")
      if (index >= 0) {
        id = id.substring(0, index)
      }

      return id
    }

    return null
  }

  @JvmStatic
  fun getTrailerSnapshot(url: String): String? {
    if (url.contains("youtube") || url.contains("youtu.be")) {
      val id = getYoutubeId(url)
      var snapshot = "http://img.youtube.com/vi/"
      snapshot += id!! + "/hqdefault.jpg"
      return snapshot
    }

    return null
  }
}
