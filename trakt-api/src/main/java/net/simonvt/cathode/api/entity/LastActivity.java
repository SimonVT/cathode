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

package net.simonvt.cathode.api.entity;

public class LastActivity {

  public static class ActivityItem {

    private IsoTime watchedAt;

    private IsoTime collectedAt;

    private IsoTime ratedAt;

    private IsoTime watchlistedAt;

    private IsoTime commentedAt;

    public IsoTime getWatchedAt() {
      return watchedAt;
    }

    public IsoTime getCollectedAt() {
      return collectedAt;
    }

    public IsoTime getRatedAt() {
      return ratedAt;
    }

    public IsoTime getWatchlistedAt() {
      return watchlistedAt;
    }

    public IsoTime getCommentedAt() {
      return commentedAt;
    }
  }

  private ActivityItem movies;

  private ActivityItem shows;

  private ActivityItem seasons;

  private ActivityItem episodes;

  public ActivityItem getMovies() {
    return movies;
  }

  public ActivityItem getShows() {
    return shows;
  }

  public ActivityItem getSeasons() {
    return seasons;
  }

  public ActivityItem getEpisodes() {
    return episodes;
  }
}
