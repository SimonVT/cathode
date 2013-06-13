package net.simonvt.trakt.ui;

import net.simonvt.trakt.ui.fragment.NavigationFragment;
import net.simonvt.trakt.util.LogWrapper;

import android.os.Bundle;
import android.view.View;

public class UiController
        implements ShowsNavigationListener, MoviesNavigationListener, NavigationFragment.OnMenuClickListener {

    private static final String TAG = "UiController";

    static final String FRAGMENT_LOGIN = "net.simonvt.trakt.ui.HomeActivity.loginFragment";
    static final String FRAGMENT_NAVIGATION = "net.simonvt.trakt.ui.HomeActivity.navigationFragment";
    static final String FRAGMENT_SHOWS = "net.simonvt.trakt.ui.HomeActivity.showsFragment";
    static final String FRAGMENT_SHOWS_UPCOMING = "net.simonvt.trakt.ui.HomeActivity.upcomingShowsFragment";
    static final String FRAGMENT_SHOWS_COLLECTION = "net.simonvt.trakt.ui.HomeActivity.collectionShowsFragment";
    static final String FRAGMENT_SHOW = "net.simonvt.trakt.ui.HomeActivity.showFragment";
    static final String FRAGMENT_SEASONS = "net.simonvt.trakt.ui.HomeActivity.seasonsFragment";
    static final String FRAGMENT_SEASON = "net.simonvt.trakt.ui.HomeActivity.seasonFragment";
    static final String FRAGMENT_EPISODE = "net.simonvt.trakt.ui.HomeActivity.episodeFragment";
    static final String FRAGMENT_SHOWS_WATCHLIST = "net.simonvt.trakt.ui.HomeActivity.showsWatchlistFragment";
    static final String FRAGMENT_EPISODES_WATCHLIST = "net.simonvt.trakt.ui.HomeActivity.episodesWatchlistFragment";
    static final String FRAGMENT_ADD_SHOW = "net.simonvt.trakt.ui.HomeActivity.addShowFragment";
    static final String FRAGMENT_MOVIES_WATCHED = "net.simonvt.trakt.ui.HomeActivity.moviesWatchedFragment";
    static final String FRAGMENT_MOVIES_COLLECTION = "net.simonvt.trakt.ui.HomeActivity.moviesCollectionFragment";
    static final String FRAGMENT_MOVIES_WATCHLIST = "net.simonvt.trakt.ui.HomeActivity.moviesWatchlistFragment";
    static final String FRAGMENT_SEARCH_MOVIE = "net.simonvt.trakt.ui.HomeActivity.searchMovieFragmentFragment";
    static final String FRAGMENT_MOVIE = "net.simonvt.trakt.ui.HomeActivity.movieFragment";

    protected HomeActivity mActivity;

    protected boolean mAttached;

    UiController(HomeActivity activity) {
        mActivity = activity;
    }

    public void onCreate(Bundle state) {
        LogWrapper.v(TAG, "[onCreate]");
    }

    public void onAttach() {
        LogWrapper.v(TAG, "[onAttach]");
        mAttached = true;
    }

    public void onDetach() {
        LogWrapper.v(TAG, "[onDetach]");
        mAttached = false;
    }

    public void onDestroy() {
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
    public void onDisplayMovie(long movieId) {
        LogWrapper.v(TAG, "[onDisplayMovie]");
    }

    @Override
    public void onSearchMovie(String query) {
        LogWrapper.v(TAG, "[onSearchMovie]");
    }

    @Override
    public void onDisplayShow(long showId, LibraryType type) {
        LogWrapper.v(TAG, "[onDisplayShow]");
    }

    @Override
    public void onDisplaySeasons(long showId, LibraryType type) {
        LogWrapper.v(TAG, "[onDisplaySeasons]");
    }

    @Override
    public void onDisplaySeason(long showId, long seasonId, LibraryType type) {
        LogWrapper.v(TAG, "[onDisplaySeason]");
    }

    @Override
    public void onDisplayEpisode(long episodeId, LibraryType type) {
        LogWrapper.v(TAG, "[onDisplayEpisode]");
    }

    @Override
    public void onSearchShow(String query) {
        LogWrapper.v(TAG, "[onSearchShow]");
    }
}
