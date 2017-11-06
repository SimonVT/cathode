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
import dagger.android.support.AndroidSupportInjection;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.common.ui.fragment.ToolbarGridFragment;
import net.simonvt.cathode.common.util.guava.Preconditions;
import net.simonvt.cathode.provider.database.SimpleMergeCursor;
import net.simonvt.cathode.sync.scheduler.CommentsTaskScheduler;

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

  @Inject CommentsTaskScheduler commentsScheduler;

  private long commentId;

  private int columnCount;

  private CommentsAdapter adapter;

  private Bundle adapterState;

  public static Bundle getArgs(long commentId) {
    Preconditions.checkArgument(commentId >= 0, "commentId must be >= 0");

    Bundle args = new Bundle();
    args.putLong(ARG_COMMENT_ID, commentId);
    return args;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    AndroidSupportInjection.inject(this);

    Bundle args = getArguments();
    commentId = args.getLong(ARG_COMMENT_ID);

    //columnCount = getResources().getInteger(R.integer.listColumns);
    columnCount = 1;

    if (inState != null) {
      adapterState = inState.getBundle(STATE_ADAPTER);
    }

    setTitle(R.string.title_comments);

    getLoaderManager().initLoader(LOADER_COMMENT, null, commentLoader);
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

  private CommentsAdapter.CommentCallbacks commentClickListener =
      new CommentsAdapter.CommentCallbacks() {
        @Override public void onCommentClick(long commentId, String comment, boolean spoiler,
            boolean isUserComment) {
          if (isUserComment) {
            UpdateCommentDialog.newInstance(commentId, comment, spoiler)
                .show(getFragmentManager(), DIALOG_COMMENT_UPDATE);
          }
        }

        @Override public void onLikeComment(long commentId) {
          commentsScheduler.like(commentId);
        }

        @Override public void onUnlikeComment(long commentId) {
          commentsScheduler.unlike(commentId);
        }
      };

  private void setCursor(Cursor cursor) {
    if (adapter == null) {
      adapter = new CommentsAdapter(getActivity(), null, true, commentClickListener);
      if (adapterState != null) {
        adapter.restoreState(adapterState);
        adapterState = null;
      }
      setAdapter(adapter);
    }

    adapter.changeCursor(cursor);
  }

  private LoaderManager.LoaderCallbacks<SimpleMergeCursor> commentLoader =
      new LoaderManager.LoaderCallbacks<SimpleMergeCursor>() {
        @Override public Loader<SimpleMergeCursor> onCreateLoader(int id, Bundle args) {
          return new CommentAndRepliesLoader(getContext(), commentId);
        }

        @Override
        public void onLoadFinished(Loader<SimpleMergeCursor> loader, SimpleMergeCursor data) {
          setCursor(data);
        }

        @Override public void onLoaderReset(Loader<SimpleMergeCursor> loader) {
        }
      };
}
