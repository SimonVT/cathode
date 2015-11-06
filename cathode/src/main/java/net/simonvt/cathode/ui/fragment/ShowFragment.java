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
import butterknife.Bind;
import butterknife.ButterKnife;
import com.squareup.otto.Bus;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.database.SimpleMergeCursor;
import net.simonvt.cathode.provider.CollectLoader;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
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
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.Loaders;
import net.simonvt.cathode.ui.NavigationListener;
import net.simonvt.cathode.ui.adapter.LinearCommentsAdapter;
import net.simonvt.cathode.ui.adapter.SeasonsAdapter;
import net.simonvt.cathode.ui.dialog.CheckInDialog;
import net.simonvt.cathode.ui.dialog.CheckInDialog.Type;
import net.simonvt.cathode.ui.dialog.ListsDialog;
import net.simonvt.cathode.ui.dialog.RatingDialog;
import net.simonvt.cathode.ui.listener.SeasonClickListener;
import net.simonvt.cathode.util.DateUtils;
import net.simonvt.cathode.util.SqlColumn;
import net.simonvt.cathode.widget.AppBarRelativeLayout;
import net.simonvt.cathode.widget.CircleTransformation;
import net.simonvt.cathode.widget.CircularProgressIndicator;
import net.simonvt.cathode.widget.HiddenPaneLayout;
import net.simonvt.cathode.widget.OverflowView;
import net.simonvt.cathode.widget.RecyclerViewManager;
import net.simonvt.cathode.widget.RemoteImageView;
import timber.log.Timber;

public class ShowFragment extends BaseFragment {

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

