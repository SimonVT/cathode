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
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.squareup.otto.Bus;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.api.util.TraktUtils;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.database.SimpleMergeCursor;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.provider.CollectLoader;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.HiddenColumns;
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowCharacterColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowGenreColumns;
import net.simonvt.cathode.provider.DatabaseContract.UserColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.ProviderSchematic;
import net.simonvt.cathode.provider.ProviderSchematic.Comments;
import net.simonvt.cathode.provider.ProviderSchematic.Seasons;
import net.simonvt.cathode.provider.ProviderSchematic.ShowGenres;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.WatchedLoader;
import net.simonvt.cathode.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.settings.TraktTimestamps;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.NavigationListener;
import net.simonvt.cathode.ui.adapter.LinearCommentsAdapter;
import net.simonvt.cathode.ui.adapter.SeasonsAdapter;
import net.simonvt.cathode.ui.dialog.CheckInDialog;
import net.simonvt.cathode.ui.dialog.CheckInDialog.Type;
import net.simonvt.cathode.ui.dialog.ListsDialog;
import net.simonvt.cathode.ui.dialog.RatingDialog;
import net.simonvt.cathode.ui.listener.SeasonClickListener;
import net.simonvt.cathode.util.DateUtils;
import net.simonvt.cathode.util.Intents;
import net.simonvt.cathode.util.SqlColumn;
import net.simonvt.cathode.widget.CircleTransformation;
import net.simonvt.cathode.widget.CircularProgressIndicator;
import net.simonvt.cathode.widget.HiddenPaneLayout;
import net.simonvt.cathode.widget.OverflowView;
import net.simonvt.cathode.widget.RecyclerViewManager;
import net.simonvt.cathode.widget.RemoteImageView;
import net.simonvt.schematic.Cursors;
import timber.log.Timber;

public class ShowFragment extends RefreshableAppBarFragment {

  public static final String TAG = "net.simonvt.cathode.ui.fragment.ShowFragment";

  private static final String ARG_SHOWID = "net.simonvt.cathode.ui.fragment.ShowFragment.showId";
  private static final String ARG_TITLE = "net.simonvt.cathode.ui.fragment.ShowFragment.title";
  private static final String ARG_OVERVIEW =
      "net.simonvt.cathode.ui.fragment.ShowFragment.overview";
  private static final String ARG_TYPE = "net.simonvt.cathode.ui.fragment.ShowFragment.type";

  private static final String DIALOG_RATING =
      "net.simonvt.cathode.ui.fragment.ShowFragment.ratingDialog";
  private static final String DIALOG_LISTS_ADD =
      "net.simonvt.cathode.ui.fragment.ShowFragment.listsAddDialog";
  private static final String DIALOG_COMMENT_UPDATE =
      "net.simonvt.cathode.ui.fragment.ShowFragment.updateCommentDialog";

  private static final int LOADER_SHOW = 1;
  private static final int LOADER_SHOW_WATCH = 2;
  private static final int LOADER_SHOW_COLLECT = 3;
  private static final int LOADER_SHOW_GENRES = 4;
  private static final int LOADER_SHOW_SEASONS = 5;
  private static final int LOADER_SHOW_ACTORS = 6;
  private static final int LOADER_SHOW_USER_COMMENTS = 7;
  private static final int LOADER_SHOW_COMMENTS = 8;

  private static final String[] SHOW_PROJECTION = new String[] {
      ShowColumns.TITLE, ShowColumns.YEAR, ShowColumns.AIR_TIME, ShowColumns.AIR_DAY,
      ShowColumns.NETWORK, ShowColumns.CERTIFICATION, ShowColumns.POSTER, ShowColumns.FANART,
      ShowColumns.USER_RATING, ShowColumns.RATING, ShowColumns.OVERVIEW, ShowColumns.IN_WATCHLIST,
      ShowColumns.IN_COLLECTION_COUNT, ShowColumns.WATCHED_COUNT, ShowColumns.LAST_SYNC,
      ShowColumns.LAST_COMMENT_SYNC, ShowColumns.LAST_ACTORS_SYNC, ShowColumns.HOMEPAGE,
      ShowColumns.TRAILER, ShowColumns.IMDB_ID, ShowColumns.TVDB_ID, ShowColumns.TMDB_ID,
      ShowColumns.NEEDS_SYNC, HiddenColumns.HIDDEN_CALENDAR,
  };

