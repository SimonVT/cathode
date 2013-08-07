package net.simonvt.trakt.ui.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import net.simonvt.trakt.R;
import net.simonvt.trakt.provider.TraktContract;

public class MovieWatchlistFragment extends MoviesFragment {

  private static final String TAG = "WatchedMoviesFragment";

  @Override
  public String getTitle() {
    return getResources().getString(R.string.title_movies_watchlist);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_movies_watchlist, container, false);
  }

  @Override
  protected int getLoaderId() {
    return 203;
  }

  @Override
  public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
    CursorLoader loader = new CursorLoader(getActivity(), TraktContract.Movies.CONTENT_URI, null,
        TraktContract.Movies.IN_WATCHLIST, null, TraktContract.Movies.DEFAULT_STORT);
    loader.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
    return loader;
  }
}