  private static final String[] SHOW_PROJECTION = new String[] {
      ShowColumns.TITLE, ShowColumns.YEAR, ShowColumns.AIR_TIME, ShowColumns.AIR_DAY,
      ShowColumns.NETWORK, ShowColumns.CERTIFICATION, ShowColumns.POSTER, ShowColumns.FANART,
      ShowColumns.USER_RATING, ShowColumns.RATING, ShowColumns.OVERVIEW, ShowColumns.IN_WATCHLIST,
      ShowColumns.IN_COLLECTION_COUNT, ShowColumns.WATCHED_COUNT,
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

  @Bind(R.id.hiddenPaneLayout) HiddenPaneLayout hiddenPaneLayout;

  @Bind(R.id.seasons) RecyclerView seasons;
  @Bind(R.id.seasonsEmpty) View seasonsEmpty;
  private SeasonsAdapter seasonsAdapter;
  private Cursor seasonsCursor;

  @Bind(R.id.appBarLayout) AppBarRelativeLayout appBarLayout;

  @Bind(R.id.rating) CircularProgressIndicator rating;
  @Bind(R.id.airtime) TextView airTime;
  @Bind(R.id.certification) TextView certification;
  @Bind(R.id.backdrop) RemoteImageView backdrop;
  @Bind(R.id.overview) TextView overview;
  @Bind(R.id.isWatched) TextView watched;
  @Bind(R.id.inCollection) TextView collection;
  @Bind(R.id.inWatchlist) TextView watchlist;

  @Bind(R.id.actorsParent) View actorsParent;
  @Bind(R.id.actorsHeader) View actorsHeader;
  @Bind(R.id.actors) LinearLayout actors;
  @Bind(R.id.peopleContainer) LinearLayout peopleContainer;

  @Bind(R.id.commentsParent) View commentsParent;
  @Bind(R.id.commentsHeader) View commentsHeader;
  @Bind(R.id.commentsContainer) LinearLayout commentsContainer;

  private Cursor userComments;
  private Cursor comments;

  @Bind(R.id.episodes) LinearLayout episodes;

  @Bind(R.id.toWatch) View toWatch;
  private EpisodeHolder toWatchHolder;
  private long toWatchId = -1;
  private String toWatchTitle;

  @Bind(R.id.lastWatched) @Nullable View lastWatched;
  private EpisodeHolder lastWatchedHolder;
  private long lastWatchedId = -1;

  @Bind(R.id.toCollect) View toCollect;
  private EpisodeHolder toCollectHolder;
  private long toCollectId = -1;

  @Bind(R.id.lastCollected) @Nullable View lastCollected;
  private EpisodeHolder lastCollectedHolder;
  private long lastCollectedId = -1;

  static class EpisodeHolder {

    @Bind(R.id.episodeScreenshot) RemoteImageView episodeScreenshot;
    @Bind(R.id.episodeTitle) TextView episodeTitle;
    @Bind(R.id.episodeAirTime) TextView episodeAirTime;
    @Bind(R.id.episodeEpisode) TextView episodeEpisode;
    @Bind(R.id.episodeOverflow) OverflowView episodeOverflow;

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

  private LibraryType type;

  RecyclerViewManager seasonsManager;

  public static Bundle getArgs(long showId, String title, String overview, LibraryType type) {
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

    seasonsAdapter = new SeasonsAdapter(getActivity(), new SeasonClickListener() {
      @Override public void onSeasonClick(View view, int position, long id) {
        navigationCallbacks.onDisplaySeason(showId, id, showTitle, seasonsCursor.getInt(
            seasonsCursor.getColumnIndex(DatabaseContract.SeasonColumns.SEASON)), type);
      }
    }, type);
  }

  private void updateTitle() {
    appBarLayout.setTitle(getTitle());
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
    updateTitle();
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

    getLoaderManager().initLoader(Loaders.SHOW, null, showCallbacks);
    getLoaderManager().initLoader(Loaders.SHOW_GENRES, null, genreCallbacks);
    getLoaderManager().initLoader(Loaders.SHOW_ACTORS, null, charactersCallback);
    getLoaderManager().initLoader(Loaders.SHOW_WATCH, null, episodeWatchCallbacks);
    getLoaderManager().initLoader(Loaders.SHOW_COLLECT, null, episodeCollectCallbacks);
    getLoaderManager().initLoader(Loaders.SHOW_SEASONS, null, seasonsLoader);
    getLoaderManager().initLoader(Loaders.SHOW_USER_COMMENTS, null, userCommentsLoader);
    getLoaderManager().initLoader(Loaders.SHOW_COMMENTS, null, commentsLoader);
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
    }

    return super.onMenuItemClick(item);
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
  }

  private void updateShowView(final Cursor cursor) {
    if (cursor == null || !cursor.moveToFirst()) return;

    String title = cursor.getString(cursor.getColumnIndex(ShowColumns.TITLE));
    if (!TextUtils.equals(title, showTitle)) {
      showTitle = title;
      updateTitle();
    }
    final String airTime = cursor.getString(cursor.getColumnIndex(ShowColumns.AIR_TIME));
    final String airDay = cursor.getString(cursor.getColumnIndex(ShowColumns.AIR_DAY));
    final String network = cursor.getString(cursor.getColumnIndex(ShowColumns.NETWORK));
    final String certification = cursor.getString(cursor.getColumnIndex(ShowColumns.CERTIFICATION));
    final String fanartUrl = cursor.getString(cursor.getColumnIndex(ShowColumns.FANART));
    if (fanartUrl != null) {
      backdrop.setImage(fanartUrl, true);
    }
    showOverview = cursor.getString(cursor.getColumnIndex(ShowColumns.OVERVIEW));
    inWatchlist = cursor.getInt(cursor.getColumnIndex(ShowColumns.IN_WATCHLIST)) == 1;
    final int inCollectionCount =
        cursor.getInt(cursor.getColumnIndex(ShowColumns.IN_COLLECTION_COUNT));
    final int watchedCount = cursor.getInt(cursor.getColumnIndex(ShowColumns.WATCHED_COUNT));

    currentRating = cursor.getInt(cursor.getColumnIndex(ShowColumns.USER_RATING));
    final float ratingAll = cursor.getFloat(cursor.getColumnIndex(ShowColumns.RATING));
    rating.setValue(ratingAll);

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
      headshot.setImage(c.getString(c.getColumnIndex(PersonColumns.HEADSHOT)));

      TextView name = (TextView) v.findViewById(R.id.person_name);
      name.setText(c.getString(c.getColumnIndex(PersonColumns.NAME)));

      TextView character = (TextView) v.findViewById(R.id.person_job);
      character.setText(c.getString(c.getColumnIndex(ShowCharacterColumns.CHARACTER)));

      peopleContainer.addView(v);

      index++;
    }
  }

