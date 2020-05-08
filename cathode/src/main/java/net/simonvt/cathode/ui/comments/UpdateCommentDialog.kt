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

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog.Builder
import androidx.fragment.app.DialogFragment
import com.google.android.material.snackbar.Snackbar
import net.simonvt.cathode.R.string
import net.simonvt.cathode.api.service.CommentsService
import net.simonvt.cathode.common.util.TextUtils
import net.simonvt.cathode.common.widget.find
import net.simonvt.cathode.databinding.DialogCommentBinding
import net.simonvt.cathode.sync.scheduler.CommentsTaskScheduler
import javax.inject.Inject

class UpdateCommentDialog @Inject constructor(private val commentsScheduler: CommentsTaskScheduler) :
  DialogFragment() {

  private var commentId: Long = 0
  private var comment: String? = null
  private var spoiler = false

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    commentId = requireArguments().getLong(ARG_COMMENT_ID)
    comment = requireArguments().getString(ARG_COMMENT)
    spoiler = requireArguments().getBoolean(ARG_SPOILER)
  }

  override fun onCreateDialog(inState: Bundle?): Dialog {
    val builder = Builder(requireContext())
    val binding = DialogCommentBinding.inflate(LayoutInflater.from(builder.context))
    binding.comment.setText(comment)
    binding.spoiler.isChecked = spoiler
    builder.setView(binding.root)
    builder.setTitle(string.title_comment_update)
    builder.setPositiveButton(string.comment_update) { dialogInterface, i ->
      val comment = binding.comment.text.toString()
      val spoiler = binding.spoiler.isChecked
      if (TextUtils.wordCount(comment) >= CommentsService.MIN_WORD_COUNT) {
        commentsScheduler.updateComment(commentId, comment, spoiler)
      } else {
        Snackbar.make(
          requireActivity().find(android.R.id.content),
          string.comment_too_short,
          Snackbar.LENGTH_LONG
        ).show()
      }
    }
    builder.setNegativeButton(string.comment_delete) { dialog, which ->
      commentsScheduler.deleteComment(commentId)
    }
    return builder.create()
  }

  companion object {
    private const val ARG_COMMENT_ID =
      "net.simonvt.cathode.ui.dialog.RatingDialog.commentId"
    private const val ARG_COMMENT =
      "net.simonvt.cathode.ui.comments.UpdateCommentDialog.comment"
    private const val ARG_SPOILER =
      "net.simonvt.cathode.ui.comments.UpdateCommentDialog.spoiler"

    fun getArgs(commentId: Long, comment: String?, spoiler: Boolean): Bundle {
      val args = Bundle()
      args.putLong(ARG_COMMENT_ID, commentId)
      args.putString(ARG_COMMENT, comment)
      args.putBoolean(ARG_SPOILER, spoiler)
      return args
    }
  }
}
