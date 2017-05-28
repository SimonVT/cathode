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
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.OnClick;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.api.util.TraktUtils;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
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
import net.simonvt.cathode.ui.comments.LinearCommentsAdapter;
import net.simonvt.cathode.ui.dialog.CheckInDialog;
import net.simonvt.cathode.ui.dialog.CheckInDialog.Type;
import net.simonvt.cathode.ui.dialog.RatingDialog;
import net.simonvt.cathode.ui.fragment.RefreshableAppBarFragment;
import net.simonvt.cathode.ui.history.AddToHistoryDialog;
import net.simonvt.cathode.ui.lists.ListsDialog;
import net.simonvt.cathode.util.DataHelper;
import net.simonvt.cathode.util.DateUtils;
import net.simonvt.cathode.util.Ids;
import net.simonvt.cathode.util.Intents;
import net.simonvt.cathode.util.SqlColumn;
import net.simonvt.cathode.util.guava.Preconditions;
import net.simonvt.cathode.widget.CheckInDrawable;
import net.simonvt.cathode.widget.CircularProgressIndicator;
import net.simonvt.schematic.Cursors;

public class EpisodeFragment extends RefreshableAppBarFragment {

  private static final String TAG = "net.simonvt.cathode.ui.show.EpisodeFragment";

  private static final int LOADER_EPISODE = 1;
  private static final int LOADER_EPISODE_USER_COMMENTS = 2;

  private static final String ARG_EPISODEID =
      "net.simonvt.cathode.ui.show.EpisodeFragment.episodeId";
  private static final String ARG_SHOW_TITLE =
      "net.simonvt.cathode.ui.show.EpisodeFragment.showTitle";

  private static final String DIALOG_RATING =
      "net.simonvt.cathode.ui.show.EpisodeFragment.ratingDialog";
  private static final String DIALOG_LISTS_ADD =
      "net.simonvt.cathode.ui.show.EpisodeFragment.listsAddDialog";
  private static final String DIALOG_COMMENT_ADD =
      "net.simonvt.cathode.ui.show.EpisodeFragment.addCommentDialog";
  private static final String DIALOG_COMMENT_UPDATE =
      "net.simonvt.cathode.ui.show.EpisodeFragment.updateCommentDialog";

  @Inject ShowTaskScheduler showScheduler;
  @Inject EpisodeTaskScheduler episodeScheduler;

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

  @BindView(R.id.viewOnTrakt) View viewOnTrakt;

  private Cursor userComments;

  private long episodeId;

  private String episodeTitle;

  private String showTitle;
  private long showId = -1L;
  private int season = -1;
  private long seasonId = -1L;

  private int currentRating;

  private boolean loaded;

  private boolean watched;

  private boolean collected;

  private boolean inWatchlist;

  private boolean watching;

  private boolean checkedIn;

  private NavigationListener navigationListener;

  private CheckInDrawable checkInDrawable;

  public static String getTag(long episodeId) {
    return TAG + "/" + episodeId + "/" + Ids.newId();
  }

