/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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
package net.simonvt.cathode.ui.show;

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
import java.util.Locale;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.api.enumeration.ShowStatus;
import net.simonvt.cathode.api.util.TraktUtils;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.database.SimpleMergeCursor;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.provider.CollectLoader;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.HiddenColumns;
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns;
import net.simonvt.cathode.provider.DatabaseContract.RelatedShowsColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowCastColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowGenreColumns;
import net.simonvt.cathode.provider.DatabaseContract.UserColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.ProviderSchematic;
import net.simonvt.cathode.provider.ProviderSchematic.Comments;
import net.simonvt.cathode.provider.ProviderSchematic.RelatedShows;
import net.simonvt.cathode.provider.ProviderSchematic.Seasons;
import net.simonvt.cathode.provider.ProviderSchematic.ShowGenres;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.WatchedLoader;
import net.simonvt.cathode.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.scheduler.PersonTaskScheduler;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.settings.TraktTimestamps;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.NavigationListener;
import net.simonvt.cathode.ui.comments.LinearCommentsAdapter;
import net.simonvt.cathode.ui.dialog.CheckInDialog;
import net.simonvt.cathode.ui.dialog.CheckInDialog.Type;
import net.simonvt.cathode.ui.dialog.RatingDialog;
import net.simonvt.cathode.ui.fragment.RefreshableAppBarFragment;
import net.simonvt.cathode.ui.listener.SeasonClickListener;
import net.simonvt.cathode.ui.lists.ListsDialog;
import net.simonvt.cathode.util.DataHelper;
import net.simonvt.cathode.util.DateUtils;
import net.simonvt.cathode.util.Ids;
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

  private static final String TAG = "net.simonvt.cathode.ui.show.ShowFragment";

  private static final String ARG_SHOWID = "net.simonvt.cathode.ui.show.ShowFragment.showId";
  private static final String ARG_TITLE = "net.simonvt.cathode.ui.show.ShowFragment.title";
  private static final String ARG_OVERVIEW = "net.simonvt.cathode.ui.show.ShowFragment.overview";
  private static final String ARG_TYPE = "net.simonvt.cathode.ui.show.ShowFragment.type";

  private static final String DIALOG_RATING =
      "net.simonvt.cathode.ui.show.ShowFragment.ratingDialog";
  private static final String DIALOG_LISTS_ADD =
      "net.simonvt.cathode.ui.show.ShowFragment.listsAddDialog";
  private static final String DIALOG_COMMENT_UPDATE =
      "net.simonvt.cathode.ui.show.ShowFragment.updateCommentDialog";

  private static final int LOADER_SHOW = 1;
  private static final int LOADER_SHOW_WATCH = 2;
  private static final int LOADER_SHOW_COLLECT = 3;
  private static final int LOADER_SHOW_GENRES = 4;
  private static final int LOADER_SHOW_SEASONS = 5;
  private static final int LOADER_SHOW_CAST = 6;
  private static final int LOADER_SHOW_USER_COMMENTS = 7;
  private static final int LOADER_SHOW_COMMENTS = 8;
  private static final int LOADER_RELATED = 9;

  private static final String[] SHOW_PROJECTION = new String[] {
      ShowColumns.TITLE, ShowColumns.YEAR, ShowColumns.AIR_TIME, ShowColumns.AIR_DAY,
      ShowColumns.NETWORK, ShowColumns.CERTIFICATION, ShowColumns.STATUS, ShowColumns.USER_RATING,
      ShowColumns.RATING, ShowColumns.OVERVIEW, ShowColumns.IN_WATCHLIST,
      ShowColumns.IN_COLLECTION_COUNT, ShowColumns.WATCHED_COUNT, ShowColumns.LAST_SYNC,
      ShowColumns.LAST_COMMENT_SYNC, ShowColumns.LAST_CREDITS_SYNC, ShowColumns.LAST_RELATED_SYNC,
      ShowColumns.HOMEPAGE, ShowColumns.TRAILER, ShowColumns.IMDB_ID, ShowColumns.TVDB_ID,
      ShowColumns.TMDB_ID, ShowColumns.NEEDS_SYNC, HiddenColumns.HIDDEN_CALENDAR,
  };

  private static final String[] EPISODE_PROJECTION = new String[] {
      EpisodeColumns.ID, EpisodeColumns.TITLE, EpisodeColumns.FIRST_AIRED, EpisodeColumns.SEASON,
      EpisodeColumns.EPISODE, EpisodeColumns.WATCHING, EpisodeColumns.CHECKED_IN,
  };

  private static final String[] GENRES_PROJECTION = new String[] {
      ShowGenreColumns.GENRE,
  };

  @Inject PersonTaskScheduler personScheduler;

  private NavigationListener navigationListener;

  private long showId;

  @BindView(R.id.hiddenPaneLayout) HiddenPaneLayout hiddenPaneLayout;

  @BindView(R.id.seasons) RecyclerView seasons;
  @BindView(R.id.seasonsEmpty) View seasonsEmpty;
  private SeasonsAdapter seasonsAdapter;
  private Cursor seasonsCursor;

  @BindView(R.id.rating) CircularProgressIndicator rating;
  @BindView(R.id.airtime) TextView airTime;
  @BindView(R.id.status) TextView status;
  @BindView(R.id.overview) TextView overview;

  @BindView(R.id.genresTitle) View genresTitle;
  @BindView(R.id.genres) TextView genres;

  @BindView(R.id.isWatched) TextView watched;
  @BindView(R.id.inCollection) TextView collection;
  @BindView(R.id.inWatchlist) TextView watchlist;

  @BindView(R.id.trailer) View trailer;

  @BindView(R.id.castParent) View castParent;
  @BindView(R.id.castHeader) View castHeader;
  @BindView(R.id.cast) LinearLayout cast;
  @BindView(R.id.castContainer) LinearLayout castContainer;

  @BindView(R.id.commentsParent) View commentsParent;
  @BindView(R.id.commentsHeader) View commentsHeader;
  @BindView(R.id.commentsContainer) LinearLayout commentsContainer;

  @BindView(R.id.relatedParent) View relatedParent;
  @BindView(R.id.relatedHeader) View relatedHeader;
  @BindView(R.id.related) LinearLayout related;
  @BindView(R.id.relatedContainer) LinearLayout relatedContainer;

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

  private String showTitle;

  private String showOverview;

  private boolean inWatchlist;

  private int currentRating;

  private boolean calendarHidden;

  private LibraryType type;

  RecyclerViewManager seasonsManager;

  public static String getTag(long showId) {
    return TAG + "/" + showId + "/" + Ids.newId();
  }

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

  public long getShowId() {
    return showId;
  }

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    navigationListener = (NavigationListener) activity;
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
        navigationListener.onDisplaySeason(showId, seasonId, showTitle, seasonNumber, type);
      }
    }, type);
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

    rating.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        RatingDialog.newInstance(RatingDialog.Type.SHOW, showId, currentRating)
            .show(getFragmentManager(), DIALOG_RATING);
      }
    });

    castHeader.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        navigationListener.onDisplayCredits(ItemType.SHOW, showId, showTitle);
      }
    });

    commentsHeader.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        navigationListener.onDisplayComments(ItemType.SHOW, showId);
      }
    });

    relatedHeader.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        navigationListener.onDisplayRelatedShows(showId, showTitle);
      }
    });

    toWatchHolder = new EpisodeHolder(toWatch);
    toWatch.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        if (toWatchId != -1) navigationListener.onDisplayEpisode(toWatchId, showTitle);
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
            navigationListener.onDisplayEpisode(lastWatchedId, showTitle);
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
        if (toCollectId != -1) navigationListener.onDisplayEpisode(toCollectId, showTitle);
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
            navigationListener.onDisplayEpisode(lastCollectedId, showTitle);
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
    getLoaderManager().initLoader(LOADER_SHOW_CAST, null, castCallback);
    getLoaderManager().initLoader(LOADER_SHOW_WATCH, null, episodeWatchCallbacks);
    getLoaderManager().initLoader(LOADER_SHOW_COLLECT, null, episodeCollectCallbacks);
    getLoaderManager().initLoader(LOADER_SHOW_SEASONS, null, seasonsLoader);
    getLoaderManager().initLoader(LOADER_SHOW_USER_COMMENTS, null, userCommentsLoader);
    getLoaderManager().initLoader(LOADER_SHOW_COMMENTS, null, commentsLoader);
    getLoaderManager().initLoader(LOADER_RELATED, null, relatedLoader);
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
    final ShowStatus status = ShowStatus.fromValue(Cursors.getString(cursor, ShowColumns.STATUS));
    final String backdropUri = ImageUri.create(ImageUri.ITEM_SHOW, ImageType.BACKDROP, showId);
    setBackdrop(backdropUri, true);
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
    if (certification != null) {
      if (airTimeString != null) {
        airTimeString += ", " + certification;
      } else {
        airTimeString = certification;
      }
    }

    this.airTime.setText(airTimeString);

    String statusString = null;
    switch (status) {
      case ENDED:
        statusString = getString(R.string.show_status_ended);
        break;

      case RETURNING:
        statusString = getString(R.string.show_status_returning);
        break;

      case CANCELED:
        statusString = getString(R.string.show_status_canceled);
        break;

      case IN_PRODUCTION:
        statusString = getString(R.string.show_status_in_production);
        break;

      case PLANNED:
        statusString = getString(R.string.show_status_planned);
        break;
    }

    this.status.setText(statusString);

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

    final long lastActorsSync = Cursors.getLong(cursor, ShowColumns.LAST_CREDITS_SYNC);
    if (lastSync > lastActorsSync) {
      showScheduler.syncCredits(showId, null);
    }

    final long lastRelatedSync = Cursors.getLong(cursor, ShowColumns.LAST_RELATED_SYNC);
    if (lastSync > lastRelatedSync) {
      showScheduler.syncRelated(showId, null);
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

      genresTitle.setVisibility(View.VISIBLE);
      genres.setVisibility(View.VISIBLE);
      genres.setText(sb.toString());
    } else {
      genresTitle.setVisibility(View.GONE);
      genres.setVisibility(View.GONE);
    }
  }

  private void updateCastViews(Cursor c) {
    castContainer.removeAllViews();

    final int count = c.getCount();
    final int visibility = count > 0 ? View.VISIBLE : View.GONE;
    castParent.setVisibility(visibility);

    int index = 0;

    c.moveToPosition(-1);
    while (c.moveToNext() && index < 3) {
      View v =
          LayoutInflater.from(getActivity()).inflate(R.layout.item_person, castContainer, false);

      final long personId = Cursors.getLong(c, ShowCastColumns.PERSON_ID);
      final String headshotUrl = ImageUri.create(ImageUri.ITEM_PERSON, ImageType.PROFILE, personId);

      RemoteImageView headshot = (RemoteImageView) v.findViewById(R.id.headshot);
      headshot.addTransformation(new CircleTransformation());
      headshot.setImage(headshotUrl);

      TextView name = (TextView) v.findViewById(R.id.person_name);
      name.setText(Cursors.getString(c, PersonColumns.NAME));

      TextView character = (TextView) v.findViewById(R.id.person_job);
      character.setText(Cursors.getString(c, ShowCastColumns.CHARACTER));

      v.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          navigationListener.onDisplayPerson(personId);
        }
      });

      castContainer.addView(v);

      index++;
    }
  }

  private void updateRelatedView(Cursor related) {
    relatedContainer.removeAllViews();

    final int count = related.getCount();
    final int visibility = count > 0 ? View.VISIBLE : View.GONE;
    relatedParent.setVisibility(visibility);

    int index = 0;

    related.moveToPosition(-1);
    while (related.moveToNext() && index < 3) {
      View v = LayoutInflater.from(getActivity())
          .inflate(R.layout.item_related, this.relatedContainer, false);

      final long relatedShowId = Cursors.getLong(related, RelatedShowsColumns.RELATED_SHOW_ID);
      final String title = Cursors.getString(related, ShowColumns.TITLE);
      final String overview = Cursors.getString(related, ShowColumns.OVERVIEW);
      final float rating = Cursors.getFloat(related, ShowColumns.RATING);
      final int votes = Cursors.getInt(related, ShowColumns.VOTES);

      final String poster = ImageUri.create(ImageUri.ITEM_SHOW, ImageType.POSTER, relatedShowId);

      RemoteImageView posterView = (RemoteImageView) v.findViewById(R.id.related_poster);
      posterView.addTransformation(new CircleTransformation());
      posterView.setImage(poster);

      TextView titleView = (TextView) v.findViewById(R.id.related_title);
      titleView.setText(title);

      final String formattedRating = String.format(Locale.getDefault(), "%.1f", rating);

      String ratingText;
      if (votes >= 1000) {
        final float convertedVotes = votes / 1000.0f;
        final String formattedVotes = String.format(Locale.getDefault(), "%.1f", convertedVotes);
        ratingText = getString(R.string.related_rating_thousands, formattedRating, formattedVotes);
      } else {
        ratingText = getString(R.string.related_rating, formattedRating, votes);
      }

      TextView ratingView = (TextView) v.findViewById(R.id.related_rating);
      ratingView.setText(ratingText);

      v.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          navigationListener.onDisplayShow(relatedShowId, title, overview, LibraryType.WATCHED);
        }
      });
      relatedContainer.addView(v);

      index++;
    }
  }

  private void updateEpisodeWatchViews(Cursor cursor) {
    if (cursor.moveToFirst()) {
      toWatch.setVisibility(View.VISIBLE);

      toWatchId = Cursors.getLong(cursor, EpisodeColumns.ID);

      final long firstAired = DataHelper.getFirstAired(cursor);

      final int season = Cursors.getInt(cursor, EpisodeColumns.SEASON);
      final int episode = Cursors.getInt(cursor, EpisodeColumns.EPISODE);
      toWatchTitle = DataHelper.getEpisodeTitle(getContext(), cursor, season, episode);
      final String toWatchEpisodeText = getString(R.string.season_x_episode_y, season, episode);

      toWatchHolder.episodeTitle.setText(toWatchTitle);
      toWatchHolder.episodeEpisode.setText(toWatchEpisodeText);

      final String screenshotUri =
          ImageUri.create(ImageUri.ITEM_EPISODE, ImageType.STILL, toWatchId);
      toWatchHolder.episodeScreenshot.setImage(screenshotUri);

      String firstAiredString = DateUtils.millisToString(getActivity(), firstAired, false);

      final boolean watching = Cursors.getBoolean(cursor, EpisodeColumns.WATCHING);
      final boolean checkedIn = Cursors.getBoolean(cursor, EpisodeColumns.CHECKED_IN);

      toWatchHolder.episodeOverflow.removeItems();
      if (checkedIn) {
        toWatchHolder.episodeOverflow.addItem(R.id.action_checkin_cancel,
            R.string.action_checkin_cancel);
        firstAiredString = getResources().getString(R.string.show_watching);
      } else if (!watching) {
        toWatchHolder.episodeOverflow.addItem(R.id.action_checkin, R.string.action_checkin);
        toWatchHolder.episodeOverflow.addItem(R.id.action_watched, R.string.action_watched);
      }

      toWatchHolder.episodeAirTime.setText(firstAiredString);
    } else {
      toWatch.setVisibility(View.GONE);
      toWatchId = -1;
    }

    if (lastWatched != null) {
      if (cursor.moveToNext()) {
        lastWatched.setVisibility(View.VISIBLE);

        lastWatchedId = Cursors.getLong(cursor, EpisodeColumns.ID);

        final int season = Cursors.getInt(cursor, EpisodeColumns.SEASON);
        final int episode = Cursors.getInt(cursor, EpisodeColumns.EPISODE);
        final String title = DataHelper.getEpisodeTitle(getContext(), cursor, season, episode);

        lastWatchedHolder.episodeTitle.setText(title);

        final long firstAired = DataHelper.getFirstAired(cursor);
        final String firstAiredString = DateUtils.millisToString(getActivity(), firstAired, false);
        lastWatchedHolder.episodeAirTime.setText(firstAiredString);

        final String lastWatchedEpisodeText =
            getString(R.string.season_x_episode_y, season, episode);
        lastWatchedHolder.episodeEpisode.setText(lastWatchedEpisodeText);

        final String screenshotUri =
            ImageUri.create(ImageUri.ITEM_EPISODE, ImageType.STILL, lastWatchedId);
        lastWatchedHolder.episodeScreenshot.setImage(screenshotUri);
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

      toCollectId = Cursors.getLong(cursor, EpisodeColumns.ID);

      final int season = Cursors.getInt(cursor, EpisodeColumns.SEASON);
      final int episode = Cursors.getInt(cursor, EpisodeColumns.EPISODE);
      final String title = DataHelper.getEpisodeTitle(getContext(), cursor, season, episode);

      toCollectHolder.episodeTitle.setText(title);

      final long firstAired = DataHelper.getFirstAired(cursor);
      final String firstAiredString = DateUtils.millisToString(getActivity(), firstAired, false);
      toCollectHolder.episodeAirTime.setText(firstAiredString);

      final String toCollectEpisodeText = getString(R.string.season_x_episode_y, season, episode);
      toCollectHolder.episodeEpisode.setText(toCollectEpisodeText);

      final String screenshotUri =
          ImageUri.create(ImageUri.ITEM_EPISODE, ImageType.STILL, toCollectId);
      toCollectHolder.episodeScreenshot.setImage(screenshotUri);
    } else {
      toCollect.setVisibility(View.GONE);
      toCollectId = -1;
    }

    if (lastCollected != null) {
      if (cursor.moveToNext()) {
        lastCollected.setVisibility(View.VISIBLE);

        lastCollectedId = Cursors.getLong(cursor, EpisodeColumns.ID);

        final int season = Cursors.getInt(cursor, EpisodeColumns.SEASON);
        final int episode = Cursors.getInt(cursor, EpisodeColumns.EPISODE);
        final String title = DataHelper.getEpisodeTitle(getContext(), cursor, season, episode);

        lastCollectedHolder.episodeTitle.setText(title);

        final long firstAired = DataHelper.getFirstAired(cursor);
        final String firstAiredString = DateUtils.millisToString(getActivity(), firstAired, false);
        lastCollectedHolder.episodeAirTime.setText(firstAiredString);

        final String lastCollectedEpisodeText =
            getString(R.string.season_x_episode_y, season, episode);
        lastCollectedHolder.episodeEpisode.setText(lastCollectedEpisodeText);

        final String screenshotUri =
            ImageUri.create(ImageUri.ITEM_EPISODE, ImageType.STILL, lastCollectedId);
        lastCollectedHolder.episodeScreenshot.setImage(screenshotUri);
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
          return new SimpleCursorLoader(getActivity(), ShowGenres.fromShow(showId),
              GENRES_PROJECTION, null, null, ShowGenres.DEFAULT_SORT);
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> cursorLoader, SimpleCursor data) {
          updateGenreViews(data);
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> cursorLoader) {
        }
      };

  private static final String[] CAST_PROJECTION = new String[] {
      Tables.SHOW_CAST + "." + ShowCastColumns.ID,
      Tables.SHOW_CAST + "." + ShowCastColumns.CHARACTER,
      Tables.SHOW_CAST + "." + ShowCastColumns.PERSON_ID, Tables.PEOPLE + "." + PersonColumns.NAME,
  };

  private LoaderManager.LoaderCallbacks<SimpleCursor> castCallback =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          return new SimpleCursorLoader(getActivity(), ProviderSchematic.ShowCast.fromShow(showId),
              CAST_PROJECTION, Tables.PEOPLE + "." + PersonColumns.NEEDS_SYNC + "=0", null, null);
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
          updateCastViews(data);
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
          return new SimpleCursorLoader(getContext(), Comments.fromShow(showId),
              COMMENTS_PROJECTION, CommentColumns.IS_USER_COMMENT + "=1", null, null);
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
          return new SimpleCursorLoader(getContext(), Comments.fromShow(showId),
              COMMENTS_PROJECTION,
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

  private static final String[] RELATED_PROJECTION = new String[] {
      SqlColumn.table(Tables.SHOW_RELATED).column(RelatedShowsColumns.RELATED_SHOW_ID),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.TITLE),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.OVERVIEW),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.RATING),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.VOTES),
  };

  private LoaderManager.LoaderCallbacks<SimpleCursor> relatedLoader =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          return new SimpleCursorLoader(getContext(), RelatedShows.fromShow(showId),
              RELATED_PROJECTION, null, null, RelatedShowsColumns.RELATED_INDEX + " ASC LIMIT 3");
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
          updateRelatedView(data);
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
        }
      };
}
