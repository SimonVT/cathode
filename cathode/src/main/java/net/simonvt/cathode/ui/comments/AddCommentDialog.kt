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
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.api.enumeration.ItemType.COMMENT
import net.simonvt.cathode.api.service.CommentsService
import net.simonvt.cathode.common.util.TextUtils
import net.simonvt.cathode.common.widget.find
import net.simonvt.cathode.databinding.DialogCommentBinding
import net.simonvt.cathode.sync.scheduler.CommentsTaskScheduler
import javax.inject.Inject

class AddCommentDialog @Inject constructor(private val commentsScheduler: CommentsTaskScheduler) :
  DialogFragment() {

  private var type: ItemType? = null
  private var id: Long = 0

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    type = requireArguments().getSerializable(ARG_TYPE) as ItemType?
    id = requireArguments().getLong(ARG_ID)
  }

  override fun onCreateDialog(inState: Bundle?): Dialog {
    val builder = Builder(requireContext())
    val binding = DialogCommentBinding.inflate(LayoutInflater.from(builder.context))
    builder.setView(binding.root)
    if (type === COMMENT) {
      builder.setTitle(string.title_comment_reply)
    } else {
      builder.setTitle(string.title_comment_add)
    }
    builder.setPositiveButton(string.action_submit) { dialogInterface, i ->
      val comment = binding.comment.text.toString()
      val spoiler = binding.spoiler.isChecked
      if (TextUtils.wordCount(comment) >= CommentsService.MIN_WORD_COUNT) {
        if (type === COMMENT) {
          commentsScheduler.reply(id, comment, spoiler)
        } else {
          commentsScheduler.comment(type!!, id, comment, spoiler)
        }
      } else {
        Snackbar.make(
          requireActivity().find(android.R.id.content),
          string.comment_too_short,
          Snackbar.LENGTH_LONG
        ).show()
      }
    }
    return builder.create()
  }

  companion object {
    private const val ARG_TYPE = "net.simonvt.cathode.ui.comments.AddCommentDialog.type"
    private const val ARG_ID = "net.simonvt.cathode.ui.comments.AddCommentDialog.id"

    fun getArgs(type: ItemType, id: Long): Bundle {
      val args = Bundle()
      args.putSerializable(ARG_TYPE, type)
      args.putLong(ARG_ID, id)
      return args
    }
  }
}
