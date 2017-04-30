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

public class SyncItems {

  public static final String TIME_RELEASED = "released";

  public static class Movie {

    @SerializedName("watched_at") String watchedAt;

    @SerializedName("collected_at") String collectedAt;

    @SerializedName("listed_at") String listedAt;

    Ids ids;

    Movie(long traktId) {
      this.ids = new Ids(traktId);
    }

    public void watchedAt(String watchedAt) {
      this.watchedAt = watchedAt;
    }

    public void collectedAt(String collectedAt) {
      this.collectedAt = collectedAt;
    }

    public void listedAt(String listedAt) {
      this.listedAt = listedAt;
    }
  }

  public static class Show {

    Ids ids;

    List<Season> seasons;

    @SerializedName("watched_at") String watchedAt;

    @SerializedName("collected_at") String collectedAt;

    @SerializedName("listed_at") String listedAt;

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

    public void watchedAt(String watchedAt) {
      this.watchedAt = watchedAt;
    }

    public void collectedAt(String collectedAt) {
      this.collectedAt = collectedAt;
    }

    public void listedAt(String listedAt) {
      this.listedAt = listedAt;
    }
  }

  public static class Season {

    int number;

    List<Episode> episodes;

    @SerializedName("watched_at") String watchedAt;

    @SerializedName("collected_at") String collectedAt;

    @SerializedName("listed_at") String listedAt;

    public Season(int number) {
      this.number = number;
    }

    public Episode episode(int episodeNumber) {
      if (episodes == null) {
        this.episodes = new ArrayList<>();
      }

      Episode episode = new Episode(episodeNumber);
      this.episodes.add(episode);

      return episode;
    }

    public Season watchedAt(String watchedAt) {
      this.watchedAt = watchedAt;
      return this;
    }

    public Season collectedAt(String collectedAt) {
      this.collectedAt = collectedAt;
      return this;
    }

    public void listedAt(String listedAt) {
      this.listedAt = listedAt;
    }
  }

  public static class Episode {

    int number;

    @SerializedName("watched_at") String watchedAt;

    @SerializedName("collected_at") String collectedAt;

    @SerializedName("listed_at") String listedAt;

    public Episode(int number) {
      this.number = number;
    }

    public Episode(int number, String watchedAt) {
      this.number = number;
      this.watchedAt = watchedAt;
    }

    public Episode watchedAt(String watchedAt) {
      this.watchedAt = watchedAt;
      return this;
    }

    public Episode collectedAt(String collectedAt) {
      this.collectedAt = collectedAt;
      return this;
    }

    public void listedAt(String listedAt) {
      this.listedAt = listedAt;
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
