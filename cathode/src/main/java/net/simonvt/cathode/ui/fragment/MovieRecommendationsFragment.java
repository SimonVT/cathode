package net.simonvt.cathode.ui.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.ui.BaseActivity;
import net.simonvt.cathode.ui.adapter.MovieRecommendationsAdapter;

public class MovieRecommendationsFragment extends MoviesFragment {

  @Override public String getTitle() {
    return getResources().getString(R.string.title_movies_recommendations);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_list_cards, container, false);
  }

  @Override public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setEmptyText(R.string.movies_loading_trending);
  }

  @Override protected CursorAdapter getAdapter(Cursor cursor) {
    return new MovieRecommendationsAdapter(getActivity(), cursor);
  }

  @Override protected int getLoaderId() {
    return BaseActivity.LOADER_MOVIES_RECOMMENDATIONS;
  }

  @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
    CursorLoader loader =
        new CursorLoader(getActivity(), CathodeContract.Movies.RECOMMENDED, null, null, null, null);
    loader.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
    return loader;
  }
}
