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

public interface Fragments {

  String NAVIGATION = "net.simonvt.cathode.ui.HomeActivity.navigationFragment";

  String SHOWS_WATCHED = "net.simonvt.cathode.ui.HomeActivity.showsFragment";
  String SHOWS_UPCOMING = "net.simonvt.cathode.ui.HomeActivity.upcomingShowsFragment";
  String SHOWS_COLLECTION = "net.simonvt.cathode.ui.HomeActivity.collectionShowsFragment";
  String SHOWS_WATCHLIST = "net.simonvt.cathode.ui.HomeActivity.showsWatchlistFragment";
  String SHOWS_TRENDING = "net.simonvt.cathode.ui.HomeActivity.trendingShowsFragment";
  String SHOWS_RECOMMENDATIONS = "net.simonvt.cathode.ui.HomeActivity.showRecommendationsFragment";

  String SHOW = "net.simonvt.cathode.ui.HomeActivity.showFragment";
  String SEASON = "net.simonvt.cathode.ui.HomeActivity.seasonFragment";
  String EPISODE = "net.simonvt.cathode.ui.HomeActivity.episodeFragment";

  String MOVIES_WATCHED = "net.simonvt.cathode.ui.HomeActivity.moviesWatchedFragment";
  String MOVIES_COLLECTION = "net.simonvt.cathode.ui.HomeActivity.moviesCollectionFragment";
  String MOVIES_WATCHLIST = "net.simonvt.cathode.ui.HomeActivity.moviesWatchlistFragment";
  String MOVIES_TRENDING = "net.simonvt.cathode.ui.HomeActivity.moviesTrendingFragment";
  String MOVIES_RECOMMENDATIONS =
      "net.simonvt.cathode.ui.HomeActivity.movieRecommendationsFragment";

  String MOVIE = "net.simonvt.cathode.ui.HomeActivity.movieFragment";

  String SEARCH_SHOW = "net.simonvt.cathode.ui.HomeActivity.searchShowFragment";
  String SEARCH_MOVIE = "net.simonvt.cathode.ui.HomeActivity.searchMovieFragment";

  String ACTORS = "net.simonvt.cathode.ui.HomeActivity.actorsFragment";

  String LISTS = "net.simonvt.cathode.ui.HomeActivity.listsFragment";
  String LIST = "net.simonvt.cathode.ui.HomeActivity.listFragment";
}
