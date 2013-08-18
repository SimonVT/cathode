package net.simonvt.cathode.ui.fragment;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.adapter.ShowsAdapter;

public class TrendingShowsFragment extends ShowsFragment {

  @Override public String getTitle() {
    return getResources().getString(R.string.title_shows_trending);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_list_cards, container, false);
  }

  @Override public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setEmptyText(R.string.shows_loading_trending);
  }

  @Override protected LibraryType getLibraryType() {
    return LibraryType.WATCHED;
  }

  protected int getLoaderId() {
    return 105;
  }

  @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
    final Uri contentUri = CathodeContract.Shows.SHOWS_TRENDING;
    CursorLoader cl =
        new CursorLoader(getActivity(), contentUri, ShowsAdapter.PROJECTION, null, null, null);
    cl.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
    return cl;
  }
}
