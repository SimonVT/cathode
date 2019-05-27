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

package net.simonvt.cathode.actions.user

import android.content.ContentProviderOperation
import android.content.Context
import net.simonvt.cathode.actions.PagedAction
import net.simonvt.cathode.actions.PagedResponse
import net.simonvt.cathode.actions.user.SyncCommentLikes.Params
import net.simonvt.cathode.api.entity.Like
import net.simonvt.cathode.api.enumeration.ItemTypes
import net.simonvt.cathode.api.service.UsersService
import net.simonvt.cathode.common.database.forEach
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns
import net.simonvt.cathode.provider.ProviderSchematic.Comments
import net.simonvt.cathode.provider.batch
import net.simonvt.cathode.provider.helper.CommentsHelper
import net.simonvt.cathode.provider.helper.UserDatabaseHelper
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.settings.TraktTimestamps
import retrofit2.Call
import javax.inject.Inject

class SyncCommentLikes @Inject constructor(
  private val context: Context,
  private val userHelper: UserDatabaseHelper,
  private val usersService: UsersService
) : PagedAction<Params, Like>() {

  override fun key(params: Params): String = "SyncCommentLikes"

  override fun getCall(params: Params, page: Int): Call<List<Like>> =
    usersService.getLikes(ItemTypes.COMMENTS, page, LIMIT)

  override suspend fun handleResponse(params: Params, pagedResponse: PagedResponse<Params, Like>) {
    val ops = arrayListOf<ContentProviderOperation>()
    val existingLikes = mutableListOf<Long>()
    val deleteLikes = mutableListOf<Long>()

    val localComments = context.contentResolver.query(
      Comments.COMMENTS,
      arrayOf(CommentColumns.ID),
      CommentColumns.LIKED + "=1"
    )
    localComments.forEach { cursor ->
      val id = cursor.getLong(CommentColumns.ID)
      existingLikes.add(id)
      deleteLikes.add(id)
    }
    localComments.close()

    var page: PagedResponse<Params, Like>? = pagedResponse
    do {
      for (like in page!!.response) {
        val comment = like.comment!!
        val commentId = comment.id
        val likedAt = like.liked_at.timeInMillis

        var exists = existingLikes.contains(commentId)
        if (!exists) {
          // May have been created by user likes
          val commentCursor = context.contentResolver.query(
            Comments.withId(commentId),
            arrayOf(CommentColumns.ID)
          )
          exists = commentCursor.moveToFirst()
          commentCursor.close()
        }

        if (exists) {
          deleteLikes.remove(commentId)

          val op = ContentProviderOperation.newUpdate(Comments.withId(commentId))
            .withValue(CommentColumns.LIKED, true)
            .withValue(CommentColumns.LIKED_AT, likedAt)
            .withValue(CommentColumns.IS_USER_COMMENT, true)
          ops.add(op.build())
        } else {
          val profile = comment.user
          val idResult = userHelper.updateOrCreate(profile)
          val userId = idResult.id

          val values = CommentsHelper.getValues(comment)
          values.put(CommentColumns.USER_ID, userId)

          values.put(CommentColumns.LIKED, true)
          values.put(CommentColumns.LIKED_AT, likedAt)
          values.put(CommentColumns.IS_USER_COMMENT, true)

          val op = ContentProviderOperation.newInsert(Comments.COMMENTS).withValues(values)
          ops.add(op.build())
          existingLikes.add(commentId)
        }
      }

      page = page.nextPage()
    } while (page != null)

    for (id in deleteLikes) {
      val op = ContentProviderOperation.newUpdate(Comments.withId(id))
      op.withValue(CommentColumns.LIKED, false)
      ops.add(op.build())
    }

    context.contentResolver.batch(ops)

    if (params.userActivityTime > 0L) {
      TraktTimestamps.getSettings(context)
        .edit()
        .putLong(TraktTimestamps.COMMENT_LIKED_AT, params.userActivityTime)
        .apply()
    }
  }

  data class Params(val userActivityTime: Long = 0L)

  companion object {
    private const val LIMIT = 100
  }
}
