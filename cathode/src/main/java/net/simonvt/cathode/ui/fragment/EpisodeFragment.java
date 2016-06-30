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
import android.support.v4.content.Loader;
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
import butterknife.OnClick;
import com.squareup.otto.Bus;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.UserColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.ProviderSchematic.Comments;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.settings.TraktTimestamps;
import net.simonvt.cathode.ui.NavigationListener;
import net.simonvt.cathode.ui.adapter.LinearCommentsAdapter;
import net.simonvt.cathode.ui.dialog.CheckInDialog;
import net.simonvt.cathode.ui.dialog.CheckInDialog.Type;
import net.simonvt.cathode.ui.dialog.ListsDialog;
import net.simonvt.cathode.ui.dialog.RatingDialog;
import net.simonvt.cathode.util.DateUtils;
import net.simonvt.cathode.util.SqlColumn;
import net.simonvt.cathode.widget.CheckInDrawable;
import net.simonvt.cathode.widget.CircularProgressIndicator;
import net.simonvt.schematic.Cursors;

public class EpisodeFragment extends RefreshableAppBarFragment {

  public static final String TAG = "net.simonvt.cathode.ui.fragment.EpisodeFragment";

  private static final int LOADER_EPISODE = 1;
  private static final int LOADER_EPISODE_USER_COMMENTS = 2;

  private static final String ARG_EPISODEID =
      "net.simonvt.cathode.ui.fragment.EpisodeFragment.episodeId";
  private static final String ARG_SHOW_TITLE =
      "net.simonvt.cathode.ui.fragment.EpisodeFragment.showTitle";

  private static final String DIALOG_RATING =
      "net.simonvt.cathode.ui.fragment.EpisodeFragment.ratingDialog";
  private static final String DIALOG_LISTS_ADD =
      "net.simonvt.cathode.ui.fragment.EpisodeFragment.listsAddDialog";
  private static final String DIALOG_COMMENT_ADD =
      "net.simonvt.cathode.ui.fragment.EpisodeFragment.addCommentDialog";
  private static final String DIALOG_COMMENT_UPDATE =
      "net.simonvt.cathode.ui.fragment.EpisodeFragment.updateCommentDialog";

  @Inject ShowTaskScheduler showScheduler;
  @Inject EpisodeTaskScheduler episodeScheduler;
  @Inject Bus bus;

  @BindView(R.id.title) TextView title;
  @BindView(R.id.overview) TextView overview;
  @BindView(R.id.firstAired) TextView firstAired;

  @BindView(R.id.rating) CircularProgressIndicator rating;

  @BindView(R.id.isWatched) View watchedView;
  @BindView(R.id.inCollection) View inCollectionView;
  @BindView(R.id.inWatchlist) View inWatchlistView;

  @BindView(R.id.commentsParent) View commentsParent;
  @BindView(R.id.commentsHeader) View commentsHeader;
  @BindView(R.id.commentsContainer) LinearLayout commentsContainer;

  private Cursor userComments;

  private long episodeId;

  private String episodeTitle;

  private String showTitle;
  private int season = -1;

  private int currentRating;

  private boolean loaded;

  private boolean watched;

  private boolean collected;

  private boolean inWatchlist;

  private boolean watching;

  private boolean checkedIn;

  private NavigationListener navigationListener;

  private MenuItem checkInItem;
  private CheckInDrawable checkInDrawable;

  public static Bundle getArgs(long episodeId, String showTitle) {
    Bundle args = new Bundle();
    args.putLong(ARG_EPISODEID, episodeId);
    args.putString(ARG_SHOW_TITLE, showTitle);
    return args;
  }

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    navigationListener = (NavigationListener) activity;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    CathodeApp.inject(getActivity(), this);

    Bundle args = getArguments();
    episodeId = args.getLong(ARG_EPISODEID);
    showTitle = args.getString(ARG_SHOW_TITLE);
    setTitle(showTitle);