  public static Bundle getArgs(long episodeId, String showTitle) {
    Preconditions.checkArgument(episodeId >= 0, "episodeId must be >= 0, was " + episodeId);

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

  @Override public boolean onBackPressed() {
    return false;
  }

  @Override protected void onHomeClicked() {
    if (showId >= 0L && seasonId >= 0L) {
      navigationListener.upFromEpisode(showId, showTitle, seasonId);
    } else {
      navigationListener.onHomeClicked();
    }
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

  @Override public void createMenu(Toolbar toolbar) {
    if (loaded) {
      Menu menu = toolbar.getMenu();

      if (checkInDrawable == null) {
        checkInDrawable = new CheckInDrawable(toolbar.getContext());
        checkInDrawable.setWatching(watching || checkedIn);
        checkInDrawable.setId(episodeId);
      }

      MenuItem checkInItem;

      if (watching || checkedIn) {
        checkInItem = menu.add(0, R.id.action_checkin, 1, R.string.action_checkin_cancel);
      } else {
        checkInItem = menu.add(0, R.id.action_checkin, 1, R.string.action_checkin);
      }

      checkInItem.setIcon(checkInDrawable).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

      if (watching) {
        checkInItem.setEnabled(false);
      } else {
        checkInItem.setEnabled(true);
      }

      menu.add(0, R.id.action_history_add, 3, R.string.action_history_add);

      if (watched) {
        menu.add(0, R.id.action_history_remove, 4, R.string.action_history_remove);
      } else {
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

      menu.add(0, R.id.action_list_add, 9, R.string.action_list_add);
    }
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_history_add:
        AddToHistoryDialog.newInstance(AddToHistoryDialog.Type.EPISODE, episodeId, episodeTitle)
            .show(getFragmentManager(), AddToHistoryDialog.TAG);
        return true;

      case R.id.action_history_remove:
        episodeScheduler.removeFromHistory(episodeId);
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

      case R.id.action_list_add:
        ListsDialog.newInstance(DatabaseContract.ItemType.EPISODE, episodeId)
            .show(getFragmentManager(), DIALOG_LISTS_ADD);
        return true;

      default:
        return super.onMenuItemClick(item);
    }
  }

  private void updateEpisodeViews(final Cursor cursor) {
    if (cursor == null || !cursor.moveToFirst()) return;

    loaded = true;

    final long traktId = Cursors.getLong(cursor, EpisodeColumns.TRAKT_ID);
    showId = Cursors.getLong(cursor, EpisodeColumns.SHOW_ID);
    seasonId = Cursors.getLong(cursor, EpisodeColumns.SEASON_ID);

    season = Cursors.getInt(cursor, EpisodeColumns.SEASON);
    final int episode = Cursors.getInt(cursor, EpisodeColumns.EPISODE);
    episodeTitle = DataHelper.getEpisodeTitle(getContext(), cursor, season, episode, watched);

    title.setText(episodeTitle);

    final String overviewText = DataHelper.getEpisodeOverview(getContext(), cursor, watched);
    overview.setText(overviewText);

    final String screenshotUri = ImageUri.create(ImageUri.ITEM_EPISODE, ImageType.STILL, episodeId);

    setBackdrop(screenshotUri, true);
    firstAired.setText(
        DateUtils.millisToString(getActivity(), DataHelper.getFirstAired(cursor), true));

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

    viewOnTrakt.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        Intents.openUrl(getContext(), TraktUtils.getTraktEpisodeUrl(traktId));
      }
    });

    final long lastCommentSync = Cursors.getLong(cursor, EpisodeColumns.LAST_COMMENT_SYNC);
    if (TraktTimestamps.shouldSyncComments(lastCommentSync)) {
      episodeScheduler.syncComments(episodeId);
    }

    invalidateMenu();
  }

  private void updateComments() {
    LinearCommentsAdapter.updateComments(getContext(), commentsContainer, userComments, null);
    commentsParent.setVisibility(View.VISIBLE);
  }

  private static final String[] EPISODE_PROJECTION = new String[] {
      EpisodeColumns.TRAKT_ID, EpisodeColumns.TITLE, EpisodeColumns.OVERVIEW,
      EpisodeColumns.FIRST_AIRED, EpisodeColumns.WATCHED, EpisodeColumns.IN_COLLECTION,
      EpisodeColumns.IN_WATCHLIST, EpisodeColumns.WATCHING, EpisodeColumns.CHECKED_IN,
      EpisodeColumns.USER_RATING, EpisodeColumns.RATING, EpisodeColumns.SEASON,
      EpisodeColumns.EPISODE, EpisodeColumns.LAST_COMMENT_SYNC, EpisodeColumns.SHOW_ID,
      EpisodeColumns.SEASON_ID,
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
