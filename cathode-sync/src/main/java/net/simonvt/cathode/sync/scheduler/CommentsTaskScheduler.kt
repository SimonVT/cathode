/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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

package net.simonvt.cathode.sync.scheduler

import android.content.ContentValues
import android.content.Context
import kotlinx.coroutines.launch
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.common.database.getInt
import net.simonvt.cathode.jobqueue.JobManager
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns
import net.simonvt.cathode.provider.ProviderSchematic.Comments
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.remote.action.comments.AddCommentJob
import net.simonvt.cathode.remote.action.comments.CommentReplyJob
import net.simonvt.cathode.remote.action.comments.DeleteCommentJob
import net.simonvt.cathode.remote.action.comments.LikeCommentJob
import net.simonvt.cathode.remote.action.comments.UnlikeCommentJob
import net.simonvt.cathode.remote.action.comments.UpdateCommentJob
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentsTaskScheduler @Inject
constructor(
  context: Context,
  jobManager: JobManager,
  private val showHelper: ShowDatabaseHelper,
  private val episodeHelper: EpisodeDatabaseHelper,
  private val movieHelper: MovieDatabaseHelper
) : BaseTaskScheduler(context, jobManager) {

  fun comment(
    type: ItemType,
    itemId: Long,
    comment: String,
    spoiler: Boolean
  ) {
    scope.launch {
      when (type) {
        ItemType.SHOW -> {
          val traktId = showHelper.getTraktId(itemId)
          queue(AddCommentJob(type, traktId, comment, spoiler))
        }

        ItemType.EPISODE -> {
          val traktId = episodeHelper.getTraktId(itemId)
          queue(AddCommentJob(type, traktId, comment, spoiler))
        }

        ItemType.MOVIE -> {
          val traktId = movieHelper.getTraktId(itemId)
          queue(AddCommentJob(type, traktId, comment, spoiler))
        }

        else -> throw RuntimeException("Unknown type $type")
      }
    }
  }

  fun updateComment(commentId: Long, comment: String, spoiler: Boolean) {
    scope.launch {
      queue(UpdateCommentJob(commentId, comment, spoiler))

      val values = ContentValues()
      values.put(CommentColumns.COMMENT, comment)
      values.put(CommentColumns.SPOILER, spoiler)
      context.contentResolver.update(Comments.withId(commentId), values, null, null)
    }
  }

  fun deleteComment(commentId: Long) {
    scope.launch {
      queue(DeleteCommentJob(commentId))
      context.contentResolver.delete(Comments.withId(commentId), null, null)
    }
  }

  fun reply(parentId: Long, comment: String, spoiler: Boolean) {
    scope.launch { queue(CommentReplyJob(parentId, comment, spoiler)) }
  }

  fun like(commentId: Long) {
    scope.launch {
      queue(LikeCommentJob(commentId))

      val c = context.contentResolver.query(
        Comments.withId(commentId),
        arrayOf(CommentColumns.LIKES)
      )
      if (c.moveToFirst()) {
        val likes = c.getInt(CommentColumns.LIKES)

        val values = ContentValues()
        values.put(CommentColumns.LIKED, true)
        values.put(CommentColumns.LIKES, likes + 1)
        context.contentResolver.update(Comments.withId(commentId), values, null, null)
      }
      c.close()
    }
  }

  fun unlike(commentId: Long) {
    scope.launch {
      queue(UnlikeCommentJob(commentId))

      val c = context.contentResolver.query(
        Comments.withId(commentId),
        arrayOf(CommentColumns.LIKES)
      )
      if (c.moveToFirst()) {
        val likes = c.getInt(CommentColumns.LIKES)

        val values = ContentValues()
        values.put(CommentColumns.LIKED, false)
        values.put(CommentColumns.LIKES, likes - 1)
        context.contentResolver.update(Comments.withId(commentId), values, null, null)
      }
      c.close()
    }
  }
}
