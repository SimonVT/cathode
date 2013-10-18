package net.simonvt.cathode.ui;

import android.os.Bundle;
import net.simonvt.cathode.ui.fragment.NavigationFragment;
import net.simonvt.cathode.util.LogWrapper;

public class UiController implements ShowsNavigationListener, MoviesNavigationListener,
    NavigationFragment.OnMenuClickListener {

  private static final String TAG = "UiController";

  static final String FRAGMENT_LOGIN = "net.simonvt.cathode.ui.HomeActivity.loginFragment";
  static final String FRAGMENT_NAVIGATION = "net.simonvt.cathode.ui.HomeActivity.navigationFragment";
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
  static final String FRAGMENT_SEARCH_SHOW = "net.simonvt.cathode.ui.HomeActivity.searchShowFragment";
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

  public void onCreate(Bundle state) {
    LogWrapper.v(TAG, "[onCreate]");
  }

  public void onAttach() {
    LogWrapper.v(TAG, "[onAttach]");
    attached = true;
  }

  public void onDetach() {
    LogWrapper.v(TAG, "[onDetach]");
    attached = false;
  }

  public void onDestroy(boolean completely) {
    LogWrapper.v(TAG, "[onDestroy]");
  }

  public Bundle onSaveInstanceState() {
    return null;
  }

  public boolean onBackClicked() {
    return false;
  }

  public void onHomeClicked() {
    LogWrapper.v(TAG, "[onHomeClicked]");
  }

  @Override
  public void onMenuItemClicked(int id) {
    LogWrapper.v(TAG, "[onMenuItemClicked]");
  }

  @Override
  public void onDisplayMovie(long movieId, String title) {
    LogWrapper.v(TAG, "[onDisplayMovie]");
  }

  @Override
  public void onStartMovieSearch() {
    LogWrapper.v(TAG, "[onStartMovieSearch]");
  }

  @Override
  public void onDisplayShow(long showId, String title, LibraryType type) {
    LogWrapper.v(TAG, "[onDisplayShow]");
  }

  @Override
  public void onDisplaySeason(long showId, long seasonId, String showTitle, int seasonNumber,
      LibraryType type) {
    LogWrapper.v(TAG, "[onDisplaySeason]");
  }

  @Override
  public void onDisplayEpisode(long episodeId, String showTitle) {
    LogWrapper.v(TAG, "[onDisplayEpisode]");
  }

  @Override
  public void onStartShowSearch() {
    LogWrapper.v(TAG, "[onStartShowSearch]");
  }
}
