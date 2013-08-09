package net.simonvt.cathode.ui.fragment;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.provider.CathodeDatabase;
import net.simonvt.cathode.ui.LibraryType;

public class UpcomingShowsFragment extends ShowsFragment {

  protected static final String[] PROJECTION = new String[] {
      CathodeDatabase.Tables.SHOWS + "." + BaseColumns._ID,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.TITLE,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.POSTER,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.AIRDATE_COUNT,
      CathodeContract.Shows.UNAIRED_COUNT,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.WATCHED_COUNT,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.STATUS,
      CathodeDatabase.Tables.EPISODES + "." + CathodeContract.Episodes.TITLE,
      CathodeDatabase.Tables.EPISODES + "." + CathodeContract.Episodes.FIRST_AIRED,
      CathodeDatabase.Tables.EPISODES + "." + CathodeContract.Episodes.SEASON,
      CathodeDatabase.Tables.EPISODES + "." + CathodeContract.Episodes.EPISODE,
  };

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_shows_upcoming, container, false);
  }

  @Override
  public String getTitle() {
    return getResources().getString(R.string.title_shows_upcoming);
  }

  @Override
  protected LibraryType getLibraryType() {
    return LibraryType.WATCHED;
  }

  @Override
  protected int getLoaderId() {
    return 101;
  }

  @Override
  public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
    final Uri contentUri = CathodeContract.Shows.SHOWS_WITHNEXT_IGNOREWATCHED;
    CursorLoader cl = new CursorLoader(getActivity(), contentUri, PROJECTION,
        CathodeContract.Shows.WATCHED_COUNT + ">0", null, CathodeContract.Shows.DEFAULT_SORT);
    cl.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
    return cl;
  }
}
