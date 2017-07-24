/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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

package net.simonvt.cathode.settings.link;

import java.util.List;
import net.simonvt.cathode.api.enumeration.Privacy;

public class LocalState {

  static class LocalShow {

    long id;
    long traktId;

     LocalShow(long id, long traktId) {
      this.id = id;
      this.traktId = traktId;
    }
  }

  static class LocalEpisode {

    long id;
    long showTraktId;
    int season;
    int episode;

     LocalEpisode(long id, long showTraktId, int season, int episode) {
      this.id = id;
      this.showTraktId = showTraktId;
      this.season = season;
      this.episode = episode;
    }
  }

  static class LocalMovie {

    long id;
    long traktId;

     LocalMovie(long id, long traktId) {
      this.id = id;
      this.traktId = traktId;
    }
  }

  static class LocalList {

    String name;
    String description;
    Privacy privacy;
    Boolean displayNumbers;
    Boolean allowComments;
    List<LocalListItem> items;

     LocalList(String name, String description, Privacy privacy, Boolean displayNumbers,
        Boolean allowComments, List<LocalListItem> items) {
      this.name = name;
      this.description = description;
      this.privacy = privacy;
      this.displayNumbers = displayNumbers;
      this.allowComments = allowComments;
      this.items = items;
    }
  }

  static class LocalListItem {

    int itemType;
    long itemId;
    long listedAt;

     LocalListItem(int itemType, long itemId, long listedAt) {
      this.itemType = itemType;
      this.itemId = itemId;
      this.listedAt = listedAt;
    }
  }

  List<LocalEpisode> watchedEpisodes;
  List<LocalEpisode> collectedEpisodes;
  List<LocalShow> showWatchlist;
  List<LocalEpisode> episodeWatchlist;

  List<LocalMovie> watchedMovies;
  List<LocalMovie> collectedMovies;
  List<LocalMovie> movieWatchlist;

  List<LocalList> lists;

  public LocalState(List<LocalEpisode> watchedEpisodes, List<LocalEpisode> collectedEpisodes,
      List<LocalShow> showWatchlist, List<LocalEpisode> episodeWatchlist,
      List<LocalMovie> watchedMovies, List<LocalMovie> collectedMovies,
      List<LocalMovie> movieWatchlist, List<LocalList> lists) {
    this.watchedEpisodes = watchedEpisodes;
    this.collectedEpisodes = collectedEpisodes;
    this.showWatchlist = showWatchlist;
    this.episodeWatchlist = episodeWatchlist;
    this.watchedMovies = watchedMovies;
    this.collectedMovies = collectedMovies;
    this.movieWatchlist = movieWatchlist;
    this.lists = lists;
  }
}
