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

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import dagger.android.support.AndroidSupportInjection
import net.simonvt.cathode.R
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.common.ui.fragment.ToolbarSwipeRefreshRecyclerFragment
import net.simonvt.cathode.common.util.guava.Preconditions
import net.simonvt.cathode.entity.Comment
import net.simonvt.cathode.settings.TraktLinkSettings
import net.simonvt.cathode.sync.scheduler.CommentsTaskScheduler
import net.simonvt.cathode.ui.CathodeViewModelFactory
import javax.inject.Inject

class CommentFragment : ToolbarSwipeRefreshRecyclerFragment<CommentsAdapter.ViewHolder>() {

  @Inject
  lateinit var commentsScheduler: CommentsTaskScheduler

  private var commentId: Long = 0

  private var columnCount: Int = 0

  @Inject
  lateinit var viewModelFactory: CathodeViewModelFactory
  private lateinit var viewModel: CommentViewModel

  private var adapter: CommentsAdapter? = null

  private var comment: Comment? = null
  private var replies: List<Comment>? = null

  private var adapterState: Bundle? = null

  private val commentClickListener = object : CommentsAdapter.CommentCallbacks {
    override fun onCommentClick(
      commentId: Long,
      comment: String,
      spoiler: Boolean,
      isUserComment: Boolean
    ) {
      if (isUserComment) {
        UpdateCommentDialog.newInstance(commentId, comment, spoiler)
          .show(fragmentManager!!, DIALOG_COMMENT_UPDATE)
      }
    }

    override fun onLikeComment(commentId: Long) {
      commentsScheduler.like(commentId)
    }

    override fun onUnlikeComment(commentId: Long) {
      commentsScheduler.unlike(commentId)
    }
  }

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    AndroidSupportInjection.inject(this)

    val args = arguments
    commentId = args!!.getLong(ARG_COMMENT_ID)

    //columnCount = getResources().getInteger(R.integer.listColumns);
    columnCount = 1

    if (inState != null) {
      adapterState = inState.getBundle(STATE_ADAPTER)
    }

    setTitle(R.string.title_comments)

    viewModel = ViewModelProviders.of(this, viewModelFactory).get(CommentViewModel::class.java)
    viewModel.setCommentId(commentId)
    viewModel.loading.observe(this, Observer { loading -> setRefreshing(loading) })
    viewModel.comment.observe(this, Observer {
      comment = it
      updateAdapter()
    })
    viewModel.replies.observe(this, Observer {
      replies = it
      updateAdapter()
    })
  }

  override fun onRefresh() {
    viewModel.refresh()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    if (adapter != null) {
      outState.putBundle(STATE_ADAPTER, adapter!!.saveState())
    } else {
      outState.putBundle(STATE_ADAPTER, adapterState)
    }
  }

  override fun getColumnCount(): Int {
    return columnCount
  }

  override fun createMenu(toolbar: Toolbar) {
    super.createMenu(toolbar)
    if (TraktLinkSettings.isLinked(requireContext())) {
      toolbar.inflateMenu(R.menu.fragment_comment)
    }
  }

  override fun onMenuItemClick(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_reply -> {
        AddCommentDialog.newInstance(ItemType.COMMENT, commentId)
          .show(fragmentManager!!, DIALOG_COMMENT_ADD)
        return true
      }

      else -> return super.onMenuItemClick(item)
    }
  }

  private fun updateAdapter() {
    val comment = this.comment
    val replies = this.replies
    if (comment == null || replies == null) {
      return
    }
    val comments = mutableListOf<Comment>()
    comments.add(comment)
    comments.addAll(replies)

    if (adapter == null) {
      adapter = CommentsAdapter(requireContext(), true, commentClickListener)
      if (adapterState != null) {
        adapter!!.restoreState(adapterState)
        adapterState = null
      }
      setAdapter(adapter)
    }

    adapter!!.setList(comments)
  }

  companion object {

    const val TAG = "net.simonvt.cathode.ui.comments.CommentFragment"

    private const val ARG_COMMENT_ID = "net.simonvt.cathode.ui.comments.CommentFragment.commentId"

    private const val DIALOG_COMMENT_ADD =
      "net.simonvt.cathode.ui.comments.CommentFragment.addCommentDialog"
    private const val DIALOG_COMMENT_UPDATE =
      "net.simonvt.cathode.ui.comments.CommentFragment.updateCommentDialog"

    private const val STATE_ADAPTER = "net.simonvt.cathode.ui.comments.CommentFragment.adapterState"

    @JvmStatic
    fun getArgs(commentId: Long): Bundle {
      Preconditions.checkArgument(commentId >= 0, "commentId must be >= 0")

      val args = Bundle()
      args.putLong(ARG_COMMENT_ID, commentId)
      return args
    }
  }
}