    getLoaderManager().initLoader(LOADER_EPISODE, null, episodeCallbacks);
    getLoaderManager().initLoader(LOADER_EPISODE_USER_COMMENTS, null, userCommentsLoader);
  }

  public long getEpisodeId() {
    return episodeId;
  }

  public String getTitle() {
    return showTitle;
  }

  public String getSubtitle() {
    return season == -1 ? null : getString(R.string.season_x, season);
  }

  @Override public boolean onBackPressed() {
    return false;
  }

  @Override public View createView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.fragment_episode, container, false);
  }

  private Job.OnDoneListener onDoneListener = new Job.OnDoneListener() {
    @Override public void onDone(Job job) {
      setRefreshing(false);
    }
  };

  @Override public void onRefresh() {
    episodeScheduler.sync(episodeId, onDoneListener);
  }

  @OnClick(R.id.rating) void onRatingClick() {
    RatingDialog.newInstance(RatingDialog.Type.EPISODE, episodeId, currentRating)
        .show(getFragmentManager(), DIALOG_RATING);
  }

  @OnClick(R.id.commentsHeader) void onShowComments() {
    navigationListener.onDisplayComments(ItemType.EPISODE, episodeId);
  }

  @Override public void onDestroyView() {
    checkInItem = null;
    super.onDestroyView();
  }

  @Override public void createMenu(Toolbar toolbar) {
    toolbar.inflateMenu(R.menu.fragment_episode);
    Menu menu = toolbar.getMenu();

    if (loaded) {
      if (checkInDrawable == null) {
        checkInDrawable = new CheckInDrawable(toolbar.getContext());
        checkInDrawable.setWatching(watching || checkedIn);
        checkInDrawable.setId(episodeId);
      }
      if (checkInItem == null) {
        if (watching || checkedIn) {
          checkInItem = menu.add(0, R.id.action_checkin, 1, R.string.action_checkin_cancel);
        } else {
          checkInItem = menu.add(0, R.id.action_checkin, 1, R.string.action_checkin);
        }

        checkInItem.setIcon(checkInDrawable).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
      } else {
        if (watching || checkedIn) {
          checkInItem.setTitle(R.string.action_checkin_cancel);
        } else {
          checkInItem.setTitle(R.string.action_checkin);
        }
      }

      if (watching) {
        checkInItem.setEnabled(false);
      } else {
        checkInItem.setEnabled(true);
      }

      menu.removeItem(R.id.action_unwatched);
      menu.removeItem(R.id.action_watched);
      menu.removeItem(R.id.action_watchlist_remove);
      menu.removeItem(R.id.action_watchlist_add);
      menu.removeItem(R.id.action_collection_remove);
      menu.removeItem(R.id.action_collection_add);

      if (watched) {
        menu.add(0, R.id.action_unwatched, 3, R.string.action_unwatched);
      } else {
        menu.add(0, R.id.action_watched, 4, R.string.action_watched);
        if (inWatchlist) {
          menu.add(0, R.id.action_watchlist_remove, 5, R.string.action_watchlist_remove);
        } else {
          menu.add(0, R.id.action_watchlist_add, 6, R.string.action_watchlist_add);
        }
      }

      if (collected) {
        menu.add(0, R.id.action_collection_remove, 7, R.string.action_collection_remove);
      } else {
        menu.add(0, R.id.action_collection_add, 8, R.string.action_collection_add);
      }
    }
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_watched:
        episodeScheduler.setWatched(episodeId, true);
        return true;

      case R.id.action_unwatched:
        episodeScheduler.setWatched(episodeId, false);
        return true;

      case R.id.action_checkin:
        if (!watching) {
          if (checkedIn) {
            showScheduler.cancelCheckin();
            if (checkInDrawable != null) {
              checkInDrawable.setWatching(false);
            }
          } else {
            if (!CheckInDialog.showDialogIfNecessary(getActivity(), Type.SHOW, episodeTitle,
                episodeId)) {
              checkInDrawable.setWatching(true);
            }
          }
        }
        return true;

      case R.id.action_checkin_cancel:
        showScheduler.cancelCheckin();
        return true;

      case R.id.action_collection_add:
        episodeScheduler.setIsInCollection(episodeId, true);
        return true;

      case R.id.action_collection_remove:
        episodeScheduler.setIsInCollection(episodeId, false);
        return true;

      case R.id.action_watchlist_add:
        episodeScheduler.setIsInWatchlist(episodeId, true);
        return true;

      case R.id.action_watchlist_remove:
        episodeScheduler.setIsInWatchlist(episodeId, false);
        return true;

      case R.id.menu_lists_add:
        ListsDialog.newInstance(DatabaseContract.ItemType.EPISODE, episodeId)
            .show(getFragmentManager(), DIALOG_LISTS_ADD);
        return true;

      default:
        return super.onMenuItemClick(item);
    }
  }

  private void updateEpisodeViews(final Cursor cursor) {
    if (cursor.moveToFirst()) {
      loaded = true;

      final int episodeNumber = Cursors.getInt(cursor, EpisodeColumns.EPISODE);
      season = Cursors.getInt(cursor, EpisodeColumns.SEASON);

      episodeTitle = Cursors.getString(cursor, EpisodeColumns.TITLE);
      if (TextUtils.isEmpty(episodeTitle)) {
        if (season == 0) {
          episodeTitle = getResources().getString(R.string.special_x, episodeNumber);
        } else {
          episodeTitle = getResources().getString(R.string.episode_x, episodeNumber);
        }
      }

      title.setText(episodeTitle);
      overview.setText(Cursors.getString(cursor, EpisodeColumns.OVERVIEW));
      final String screenshot = Cursors.getString(cursor, EpisodeColumns.SCREENSHOT);
      setBackdrop(screenshot, true);
      firstAired.setText(DateUtils.millisToString(getActivity(),
          Cursors.getLong(cursor, EpisodeColumns.FIRST_AIRED), true));

      watched = Cursors.getBoolean(cursor, EpisodeColumns.WATCHED);
      collected = Cursors.getBoolean(cursor, EpisodeColumns.IN_COLLECTION);
      inWatchlist = Cursors.getBoolean(cursor, EpisodeColumns.IN_WATCHLIST);
      watching = Cursors.getBoolean(cursor, EpisodeColumns.WATCHING);
      checkedIn = Cursors.getBoolean(cursor, EpisodeColumns.CHECKED_IN);

      if (checkInDrawable != null) {
        checkInDrawable.setWatching(watching || checkedIn);
      }

      watchedView.setVisibility(watched ? View.VISIBLE : View.GONE);
      inCollectionView.setVisibility(collected ? View.VISIBLE : View.GONE);
      inWatchlistView.setVisibility(inWatchlist ? View.VISIBLE : View.GONE);

      currentRating = Cursors.getInt(cursor, EpisodeColumns.USER_RATING);
      final float ratingAll = Cursors.getFloat(cursor, EpisodeColumns.RATING);
      rating.setValue(ratingAll);

      final long lastCommentSync = Cursors.getLong(cursor, EpisodeColumns.LAST_COMMENT_SYNC);
      if (TraktTimestamps.shouldSyncComments(lastCommentSync)) {
        episodeScheduler.syncComments(episodeId);
      }

      if (toolbar != null) {
        createMenu(toolbar);
      }
    }
  }

  private void updateComments() {
    LinearCommentsAdapter.updateComments(getContext(), commentsContainer, userComments, null);
    commentsParent.setVisibility(View.VISIBLE);
  }

  private static final String[] EPISODE_PROJECTION = new String[] {
      EpisodeColumns.TITLE, EpisodeColumns.SCREENSHOT, EpisodeColumns.OVERVIEW,
      EpisodeColumns.FIRST_AIRED, EpisodeColumns.WATCHED, EpisodeColumns.IN_COLLECTION,
      EpisodeColumns.IN_WATCHLIST, EpisodeColumns.WATCHING, EpisodeColumns.CHECKED_IN,
      EpisodeColumns.USER_RATING, EpisodeColumns.RATING, EpisodeColumns.SEASON,
      EpisodeColumns.EPISODE, EpisodeColumns.LAST_COMMENT_SYNC,
  };

  private LoaderManager.LoaderCallbacks<SimpleCursor> episodeCallbacks =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          return new SimpleCursorLoader(getActivity(), Episodes.withId(episodeId),
              EPISODE_PROJECTION, null, null, null);
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> cursorLoader, SimpleCursor data) {
          updateEpisodeViews(data);
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> cursorLoader) {
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
          return new SimpleCursorLoader(getContext(), Comments.fromEpisode(episodeId),
              COMMENTS_PROJECTION,
              CommentColumns.IS_USER_COMMENT + "=0 AND " + CommentColumns.SPOILER + "=0", null,
              CommentColumns.LIKES + " DESC LIMIT 3");
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
          userComments = data;
          updateComments();
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
        }
      };
}
