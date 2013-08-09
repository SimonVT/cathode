package net.simonvt.cathode.ui.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.CathodeContract;

public class MovieCollectionFragment extends MoviesFragment {

  private static final String TAG = "WatchedMoviesFragment";

  @Override
  public String getTitle() {
    return getResources().getString(R.string.title_movies_collection);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_movies_collection, container, false);
  }

  @Override
  protected int getLoaderId() {
    return 202;
  }

  @Override
  public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
    CursorLoader loader = new CursorLoader(getActivity(), CathodeContract.Movies.CONTENT_URI, null,
        CathodeContract.Movies.IN_COLLECTION, null, CathodeContract.Movies.DEFAULT_STORT);
    loader.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
    return loader;
  }
}
