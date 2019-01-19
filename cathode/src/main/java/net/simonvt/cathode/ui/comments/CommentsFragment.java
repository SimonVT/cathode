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
import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import dagger.android.support.AndroidSupportInjection;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.common.entity.Comment;
import net.simonvt.cathode.common.ui.fragment.ToolbarGridFragment;
import net.simonvt.cathode.common.util.guava.Preconditions;
import net.simonvt.cathode.sync.scheduler.CommentsTaskScheduler;
import net.simonvt.cathode.ui.NavigationListener;

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

  @Inject CommentsTaskScheduler commentsScheduler;

  private NavigationListener navigationListener;

  private ItemType itemType;

  private long itemId;

  private CommentsViewModel viewModel;

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
    AndroidSupportInjection.inject(this);

    Bundle args = getArguments();
    itemType = (ItemType) args.getSerializable(ARG_ITEM_TYPE);
    itemId = args.getLong(ARG_ITEM_ID);

    //columnCount = getResources().getInteger(R.integer.listColumns);
    columnCount = 1;

    if (inState != null) {
      adapterState = inState.getBundle(STATE_ADAPTER);
    }

    setTitle(R.string.title_comments);

    viewModel = ViewModelProviders.of(this).get(CommentsViewModel.class);
    viewModel.setItemTypeAndId(itemType, itemId);
    viewModel.getComments().observe(this, new Observer<List<Comment>>() {
      @Override public void onChanged(List<Comment> comments) {
        setComments(comments);
      }
    });
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

  private CommentsAdapter.CommentCallbacks commentCallbacks =
      new CommentsAdapter.CommentCallbacks() {
        @Override public void onCommentClick(long commentId, String comment, boolean spoiler,
            boolean isUserComment) {
          if (isUserComment) {
            UpdateCommentDialog.newInstance(commentId, comment, spoiler)
                .show(getFragmentManager(), DIALOG_COMMENT_UPDATE);
          } else {
            navigationListener.onDisplayComment(commentId);
          }
        }

        @Override public void onLikeComment(long commentId) {
          commentsScheduler.like(commentId);
        }

        @Override public void onUnlikeComment(long commentId) {
          commentsScheduler.unlike(commentId);
        }
      };

  private void setComments(List<Comment> comments) {
    if (adapter == null) {
      adapter = new CommentsAdapter(requireContext(), false, commentCallbacks);
      if (adapterState != null) {
        adapter.restoreState(adapterState);
      }
      setAdapter(adapter);
    }

    adapter.setList(comments);
  }
}
