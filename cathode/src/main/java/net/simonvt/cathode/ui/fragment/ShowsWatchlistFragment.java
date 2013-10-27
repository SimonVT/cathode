package net.simonvt.cathode.ui.fragment;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ListView;
import net.simonvt.cathode.R;
import net.simonvt.cathode.database.MutableCursor;
import net.simonvt.cathode.database.MutableCursorLoader;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.ui.BaseActivity;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.adapter.ShowDescriptionAdapter;
import net.simonvt.cathode.ui.adapter.ShowWatchlistAdapter;
import net.simonvt.cathode.widget.AnimatorHelper;

public class ShowsWatchlistFragment extends ShowsFragment<MutableCursor>
    implements ShowWatchlistAdapter.RemoveListener {

  private boolean isTablet;

  private MutableCursor cursor;
  private MutableCursor newCursor;

  private boolean removing;

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    isTablet = getResources().getBoolean(R.bool.isTablet);
  }

  @Override public String getTitle() {
    return getResources().getString(R.string.title_shows_watchlist);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_shows_watchlist, container, false);
  }

  @Override
  protected LibraryType getLibraryType() {
    return LibraryType.WATCHLIST;
  }

  @Override protected int getLoaderId() {
    return BaseActivity.LOADER_SHOWS_WATCHLIST;
  }

  @Override public void onRemoveItem(View view, int position) {
    removing = true;

    if (isTablet) {
      AnimatorHelper.removeView((GridView) getAdapterView(), view, animatorCallback);
    } else {
      AnimatorHelper.removeView((ListView) getAdapterView(), view, animatorCallback);
    }
  }

  private AnimatorHelper.Callback animatorCallback = new AnimatorHelper.Callback() {
    @Override public void removeItem(int position) {
      cursor.remove(position);
    }

    @Override public void onAnimationEnd() {
      if (newCursor != null) {
        cursor = newCursor;
        newCursor = null;
        ((CursorAdapter) getAdapter()).changeCursor(cursor);
      }
      removing = false;
    }
  };

  @Override protected void setCursor(Cursor c) {
    MutableCursor cursor = (MutableCursor) c;
    if (getAdapter() == null) {
      this.cursor = cursor;
      showsAdapter = new ShowWatchlistAdapter(getActivity(), cursor, this);
      setAdapter(showsAdapter);
      return;
    }

    if (!removing) {
      this.cursor = cursor;
      ((CursorAdapter) getAdapter()).changeCursor(cursor);
    } else {
      this.newCursor = cursor;
    }
  }

  @Override public Loader<MutableCursor> onCreateLoader(int i, Bundle bundle) {
    final Uri contentUri = CathodeContract.Shows.SHOWS_WATCHLIST;
    MutableCursorLoader cl =
        new MutableCursorLoader(getActivity(), contentUri, ShowDescriptionAdapter.PROJECTION, null,
            null, CathodeContract.Shows.DEFAULT_SORT);
    cl.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
    return cl;
  }
}
