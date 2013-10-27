package net.simonvt.cathode.ui.fragment;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.database.MutableCursor;
import net.simonvt.cathode.database.MutableCursorLoader;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.remote.TraktTaskQueue;
import net.simonvt.cathode.remote.sync.SyncTask;
import net.simonvt.cathode.ui.BaseActivity;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.ShowsNavigationListener;
import net.simonvt.cathode.ui.adapter.ShowDescriptionAdapter;
import net.simonvt.cathode.ui.adapter.ShowRecommendationsAdapter;
import net.simonvt.cathode.widget.AnimatorHelper;

public class ShowRecommendationsFragment extends AbsAdapterFragment
    implements LoaderManager.LoaderCallbacks<MutableCursor>,
    ShowRecommendationsAdapter.DismissListener {

  private static final String TAG = "ShowRecommendationsFragment";

  private ShowRecommendationsAdapter showsAdapter;

  private ShowsNavigationListener navigationListener;

  @Inject TraktTaskQueue queue;

  private boolean isTablet;

  private MutableCursor cursor;
  private MutableCursor newCursor;

  private boolean removing;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      navigationListener = (ShowsNavigationListener) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString() + " must implement ShowsNavigationListener");
    }
  }

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    CathodeApp.inject(getActivity(), this);

    setHasOptionsMenu(true);

    getLoaderManager().initLoader(BaseActivity.LOADER_SHOWS_RECOMMENDATIONS, null, this);

    isTablet = getResources().getBoolean(R.bool.isTablet);
  }

  @Override public String getTitle() {
    return getResources().getString(R.string.title_shows_recommendations);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_shows_watched, container, false);
  }

  @Override public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.fragment_shows, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_refresh:
        queue.add(new SyncTask());
        return true;

      case R.id.menu_search:
        navigationListener.onStartShowSearch();
        return true;

      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  protected void onItemClick(AdapterView l, View v, int position, long id) {
    Cursor c = (Cursor) getAdapter().getItem(position);
    navigationListener.onDisplayShow(id, c.getString(c.getColumnIndex(CathodeContract.Shows.TITLE)),
        LibraryType.WATCHED);
  }

  @Override public void onDismissItem(final View view, final int position) {
    removing = true;

    if (isTablet) {
          AnimatorHelper.removeView((GridView) getAdapterView(), view,
              new AnimatorHelper.Callback() {
                @Override public void removeItem(int position) {
                  cursor.remove(position);
                }

                @Override public void onAnimationEnd() {
                  if (newCursor != null) {
                    cursor = newCursor;
                    newCursor = null;
                    showsAdapter.changeCursor(cursor);
                  }
                  removing = false;
                }
              });
    } else {
              AnimatorHelper.removeView((ListView) getAdapterView(), view,
                  new AnimatorHelper.Callback() {
                    @Override public void removeItem(int position) {
                      cursor.remove(position);
                    }

                    @Override public void onAnimationEnd() {
                      if (newCursor != null) {
                        cursor = newCursor;
                        newCursor = null;
                        showsAdapter.changeCursor(cursor);
                      }
                      removing = false;
                    }
                  });
    }
  }

  private void setCursor(Cursor c) {
    MutableCursor cursor = (MutableCursor) c;
    if (showsAdapter == null) {
      this.cursor = cursor;
      showsAdapter = new ShowRecommendationsAdapter(getActivity(), cursor, this);
      setAdapter(showsAdapter);
      return;
    }

    if (!removing) {
      this.cursor = cursor;
      showsAdapter.changeCursor(cursor);
    } else {
      this.newCursor = cursor;
    }
  }

  @Override public Loader<MutableCursor> onCreateLoader(int i, Bundle bundle) {
    final Uri contentUri = CathodeContract.Shows.SHOWS_RECOMMENDED;
    MutableCursorLoader cl =
        new MutableCursorLoader(getActivity(), contentUri, ShowDescriptionAdapter.PROJECTION, null,
            null, null);
    cl.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
    return cl;
  }

  @Override
  public void onLoadFinished(Loader<MutableCursor> loader, MutableCursor data) {
    setCursor(data);
  }

  @Override
  public void onLoaderReset(Loader<MutableCursor> loader) {
  }
}
