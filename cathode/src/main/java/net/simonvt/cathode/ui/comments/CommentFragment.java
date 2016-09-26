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

package net.simonvt.cathode.ui.comments;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.database.SimpleMergeCursor;
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.ProviderSchematic.Comments;
import net.simonvt.cathode.ui.fragment.ToolbarGridFragment;

public class CommentFragment extends ToolbarGridFragment<CommentsAdapter.ViewHolder> {

  public static final String TAG = "net.simonvt.cathode.ui.comments.CommentFragment";

  private static final String ARG_COMMENT_ID =
      "net.simonvt.cathode.ui.comments.CommentFragment.commentId";

  private static final String DIALOG_COMMENT_ADD =
      "net.simonvt.cathode.ui.comments.CommentFragment.addCommentDialog";
  private static final String DIALOG_COMMENT_UPDATE =
      "net.simonvt.cathode.ui.comments.CommentFragment.updateCommentDialog";

  private static final String STATE_ADAPTER =
      "net.simonvt.cathode.ui.comments.CommentFragment.adapterState";

  private static final int LOADER_COMMENT = 1;
  private static final int LOADER_COMMENTS = 2;

  private long commentId;

  private int columnCount;

  private CommentsAdapter adapter;

  private Cursor comment;

  private Cursor replies;

  private Bundle adapterState;

  public static Bundle getArgs(long commentId) {
    Bundle args = new Bundle();
    args.putLong(ARG_COMMENT_ID, commentId);
    return args;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    CathodeApp.inject(getActivity(), this);

    Bundle args = getArguments();
    commentId = args.getLong(ARG_COMMENT_ID);

    //columnCount = getResources().getInteger(R.integer.listColumns);
    columnCount = 1;

    if (inState != null) {
      adapterState = inState.getBundle(STATE_ADAPTER);
    }

    setTitle(R.string.title_comments);

    getLoaderManager().initLoader(LOADER_COMMENT, null, commentLoader);
    getLoaderManager().initLoader(LOADER_COMMENTS, null, repliesLoader);
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBundle(STATE_ADAPTER, adapter.saveState());
  }

  @Override protected int getColumnCount() {
    return columnCount;
  }

  @Override public void createMenu(Toolbar toolbar) {
    super.createMenu(toolbar);
    toolbar.inflateMenu(R.menu.fragment_comment);
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_reply:
        AddCommentDialog.newInstance(ItemType.COMMENT, commentId)
            .show(getFragmentManager(), DIALOG_COMMENT_ADD);
        return true;

      default:
        return super.onMenuItemClick(item);
    }
  }

  private CommentsAdapter.OnCommentClickListener commentClickListener =
      new CommentsAdapter.OnCommentClickListener() {
        @Override public void onCommentClick(long commentId, String comment, boolean spoiler,
            boolean isUserComment) {
          if (isUserComment) {
            UpdateCommentDialog.newInstance(commentId, comment, spoiler)
                .show(getFragmentManager(), DIALOG_COMMENT_UPDATE);
          }
        }
      };

  private void updateCursor() {
    if (comment == null || replies == null) {
      return;
    }

    SimpleMergeCursor cursor = new SimpleMergeCursor(comment, replies);
    setCursor(cursor);
  }

  private void setCursor(Cursor cursor) {
    if (adapter == null) {
      adapter = new CommentsAdapter(getActivity(), null, true, commentClickListener);
      if (adapterState != null) {
        adapter.restoreState(adapterState);
      }
      setAdapter(adapter);
    }

    adapter.changeCursor(cursor);
  }

  private LoaderManager.LoaderCallbacks<SimpleCursor> commentLoader =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          return new SimpleCursorLoader(getContext(), Comments.COMMENTS_WITH_PROFILE,
              CommentsAdapter.PROJECTION, Tables.COMMENTS + "." + CommentColumns.ID + "=?",
              new String[] {
                  String.valueOf(commentId),
              }, null);
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
          comment = data;
          updateCursor();
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
        }
      };

  private LoaderManager.LoaderCallbacks<SimpleCursor> repliesLoader =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          return new SimpleCursorLoader(getContext(), Comments.withParent(commentId),
              CommentsAdapter.PROJECTION, null, null, CommentColumns.CREATED_AT + " DESC");
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
          replies = data;
          updateCursor();
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
        }
      };
}
