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

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import net.simonvt.cathode.Injector;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.common.util.guava.Preconditions;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Comments;
import net.simonvt.cathode.ui.NavigationListener;
import net.simonvt.cathode.ui.fragment.ToolbarGridFragment;

public class CommentsFragment extends ToolbarGridFragment<CommentsAdapter.ViewHolder> {

  public static final String TAG = "net.simonvt.cathode.ui.comments.CommentsFragment";

  private static final String ARG_ITEM_TYPE =
      "net.simonvt.cathode.ui.comments.CommentsFragment.itemType";
  private static final String ARG_ITEM_ID =
      "net.simonvt.cathode.ui.comments.CommentsFragment.itemId";

  private static final String DIALOG_COMMENT_ADD =
      "net.simonvt.cathode.ui.comments.CommentsFragment.addCommentDialog";
  private static final String DIALOG_COMMENT_UPDATE =
      "net.simonvt.cathode.ui.comments.CommentsFragment.updateCommentDialog";

  private static final String STATE_ADAPTER =
      "net.simonvt.cathode.ui.comments.CommentFragment.adapterState";

  private static final int LOADER_COMMENTS = 1;

  private NavigationListener navigationListener;

  private ItemType itemType;

  private long itemId;

  private int columnCount;

  private CommentsAdapter adapter;

  private Bundle adapterState;

  public static Bundle getArgs(ItemType itemType, long itemId) {
    Preconditions.checkArgument(itemId >= 0, "itemId must be >= 0");

    Bundle args = new Bundle();
    args.putSerializable(ARG_ITEM_TYPE, itemType);
    args.putLong(ARG_ITEM_ID, itemId);
    return args;
  }

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    navigationListener = (NavigationListener) activity;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    Injector.obtain().inject(this);

    Bundle args = getArguments();
    itemType = (ItemType) args.getSerializable(ARG_ITEM_TYPE);
    itemId = args.getLong(ARG_ITEM_ID);

    //columnCount = getResources().getInteger(R.integer.listColumns);
    columnCount = 1;

    if (inState != null) {
      adapterState = inState.getBundle(STATE_ADAPTER);
    }

    setTitle(R.string.title_comments);

    getLoaderManager().initLoader(LOADER_COMMENTS, null, commentsLoader);
  }

  @Override protected int getColumnCount() {
    return columnCount;
  }

  @Override public void createMenu(Toolbar toolbar) {
    super.createMenu(toolbar);
    toolbar.inflateMenu(R.menu.fragment_comments);
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_comment_add:
        AddCommentDialog.newInstance(itemType, itemId)
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
          } else {
            navigationListener.onDisplayComment(commentId);
          }
        }
      };

  private void setCursor(SimpleCursor cursor) {
    if (adapter == null) {
      adapter = new CommentsAdapter(getActivity(), null, false, commentClickListener);
      if (adapterState != null) {
        adapter.restoreState(adapterState);
      }
      setAdapter(adapter);
    }

    adapter.changeCursor(cursor);
  }

  private LoaderManager.LoaderCallbacks<SimpleCursor> commentsLoader =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          Uri uri;
          switch (itemType) {
            case SHOW:
              uri = Comments.fromShow(itemId);
              break;

            case EPISODE:
              uri = Comments.fromEpisode(itemId);
              break;

            case MOVIE:
              uri = Comments.fromMovie(itemId);
              break;

            default:
              throw new IllegalArgumentException("Type " + itemType.toString() + " not supported");
          }

          return new SimpleCursorLoader(getContext(), uri, CommentsAdapter.PROJECTION,
              CommentColumns.PARENT_ID + "=0", null, CommentColumns.IS_USER_COMMENT
              + " DESC, "
              + CommentColumns.LIKES
              + " DESC, "
              + CommentColumns.CREATED_AT
              + " DESC");
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
          setCursor(data);
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
        }
      };
}
