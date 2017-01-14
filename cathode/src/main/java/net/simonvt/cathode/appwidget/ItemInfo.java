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

package net.simonvt.cathode.appwidget;

public class ItemInfo extends WidgetItem {

  final long showId;
  final String showTitle;
  final String showOverview;

  final long episodeId;
  final String episodeTitle;
  final int season;
  final int episode;
  final long firstAired;
  final String airTime;

  public ItemInfo(long showId, String showTitle, String showOverview, long episodeId,
      String episodeTitle, int season, int episode, long firstAired, String airTime) {
    super(TYPE_ITEM);
    this.showId = showId;
    this.showTitle = showTitle;
    this.showOverview = showOverview;
    this.episodeId = episodeId;
    this.episodeTitle = episodeTitle;
    this.season = season;
    this.episode = episode;
    this.firstAired = firstAired;
    this.airTime = airTime;
  }

  public long getShowId() {
    return showId;
  }

  public String getShowTitle() {
    return showTitle;
  }

  public String getShowOverview() {
    return showOverview;
  }

  public long getEpisodeId() {
    return episodeId;
  }

  public String getEpisodeTitle() {
    return episodeTitle;
  }

  public int getSeason() {
    return season;
  }

  public int getEpisode() {
    return episode;
  }

  public long getFirstAired() {
    return firstAired;
  }

  public String getAirTime() {
    return airTime;
  }
}