  private void updateEpisodeWatchViews(Cursor cursor) {
    if (cursor.moveToFirst()) {
      toWatch.setVisibility(View.VISIBLE);

      toWatchId = cursor.getLong(cursor.getColumnIndex(ShowColumns.ID));
      toWatchTitle = cursor.getString(cursor.getColumnIndex(EpisodeColumns.TITLE));

      toWatchHolder.episodeTitle.setText(toWatchTitle);

      final long airTime = cursor.getLong(cursor.getColumnIndex(EpisodeColumns.FIRST_AIRED));

      final int season = cursor.getInt(cursor.getColumnIndex(EpisodeColumns.SEASON));
      final int episode = cursor.getInt(cursor.getColumnIndex(EpisodeColumns.EPISODE));
      toWatchHolder.episodeEpisode.setText("S" + season + "E" + episode);

      final String screenshotUrl =
          cursor.getString(cursor.getColumnIndex(EpisodeColumns.SCREENSHOT));
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

        final String screenshotUrl =
            cursor.getString(cursor.getColumnIndex(EpisodeColumns.SCREENSHOT));
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

      toCollectId = cursor.getLong(cursor.getColumnIndex(ShowColumns.ID));

      toCollectHolder.episodeTitle.setText(
          cursor.getString(cursor.getColumnIndex(EpisodeColumns.TITLE)));

      final long airTime = cursor.getLong(cursor.getColumnIndex(EpisodeColumns.FIRST_AIRED));
      final String airTimeStr = DateUtils.millisToString(getActivity(), airTime, false);
      toCollectHolder.episodeAirTime.setText(airTimeStr);

      final int season = cursor.getInt(cursor.getColumnIndex(EpisodeColumns.SEASON));
      final int episode = cursor.getInt(cursor.getColumnIndex(EpisodeColumns.EPISODE));
      toCollectHolder.episodeEpisode.setText("S" + season + "E" + episode);

      final String screenshotUrl =
          cursor.getString(cursor.getColumnIndex(EpisodeColumns.SCREENSHOT));
      toCollectHolder.episodeScreenshot.setImage(screenshotUrl);
    } else {
      toCollect.setVisibility(View.GONE);
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

        final String screenshotUrl =
            cursor.getString(cursor.getColumnIndex(EpisodeColumns.SCREENSHOT));
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
          SimpleCursorLoader cl =
              new SimpleCursorLoader(getActivity(), Shows.withId(showId), SHOW_PROJECTION, null,
                  null, null);
          cl.setUpdateThrottle(2 * android.text.format.DateUtils.SECOND_IN_MILLIS);
          return cl;
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
          SimpleCursorLoader cl =
              new SimpleCursorLoader(getActivity(), ShowGenres.fromShow(showId), GENRES_PROJECTION,
                  null, null, ShowGenres.DEFAULT_SORT);
          cl.setUpdateThrottle(2 * android.text.format.DateUtils.SECOND_IN_MILLIS);
          return cl;
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
          SimpleCursorLoader cl = new SimpleCursorLoader(getActivity(),
              ProviderSchematic.ShowCharacters.fromShow(showId), CHARACTERS_PROJECTION,
              Tables.PEOPLE + "." + PersonColumns.NEEDS_SYNC + "=0", null, null);
          cl.setUpdateThrottle(2 * android.text.format.DateUtils.SECOND_IN_MILLIS);
          return cl;
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
          SimpleCursorLoader cl = new SimpleCursorLoader(getActivity(), Seasons.fromShow(showId),
              SeasonsAdapter.PROJECTION, null, null, Seasons.DEFAULT_SORT);
          cl.setUpdateThrottle(2 * android.text.format.DateUtils.SECOND_IN_MILLIS);
          return cl;
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
          SimpleCursorLoader loader =
              new SimpleCursorLoader(getContext(), Comments.fromShow(showId), COMMENTS_PROJECTION,
                  CommentColumns.IS_USER_COMMENT + "=1", null, null);
          loader.setUpdateThrottle(2 * android.text.format.DateUtils.SECOND_IN_MILLIS);
          return loader;
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
          SimpleCursorLoader loader =
              new SimpleCursorLoader(getContext(), Comments.fromShow(showId), COMMENTS_PROJECTION,
                  CommentColumns.IS_USER_COMMENT + "=0 AND " + CommentColumns.SPOILER + "=0", null,
                  CommentColumns.LIKES + " DESC LIMIT 3");
          loader.setUpdateThrottle(2 * android.text.format.DateUtils.SECOND_IN_MILLIS);
          return loader;
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
          comments = data;
          updateComments();
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
        }
      };
}
