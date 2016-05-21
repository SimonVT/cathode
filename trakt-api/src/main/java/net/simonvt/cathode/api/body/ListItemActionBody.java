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

package net.simonvt.cathode.api.body;

import java.util.ArrayList;
import java.util.List;

public class ListItemActionBody {

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

    List<Episode> episodes;

    public Season(int number) {
      this.number = number;
    }

    public Episode episode(int episodeNumber) {
      if (this.episodes == null) {
        this.episodes = new ArrayList<>();
      }

      Episode episode = new Episode(episodeNumber);
      episodes.add(episode);
      return episode;
    }
  }

  public static class Episode {

    int number;

    public Episode(int number) {
      this.number = number;
    }
  }

  public static class Person {

    Ids ids;

    public Person(long traktId) {
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

  List<Person> people = new ArrayList<>();

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

  public Person person(long traktId) {
    Person person = new Person(traktId);
    people.add(person);
    return person;
  }
}
