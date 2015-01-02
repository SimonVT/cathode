/*
 * Copyright (C) 2013 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.simonvt.cathode.ui.fragment;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;
import com.squareup.otto.Bus;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.CollectLoader;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowCharacterColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowGenreColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.ProviderSchematic;
import net.simonvt.cathode.provider.ProviderSchematic.Seasons;
import net.simonvt.cathode.provider.ProviderSchematic.ShowGenres;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.WatchedLoader;
import net.simonvt.cathode.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.ui.BaseActivity;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.ShowsNavigationListener;
import net.simonvt.cathode.ui.adapter.SeasonsAdapter;
import net.simonvt.cathode.ui.dialog.CheckInDialog;
import net.simonvt.cathode.ui.dialog.CheckInDialog.Type;
import net.simonvt.cathode.ui.dialog.RatingDialog;
import net.simonvt.cathode.util.DateUtils;
import net.simonvt.cathode.widget.CircularProgressIndicator;
import net.simonvt.cathode.widget.HiddenPaneLayout;
import net.simonvt.cathode.widget.ObservableScrollView;
import net.simonvt.cathode.widget.OverflowView;
import net.simonvt.cathode.widget.RecyclerViewManager;
import net.simonvt.cathode.widget.RemoteImageView;
import timber.log.Timber;

public class ShowFragment extends ProgressFragment {

  private static final String ARG_SHOWID = "net.simonvt.cathode.ui.fragment.ShowFragment.showId";
  private static final String ARG_TITLE = "net.simonvt.cathode.ui.fragment.ShowFragment.title";
  private static final String ARG_TYPE = "net.simonvt.cathode.ui.fragment.ShowFragment.type";

  private static final String DIALOG_RATING =
      "net.simonvt.cathode.ui.fragment.ShowFragment.ratingDialog";

  private static final String[] SHOW_PROJECTION = new String[] {
      ShowColumns.TITLE, ShowColumns.YEAR, ShowColumns.AIR_TIME, ShowColumns.AIR_DAY,
      ShowColumns.NETWORK, ShowColumns.CERTIFICATION, ShowColumns.POSTER, ShowColumns.FANART,
      ShowColumns.USER_RATING, ShowColumns.RATING, ShowColumns.OVERVIEW, ShowColumns.IN_WATCHLIST,
      ShowColumns.IN_COLLECTION_COUNT, ShowColumns.WATCHED_COUNT, ShowColumns.HIDDEN,
  };

  private static final String[] EPISODE_PROJECTION = new String[] {
      EpisodeColumns.ID, EpisodeColumns.TITLE, EpisodeColumns.SCREENSHOT, EpisodeColumns.FIRST_AIRED,
      EpisodeColumns.SEASON, EpisodeColumns.EPISODE,
  };

  private static final String[] GENRES_PROJECTION = new String[] {
      ShowGenreColumns.GENRE,
  };

  private ShowsNavigationListener navigationCallbacks;

  private long showId;

  @InjectView(R.id.hiddenPaneLayout) HiddenPaneLayout hiddenPaneLayout;

  @InjectView(R.id.seasons) RecyclerView seasons;
  @InjectView(R.id.seasonsEmpty) View seasonsEmpty;
  private SeasonsAdapter seasonsAdapter;
  private Cursor seasonsCursor;

  @InjectView(R.id.scrollView) ObservableScrollView scrollView;

  @InjectView(R.id.rating) CircularProgressIndicator rating;
  @InjectView(R.id.airtime) TextView airTime;
  @InjectView(R.id.certification) TextView certification;
  @InjectView(R.id.poster) RemoteImageView poster;
  @InjectView(R.id.fanart) RemoteImageView fanart;
  @InjectView(R.id.overview) TextView overview;
  @InjectView(R.id.isWatched) TextView watched;
  @InjectView(R.id.inCollection) TextView collection;
  @InjectView(R.id.inWatchlist) TextView watchlist;

  @InjectView(R.id.actorsTitle) View actorsTitle;
  @InjectView(R.id.actorsParent) View actorsParent;
  @InjectView(R.id.actors) LinearLayout actors;

  @InjectView(R.id.episodes) LinearLayout episodes;

  @InjectView(R.id.watchTitle) View watchTitle;
  @InjectView(R.id.collectTitle) View collectTitle;

  @InjectView(R.id.toWatch) View toWatch;
  private EpisodeHolder toWatchHolder;
  private long toWatchId = -1;
  private String toWatchTitle;

  @InjectView(R.id.lastWatched) @Optional View lastWatched;
  private EpisodeHolder lastWatchedHolder;
  private long lastWatchedId = -1;

  @InjectView(R.id.toCollect) View toCollect;
  private EpisodeHolder toCollectHolder;
  private long toCollectId = -1;

  @InjectView(R.id.lastCollected) @Optional View lastCollected;
  private EpisodeHolder lastCollectedHolder;
  private long lastCollectedId = -1;

  static class EpisodeHolder {

    @InjectView(R.id.episodeScreenshot) RemoteImageView episodeScreenshot;
    @InjectView(R.id.episodeTitle) TextView episodeTitle;
    @InjectView(R.id.episodeAirTime) TextView episodeAirTime;
    @InjectView(R.id.episodeEpisode) TextView episodeEpisode;
    @InjectView(R.id.episodeOverflow) OverflowView episodeOverflow;

    public EpisodeHolder(View v) {
      ButterKnife.inject(this, v);
    }
  }

  @Inject ShowTaskScheduler showScheduler;
  @Inject EpisodeTaskScheduler episodeScheduler;

  @Inject Bus bus;

  private String showTitle;

  private String genres;

  private boolean inWatchlist;

  private int currentRating;

  private boolean isHidden;

  private LibraryType type;

  RecyclerViewManager seasonsManager;

  public static Bundle getArgs(long showId, String title, LibraryType type) {
    Bundle args = new Bundle();
    args.putLong(ARG_SHOWID, showId);
    args.putString(ARG_TITLE, title);
    args.putSerializable(ARG_TYPE, type);
    return args;
  }

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      navigationCallbacks = (ShowsNavigationListener) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString() + " must implement ShowsNavigationListener");
    }
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    Timber.d("ShowFragment#onCreate");
    CathodeApp.inject(getActivity(), this);

    Bundle args = getArguments();
    showId = args.getLong(ARG_SHOWID);
    showTitle = args.getString(ARG_TITLE);
    type = (LibraryType) args.getSerializable(ARG_TYPE);

    seasonsAdapter = new SeasonsAdapter(getActivity(), new SeasonsAdapter.SeasonClickListener() {
      @Override public void onSeasonClick(View view, int position, long id) {
        navigationCallbacks.onDisplaySeason(showId, id, showTitle, seasonsCursor.getInt(
            seasonsCursor.getColumnIndex(DatabaseContract.SeasonColumns.SEASON)), type);
      }
    }, type);
  }

  private void updateTitle() {
    if (toolbar != null) {
      toolbar.setTitle(getTitle());
      toolbar.setSubtitle(getSubtitle());
    }
  }

  public String getTitle() {
    return showTitle == null ? "" : showTitle;
  }

  public String getSubtitle() {
    return genres;
  }

  @Override public boolean onBackPressed() {
    if (hiddenPaneLayout != null) {
      final int state = hiddenPaneLayout.getState();
      if (state == HiddenPaneLayout.STATE_OPEN || state == HiddenPaneLayout.STATE_OPENING) {
        hiddenPaneLayout.close();
        return true;
      }
    }

    return super.onBackPressed();
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.fragment_show, container, false);
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    seasonsManager =
        new RecyclerViewManager(seasons, new LinearLayoutManager(getActivity()), seasonsEmpty);
    seasonsManager.setAdapter(seasonsAdapter);
    seasons.setAdapter(seasonsAdapter);

    seasonsEmpty.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        showScheduler.sync(showId);
      }
    });

    scrollView.setListener(new ObservableScrollView.ScrollListener() {
      @Override public void onScrollChanged(int l, int t) {
        final int offset = (int) (t / 2.0f);
        fanart.setTranslationY(offset);
      }
    });

    rating.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        RatingDialog.newInstance(RatingDialog.Type.SHOW, showId, currentRating)
            .show(getFragmentManager(), DIALOG_RATING);
      }
    });

    toWatchHolder = new EpisodeHolder(toWatch);
    toWatch.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        if (toWatchId != -1) navigationCallbacks.onDisplayEpisode(toWatchId, showTitle);
      }
    });

    toWatchHolder.episodeOverflow.setListener(new OverflowView.OverflowActionListener() {
      @Override public void onPopupShown() {
      }

      @Override public void onPopupDismissed() {
      }

      @Override public void onActionSelected(int action) {
        switch (action) {
          case R.id.action_checkin:
            if (toWatchId != -1) {
              CheckInDialog.showDialogIfNecessary(getActivity(), Type.SHOW, toWatchTitle,
                  toWatchId);
            }
            break;
          case R.id.action_checkin_cancel:
            if (toWatchId != -1) {
              showScheduler.cancelCheckin();
            }
            break;
          case R.id.action_watched:
            if (toWatchId != -1) {
              episodeScheduler.setWatched(toWatchId, true);
            }
            break;
        }
      }
    });

    if (lastWatched != null) {
      lastWatchedHolder = new EpisodeHolder(lastWatched);
      lastWatched.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View view) {
          if (lastWatchedId != -1) {
            navigationCallbacks.onDisplayEpisode(lastWatchedId, showTitle);
          }
        }
      });

      lastWatchedHolder.episodeOverflow.addItem(R.id.action_unwatched, R.string.action_unwatched);
      lastWatchedHolder.episodeOverflow.setListener(new OverflowView.OverflowActionListener() {
        @Override public void onPopupShown() {
        }

        @Override public void onPopupDismissed() {
        }

        @Override public void onActionSelected(int action) {
          switch (action) {
            case R.id.action_unwatched:
              if (lastWatchedId != -1) {
                episodeScheduler.setWatched(lastWatchedId, false);
              }
              break;
          }
        }
      });
    }

    toCollectHolder = new EpisodeHolder(toCollect);
    toCollect.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        if (toCollectId != -1) navigationCallbacks.onDisplayEpisode(toCollectId, showTitle);
      }
    });

    toCollectHolder.episodeOverflow.addItem(R.id.action_collection_add,
        R.string.action_collection_add);
    toCollectHolder.episodeOverflow.setListener(new OverflowView.OverflowActionListener() {
      @Override public void onPopupShown() {
      }

      @Override public void onPopupDismissed() {
      }

      @Override public void onActionSelected(int action) {
        switch (action) {
          case R.id.action_collection_add:
            if (toCollectId != -1) {
              episodeScheduler.setIsInCollection(toCollectId, true);
            }
            break;
        }
      }
    });

    if (lastCollected != null) {
      lastCollectedHolder = new EpisodeHolder(lastCollected);
      lastCollected.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View view) {
          if (lastCollectedId != -1) {
            navigationCallbacks.onDisplayEpisode(lastCollectedId, showTitle);
          }
        }
      });

      lastCollectedHolder.episodeOverflow.addItem(R.id.action_collection_remove,
          R.string.action_collection_remove);
      lastCollectedHolder.episodeOverflow.setListener(new OverflowView.OverflowActionListener() {
        @Override public void onPopupShown() {
        }

        @Override public void onPopupDismissed() {
        }

        @Override public void onActionSelected(int action) {
          switch (action) {
            case R.id.action_collection_add:
              if (lastCollectedId != -1) {
                episodeScheduler.setIsInCollection(lastCollectedId, true);
              }
              break;
          }
        }
      });
    }

    getLoaderManager().initLoader(BaseActivity.LOADER_SHOW, null, showCallbacks);
    getLoaderManager().initLoader(BaseActivity.LOADER_SHOW_GENRES, null, genreCallbacks);
    getLoaderManager().initLoader(BaseActivity.LOADER_SHOW_ACTORS, null, charactersCallback);
    getLoaderManager().initLoader(BaseActivity.LOADER_SHOW_WATCH, null, episodeWatchCallbacks);
    getLoaderManager().initLoader(BaseActivity.LOADER_SHOW_COLLECT, null, episodeCollectCallbacks);
    getLoaderManager().initLoader(BaseActivity.LOADER_SHOW_SEASONS, null, seasonsLoader);
  }

  @Override public void createMenu(Toolbar toolbar) {
    super.createMenu(toolbar);
    Menu menu = toolbar.getMenu();
    toolbar.inflateMenu(R.menu.fragment_show);

    if (inWatchlist) {
      menu.add(0, R.id.action_watchlist_remove, 300, R.string.action_watchlist_remove);
    } else {
      menu.add(0, R.id.action_watchlist_add, 300, R.string.action_watchlist_add);
    }

    if (isHidden) {
      menu.add(0, R.id.menu_show_show_upcoming, 400, R.string.action_show_show_upcoming);
    } else {
      menu.add(0, R.id.menu_show_hide_upcoming, 400, R.string.action_show_hide_upcoming);
    }
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_seasons:
        hiddenPaneLayout.toggle();
        return true;

      case R.id.menu_show_hide_upcoming:
        showScheduler.setIsHidden(showId, true);
        return true;

      case R.id.menu_show_show_upcoming:
        showScheduler.setIsHidden(showId, false);
        return true;

      case R.id.action_watchlist_remove:
        showScheduler.setIsInWatchlist(showId, false);
        return true;

      case R.id.action_watchlist_add:
        showScheduler.setIsInWatchlist(showId, true);
        return true;
    }

    return super.onMenuItemClick(item);
  }

  @Override public void onDestroyView() {
    scrollView.setListener(null);
    super.onDestroyView();
  }

  private void updateShowView(final Cursor cursor) {
    if (cursor == null || !cursor.moveToFirst()) return;

    String title = cursor.getString(cursor.getColumnIndex(ShowColumns.TITLE));
    if (!title.equals(showTitle)) {
      showTitle = title;
      updateTitle();
    }
    final String airTime = cursor.getString(cursor.getColumnIndex(ShowColumns.AIR_TIME));
    final String airDay = cursor.getString(cursor.getColumnIndex(ShowColumns.AIR_DAY));
    final String network = cursor.getString(cursor.getColumnIndex(ShowColumns.NETWORK));
    final String certification = cursor.getString(cursor.getColumnIndex(ShowColumns.CERTIFICATION));
    final String posterUrl = cursor.getString(cursor.getColumnIndex(ShowColumns.POSTER));
    if (posterUrl != null) {
      poster.setImage(posterUrl);
    }
    final String fanartUrl = cursor.getString(cursor.getColumnIndex(ShowColumns.FANART));
    if (fanartUrl != null) {
      fanart.setImage(fanartUrl);
    }
    final String overview = cursor.getString(cursor.getColumnIndex(ShowColumns.OVERVIEW));
    inWatchlist = cursor.getInt(cursor.getColumnIndex(ShowColumns.IN_WATCHLIST)) == 1;
    final int inCollectionCount =
        cursor.getInt(cursor.getColumnIndex(ShowColumns.IN_COLLECTION_COUNT));
    final int watchedCount = cursor.getInt(cursor.getColumnIndex(ShowColumns.WATCHED_COUNT));
    isHidden = cursor.getInt(cursor.getColumnIndex(ShowColumns.HIDDEN)) == 1;

    currentRating = cursor.getInt(cursor.getColumnIndex(ShowColumns.USER_RATING));
    final float ratingAll = cursor.getFloat(cursor.getColumnIndex(ShowColumns.RATING));
    rating.setValue(ratingAll);

    watched.setVisibility(watchedCount > 0 ? View.VISIBLE : View.GONE);
    collection.setVisibility(inCollectionCount > 0 ? View.VISIBLE : View.GONE);
    watchlist.setVisibility(inWatchlist ? View.VISIBLE : View.GONE);

    this.airTime.setText(airDay + " " + airTime + ", " + network);
    this.certification.setText(certification);
    this.overview.setText(overview);

    setContentVisible(true);
    invalidateMenu();
  }

  private void updateGenreViews(final Cursor cursor) {
    if (cursor.getCount() > 0) {
      StringBuilder sb = new StringBuilder();
      final int genreColumnIndex = cursor.getColumnIndex(ShowGenreColumns.GENRE);

      cursor.moveToPosition(-1);

      while (cursor.moveToNext()) {
        sb.append(cursor.getString(genreColumnIndex));
        if (!cursor.isLast()) sb.append(", ");
      }

      genres = sb.toString();
    } else {
      genres = null;
    }

    updateTitle();
  }

  private void updateActorViews(Cursor c) {
    actors.removeAllViews();
    final int count = c.getCount();
    Timber.d("Actor count: %d", count);
    final int visibility = count > 0 ? View.VISIBLE : View.GONE;
    actorsTitle.setVisibility(visibility);
    actorsParent.setVisibility(visibility);

    c.moveToPosition(-1);
    while (c.moveToNext()) {
      View v = LayoutInflater.from(getActivity()).inflate(R.layout.person, actors, false);

      RemoteImageView headshot = (RemoteImageView) v.findViewById(R.id.headshot);
      headshot.setImage(c.getString(c.getColumnIndex(PersonColumns.HEADSHOT)));
      TextView name = (TextView) v.findViewById(R.id.name);
      name.setText(c.getString(c.getColumnIndex(PersonColumns.NAME)));
      TextView character = (TextView) v.findViewById(R.id.job);
      character.setText(c.getString(c.getColumnIndex(ShowCharacterColumns.CHARACTER)));

      actors.addView(v);
    }
  }

  private void updateEpisodeWatchViews(Cursor cursor) {
    if (cursor.moveToFirst()) {
      toWatch.setVisibility(View.VISIBLE);
      watchTitle.setVisibility(View.VISIBLE);

      toWatchId = cursor.getLong(cursor.getColumnIndex(ShowColumns.ID));
      toWatchTitle = cursor.getString(cursor.getColumnIndex(EpisodeColumns.TITLE));

      toWatchHolder.episodeTitle.setText(toWatchTitle);

      final long airTime = cursor.getLong(cursor.getColumnIndex(EpisodeColumns.FIRST_AIRED));

      final int season = cursor.getInt(cursor.getColumnIndex(EpisodeColumns.SEASON));
      final int episode = cursor.getInt(cursor.getColumnIndex(EpisodeColumns.EPISODE));
      toWatchHolder.episodeEpisode.setText("S" + season + "E" + episode);

      final String screenshotUrl = cursor.getString(cursor.getColumnIndex(EpisodeColumns.SCREENSHOT));
      toWatchHolder.episodeScreenshot.setImage(screenshotUrl);

      String airTimeStr = DateUtils.millisToString(getActivity(), airTime, false);

      final boolean watching = cursor.getInt(cursor.getColumnIndex(EpisodeColumns.WATCHING)) == 1;
      final boolean checkedIn =
          cursor.getInt(cursor.getColumnIndex(EpisodeColumns.CHECKED_IN)) == 1;

      toWatchHolder.episodeOverflow.removeItems();
      if (checkedIn) {
        toWatchHolder.episodeOverflow.addItem(R.id.action_checkin_cancel,
            R.string.action_checkin_cancel);
        airTimeStr = getResources().getString(R.string.show_watching);
      } else if (!watching) {
        toWatchHolder.episodeOverflow.addItem(R.id.action_checkin, R.string.action_checkin);
        toWatchHolder.episodeOverflow.addItem(R.id.action_watched, R.string.action_watched);
      }

      toWatchHolder.episodeAirTime.setText(airTimeStr);
    } else {
      toWatch.setVisibility(View.GONE);
      watchTitle.setVisibility(View.GONE);
      toWatchId = -1;
    }

    if (lastWatched != null) {
      if (cursor.moveToNext()) {
        lastWatched.setVisibility(View.VISIBLE);

        lastWatchedId = cursor.getLong(cursor.getColumnIndex(ShowColumns.ID));

        lastWatchedHolder.episodeTitle.setText(
            cursor.getString(cursor.getColumnIndex(EpisodeColumns.TITLE)));

        final long airTime = cursor.getLong(cursor.getColumnIndex(EpisodeColumns.FIRST_AIRED));
        final String airTimeStr = DateUtils.millisToString(getActivity(), airTime, false);
        lastWatchedHolder.episodeAirTime.setText(airTimeStr);

        final int season = cursor.getInt(cursor.getColumnIndex(EpisodeColumns.SEASON));
        final int episode = cursor.getInt(cursor.getColumnIndex(EpisodeColumns.EPISODE));
        lastWatchedHolder.episodeEpisode.setText("S" + season + "E" + episode);

        final String screenshotUrl = cursor.getString(cursor.getColumnIndex(EpisodeColumns.SCREENSHOT));
        lastWatchedHolder.episodeScreenshot.setImage(screenshotUrl);
      } else {
        lastWatched.setVisibility(toWatchId == -1 ? View.GONE : View.INVISIBLE);
        lastWatchedId = -1;
      }
    }

    if (toWatchId == -1 && lastWatchedId == -1 && toCollectId == -1 && lastCollectedId == -1) {
      episodes.setVisibility(View.GONE);
    } else {
      episodes.setVisibility(View.VISIBLE);
    }
  }

  private void updateEpisodeCollectViews(Cursor cursor) {
    if (cursor.moveToFirst()) {
      toCollect.setVisibility(View.VISIBLE);
      collectTitle.setVisibility(View.VISIBLE);

      toCollectId = cursor.getLong(cursor.getColumnIndex(ShowColumns.ID));

      toCollectHolder.episodeTitle.setText(
          cursor.getString(cursor.getColumnIndex(EpisodeColumns.TITLE)));

      final long airTime = cursor.getLong(cursor.getColumnIndex(EpisodeColumns.FIRST_AIRED));
      final String airTimeStr = DateUtils.millisToString(getActivity(), airTime, false);
      toCollectHolder.episodeAirTime.setText(airTimeStr);

      final int season = cursor.getInt(cursor.getColumnIndex(EpisodeColumns.SEASON));
      final int episode = cursor.getInt(cursor.getColumnIndex(EpisodeColumns.EPISODE));
      toCollectHolder.episodeEpisode.setText("S" + season + "E" + episode);

      final String screenshotUrl = cursor.getString(cursor.getColumnIndex(EpisodeColumns.SCREENSHOT));
      toCollectHolder.episodeScreenshot.setImage(screenshotUrl);
    } else {
      toCollect.setVisibility(View.GONE);
      collectTitle.setVisibility(View.GONE);
      toCollectId = -1;
    }

    if (lastCollected != null) {
      if (cursor.moveToNext()) {
        lastCollected.setVisibility(View.VISIBLE);

        lastCollectedId = cursor.getLong(cursor.getColumnIndex(ShowColumns.ID));

        lastCollectedHolder.episodeTitle.setText(
            cursor.getString(cursor.getColumnIndex(EpisodeColumns.TITLE)));

        final long airTime = cursor.getLong(cursor.getColumnIndex(EpisodeColumns.FIRST_AIRED));
        final String airTimeStr = DateUtils.millisToString(getActivity(), airTime, false);
        lastCollectedHolder.episodeAirTime.setText(airTimeStr);

        final int season = cursor.getInt(cursor.getColumnIndex(EpisodeColumns.SEASON));
        final int episode = cursor.getInt(cursor.getColumnIndex(EpisodeColumns.EPISODE));
        lastCollectedHolder.episodeEpisode.setText("S" + season + "E" + episode);

        // TODO: Fanart? Is SCREEN missing?
        final String screenshotUrl = cursor.getString(cursor.getColumnIndex(EpisodeColumns.SCREENSHOT));
        lastCollectedHolder.episodeScreenshot.setImage(screenshotUrl);
      } else {
        lastCollectedId = -1;
        lastCollected.setVisibility(View.INVISIBLE);
      }
    }

    if (toWatchId == -1 && lastWatchedId == -1 && toCollectId == -1 && lastCollectedId == -1) {
      episodes.setVisibility(View.GONE);
    } else {
      episodes.setVisibility(View.VISIBLE);
    }
  }

  private LoaderManager.LoaderCallbacks<Cursor> showCallbacks =
      new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override public Loader<Cursor> onCreateLoader(int id, Bundle args) {
          CursorLoader cl =
              new CursorLoader(getActivity(), Shows.withId(showId), SHOW_PROJECTION, null, null,
                  null);
          cl.setUpdateThrottle(2 * android.text.format.DateUtils.SECOND_IN_MILLIS);
          return cl;
        }

        @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor data) {
          updateShowView(data);
        }

        @Override public void onLoaderReset(Loader<Cursor> cursorLoader) {
        }
      };

  private LoaderManager.LoaderCallbacks<Cursor> genreCallbacks =
      new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override public Loader<Cursor> onCreateLoader(int id, Bundle args) {
          CursorLoader cl =
              new CursorLoader(getActivity(), ShowGenres.fromShow(showId), GENRES_PROJECTION, null,
                  null, ShowGenres.DEFAULT_SORT);
          cl.setUpdateThrottle(2 * android.text.format.DateUtils.SECOND_IN_MILLIS);
          return cl;
        }

        @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor data) {
          updateGenreViews(data);
        }

        @Override public void onLoaderReset(Loader<Cursor> cursorLoader) {
        }
      };

  private static final String[] CHARACTERS_PROJECTION = new String[] {
      Tables.SHOW_CHARACTERS + "." + ShowCharacterColumns.ID,
      Tables.SHOW_CHARACTERS + "." + ShowCharacterColumns.CHARACTER,
      Tables.PEOPLE + "." + PersonColumns.NAME, Tables.PEOPLE + "." + PersonColumns.HEADSHOT,
  };

  private LoaderManager.LoaderCallbacks<Cursor> charactersCallback =
      new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override public Loader<Cursor> onCreateLoader(int id, Bundle args) {
          CursorLoader cl =
              new CursorLoader(getActivity(), ProviderSchematic.ShowCharacters.fromShow(showId),
                  CHARACTERS_PROJECTION, Tables.PEOPLE + "." + PersonColumns.NEEDS_SYNC + "=0",
                  null, null);
          cl.setUpdateThrottle(2 * android.text.format.DateUtils.SECOND_IN_MILLIS);
          return cl;
        }

        @Override public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
          updateActorViews(data);
        }

        @Override public void onLoaderReset(Loader<Cursor> loader) {
        }
      };

  private LoaderManager.LoaderCallbacks<Cursor> episodeWatchCallbacks =
      new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
          return new WatchedLoader(getActivity(), showId, EPISODE_PROJECTION);
        }

        @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
          updateEpisodeWatchViews(cursor);
        }

        @Override public void onLoaderReset(Loader<Cursor> cursorLoader) {
        }
      };

  private LoaderManager.LoaderCallbacks<Cursor> episodeCollectCallbacks =
      new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
          return new CollectLoader(getActivity(), showId, EPISODE_PROJECTION);
        }

        @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
          updateEpisodeCollectViews(cursor);
        }

        @Override public void onLoaderReset(Loader<Cursor> cursorLoader) {
        }
      };

  private LoaderManager.LoaderCallbacks<Cursor> seasonsLoader =
      new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override public Loader<Cursor> onCreateLoader(int id, Bundle args) {
          CursorLoader cl =
              new CursorLoader(getActivity(), Seasons.fromShow(showId), SeasonsAdapter.PROJECTION,
                  null, null, Seasons.DEFAULT_SORT);
          cl.setUpdateThrottle(2 * android.text.format.DateUtils.SECOND_IN_MILLIS);
          return cl;
        }

        @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor data) {
          seasonsCursor = data;
          seasonsAdapter.changeCursor(data);
        }

        @Override public void onLoaderReset(Loader<Cursor> cursorLoader) {
          seasonsCursor = null;
          seasonsAdapter.changeCursor(null);
        }
      };
}
