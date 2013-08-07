package net.simonvt.trakt.ui.fragment;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import butterknife.InjectView;
import com.squareup.otto.Bus;
import javax.inject.Inject;
import net.simonvt.trakt.R;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.ui.LibraryType;
import net.simonvt.trakt.ui.ShowsNavigationListener;
import net.simonvt.trakt.ui.adapter.SeasonAdapter;
import net.simonvt.trakt.widget.RemoteImageView;

public class SeasonFragment extends AbsAdapterFragment {

  private static final String TAG = "SeasonFragment";

  private static final String ARG_SHOW_ID = "net.simonvt.trakt.ui.fragment.SeasonFragment.showId";
  private static final String ARG_SEASONID =
      "net.simonvt.trakt.ui.fragment.SeasonFragment.seasonId";
  private static final String ARG_SHOW_TITLE =
      "net.simonvt.trakt.ui.fragment.SeasonFragment.showTitle";
  private static final String ARG_SEASON_NUMBER =
      "net.simonvt.trakt.ui.fragment.SeasonFragment.seasonNumber";
  private static final String ARG_TYPE = "net.simonvt.trakt.ui.fragment.SeasonFragment.type";

  private static final String STATE_SHOW_BANNER =
      "net.simonvt.trakt.ui.fragment.SeasonFragment.showBanner";

  private static final int LOADER_EPISODES = 30;

  private long showId;

  private long seasonId;

  private LibraryType type;

  private String title;

  private String bannerUrl;

  private int seasonNumber = -1;

  private SeasonAdapter episodeAdapter;

  private ShowsNavigationListener navigationCallbacks;

  @Inject Bus bus;

  private Handler handler = new Handler();

  @InjectView(R.id.banner) RemoteImageView showBanner;

  public static Bundle getArgs(long showId, long seasonId, String showTitle, int seasonNumber,
      LibraryType type) {
    Bundle args = new Bundle();
    args.putLong(ARG_SHOW_ID, showId);
    args.putLong(ARG_SEASONID, seasonId);
    args.putString(ARG_SHOW_TITLE, showTitle);
    args.putInt(ARG_SEASON_NUMBER, seasonNumber);
    args.putSerializable(ARG_TYPE, type);
    return args;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      navigationCallbacks = (ShowsNavigationListener) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString() + " must implement ShowsNavigationListener");
    }
  }

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    TraktApp.inject(getActivity(), this);

    Bundle args = getArguments();
    showId = args.getLong(ARG_SHOW_ID);
    seasonId = args.getLong(ARG_SEASONID);
    title = args.getString(ARG_SHOW_TITLE);
    seasonNumber = args.getInt(ARG_SEASON_NUMBER);
    type = (LibraryType) args.getSerializable(ARG_TYPE);

    if (state != null) {
      bannerUrl = state.getString(STATE_SHOW_BANNER);
    }

    episodeAdapter = new SeasonAdapter(getActivity(), type);
    setAdapter(episodeAdapter);

    getLoaderManager().initLoader(LOADER_EPISODES, null, episodesLoader);

    if (title == null) {
      CursorLoader loader =
          new CursorLoader(getActivity(), TraktContract.Shows.buildShowUri(showId), new String[] {
              TraktContract.Shows.TITLE, TraktContract.Shows.BANNER,
          }, null, null, null);
      loader.registerListener(0, new Loader.OnLoadCompleteListener<Cursor>() {
        @Override
        public void onLoadComplete(Loader<Cursor> cursorLoader, Cursor cursor) {
          cursor.moveToFirst();
          title = cursor.getString(cursor.getColumnIndex(TraktContract.Shows.TITLE));
          bannerUrl = cursor.getString(cursor.getColumnIndex(TraktContract.Shows.BANNER));
          cursor.close();

          if (showBanner != null) showBanner.setImage(bannerUrl);

          cursorLoader.stopLoading();
        }
      });
      loader.startLoading();
    }

    if (seasonNumber == -1) {
      new Thread(new Runnable() {
        @Override
        public void run() {
          Cursor c = getActivity().getContentResolver()
              .query(TraktContract.Seasons.buildFromShowId(showId), new String[] {
                  TraktContract.Seasons.SEASON,
              }, null, null, null);

          if (c.moveToFirst()) {
            seasonNumber = c.getInt(c.getColumnIndex(TraktContract.Seasons.SEASON));
          }
        }
      }).start();
    }
  }

  @Override
  public String getTitle() {
    return title;
  }

  @Override
  public String getSubtitle() {
    if (seasonNumber == 0) {
      return getResources().getString(R.string.season_special);
    } else {
      return getResources().getString(R.string.season_x, seasonNumber);
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString(STATE_SHOW_BANNER, bannerUrl);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_season, container, false);
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    showBanner.setImage(bannerUrl);
  }

  @Override
  public void onDestroy() {
    if (getActivity().isFinishing() || isRemoving()) {
      getLoaderManager().destroyLoader(LOADER_EPISODES);
    }
    super.onDestroy();
  }

  @Override
  protected void onItemClick(AdapterView l, View v, int position, long id) {
    navigationCallbacks.onDisplayEpisode(id, title);
  }

  private LoaderManager.LoaderCallbacks<Cursor> episodesLoader =
      new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
          CursorLoader cl =
              new CursorLoader(getActivity(), TraktContract.Episodes.buildFromSeasonId(seasonId),
                  null, null, null, TraktContract.Episodes.EPISODE + " ASC");
          cl.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
          return cl;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor data) {
          episodeAdapter.changeCursor(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
          episodeAdapter.changeCursor(null);
        }
      };
}
