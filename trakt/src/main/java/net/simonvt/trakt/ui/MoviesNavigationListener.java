package net.simonvt.trakt.ui;

public interface MoviesNavigationListener {

    void onDisplayMovie(long movieId, String title);

    void onSearchMovie(String query);
}
