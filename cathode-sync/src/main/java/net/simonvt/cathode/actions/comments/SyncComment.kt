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

import android.content.Context
import net.simonvt.cathode.actions.CallAction
import net.simonvt.cathode.actions.comments.SyncComment.Params
import net.simonvt.cathode.api.entity.Comment
import net.simonvt.cathode.api.service.CommentsService
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns
import net.simonvt.cathode.provider.ProviderSchematic.Comments
import net.simonvt.cathode.provider.helper.CommentsHelper
import net.simonvt.cathode.provider.helper.UserDatabaseHelper
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.provider.update
import retrofit2.Call
import javax.inject.Inject

class SyncComment @Inject constructor(
  private val context: Context,
  private val usersHelper: UserDatabaseHelper,
  private val commentsService: CommentsService,
  private val syncCommentReplies: SyncCommentReplies
) : CallAction<Params, Comment>() {

  override fun key(params: Params): String = "SyncComment&traktId=${params.traktId}"

  override fun getCall(params: Params): Call<Comment> = commentsService.comment(params.traktId)

  override suspend fun handleResponse(params: Params, response: Comment) {
    val localComment = context.contentResolver.query(Comments.withId(params.traktId))
    val exists = localComment.moveToFirst()
    if (!exists) {
      IllegalStateException("Comment with id ${params.traktId} must exist")
    }

    syncCommentReplies(SyncCommentReplies.Params(params.traktId))

    val profile = response.user
    val idResult = usersHelper.updateOrCreate(profile)
    val userId = idResult.id

    val values = CommentsHelper.getValues(response)
    values.put(CommentColumns.USER_ID, userId)
    values.put(CommentColumns.LAST_SYNC, System.currentTimeMillis())

    context.contentResolver.update(Comments.withId(params.traktId), values)
  }

  data class Params(val traktId: Long)
}
