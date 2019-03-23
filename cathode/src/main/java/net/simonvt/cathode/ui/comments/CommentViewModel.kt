/*
 * Copyright (C) 2018 Simon Vig Therkildsen
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
import android.text.format.DateUtils
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.simonvt.cathode.actions.ActionManager
import net.simonvt.cathode.actions.comments.SyncComment
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.common.entity.Comment
import net.simonvt.cathode.entitymapper.CommentListMapper
import net.simonvt.cathode.entitymapper.CommentMapper
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns
import net.simonvt.cathode.provider.DatabaseSchematic.Tables
import net.simonvt.cathode.provider.ProviderSchematic.Comments
import net.simonvt.cathode.ui.RefreshableViewModel
import javax.inject.Inject

class CommentViewModel @Inject constructor(
  private val context: Context,
  private val syncComment: SyncComment
) : RefreshableViewModel() {

  private var commentId = -1L

  lateinit var comment: MappedCursorLiveData<Comment>
    private set
  lateinit var replies: MappedCursorLiveData<List<Comment>>
    private set

  fun setCommentId(commentId: Long) {
    if (this.commentId == -1L) {
      this.commentId = commentId
      comment = MappedCursorLiveData(
        context,
        Comments.COMMENTS_WITH_PROFILE,
        CommentMapper.PROJECTION,
        Tables.COMMENTS + "." + CommentColumns.ID + "=?",
        arrayOf(commentId.toString()),
        null,
        CommentMapper()
      )

      replies = MappedCursorLiveData(
        context,
        Comments.withParent(commentId),
        CommentMapper.PROJECTION,
        null,
        null,
        null,
        CommentListMapper()
      )

      comment.observeForever(commentObserver)
    }
  }

  override fun onCleared() {
    comment.removeObserver(commentObserver)
    super.onCleared()
  }

  private val commentObserver = Observer<Comment> { comment ->
    if (comment != null) {
      viewModelScope.launch {
        if (System.currentTimeMillis() > comment.lastSync + SYNC_INTERVAL) {
          ActionManager.invokeAsync(
            SyncComment.key(commentId),
            syncComment,
            SyncComment.Params(commentId)
          )
        }
      }
    }
  }

  override suspend fun onRefresh() {
    ActionManager.invokeSync(
      SyncComment.key(commentId),
      syncComment,
      SyncComment.Params(commentId)
    )
  }

  companion object {

    private const val SYNC_INTERVAL = 3 * DateUtils.HOUR_IN_MILLIS
  }
}