  private static final String[] EPISODE_PROJECTION = new String[] {
      EpisodeColumns.ID, EpisodeColumns.TITLE, EpisodeColumns.SCREENSHOT,
      EpisodeColumns.FIRST_AIRED, EpisodeColumns.SEASON, EpisodeColumns.EPISODE,
      EpisodeColumns.WATCHING, EpisodeColumns.CHECKED_IN,
  };

  private static final String[] GENRES_PROJECTION = new String[] {
      ShowGenreColumns.GENRE,
  };

  private NavigationListener navigationCallbacks;

  private long showId;

  @BindView(R.id.hiddenPaneLayout) HiddenPaneLayout hiddenPaneLayout;

  @BindView(R.id.seasons) RecyclerView seasons;
  @BindView(R.id.seasonsEmpty) View seasonsEmpty;
  private SeasonsAdapter seasonsAdapter;
  private Cursor seasonsCursor;

  @BindView(R.id.rating) CircularProgressIndicator rating;
  @BindView(R.id.airtime) TextView airTime;
  @BindView(R.id.certification) TextView certification;
  @BindView(R.id.overview) TextView overview;
  @BindView(R.id.isWatched) TextView watched;
  @BindView(R.id.inCollection) TextView collection;
  @BindView(R.id.inWatchlist) TextView watchlist;

  @BindView(R.id.trailer) View trailer;

  @BindView(R.id.actorsParent) View actorsParent;
  @BindView(R.id.actorsHeader) View actorsHeader;
  @BindView(R.id.actors) LinearLayout actors;
  @BindView(R.id.peopleContainer) LinearLayout peopleContainer;

  @BindView(R.id.commentsParent) View commentsParent;
  @BindView(R.id.commentsHeader) View commentsHeader;
  @BindView(R.id.commentsContainer) LinearLayout commentsContainer;

  @BindView(R.id.websiteTitle) View websiteTitle;
  @BindView(R.id.website) TextView website;

  @BindView(R.id.viewOnTitle) View viewOnTitle;
  @BindView(R.id.viewOnContainer) ViewGroup viewOnContainer;
  @BindView(R.id.viewOnImdb) View viewOnImdb;
  @BindView(R.id.viewOnTvdb) View viewOnTvdb;
  @BindView(R.id.viewOnTmdb) View viewOnTmdb;

  private Cursor userComments;
  private Cursor comments;

  @BindView(R.id.episodes) LinearLayout episodes;

  @BindView(R.id.toWatch) View toWatch;
  private EpisodeHolder toWatchHolder;
  private long toWatchId = -1;
  private String toWatchTitle;

  @BindView(R.id.lastWatched) @Nullable View lastWatched;
  private EpisodeHolder lastWatchedHolder;
  private long lastWatchedId = -1;

  @BindView(R.id.toCollect) View toCollect;
  private EpisodeHolder toCollectHolder;
  private long toCollectId = -1;

  @BindView(R.id.lastCollected) @Nullable View lastCollected;
  private EpisodeHolder lastCollectedHolder;
  private long lastCollectedId = -1;

  static class EpisodeHolder {

    @BindView(R.id.episodeScreenshot) RemoteImageView episodeScreenshot;
    @BindView(R.id.episodeTitle) TextView episodeTitle;
    @BindView(R.id.episodeAirTime) TextView episodeAirTime;
    @BindView(R.id.episodeEpisode) TextView episodeEpisode;
    @BindView(R.id.episodeOverflow) OverflowView episodeOverflow;

    public EpisodeHolder(View v) {
      ButterKnife.bind(this, v);
    }
  }

  @Inject ShowTaskScheduler showScheduler;
  @Inject EpisodeTaskScheduler episodeScheduler;

  @Inject Bus bus;

  private String showTitle;

  private String showOverview;

  private String genres;

  private boolean inWatchlist;

  private int currentRating;

  private boolean calendarHidden;

  private LibraryType type;

