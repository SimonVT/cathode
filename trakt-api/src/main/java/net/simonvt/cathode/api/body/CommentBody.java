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

public class CommentBody {

  String comment;

  Boolean spoiler;

  Boolean review;

  @SerializedName("tvdb_id") Integer tvdbId;

  @SerializedName("tmdb_id") Long tmdbId;

  String title;

  Integer year;

  Integer season;

  Integer episode;

  public static CommentBody tvdbId(int tvdbId) {
    CommentBody cb = new CommentBody();
    cb.tvdbId = tvdbId;
    return cb;
  }

  public static CommentBody tmdbId(long tmdbId) {
    CommentBody cb = new CommentBody();
    cb.tmdbId = tmdbId;
    return cb;
  }

  public static CommentBody title(String title, int year) {
    CommentBody cb = new CommentBody();
    cb.title = title;
    cb.year = year;
    return cb;
  }

  public CommentBody comment(String comment) {
    this.comment = comment;
    return this;
  }

  public CommentBody spoiler(Boolean spoiler) {
    this.spoiler = spoiler;
    return this;
  }

  public CommentBody review(Boolean review) {
    this.review = review;
    return this;
  }

  public CommentBody season(Integer season) {
    this.season = season;
    return this;
  }

  public CommentBody episode(Integer episode) {
    this.episode = episode;
    return this;
  }
}
