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
import net.simonvt.cathode.api.enumeration.ListItemType;

public class ListItemBody {

  private static class ListItem {

    ListItemType type;

    @SerializedName("tvdb_id") Integer tvdbId;

    @SerializedName("tmdb_id") Long tmdbId;

    Integer season;

    Integer episode;

    public static ListItem show(Integer tvdbId) {
      ListItem item = new ListItem();
      item.type = ListItemType.SHOW;
      item.tvdbId = tvdbId;
      return item;
    }

    public static ListItem season(Integer tvdbId, Integer season) {
      ListItem item = new ListItem();
      item.type = ListItemType.SEASON;
      item.tvdbId = tvdbId;
      item.season = season;
      return item;
    }

    public static ListItem episode(Integer tvdbId, Integer season, Integer episode) {
      ListItem item = new ListItem();
      item.type = ListItemType.EPISODE;
      item.tvdbId = tvdbId;
      item.season = season;
      item.episode = episode;
      return item;
    }

    public static ListItem movie(Long tmdbId) {
      ListItem item = new ListItem();
      item.type = ListItemType.MOVIE;
      item.tmdbId = tmdbId;
      return item;
    }
  }

  String slug;

  List<ListItem> items = new ArrayList<ListItem>();

  public static ListItemBody slug(String slug) {
    ListItemBody body = new ListItemBody();
    body.slug = slug;
    return body;
  }

  public ListItemBody show(Integer tvdbId) {
    items.add(ListItem.show(tvdbId));
    return this;
  }

  public ListItemBody season(Integer tvdbId, Integer season) {
    items.add(ListItem.season(tvdbId, season));
    return this;
  }

  public ListItemBody episode(Integer tvdbId, Integer season, Integer episode) {
    items.add(ListItem.episode(tvdbId, season, episode));
    return this;
  }

  public ListItemBody movie(Long tmdbId) {
    items.add(ListItem.movie(tmdbId));
    return this;
  }
}
