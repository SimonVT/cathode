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

package net.simonvt.cathode.api.body;

import java.util.ArrayList;
import java.util.List;

public class RemoveHistoryBody {

  public static class Movie {

    Ids ids;

    Movie(long traktId) {
      this.ids = new Ids(traktId);
    }
  }

  public static class Show {

    Ids ids;

    List<Season> seasons;

    Show(long traktId) {
      ids = new Ids(traktId);
    }

    public Season season(int number) {
      if (seasons == null) {
        seasons = new ArrayList<>();
      }
      Season season = new Season(number);
      seasons.add(season);
      return season;
    }
  }

  public static class Season {

    int number;

    List<SeasonEpisode> episodes;

    public Season(int number) {
      this.number = number;
    }

    public Season episode(int episodeNumber) {
      if (this.episodes == null) {
        this.episodes = new ArrayList<>();
      }

      SeasonEpisode episode = new SeasonEpisode(episodeNumber);
      episodes.add(episode);
      return this;
    }
  }

  public static class SeasonEpisode {

    int number;

    public SeasonEpisode(int number) {
      this.number = number;
    }
  }

  public static class Episode {

    Ids ids;

    public Episode(long traktId) {
      this.ids = new Ids(traktId);
    }
  }

  private static class Ids {

    final long trakt;

    private Ids(long trakt) {
      this.trakt = trakt;
    }
  }

  List<Movie> movies = new ArrayList<>();

  List<Show> shows = new ArrayList<>();

  List<Episode> episodes = new ArrayList<>();

  List<Long> ids = new ArrayList<>();

  public Movie movie(long traktId) {
    Movie movie = new Movie(traktId);
    movies.add(movie);
    return movie;
  }

  public Show show(long traktId) {
    Show show = new Show(traktId);
    shows.add(show);
    return show;
  }

  public RemoveHistoryBody episode(long traktId) {
    Episode person = new Episode(traktId);
    episodes.add(person);
    return this;
  }

  public RemoveHistoryBody id(long id) {
    ids.add(id);
    return this;
  }
}
