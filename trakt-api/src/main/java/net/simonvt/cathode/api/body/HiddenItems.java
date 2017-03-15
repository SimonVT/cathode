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

package net.simonvt.cathode.api.body;

import java.util.ArrayList;
import java.util.List;

public class HiddenItems {

  public static class Item {

    Ids ids;

    Item(long traktId) {
      this.ids = new Ids(traktId);
    }
  }

  private static class Ids {

    final long trakt;

    private Ids(long trakt) {
      this.trakt = trakt;
    }
  }

  List<Item> movies = new ArrayList<>();

  List<Item> shows = new ArrayList<>();

  List<Item> seasons = new ArrayList<>();

  public HiddenItems movie(long traktId) {
    Item movie = new Item(traktId);
    movies.add(movie);
    return this;
  }

  public HiddenItems show(long traktId) {
    Item show = new Item(traktId);
    shows.add(show);
    return this;
  }

  public HiddenItems season(long traktId) {
    Item show = new Item(traktId);
    seasons.add(show);
    return this;
  }
}
