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
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import net.simonvt.cathode.R.layout
import net.simonvt.cathode.R.string
import net.simonvt.cathode.common.widget.CircleTransformation
import net.simonvt.cathode.databinding.SectionCommentsItemBinding
import net.simonvt.cathode.entity.Comment
import java.text.DateFormat
import java.util.Date

object LinearCommentsAdapter {

  fun updateComments(
    context: Context,
    parent: ViewGroup,
    userComments: List<Comment>?,
    comments: List<Comment>?
  ) {
    parent.removeAllViews()
    if (userComments != null) {
      for (comment in userComments) {
        val visibleName: String? = if (!TextUtils.isEmpty(comment.user.name)) {
          comment.user.name
        } else {
          comment.user.username
        }

        val itemBinding =
          SectionCommentsItemBinding.inflate(LayoutInflater.from(context), parent, false)
        itemBinding.avatar.addTransformation(CircleTransformation())
        itemBinding.avatar.setImage(comment.user.avatar)
        itemBinding.username.text = visibleName
        val format = DateFormat.getDateTimeInstance()
        val dateString = format.format(Date(comment.createdAt))
        itemBinding.date.text = dateString
        itemBinding.commentText.text = comment.comment
        parent.addView(itemBinding.root)
      }
    }
    if (comments != null) {
      for (comment in comments) {
        val visibleName: String? = if (!TextUtils.isEmpty(comment.user.name)) {
          comment.user.name
        } else {
          comment.user.username
        }

        val itemBinding =
          SectionCommentsItemBinding.inflate(LayoutInflater.from(context), parent, false)
        itemBinding.avatar.addTransformation(CircleTransformation())
        itemBinding.avatar.setImage(comment.user.avatar)
        val usernameText = context.resources
          .getString(string.comments_username_rating, visibleName, comment.userRating)
        itemBinding.username.text = usernameText
        val format = DateFormat.getDateTimeInstance()
        val dateString = format.format(Date(comment.createdAt))
        itemBinding.date.text = dateString
        itemBinding.commentText.text = comment.comment
        parent.addView(itemBinding.root)
      }
    }
    val count = parent.childCount
    if (count == 0) {
      LayoutInflater.from(context).inflate(layout.section_comments_none, parent, true)
    }
  }
}
