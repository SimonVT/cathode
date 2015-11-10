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

package net.simonvt.cathode.ui;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.simonvt.cathode.R;
import net.simonvt.cathode.ui.fragment.ListsFragment;
import net.simonvt.cathode.ui.fragment.MovieCollectionFragment;
import net.simonvt.cathode.ui.fragment.MovieRecommendationsFragment;
import net.simonvt.cathode.ui.fragment.MovieWatchlistFragment;
import net.simonvt.cathode.ui.fragment.ShowRecommendationsFragment;
import net.simonvt.cathode.ui.fragment.ShowsCollectionFragment;
import net.simonvt.cathode.ui.fragment.ShowsWatchlistFragment;
import net.simonvt.cathode.ui.fragment.TrendingMoviesFragment;
import net.simonvt.cathode.ui.fragment.TrendingShowsFragment;
import net.simonvt.cathode.ui.fragment.UpcomingShowsFragment;
import net.simonvt.cathode.ui.fragment.WatchedMoviesFragment;
import net.simonvt.cathode.ui.fragment.WatchedShowsFragment;

public enum StartPage {
  SHOWS_UPCOMING("showsUpcoming", UpcomingShowsFragment.class, Fragments.SHOWS_UPCOMING,
      R.id.menu_shows_upcoming, R.string.startpage_shows_upcoming),
  SHOWS_WATCHED("showsCollected", WatchedShowsFragment.class, Fragments.SHOWS_WATCHED,
      R.id.menu_shows_watched, R.string.startpage_shows_watched),
  SHOWS_COLLECTED("showsCollected", ShowsCollectionFragment.class, Fragments.SHOWS_COLLECTION,
      R.id.menu_shows_collection, R.string.startpage_shows_collected),
  SHOWS_WATCHLIST("showsWatchlist", ShowsWatchlistFragment.class, Fragments.SHOWS_WATCHLIST,
      R.id.menu_shows_watchlist, R.string.startpage_shows_watchlist),
  SHOWS_TRENDING("showsTrending", TrendingShowsFragment.class, Fragments.SHOWS_TRENDING,
      R.id.menu_shows_trending, R.string.startpage_shows_trending),
  SHOWS_RECOMMENDATIONS("showsRecommendations", ShowRecommendationsFragment.class,
      Fragments.SHOWS_RECOMMENDATIONS, R.id.menu_shows_recommendations,
      R.string.startpage_shows_recommendations),

  MOVIES_WATCHED("moviesWatched", WatchedMoviesFragment.class, Fragments.MOVIES_WATCHED,
      R.id.menu_movies_watched, R.string.startpage_movies_watched),
  MOVIES_COLLECTED("moviesCollected", MovieCollectionFragment.class, Fragments.MOVIES_COLLECTION,
      R.id.menu_movies_collection, R.string.startpage_movies_collected),
  MOVIES_WATCHLIST("movieWatchlist", MovieWatchlistFragment.class, Fragments.MOVIES_WATCHLIST,
      R.id.menu_movies_watchlist, R.string.startpage_movies_watchlist),
  MOVIES_TRENDING("moviesTrending", TrendingMoviesFragment.class, Fragments.MOVIES_TRENDING,
      R.id.menu_movies_trending, R.string.startpage_movies_trending),
  MOVIES_RECOMMENDATIONS("movieRecommendations", MovieRecommendationsFragment.class,
      Fragments.MOVIES_RECOMMENDATIONS, R.id.menu_movies_recommendations,
      R.string.startpage_movies_recommendations),

  LISTS("lists", ListsFragment.class, Fragments.LISTS, R.id.menu_lists, R.string.startpage_lists);

  private String startPage;
  private Class clazz;
  private String tag;
  private int menuId;
  private int label;

  StartPage(String startPage, Class clazz, String tag, int menuId, int label) {
    this.startPage = startPage;
    this.clazz = clazz;
    this.tag = tag;
    this.menuId = menuId;
    this.label = label;
  }

  private static final Map<String, StartPage> STRING_MAPPING = new HashMap<>();

  static {
    for (StartPage via : StartPage.values()) {
      STRING_MAPPING.put(via.toString().toUpperCase(Locale.US), via);
    }
  }

  public static StartPage fromValue(String value, StartPage defaultPage) {
    if (value == null) {
      return defaultPage;
    }

    StartPage startPage = STRING_MAPPING.get(value.toUpperCase(Locale.US));

    if (startPage == null) {
      startPage = defaultPage;
    }

    return startPage;
  }

  @Override public String toString() {
    return startPage;
  }

  public Class getPageClass() {
    return clazz;
  }

  public String getTag() {
    return tag;
  }

  public int getMenuId() {
    return menuId;
  }

  public int getLabel() {
    return label;
  }
}
