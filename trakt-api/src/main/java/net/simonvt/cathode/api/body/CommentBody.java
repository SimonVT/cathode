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

public class CommentBody {

  public static class Movie {

    Ids ids;

    Movie(long traktId) {
      this.ids = new Ids(traktId);
    }
  }

  public static class Show {

    Ids ids;

    Show(long traktId) {
      ids = new Ids(traktId);
    }
  }

  public static class Season {

    private Ids ids;

    public Season(long traktId) {
      ids = new Ids(traktId);
    }
  }

  public static class Episode {

    private Ids ids;

    public Episode(long traktId) {
      ids = new Ids(traktId);
    }
  }

  private static class Ids {

    final long trakt;

    private Ids(long trakt) {
      this.trakt = trakt;
    }
  }

  private String comment;

  private boolean spoiler;

  private Show show;

  private Episode episode;

  private Movie movie;

  public static CommentBody comment(String comment) {
    CommentBody body = new CommentBody();
    body.comment = comment;
    return body;
  }

  public CommentBody spoiler() {
    spoiler = true;
    return this;
  }

  public CommentBody show(long traktId) {
    show = new Show(traktId);
    return this;
  }

  public CommentBody episode(long traktId) {
    episode = new Episode(traktId);
    return this;
  }

  public CommentBody movie(long traktId) {
    movie = new Movie(traktId);
    return this;
  }
}
