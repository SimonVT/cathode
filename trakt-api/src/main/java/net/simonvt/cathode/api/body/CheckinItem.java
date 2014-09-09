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

public class CheckinItem {

  private static class Episode {

    Ids ids;

    Episode(long traktId) {
      ids = new Ids(traktId);
    }
  }

  private static class Movie {

    Ids ids;

    Movie(long traktId) {
      this.ids = new Ids(traktId);
    }
  }

  private static class Ids {

    Long trakt;

    Ids(long traktId) {
      trakt = traktId;
    }
  }

  public static class Sharing {

    Boolean facebook;

    Boolean twitter;

    Boolean tumblr;

    public Boolean getFacebook() {
      return facebook;
    }

    public Boolean getTwitter() {
      return twitter;
    }

    public Boolean getTumblr() {
      return tumblr;
    }
  }

  private Movie movie;

  private Episode episode;

  private Sharing sharing;

  private String message;

  @SerializedName("venue_id") private String venueId;

  @SerializedName("venue_name") private String venueName;

  @SerializedName("app_version") private String appVersion;

  @SerializedName("app_name") private String appDate;

  public CheckinItem movie(long traktId) {
    this.movie = new Movie(traktId);
    return this;
  }

  public CheckinItem episode(long traktId) {
    this.episode = new Episode(traktId);
    return this;
  }

  public CheckinItem facebook(boolean facebook) {
    if (sharing == null) {
      sharing = new Sharing();
    }

    sharing.facebook = facebook;

    return this;
  }

  public CheckinItem twitter(boolean twitter) {
    if (sharing == null) {
      sharing = new Sharing();
    }

    sharing.twitter = twitter;

    return this;
  }

  public CheckinItem tumblr(boolean tumblr) {
    if (sharing == null) {
      sharing = new Sharing();
    }

    sharing.tumblr = tumblr;

    return this;
  }

  public CheckinItem sharing(Sharing sharing) {
    this.sharing = sharing;
    return this;
  }

  public CheckinItem message(String message) {
    this.message = message;
    return this;
  }

  public CheckinItem venueId(String venueId) {
    this.venueId = venueId;
    return this;
  }

  public CheckinItem venueName(String venueName) {
    this.venueName = venueName;
    return this;
  }

  public CheckinItem appVersion(String appVersion) {
    this.appVersion = appVersion;
    return this;
  }

  public CheckinItem appDate(String appDate) {
    this.appDate = appDate;
    return this;
  }
}
