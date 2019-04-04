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

package net.simonvt.cathode.actions.comments

import android.content.ContentProviderOperation
import android.content.Context
import net.simonvt.cathode.actions.PagedAction
import net.simonvt.cathode.actions.comments.SyncCommentReplies.Params
import net.simonvt.cathode.api.entity.Comment
import net.simonvt.cathode.api.enumeration.Extended
import net.simonvt.cathode.api.service.CommentsService
import net.simonvt.cathode.common.database.Cursors
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns
import net.simonvt.cathode.provider.DatabaseSchematic.Tables
import net.simonvt.cathode.provider.ProviderSchematic.Comments
import net.simonvt.cathode.provider.batch
import net.simonvt.cathode.provider.helper.CommentsHelper
import net.simonvt.cathode.provider.helper.UserDatabaseHelper
import net.simonvt.cathode.provider.query
import retrofit2.Call
import javax.inject.Inject

class SyncCommentReplies @Inject constructor(
  private val context: Context,
  private val usersHelper: UserDatabaseHelper,
  private val commentsService: CommentsService
) : PagedAction<Params, Comment>() {

  override fun key(params: Params): String = "SyncCommentReplies&traktId=${params.traktId}"

  override fun getCall(params: Params, page: Int): Call<List<Comment>> {
    return commentsService.replies(
      params.traktId,
      page,
      LIMIT,
      Extended.FULL_IMAGES
    )
  }

  override suspend fun handleResponse(params: Params, page: Int, response: List<Comment>) {
    val ops = arrayListOf<ContentProviderOperation>()

    val itemType: Int
    val itemId: Long

    val parentComment = context.contentResolver.query(
      Comments.withId(params.traktId),
      arrayOf(CommentColumns.ITEM_TYPE, CommentColumns.ITEM_ID)
    )
    parentComment.moveToFirst()
    itemType = Cursors.getInt(parentComment, CommentColumns.ITEM_TYPE)
    itemId = Cursors.getLong(parentComment, CommentColumns.ITEM_ID)

    val existingComments = mutableListOf<Long>()
    val deleteComments = mutableListOf<Long>()

    val localComments = context.contentResolver.query(
      Comments.withParent(params.traktId),
      arrayOf(Tables.COMMENTS + "." + CommentColumns.ID)
    )
    while (localComments.moveToNext()) {
      val id = Cursors.getLong(localComments, CommentColumns.ID)
      existingComments.add(id)
      deleteComments.add(id)
    }
    localComments.close()

    for (comment in response) {
      val profile = comment.user
      val idResult = usersHelper.updateOrCreate(profile)
      val userId = idResult.id

      val values = CommentsHelper.getValues(comment)
      values.put(CommentColumns.USER_ID, userId)

      values.put(CommentColumns.ITEM_TYPE, itemType)
      values.put(CommentColumns.ITEM_ID, itemId)

      val commentId = comment.id
      var exists = existingComments.contains(commentId)
      if (!exists) {
        // May have been created by user likes
        val c = context.contentResolver.query(
          Comments.withId(commentId),
          arrayOf(CommentColumns.ID)
        )
        exists = c.moveToFirst()
        c.close()
      }

      if (exists) {
        deleteComments.remove(commentId)
        val op = ContentProviderOperation.newUpdate(Comments.withId(commentId)).withValues(values)
        ops.add(op.build())
      } else {
        // The same comment can exist multiple times in the result from Trakt, so any comments we
        // insert are added to the list of existing comments.
        existingComments.add(commentId)
        val op = ContentProviderOperation.newInsert(Comments.COMMENTS).withValues(values)
        ops.add(op.build())
      }
    }

    for (id in deleteComments) {
      val op = ContentProviderOperation.newDelete(Comments.withId(id))
      ops.add(op.build())
    }

    context.contentResolver.batch(ops)
  }

  data class Params(val traktId: Long)

  companion object {
    private const val LIMIT = 100
  }
}
