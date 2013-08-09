package net.simonvt.cathode.ui;

public interface MoviesNavigationListener {

  void onDisplayMovie(long movieId, String title);

  void onStartMovieSearch();
}
