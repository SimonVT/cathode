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
package net.simonvt.cathode.ui.comments

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import net.simonvt.cathode.R
import net.simonvt.cathode.R.menu
import net.simonvt.cathode.R.string
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.common.ui.FragmentsUtils
import net.simonvt.cathode.common.ui.fragment.ToolbarGridFragment
import net.simonvt.cathode.common.util.guava.Preconditions
import net.simonvt.cathode.entity.Comment
import net.simonvt.cathode.settings.TraktLinkSettings
import net.simonvt.cathode.sync.scheduler.CommentsTaskScheduler
import net.simonvt.cathode.ui.NavigationListener
import net.simonvt.cathode.ui.comments.CommentsAdapter.CommentCallbacks
import net.simonvt.cathode.ui.comments.CommentsAdapter.ViewHolder
import javax.inject.Inject

class CommentsFragment @Inject constructor(private val commentsScheduler: CommentsTaskScheduler) :
  ToolbarGridFragment<ViewHolder?>() {

  private var navigationListener: NavigationListener? = null

  private var itemType: ItemType? = null
  private var itemId: Long = 0

  private val viewModel: CommentsViewModel by viewModels()

  private var columnCount = 0

  private var adapter: CommentsAdapter? = null
  private var adapterState: Bundle? = null

  override fun onAttach(context: Context) {
    super.onAttach(context)
    navigationListener = requireActivity() as NavigationListener
  }

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    val args = arguments
    itemType =
      args!!.getSerializable(ARG_ITEM_TYPE) as ItemType?
    itemId = args.getLong(ARG_ITEM_ID)

    columnCount = 1

    if (inState != null) {
      adapterState = inState.getBundle(STATE_ADAPTER)
    }

    setTitle(string.title_comments)

    viewModel.setItemTypeAndId(itemType!!, itemId)
    viewModel.comments.observe(this, Observer { comments -> setComments(comments) })
  }

  override fun getColumnCount(): Int {
    return columnCount
  }

  override fun createMenu(toolbar: Toolbar) {
    super.createMenu(toolbar)
    if (TraktLinkSettings.isLinked(requireContext())) {
      toolbar.inflateMenu(menu.fragment_comments)
    }
  }

  override fun onMenuItemClick(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.menu_comment_add -> {
        FragmentsUtils.instantiate(
          parentFragmentManager,
          AddCommentDialog::class.java,
          AddCommentDialog.getArgs(itemType!!, itemId)
        ).show(parentFragmentManager, DIALOG_COMMENT_ADD)
        true
      }
      else -> super.onMenuItemClick(item)
    }
  }

  private val commentCallbacks: CommentCallbacks = object : CommentCallbacks {
    override fun onCommentClick(
      commentId: Long,
      comment: String?,
      spoiler: Boolean,
      isUserComment: Boolean
    ) {
      if (isUserComment) {
        FragmentsUtils.instantiate(
          parentFragmentManager,
          UpdateCommentDialog::class.java,
          UpdateCommentDialog.getArgs(commentId, comment, spoiler)
        ).show(parentFragmentManager, DIALOG_COMMENT_UPDATE)
      } else {
        navigationListener!!.onDisplayComment(commentId)
      }
    }

    override fun onLikeComment(commentId: Long) {
      commentsScheduler.like(commentId)
    }

    override fun onUnlikeComment(commentId: Long) {
      commentsScheduler.unlike(commentId)
    }
  }

  private fun setComments(comments: List<Comment>) {
    if (adapter == null) {
      adapter = CommentsAdapter(requireContext(), false, commentCallbacks)
      if (adapterState != null) {
        adapter!!.restoreState(adapterState!!)
      }
      setAdapter(adapter)
    }
    adapter!!.setList(comments)
  }

  companion object {
    const val TAG = "net.simonvt.cathode.ui.comments.CommentsFragment"
    private const val ARG_ITEM_TYPE =
      "net.simonvt.cathode.ui.comments.CommentsFragment.itemType"
    private const val ARG_ITEM_ID =
      "net.simonvt.cathode.ui.comments.CommentsFragment.itemId"
    private const val DIALOG_COMMENT_ADD =
      "net.simonvt.cathode.ui.comments.CommentsFragment.addCommentDialog"
    private const val DIALOG_COMMENT_UPDATE =
      "net.simonvt.cathode.ui.comments.CommentsFragment.updateCommentDialog"
    private const val STATE_ADAPTER =
      "net.simonvt.cathode.ui.comments.CommentFragment.adapterState"

    fun getArgs(itemType: ItemType?, itemId: Long): Bundle {
      Preconditions.checkArgument(itemId >= 0, "itemId must be >= 0")
      val args = Bundle()
      args.putSerializable(ARG_ITEM_TYPE, itemType)
      args.putLong(ARG_ITEM_ID, itemId)
      return args
    }
  }
}
