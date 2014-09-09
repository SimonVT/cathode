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

package net.simonvt.cathode.api.body;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class RateItems {

  public static class Movie {

    Ids ids;

    Integer rating;

    @SerializedName("rated_at") String ratedAt;

    Movie(long traktId) {
      this.ids = new Ids(traktId);
    }

    public Movie rating(int rating) {
      this.rating = rating;
      return this;
    }

    public Movie ratedAt(String ratedAt) {
      this.ratedAt = ratedAt;
      return this;
    }
  }

  public static class Show {

    Ids ids;

    List<Season> seasons;

    Integer rating;

    @SerializedName("rated_at") String ratedAt;

    Show(long traktId) {
      ids = new Ids(traktId);
    }

    public Show rating(int rating) {
      this.rating = rating;
      return this;
    }

    public Season season(int number) {
      if (seasons == null) {
        seasons = new ArrayList<Season>();
      }
      Season season = new Season(number);
      seasons.add(season);
      return season;
    }

    public Show ratedAt(String ratedAt) {
      this.ratedAt = ratedAt;
      return this;
    }
  }

  public static class Season {

    int number;

    Integer rating;

    List<Episode> episodes;

    @SerializedName("rated_at") String ratedAt;

    public Season(int number) {
      this.number = number;
    }

    public Season rating(int rating) {
      this.rating = rating;
      return this;
    }

    public Episode episode(int episodeNumber) {
      if (this.episodes == null) {
        this.episodes = new ArrayList<Episode>();
      }

      Episode episode = new Episode(episodeNumber);
      episodes.add(episode);
      return episode;
    }

    public Season ratedAt(String ratedAt) {
      this.ratedAt = ratedAt;
      return this;
    }
  }

  public static class Episode {

    int number;

    Integer rating;

    @SerializedName("rated_at") String ratedAt;

    public Episode(int number) {
      this.number = number;
    }

    public Episode rating(int rating) {
      this.rating = rating;
      return this;
    }

    public Episode ratedAt(String ratedAt) {
      this.ratedAt = ratedAt;
      return this;
    }
  }

  private static class Ids {

    final long trakt;

    private Ids(long trakt) {
      this.trakt = trakt;
    }
  }

  List<Movie> movies = new ArrayList<Movie>();

  List<Show> shows = new ArrayList<Show>();

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
}
