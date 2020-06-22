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
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import net.simonvt.cathode.R
import net.simonvt.cathode.common.ui.adapter.BaseAdapter
import net.simonvt.cathode.common.widget.CircleTransformation
import net.simonvt.cathode.common.widget.RemoteImageView
import net.simonvt.cathode.common.widget.find
import net.simonvt.cathode.common.widget.findOrNull
import net.simonvt.cathode.entity.Comment
import net.simonvt.cathode.ui.comments.CommentsAdapter.ViewHolder
import java.text.DateFormat
import java.util.Date
import java.util.HashSet

class CommentsAdapter(
  context: Context,
  private val showsReplies: Boolean,
  private val callbacks: CommentCallbacks
) : BaseAdapter<Comment, ViewHolder>(context) {

  interface CommentCallbacks {
    fun onCommentClick(commentId: Long, comment: String?, spoiler: Boolean, isUserComment: Boolean)
    fun onLikeComment(commentId: Long)
    fun onUnlikeComment(commentId: Long)
  }

  private val revealedSpoilers: MutableSet<Long> = HashSet()
  private val tintColor: Int
  private val likedTintColor: Int

  fun restoreState(state: Bundle) {
    val revealed = state.getLongArray(STATE_REVEALED_SPOILERS)
    for (id in revealed!!) {
      revealedSpoilers.add(id)
    }
  }

  fun saveState(): Bundle {
    val state = Bundle()
    val revealed = LongArray(revealedSpoilers.size)
    var i = 0
    for (id in revealedSpoilers) {
      revealed[i++] = id
    }
    state.putLongArray(STATE_REVEALED_SPOILERS, revealed)
    return state
  }

  override fun getItemId(position: Int): Long {
    return list[position]!!.id
  }

  override fun getItemViewType(position: Int): Int {
    return if (showsReplies && position > 0) REPLY else COMMENT
  }

  override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean {
    return oldItem.id == newItem.id
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val v: View = if (viewType == COMMENT) {
      LayoutInflater.from(context).inflate(R.layout.comment_post, parent, false)
    } else {
      LayoutInflater.from(context).inflate(R.layout.comment_reply, parent, false)
    }
    val holder = ViewHolder(v)
    holder.avatar.addTransformation(CircleTransformation())
    val likes = context.getDrawable(R.drawable.ic_thumb_up)!!
    holder.likeDrawable = DrawableCompat.wrap(likes)
    holder.likes.setCompoundDrawablesWithIntrinsicBounds(likes, null, null, null)
    if (holder.replies != null) {
      val replies = context.getDrawable(R.drawable.ic_comment)!!
      holder.repliesDrawable = DrawableCompat.wrap(replies)
      DrawableCompat.setTint(replies, tintColor)
      holder.replies.setCompoundDrawablesWithIntrinsicBounds(replies, null, null, null)
    }
    if (!showsReplies) {
      v.setOnClickListener {
        if (holder.isSpoiler && !holder.isRevealed) {
          revealedSpoilers.add(holder.itemId)
          holder.isRevealed = true
          holder.setIsRecyclable(false)
          holder.commentText.visibility = View.VISIBLE
          holder.commentText.alpha = 0.0f
          holder.commentText.animate().alpha(1.0f)
          holder.spoilerOverlay.animate().alpha(0.0f).withEndAction {
            holder.setIsRecyclable(true)
            holder.spoilerOverlay.visibility = View.GONE
          }
        } else {
          callbacks.onCommentClick(
            holder.itemId,
            holder.comment,
            holder.isSpoiler,
            holder.isUserComment
          )
        }
      }
    }
    holder.infoPane.setOnClickListener {
      val liked = holder.liked
      if (liked) {
        callbacks.onUnlikeComment(holder.itemId)
        holder.likeCount--
        DrawableCompat.setTint(holder.likeDrawable!!, tintColor)
      } else {
        callbacks.onLikeComment(holder.itemId)
        holder.likeCount++
        DrawableCompat.setTint(holder.likeDrawable!!, likedTintColor)
      }
      holder.likes.text = holder.likeCount.toString()
      holder.liked = !liked
    }
    return holder
  }

  override fun onViewRecycled(holder: ViewHolder) {
    super.onViewRecycled(holder)
    if (holder.commentAnimator != null) {
      holder.commentAnimator!!.cancel()
    }
    if (holder.spoilerAnimator != null) {
      holder.spoilerAnimator!!.cancel()
    }
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val comment = list[position]!!
    val revealed = revealedSpoilers.contains(comment.id)
    val format = DateFormat.getDateTimeInstance()
    val dateString = format.format(Date(comment.createdAt))
    val visibleName: String?
    visibleName = if (!TextUtils.isEmpty(comment.user.name)) {
      comment.user.name
    } else {
      comment.user.username
    }
    holder.avatar.setImage(comment.user.avatar)
    val usernameText = context.resources
      .getString(R.string.comments_username_rating, visibleName, comment.userRating)
    holder.username.text = usernameText
    holder.date.text = dateString
    holder.commentText.text = comment.comment
    holder.likes.text = comment.likes.toString()
    if (holder.replies != null) {
      holder.replies.text = comment.replies.toString()
    }
    holder.likeCount = comment.likes
    if (comment.isLiked) {
      DrawableCompat.setTint(holder.likeDrawable!!, likedTintColor)
    } else {
      DrawableCompat.setTint(holder.likeDrawable!!, tintColor)
    }
    holder.isRevealed = revealed
    if (comment.isSpoiler && !revealed) {
      holder.commentText.visibility = View.GONE
      holder.spoilerOverlay.visibility = View.VISIBLE
      holder.spoilerOverlay.alpha = 1.0f
    } else {
      holder.commentText.visibility = View.VISIBLE
      holder.commentText.alpha = 1.0f
      holder.spoilerOverlay.visibility = View.GONE
    }
    if (showsReplies) {
      if (comment.isSpoiler && !revealed) {
        holder.itemView.setOnClickListener {
          revealedSpoilers.add(holder.itemId)
          holder.isRevealed = true
          holder.commentText.visibility = View.VISIBLE
          holder.commentText.alpha = 0.0f
          holder.commentAnimator = holder.commentText.animate().alpha(1.0f)
          holder.spoilerAnimator = holder.spoilerOverlay.animate().alpha(0.0f)
            .withEndAction { holder.spoilerOverlay.visibility = View.GONE }
        }
      } else {
        holder.itemView.setOnClickListener(null)
      }
    }
    holder.spoiler.visibility = if (comment.isSpoiler) View.VISIBLE else View.INVISIBLE
    holder.comment = comment.comment
    holder.isSpoiler = comment.isSpoiler
    holder.isUserComment = comment.isUserComment
    holder.liked = comment.isLiked
  }

  class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val infoPane: View = view.find(R.id.infoPane)
    val avatar: RemoteImageView = view.find(R.id.avatar)
    val username: TextView = view.find(R.id.username)
    val date: TextView = view.find(R.id.date)
    val commentText: TextView = view.find(R.id.commentText)
    val likes: TextView = view.find(R.id.likes)
    val replies: TextView? = view.findOrNull(R.id.replies)
    val spoiler: TextView = view.find(R.id.spoiler)
    val spoilerOverlay: TextView = view.find(R.id.spoilerOverlay)

    var commentAnimator: ViewPropertyAnimator? = null
    var spoilerAnimator: ViewPropertyAnimator? = null
    var likeDrawable: Drawable? = null
    var repliesDrawable: Drawable? = null

    var likeCount = 0
    var comment: String? = null
    var isUserComment = false
    var isSpoiler = false
    var isRevealed = false
    var liked = false
  }

  companion object {
    private const val STATE_REVEALED_SPOILERS = "net.simonvt.cathode.ui.comments.CommentsAdapter"
    private const val COMMENT = 0
    private const val REPLY = 1
    private const val USER_COMMENT = 2
    private const val USER_REPLY = 3
  }

  init {
    setHasStableIds(true)
    tintColor = ContextCompat.getColor(context, R.color.commentIconTint)
    likedTintColor = ContextCompat.getColor(context, R.color.commentLikedTint)
  }
}
