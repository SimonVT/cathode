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

public class TrendingMoviesFragment extends MoviesFragment {

  @Override public String getTitle() {
    return getResources().getString(R.string.title_movies_trending);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_list_cards, container, false);
  }

  @Override public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setEmptyText(R.string.movies_loading_trending);
  }

  @Override protected int getLoaderId() {
    return 215;
  }

  @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
    CursorLoader loader =
        new CursorLoader(getActivity(), CathodeContract.Movies.TRENDING, null, null, null, null);
    loader.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
    return loader;
  }
}