  RecyclerViewManager seasonsManager;

  public static Bundle getArgs(long showId, String title, String overview, LibraryType type) {
    if (showId < 0) {
      throw new IllegalArgumentException("showId must be >= 0");
    }

    Bundle args = new Bundle();
    args.putLong(ARG_SHOWID, showId);
    args.putString(ARG_TITLE, title);
    args.putString(ARG_OVERVIEW, overview);
    args.putSerializable(ARG_TYPE, type);
    return args;
  }

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    navigationCallbacks = (NavigationListener) activity;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    Timber.d("ShowFragment#onCreate");
    CathodeApp.inject(getActivity(), this);

    Bundle args = getArguments();
    showId = args.getLong(ARG_SHOWID);
    showTitle = args.getString(ARG_TITLE);
    showOverview = args.getString(ARG_OVERVIEW);
    type = (LibraryType) args.getSerializable(ARG_TYPE);

    setTitle(showTitle);

    seasonsAdapter = new SeasonsAdapter(getActivity(), new SeasonClickListener() {
      @Override
      public void onSeasonClick(long showId, long seasonId, String showTitle, int seasonNumber) {
        navigationCallbacks.onDisplaySeason(showId, seasonId, showTitle, seasonNumber, type);
      }
    }, type);
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
    HiddenPaneLayout hiddenPane =
        (HiddenPaneLayout) inflater.inflate(R.layout.fragment_show_hiddenpanelayout, container,
            false);
    View v = super.onCreateView(inflater, hiddenPane, inState);
    hiddenPane.addView(v);

    inflater.inflate(R.layout.fragment_show_seasons, hiddenPane, true);

