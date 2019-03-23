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
import net.simonvt.cathode.actions.comments.SyncMovieComments.Params
import net.simonvt.cathode.api.entity.Comment
import net.simonvt.cathode.api.enumeration.Extended
import net.simonvt.cathode.api.service.MoviesService
import net.simonvt.cathode.common.database.Cursors
import net.simonvt.cathode.common.database.forEach
import net.simonvt.cathode.provider.DatabaseContract
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns
import net.simonvt.cathode.provider.ProviderSchematic.Comments
import net.simonvt.cathode.provider.ProviderSchematic.Movies
import net.simonvt.cathode.provider.batch
import net.simonvt.cathode.provider.helper.CommentsHelper
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper
import net.simonvt.cathode.provider.helper.UserDatabaseHelper
import net.simonvt.cathode.provider.query
import retrofit2.Call
import javax.inject.Inject

class SyncMovieComments @Inject constructor(
  private val context: Context,
  private val movieHelper: MovieDatabaseHelper,
  private val userHelper: UserDatabaseHelper,
  private val moviesService: MoviesService
) : PagedAction<Params, Comment>() {

  override fun getCall(params: Params, page: Int): Call<List<Comment>> {
    return moviesService.getComments(
      params.traktId,
      page,
      LIMIT,
      Extended.FULL_IMAGES
    )
  }

  override suspend fun handleResponse(params: Params, page: Int, response: List<Comment>) {
    val itemType = DatabaseContract.ItemType.MOVIE
    val movieId = movieHelper.getId(params.traktId)

    val ops = arrayListOf<ContentProviderOperation>()
    val existingComments = mutableListOf<Long>()
    val deleteComments = mutableListOf<Long>()

    val localComments = context.contentResolver.query(
      Comments.COMMENTS,
      arrayOf(CommentColumns.ID),
      CommentColumns.ITEM_TYPE + "=? AND " + CommentColumns.ITEM_ID + "=?",
      arrayOf(itemType.toString(), movieId.toString())
    )
    localComments.forEach { cursor ->
      val id = Cursors.getLong(cursor, CommentColumns.ID)
      existingComments.add(id)
      deleteComments.add(id)
    }
    localComments.close()

    for (comment in response) {
      val profile = comment.user
      val idResult = userHelper.updateOrCreate(profile)
      val userId = idResult.id

      val values = CommentsHelper.getValues(comment)
      values.put(CommentColumns.USER_ID, userId)

      values.put(CommentColumns.ITEM_TYPE, itemType)
      values.put(CommentColumns.ITEM_ID, movieId)

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

    ops.add(
      ContentProviderOperation.newUpdate(Movies.withId(movieId)).withValue(
        MovieColumns.LAST_COMMENT_SYNC,
        System.currentTimeMillis()
      ).build()
    )

    context.contentResolver.batch(ops)
  }

  data class Params(val traktId: Long)

  companion object {

    private const val LIMIT = 100

    fun key(traktId: Long): String {
      return "SyncMovieComments&traktId=$traktId"
    }
  }
}
