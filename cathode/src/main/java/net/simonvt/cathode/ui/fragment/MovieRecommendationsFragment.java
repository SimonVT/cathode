package net.simonvt.cathode.ui.fragment;

import android.app.Activity;
import android.database.Cursor;
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
import net.simonvt.cathode.ui.MoviesNavigationListener;
import net.simonvt.cathode.ui.adapter.MovieRecommendationsAdapter;
import net.simonvt.cathode.widget.AdapterViewAnimator;
import net.simonvt.cathode.widget.AnimatorHelper;
import net.simonvt.cathode.widget.DefaultAdapterAnimator;

public class MovieRecommendationsFragment extends AbsAdapterFragment
    implements LoaderManager.LoaderCallbacks<MutableCursor>,
    MovieRecommendationsAdapter.DismissListener {

  @Inject TraktTaskQueue queue;

  private MoviesNavigationListener navigationListener;

  private boolean isTablet;

  private MovieRecommendationsAdapter movieAdapter;

  private MutableCursor cursor;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      navigationListener = (MoviesNavigationListener) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(
          activity.toString() + " must implement MoviesNavigationListener");
    }
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    CathodeApp.inject(getActivity(), this);
    setHasOptionsMenu(true);

    getLoaderManager().initLoader(BaseActivity.LOADER_MOVIES_RECOMMENDATIONS, null, this);

    isTablet = getResources().getBoolean(R.bool.isTablet);
  }

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

  @Override public void onDestroy() {
    if (getActivity().isFinishing() || isRemoving()) {
      getLoaderManager().destroyLoader(BaseActivity.LOADER_MOVIES_RECOMMENDATIONS);
    }
    super.onDestroy();
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.fragment_movies, menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_refresh:
        queue.add(new SyncTask());
        return true;

      case R.id.menu_search:
        navigationListener.onStartMovieSearch();
        return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override protected void onItemClick(AdapterView l, View v, int position, long id) {
    Cursor c = (Cursor) getAdapter().getItem(position);
    navigationListener.onDisplayMovie(id,
        c.getString(c.getColumnIndex(CathodeContract.Movies.TITLE)));
  }

  @Override public void onDismissItem(final View view, final int position) {
    Loader loader = getLoaderManager().getLoader(BaseActivity.LOADER_MOVIES_RECOMMENDATIONS);
    MutableCursorLoader cursorLoader = (MutableCursorLoader) loader;
    cursorLoader.throttle(2000);

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
    }
  };

  protected void setCursor(MutableCursor c) {
    if (movieAdapter == null) {
      this.cursor = c;
      movieAdapter = new MovieRecommendationsAdapter(getActivity(), c, this);
      setAdapter(movieAdapter);
      return;
    }

    AdapterViewAnimator animator =
        new AdapterViewAnimator(adapterView, new DefaultAdapterAnimator());
    movieAdapter.changeCursor(c);
    animator.animate();
  }

  @Override public Loader<MutableCursor> onCreateLoader(int i, Bundle bundle) {
    MutableCursorLoader loader =
        new MutableCursorLoader(getActivity(), CathodeContract.Movies.RECOMMENDED, null, null, null,
            null);
    loader.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
    return loader;
  }

  @Override public void onLoadFinished(Loader<MutableCursor> loader, MutableCursor data) {
    setCursor(data);
  }

  @Override public void onLoaderReset(Loader<MutableCursor> loader) {
    setAdapter(null);
  }
}
