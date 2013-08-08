package net.simonvt.trakt.ui.fragment;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import net.simonvt.trakt.R;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.ui.LibraryType;
import net.simonvt.trakt.ui.adapter.ShowsAdapter;

public class ShowsCollectionFragment extends ShowsFragment {

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_shows_collection, container, false);
  }

  @Override
  public String getTitle() {
    return getResources().getString(R.string.title_shows_collection);
  }

  @Override
  protected LibraryType getLibraryType() {
    return LibraryType.COLLECTION;
  }

  @Override
  protected int getLoaderId() {
    return 345;
  }

  @Override
  public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
    final Uri contentUri = TraktContract.Shows.SHOWS_COLLECTION;
    CursorLoader cl =
        new CursorLoader(getActivity(), contentUri, ShowsAdapter.PROJECTION, null, null,
            TraktContract.Shows.DEFAULT_SORT);
    cl.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
    return cl;
  }
}
