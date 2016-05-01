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
import com.squareup.otto.Bus;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.UserColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.ProviderSchematic.Comments;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.ui.Loaders;
import net.simonvt.cathode.ui.NavigationClickListener;
import net.simonvt.cathode.ui.adapter.LinearCommentsAdapter;
import net.simonvt.cathode.ui.dialog.AddCommentDialog;
import net.simonvt.cathode.ui.dialog.CheckInDialog;
import net.simonvt.cathode.ui.dialog.CheckInDialog.Type;
import net.simonvt.cathode.ui.dialog.ListsDialog;
import net.simonvt.cathode.ui.dialog.RatingDialog;
import net.simonvt.cathode.util.Cursors;
import net.simonvt.cathode.util.DateUtils;
import net.simonvt.cathode.util.SqlColumn;
import net.simonvt.cathode.widget.CircularProgressIndicator;

public class EpisodeFragment extends AppBarFragment {

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

  private NavigationClickListener navigationListener;

  public static Bundle getArgs(long episodeId, String showTitle) {
    Bundle args = new Bundle();
    args.putLong(ARG_EPISODEID, episodeId);
    args.putString(ARG_SHOW_TITLE, showTitle);
    return args;
  }

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      navigationListener = (NavigationClickListener) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString() + " must implement NavigationClickListener");
    }
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    CathodeApp.inject(getActivity(), this);

    Bundle args = getArguments();
    episodeId = args.getLong(ARG_EPISODEID);
    showTitle = args.getString(ARG_SHOW_TITLE);
    setTitle(showTitle);

    getLoaderManager().initLoader(Loaders.EPISODE, null, episodeCallbacks);
    getLoaderManager().initLoader(Loaders.EPISODE_USER_COMMENTS, null, userCommentsLoader);
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

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);

    rating.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        RatingDialog.newInstance(RatingDialog.Type.EPISODE, episodeId, currentRating)
            .show(getFragmentManager(), DIALOG_RATING);
      }
    });

    commentsHeader.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        AddCommentDialog.newInstance(ItemType.EPISODE, episodeId)
            .show(getFragmentManager(), DIALOG_COMMENT_ADD);
      }
    });
  }

  @Override public void createMenu(Toolbar toolbar) {
    toolbar.inflateMenu(R.menu.fragment_episode);

    Menu menu = toolbar.getMenu();

    if (loaded) {
      if (checkedIn) {
        menu.add(0, R.id.action_checkin_cancel, 1, R.string.action_checkin_cancel)
            .setIcon(R.drawable.ic_action_cancel)
            .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM);
      } else if (!watching) {
        menu.add(0, R.id.action_checkin, 2, R.string.action_checkin)
            .setIcon(R.drawable.ic_action_checkin)
            .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM);
      }

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
        CheckInDialog.showDialogIfNecessary(getActivity(), Type.SHOW, episodeTitle, episodeId);
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
      season = cursor.getInt(cursor.getColumnIndex(EpisodeColumns.SEASON));

      episodeTitle = cursor.getString(cursor.getColumnIndex(EpisodeColumns.TITLE));
      if (TextUtils.isEmpty(episodeTitle)) {
        if (season == 0) {
          episodeTitle = getResources().getString(R.string.special_x, episodeNumber);
        } else {
          episodeTitle = getResources().getString(R.string.episode_x, episodeNumber);
        }
      }

      title.setText(episodeTitle);
      overview.setText(cursor.getString(cursor.getColumnIndex(EpisodeColumns.OVERVIEW)));
      final String screenshot = cursor.getString(cursor.getColumnIndex(EpisodeColumns.SCREENSHOT));
      setBackdrop(screenshot, true);
      firstAired.setText(DateUtils.millisToString(getActivity(),
          cursor.getLong(cursor.getColumnIndex(EpisodeColumns.FIRST_AIRED)), true));

      watched = cursor.getInt(cursor.getColumnIndex(EpisodeColumns.WATCHED)) == 1;
      collected = cursor.getInt(cursor.getColumnIndex(EpisodeColumns.IN_COLLECTION)) == 1;
      inWatchlist = cursor.getInt(cursor.getColumnIndex(EpisodeColumns.IN_WATCHLIST)) == 1;
      watching = cursor.getInt(cursor.getColumnIndex(EpisodeColumns.WATCHING)) == 1;
      checkedIn = cursor.getInt(cursor.getColumnIndex(EpisodeColumns.CHECKED_IN)) == 1;

      watchedView.setVisibility(watched ? View.VISIBLE : View.GONE);
      inCollectionView.setVisibility(collected ? View.VISIBLE : View.GONE);
      inWatchlistView.setVisibility(inWatchlist ? View.VISIBLE : View.GONE);

      currentRating = cursor.getInt(cursor.getColumnIndex(EpisodeColumns.USER_RATING));
      final float ratingAll = cursor.getFloat(cursor.getColumnIndex(EpisodeColumns.RATING));
      rating.setValue(ratingAll);

      invalidateMenu();
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
      EpisodeColumns.EPISODE,
  };

  private LoaderManager.LoaderCallbacks<SimpleCursor> episodeCallbacks =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          SimpleCursorLoader cl =
              new SimpleCursorLoader(getActivity(), Episodes.withId(episodeId), EPISODE_PROJECTION,
                  null, null, null);
          cl.setUpdateThrottle(2 * android.text.format.DateUtils.SECOND_IN_MILLIS);
          return cl;
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
          SimpleCursorLoader loader =
              new SimpleCursorLoader(getContext(), Comments.fromEpisode(episodeId),
                  COMMENTS_PROJECTION, CommentColumns.IS_USER_COMMENT + "=1", null, null);
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
}
