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

public class WatchingBody {

  private class Share {
    Boolean facebook;

    Boolean twitter;

    Boolean tumblr;

    Boolean path;
  }

  @SerializedName("tvdb_id") Integer tvdbId;

  @SerializedName("tmdb_id") Long tmdbId;

  String title;

  Integer year;

  Integer season;

  Integer episode;

  @SerializedName("episode_tvdb_id") Integer episodeTvdbId;

  Integer duration;

  Integer progress;

  @SerializedName("venue_id") Integer venueId;

  @SerializedName("venue_name") String venueName;

  Share share;

  String message;

  private WatchingBody() {
  }

  public static WatchingBody tvdbId(int tvdbId) {
    WatchingBody cb = new WatchingBody();
    cb.tvdbId = tvdbId;
    return cb;
  }

  public static WatchingBody tmdbId(long tmdbId) {
    WatchingBody cb = new WatchingBody();
    cb.tmdbId = tmdbId;
    return cb;
  }

  public static WatchingBody title(String title, int year) {
    WatchingBody cb = new WatchingBody();
    cb.title = title;
    cb.year = year;
    return cb;
  }

  public WatchingBody season(Integer season) {
    this.season = season;
    return this;
  }

  public WatchingBody episode(Integer episode) {
    this.episode = episode;
    return this;
  }

  public WatchingBody episodeTvdbId(Integer episodeTvdbId) {
    this.episodeTvdbId = episodeTvdbId;
    return this;
  }

  public WatchingBody duration(Integer duration) {
    this.duration = duration;
    return this;
  }

  public WatchingBody progress(Integer progress) {
    this.progress = progress;
    return this;
  }

  public WatchingBody venueId(Integer venueId) {
    this.venueId = venueId;
    return this;
  }

  public WatchingBody venueName(String venueName) {
    this.venueName = venueName;
    return this;
  }

  public WatchingBody facebook(boolean share) {
    if (this.share == null) this.share = new Share();
    this.share.facebook = share;
    return this;
  }

  public WatchingBody twitter(boolean share) {
    if (this.share == null) this.share = new Share();
    this.share.twitter = share;
    return this;
  }

  public WatchingBody tumblr(boolean share) {
    if (this.share == null) this.share = new Share();
    this.share.tumblr = share;
    return this;
  }

  public WatchingBody path(boolean share) {
    if (this.share == null) this.share = new Share();
    this.share.path = share;
    return this;
  }

  public WatchingBody message(String message) {
    this.message = message;
    return this;
  }
}
