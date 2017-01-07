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

package net.simonvt.cathode.api.util;

public final class TraktUtils {

  private TraktUtils() {
  }

  public static String getTraktShowUrl(long traktId) {
    return "https://trakt.tv/search/trakt/" + traktId + "?id_type=show";
  }

  public static String getTraktSeasonUrl(long traktId) {
    return "https://trakt.tv/search/trakt/" + traktId + "?id_type=season";
  }

  public static String getTraktEpisodeUrl(long traktId) {
    return "https://trakt.tv/search/trakt/" + traktId + "?id_type=episode";
  }

  public static String getTraktMovieUrl(long traktId) {
    return "https://trakt.tv/search/trakt/" + traktId + "?id_type=movie";
  }

  public static String getTraktPersonUrl(long traktId) {
    return "https://trakt.tv/search/trakt/" + traktId + "?id_type=person";
  }

  public static String getImdbUrl(String imdbId) {
    return "http://www.imdb.com/title/" + imdbId;
  }

  public static String getTvdbUrl(int tvdbId) {
    return "http://thetvdb.com/?tab=series&id=" + tvdbId;
  }

  public static String getTmdbTvUrl(int tmdbId) {
    return "https://www.themoviedb.org/tv/" + tmdbId;
  }

  public static String getTmdbMovieUrl(int tmdbId) {
    return "https://www.themoviedb.org/movie/" + tmdbId;
  }

  public static String getYoutubeId(String url) {
    if (url.contains("youtube.com")) {
      String id = url.substring(url.indexOf("?v=") + 3);
      int index = id.indexOf("&");
      if (index >= 0) {
        id = id.substring(0, index);
      }
      return id;
    } else if (url.contains("youtu.be")) {
      String id = url.substring(url.lastIndexOf("/") + 1);
      int index = id.indexOf("&");
      if (index >= 0) {
        id = id.substring(0, index);
      }

      return id;
    }

    return null;
  }

  public static String getTrailerSnapshot(String url) {
    if (url.contains("youtube") || url.contains("youtu.be")) {
      final String id = getYoutubeId(url);
      String snapshot = "http://img.youtube.com/vi/";
      snapshot += id + "/hqdefault.jpg";
      return snapshot;
    }

    return null;
  }
}
