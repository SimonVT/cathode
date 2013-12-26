/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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

import android.os.Bundle;
import net.simonvt.cathode.ui.fragment.NavigationFragment;
import timber.log.Timber;

public class UiController implements ShowsNavigationListener, MoviesNavigationListener,
    NavigationFragment.OnMenuClickListener {

  private static final String TAG = "UiController";

  static final String FRAGMENT_LOGIN = "net.simonvt.cathode.ui.HomeActivity.loginFragment";
  static final String FRAGMENT_NAVIGATION =
      "net.simonvt.cathode.ui.HomeActivity.navigationFragment";
  static final String FRAGMENT_SHOWS = "net.simonvt.cathode.ui.HomeActivity.showsFragment";
  static final String FRAGMENT_SHOWS_UPCOMING =
      "net.simonvt.cathode.ui.HomeActivity.upcomingShowsFragment";
  static final String FRAGMENT_SHOWS_COLLECTION =
      "net.simonvt.cathode.ui.HomeActivity.collectionShowsFragment";
  static final String FRAGMENT_SHOWS_TRENDING =
      "net.simonvt.cathode.ui.HomeActivity.trendingShowsFragment";
  static final String FRAGMENT_SHOWS_RECOMMENDATIONS =
      "net.simonvt.cathode.ui.HomeActivity.showRecommendationsFragment";
  static final String FRAGMENT_SHOW = "net.simonvt.cathode.ui.HomeActivity.showFragment";
  static final String FRAGMENT_SEASONS = "net.simonvt.cathode.ui.HomeActivity.seasonsFragment";
  static final String FRAGMENT_SEASON = "net.simonvt.cathode.ui.HomeActivity.seasonFragment";
  static final String FRAGMENT_EPISODE = "net.simonvt.cathode.ui.HomeActivity.episodeFragment";
  static final String FRAGMENT_SHOWS_WATCHLIST =
      "net.simonvt.cathode.ui.HomeActivity.showsWatchlistFragment";
  static final String FRAGMENT_EPISODES_WATCHLIST =
      "net.simonvt.cathode.ui.HomeActivity.episodesWatchlistFragment";
  static final String FRAGMENT_SEARCH_SHOW =
      "net.simonvt.cathode.ui.HomeActivity.searchShowFragment";
  static final String FRAGMENT_MOVIES_WATCHED =
      "net.simonvt.cathode.ui.HomeActivity.moviesWatchedFragment";
  static final String FRAGMENT_MOVIES_COLLECTION =
      "net.simonvt.cathode.ui.HomeActivity.moviesCollectionFragment";
  static final String FRAGMENT_MOVIES_WATCHLIST =
      "net.simonvt.cathode.ui.HomeActivity.moviesWatchlistFragment";
  static final String FRAGMENT_MOVIES_TRENDING =
      "net.simonvt.cathode.ui.HomeActivity.moviesTrendingFragment";
  static final String FRAGMENT_MOVIES_RECOMMENDATIONS =
      "net.simonvt.cathode.ui.HomeActivity.movieRecommendationsFragment";
  static final String FRAGMENT_SEARCH_MOVIE =
      "net.simonvt.cathode.ui.HomeActivity.searchMovieFragment";
  static final String FRAGMENT_MOVIE = "net.simonvt.cathode.ui.HomeActivity.movieFragment";

  protected HomeActivity activity;

  protected boolean attached;

  UiController(HomeActivity activity) {
    this.activity = activity;
  }

  public void onCreate(Bundle inState) {
    Timber.d("[onCreate]");
  }

  public void onAttach() {
    Timber.d("[onAttach]");
    attached = true;
  }

  public void onDetach() {
    Timber.d("[onDetach]");
    attached = false;
  }

  public void onDestroy(boolean completely) {
    Timber.d("[onDestroy]");
  }

  public Bundle onSaveInstanceState() {
    return null;
  }

  public boolean onBackClicked() {
    return false;
  }

  public void onHomeClicked() {
    Timber.d("[onHomeClicked]");
  }

  @Override public void onMenuItemClicked(int id) {
    Timber.d("[onMenuItemClicked]");
  }

  @Override public void onDisplayMovie(long movieId, String title) {
    Timber.d("[onDisplayMovie]");
  }

  @Override public void onStartMovieSearch() {
    Timber.d("[onStartMovieSearch]");
  }

  @Override public void onDisplayShow(long showId, String title, LibraryType type) {
    Timber.d("[onDisplayShow]");
  }

  @Override
  public void onDisplaySeason(long showId, long seasonId, String showTitle, int seasonNumber,
      LibraryType type) {
    Timber.d("[onDisplaySeason]");
  }

  @Override public void onDisplayEpisode(long episodeId, String showTitle) {
    Timber.d("[onDisplayEpisode]");
  }

  @Override public void onStartShowSearch() {
    Timber.d("[onStartShowSearch]");
  }
}
