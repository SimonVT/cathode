package net.simonvt.trakt.ui;

public interface MoviesNavigationListener {

    void onDisplayMovie(long movieId);

    void onSearchMovie(String query);
}