    return hiddenPane;
  }

  @Override public View createView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.fragment_show, container, false);
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    overview.setText(showOverview);

    seasonsManager =
        new RecyclerViewManager(seasons, new LinearLayoutManager(getActivity()), seasonsEmpty);
    seasonsManager.setAdapter(seasonsAdapter);
    seasons.setAdapter(seasonsAdapter);
    ((DefaultItemAnimator) seasons.getItemAnimator()).setSupportsChangeAnimations(false);

    seasonsEmpty.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        showScheduler.sync(showId);
      }
    });

    rating.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        RatingDialog.newInstance(RatingDialog.Type.SHOW, showId, currentRating)
            .show(getFragmentManager(), DIALOG_RATING);
      }
    });

    actorsHeader.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        navigationCallbacks.onDisplayShowActors(showId, showTitle);
      }
    });

    commentsHeader.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        navigationCallbacks.onDisplayComments(ItemType.SHOW, showId);
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

    getLoaderManager().initLoader(LOADER_SHOW, null, showCallbacks);
    getLoaderManager().initLoader(LOADER_SHOW_GENRES, null, genreCallbacks);
    getLoaderManager().initLoader(LOADER_SHOW_ACTORS, null, charactersCallback);
    getLoaderManager().initLoader(LOADER_SHOW_WATCH, null, episodeWatchCallbacks);
    getLoaderManager().initLoader(LOADER_SHOW_COLLECT, null, episodeCollectCallbacks);
    getLoaderManager().initLoader(LOADER_SHOW_SEASONS, null, seasonsLoader);
    getLoaderManager().initLoader(LOADER_SHOW_USER_COMMENTS, null, userCommentsLoader);
    getLoaderManager().initLoader(LOADER_SHOW_COMMENTS, null, commentsLoader);
  }

  private Job.OnDoneListener onDoneListener = new Job.OnDoneListener() {
    @Override public void onDone(Job job) {
      setRefreshing(false);
    }
  };

  @Override public void onRefresh() {
    showScheduler.sync(showId, onDoneListener);
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

    if (calendarHidden) {
      menu.add(0, R.id.action_calendar_unhide, 400, R.string.action_calendar_unhide);
    } else {
      menu.add(0, R.id.action_calendar_hide, 400, R.string.action_calendar_hide);
    }
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_seasons:
        hiddenPaneLayout.toggle();
        return true;

      case R.id.action_watchlist_remove:
        showScheduler.setIsInWatchlist(showId, false);
        return true;

      case R.id.action_watchlist_add:
        showScheduler.setIsInWatchlist(showId, true);
        return true;

      case R.id.menu_lists_add:
        ListsDialog.newInstance(DatabaseContract.ItemType.SHOW, showId)
            .show(getFragmentManager(), DIALOG_LISTS_ADD);
        return true;

      case R.id.action_calendar_hide:
        showScheduler.hideFromCalendar(showId, true);
        return true;

      case R.id.action_calendar_unhide:
        showScheduler.hideFromCalendar(showId, false);
        return true;
    }

    return super.onMenuItemClick(item);
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
  }

  private void updateShowView(final Cursor cursor) {
    if (cursor == null || !cursor.moveToFirst()) return;

    String title = Cursors.getString(cursor, ShowColumns.TITLE);
    if (!TextUtils.equals(title, showTitle)) {
      showTitle = title;
      setTitle(title);
    }
    final String airTime = Cursors.getString(cursor, ShowColumns.AIR_TIME);
    final String airDay = Cursors.getString(cursor, ShowColumns.AIR_DAY);
    final String network = Cursors.getString(cursor, ShowColumns.NETWORK);
    final String certification = Cursors.getString(cursor, ShowColumns.CERTIFICATION);
    final String fanartUrl = Cursors.getString(cursor, ShowColumns.FANART);
    if (fanartUrl != null) {
      setBackdrop(fanartUrl, true);
    }
    showOverview = Cursors.getString(cursor, ShowColumns.OVERVIEW);
    inWatchlist = Cursors.getBoolean(cursor, ShowColumns.IN_WATCHLIST);
    final int inCollectionCount = Cursors.getInt(cursor, ShowColumns.IN_COLLECTION_COUNT);
    final int watchedCount = Cursors.getInt(cursor, ShowColumns.WATCHED_COUNT);

    currentRating = Cursors.getInt(cursor, ShowColumns.USER_RATING);
    final float ratingAll = Cursors.getFloat(cursor, ShowColumns.RATING);
    rating.setValue(ratingAll);

    calendarHidden = Cursors.getBoolean(cursor, HiddenColumns.HIDDEN_CALENDAR);

    watched.setVisibility(watchedCount > 0 ? View.VISIBLE : View.GONE);
    collection.setVisibility(inCollectionCount > 0 ? View.VISIBLE : View.GONE);
    watchlist.setVisibility(inWatchlist ? View.VISIBLE : View.GONE);

    String airTimeString = null;
    if (airDay != null && airTime != null) {
      airTimeString = airDay + " " + airTime;
    }
    if (network != null) {
      if (airTimeString != null) {
        airTimeString += ", " + network;
      } else {
        airTimeString = network;
      }
    }

    this.airTime.setText(airTimeString);
    this.certification.setText(certification);
    this.overview.setText(showOverview);

    final String trailer = Cursors.getString(cursor, ShowColumns.TRAILER);
    if (!TextUtils.isEmpty(trailer)) {
      this.trailer.setVisibility(View.VISIBLE);
      this.trailer.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          Intents.openUrl(getActivity(), trailer);
        }
      });
    } else {
      this.trailer.setVisibility(View.GONE);
    }

    final boolean needsSync = Cursors.getBoolean(cursor, ShowColumns.NEEDS_SYNC);
    if (needsSync) {
      Timber.d("Needs sync: %d", showId);
      showScheduler.sync(showId);
    }

    final long lastSync = Cursors.getLong(cursor, ShowColumns.LAST_SYNC);

    final long lastCommentSync = Cursors.getLong(cursor, ShowColumns.LAST_COMMENT_SYNC);
    if (TraktTimestamps.shouldSyncComments(lastCommentSync)) {
      showScheduler.syncComments(showId);
    }

    final long lastActorsSync = Cursors.getLong(cursor, ShowColumns.LAST_ACTORS_SYNC);
    if (lastSync > lastActorsSync) {
      showScheduler.syncActors(showId);
    }

    final String website = Cursors.getString(cursor, ShowColumns.HOMEPAGE);
    if (!TextUtils.isEmpty(website)) {
      this.websiteTitle.setVisibility(View.VISIBLE);
      this.website.setVisibility(View.VISIBLE);

      this.website.setText(website);
      this.website.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          Intents.openUrl(getContext(), website);
        }
      });
    } else {
      this.websiteTitle.setVisibility(View.GONE);
      this.website.setVisibility(View.GONE);
    }

    final String imdbId = Cursors.getString(cursor, ShowColumns.IMDB_ID);
    final int tvdbId = Cursors.getInt(cursor, ShowColumns.TVDB_ID);
    final int tmdbId = Cursors.getInt(cursor, ShowColumns.TMDB_ID);

    final boolean hasImdbId = !TextUtils.isEmpty(imdbId);
    if (hasImdbId) {
      viewOnImdb.setVisibility(View.VISIBLE);
      viewOnImdb.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          Intents.openUrl(getContext(), TraktUtils.getImdbUrl(imdbId));
        }
      });
    } else {
      viewOnImdb.setVisibility(View.GONE);
    }

    if (tvdbId > 0) {
      viewOnTvdb.setVisibility(View.VISIBLE);
      viewOnTvdb.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          Intents.openUrl(getContext(), TraktUtils.getTvdbUrl(tvdbId));
        }
      });
    } else {
      viewOnTvdb.setVisibility(View.GONE);
    }

    if (tmdbId > 0) {
      viewOnTmdb.setVisibility(View.VISIBLE);
      viewOnTmdb.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          Intents.openUrl(getContext(), TraktUtils.getTmdbTvUrl(tmdbId));
        }
      });
    } else {
      viewOnTmdb.setVisibility(View.GONE);
    }

    if (hasImdbId || tvdbId > 0 || tmdbId > 0) {
      viewOnTitle.setVisibility(View.VISIBLE);
      viewOnContainer.setVisibility(View.VISIBLE);
    } else {
      viewOnTitle.setVisibility(View.GONE);
      viewOnContainer.setVisibility(View.GONE);
    }

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
  }

  private void updateActorViews(Cursor c) {
    peopleContainer.removeAllViews();

    final int count = c.getCount();
    final int visibility = count > 0 ? View.VISIBLE : View.GONE;
    actorsParent.setVisibility(visibility);

    int index = 0;

    c.moveToPosition(-1);
    while (c.moveToNext() && index < 3) {
      View v = LayoutInflater.from(getActivity()).inflate(R.layout.item_person, actors, false);

      RemoteImageView headshot = (RemoteImageView) v.findViewById(R.id.headshot);
      headshot.addTransformation(new CircleTransformation());
      headshot.setImage(Cursors.getString(c, PersonColumns.HEADSHOT));

      TextView name = (TextView) v.findViewById(R.id.person_name);
      name.setText(Cursors.getString(c, PersonColumns.NAME));

      TextView character = (TextView) v.findViewById(R.id.person_job);
      character.setText(Cursors.getString(c, ShowCharacterColumns.CHARACTER));

      peopleContainer.addView(v);

      index++;
    }
  }

  private void updateEpisodeWatchViews(Cursor cursor) {
    if (cursor.moveToFirst()) {
      toWatch.setVisibility(View.VISIBLE);

      toWatchId = Cursors.getLong(cursor, ShowColumns.ID);
      toWatchTitle = Cursors.getString(cursor, EpisodeColumns.TITLE);

      toWatchHolder.episodeTitle.setText(toWatchTitle);

      final long airTime = Cursors.getLong(cursor, EpisodeColumns.FIRST_AIRED);

      final int season = Cursors.getInt(cursor, EpisodeColumns.SEASON);
      final int episode = Cursors.getInt(cursor, EpisodeColumns.EPISODE);
      final String toWatchEpisodeText = getString(R.string.season_x_episode_y, season, episode);
      toWatchHolder.episodeEpisode.setText(toWatchEpisodeText);

      final String screenshotUrl = Cursors.getString(cursor, EpisodeColumns.SCREENSHOT);
      toWatchHolder.episodeScreenshot.setImage(screenshotUrl);

      String airTimeStr = DateUtils.millisToString(getActivity(), airTime, false);

      final boolean watching = Cursors.getBoolean(cursor, EpisodeColumns.WATCHING);
      final boolean checkedIn = Cursors.getBoolean(cursor, EpisodeColumns.CHECKED_IN);

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
      toWatchId = -1;
    }

    if (lastWatched != null) {
      if (cursor.moveToNext()) {
        lastWatched.setVisibility(View.VISIBLE);

        lastWatchedId = Cursors.getLong(cursor, ShowColumns.ID);

        lastWatchedHolder.episodeTitle.setText(Cursors.getString(cursor, EpisodeColumns.TITLE));

        final long airTime = Cursors.getLong(cursor, EpisodeColumns.FIRST_AIRED);
        final String airTimeStr = DateUtils.millisToString(getActivity(), airTime, false);
        lastWatchedHolder.episodeAirTime.setText(airTimeStr);

        final int season = Cursors.getInt(cursor, EpisodeColumns.SEASON);
        final int episode = Cursors.getInt(cursor, EpisodeColumns.EPISODE);
        final String lastWatchedEpisodeText =
            getString(R.string.season_x_episode_y, season, episode);
        lastWatchedHolder.episodeEpisode.setText(lastWatchedEpisodeText);

        final String screenshotUrl = Cursors.getString(cursor, EpisodeColumns.SCREENSHOT);
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

      toCollectId = Cursors.getLong(cursor, ShowColumns.ID);

      toCollectHolder.episodeTitle.setText(Cursors.getString(cursor, EpisodeColumns.TITLE));

      final long airTime = Cursors.getLong(cursor, EpisodeColumns.FIRST_AIRED);
      final String airTimeStr = DateUtils.millisToString(getActivity(), airTime, false);
      toCollectHolder.episodeAirTime.setText(airTimeStr);

      final int season = Cursors.getInt(cursor, EpisodeColumns.SEASON);
      final int episode = Cursors.getInt(cursor, EpisodeColumns.EPISODE);
      final String toCollectEpisodeText = getString(R.string.season_x_episode_y, season, episode);
      toCollectHolder.episodeEpisode.setText(toCollectEpisodeText);

      final String screenshotUrl = Cursors.getString(cursor, EpisodeColumns.SCREENSHOT);
      toCollectHolder.episodeScreenshot.setImage(screenshotUrl);
    } else {
      toCollect.setVisibility(View.GONE);
      toCollectId = -1;
    }

    if (lastCollected != null) {
      if (cursor.moveToNext()) {
        lastCollected.setVisibility(View.VISIBLE);

        lastCollectedId = Cursors.getLong(cursor, ShowColumns.ID);

        lastCollectedHolder.episodeTitle.setText(Cursors.getString(cursor, EpisodeColumns.TITLE));

        final long airTime = Cursors.getLong(cursor, EpisodeColumns.FIRST_AIRED);
        final String airTimeStr = DateUtils.millisToString(getActivity(), airTime, false);
        lastCollectedHolder.episodeAirTime.setText(airTimeStr);

        final int season = Cursors.getInt(cursor, EpisodeColumns.SEASON);
        final int episode = Cursors.getInt(cursor, EpisodeColumns.EPISODE);
        final String lastCollectedEpisodeText =
            getString(R.string.season_x_episode_y, season, episode);
        lastCollectedHolder.episodeEpisode.setText(lastCollectedEpisodeText);

        final String screenshotUrl = Cursors.getString(cursor, EpisodeColumns.SCREENSHOT);
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

  private void updateComments() {
    LinearCommentsAdapter.updateComments(getContext(), commentsContainer, userComments, comments);
    commentsParent.setVisibility(View.VISIBLE);
  }

  private LoaderManager.LoaderCallbacks<SimpleCursor> showCallbacks =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          return new SimpleCursorLoader(getActivity(), Shows.withId(showId), SHOW_PROJECTION, null,
              null, null);
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> cursorLoader, SimpleCursor data) {
          updateShowView(data);
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> cursorLoader) {
        }
      };

  private LoaderManager.LoaderCallbacks<SimpleCursor> genreCallbacks =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          return new SimpleCursorLoader(getActivity(), ShowGenres.fromShow(showId), GENRES_PROJECTION,
              null, null, ShowGenres.DEFAULT_SORT);
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> cursorLoader, SimpleCursor data) {
          updateGenreViews(data);
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> cursorLoader) {
        }
      };

  private static final String[] CHARACTERS_PROJECTION = new String[] {
      Tables.SHOW_CHARACTERS + "." + ShowCharacterColumns.ID,
      Tables.SHOW_CHARACTERS + "." + ShowCharacterColumns.CHARACTER,
      Tables.PEOPLE + "." + PersonColumns.NAME, Tables.PEOPLE + "." + PersonColumns.HEADSHOT,
  };

  private LoaderManager.LoaderCallbacks<SimpleCursor> charactersCallback =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          return new SimpleCursorLoader(getActivity(),
              ProviderSchematic.ShowCharacters.fromShow(showId), CHARACTERS_PROJECTION,
              Tables.PEOPLE + "." + PersonColumns.NEEDS_SYNC + "=0", null, null);
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
          updateActorViews(data);
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
        }
      };

  private LoaderManager.LoaderCallbacks<SimpleMergeCursor> episodeWatchCallbacks =
      new LoaderManager.LoaderCallbacks<SimpleMergeCursor>() {
        @Override public Loader<SimpleMergeCursor> onCreateLoader(int i, Bundle bundle) {
          return new WatchedLoader(getActivity(), showId, EPISODE_PROJECTION);
        }

        @Override public void onLoadFinished(Loader<SimpleMergeCursor> cursorLoader,
            SimpleMergeCursor cursor) {
          updateEpisodeWatchViews(cursor);
        }

        @Override public void onLoaderReset(Loader<SimpleMergeCursor> cursorLoader) {
        }
      };

  private LoaderManager.LoaderCallbacks<SimpleMergeCursor> episodeCollectCallbacks =
      new LoaderManager.LoaderCallbacks<SimpleMergeCursor>() {
        @Override public Loader<SimpleMergeCursor> onCreateLoader(int i, Bundle bundle) {
          return new CollectLoader(getActivity(), showId, EPISODE_PROJECTION);
        }

        @Override public void onLoadFinished(Loader<SimpleMergeCursor> cursorLoader,
            SimpleMergeCursor cursor) {
          updateEpisodeCollectViews(cursor);
        }

        @Override public void onLoaderReset(Loader<SimpleMergeCursor> cursorLoader) {
        }
      };

  private LoaderManager.LoaderCallbacks<SimpleCursor> seasonsLoader =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          return new SimpleCursorLoader(getActivity(), Seasons.fromShow(showId),
              SeasonsAdapter.PROJECTION, null, null, Seasons.DEFAULT_SORT);
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> cursorLoader, SimpleCursor data) {
          seasonsCursor = data;
          seasonsAdapter.changeCursor(data);
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> cursorLoader) {
          seasonsCursor = null;
          seasonsAdapter.changeCursor(null);
        }
      };

  private static final String[] COMMENTS_PROJECTION = new String[] {
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.ID),
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.COMMENT),
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.SPOILER),
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.REVIEW),
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.CREATED_AT),
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.LIKES),
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.USER_RATING),
      SqlColumn.table(Tables.USERS).column(UserColumns.USERNAME),
      SqlColumn.table(Tables.USERS).column(UserColumns.NAME),
      SqlColumn.table(Tables.USERS).column(UserColumns.AVATAR),
  };

  private LoaderManager.LoaderCallbacks<SimpleCursor> userCommentsLoader =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          return new SimpleCursorLoader(getContext(), Comments.fromShow(showId), COMMENTS_PROJECTION,
              CommentColumns.IS_USER_COMMENT + "=1", null, null);
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
          userComments = data;
          updateComments();
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
        }
      };

  private LoaderManager.LoaderCallbacks<SimpleCursor> commentsLoader =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          return new SimpleCursorLoader(getContext(), Comments.fromShow(showId), COMMENTS_PROJECTION,
              CommentColumns.IS_USER_COMMENT + "=0 AND " + CommentColumns.SPOILER + "=0", null,
              CommentColumns.LIKES + " DESC LIMIT 3");
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
          comments = data;
          updateComments();
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
        }
      };
}
