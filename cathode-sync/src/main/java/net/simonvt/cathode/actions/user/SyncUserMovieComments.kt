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
import net.simonvt.cathode.actions.user.SyncUserMovieComments.Params
import net.simonvt.cathode.api.entity.CommentItem
import net.simonvt.cathode.api.enumeration.CommentType
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.api.enumeration.ItemTypes
import net.simonvt.cathode.api.service.UsersService
import net.simonvt.cathode.common.database.forEach
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns
import net.simonvt.cathode.provider.ProviderSchematic.Comments
import net.simonvt.cathode.provider.batch
import net.simonvt.cathode.provider.entity.ItemTypeString
import net.simonvt.cathode.provider.helper.CommentsHelper
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper
import net.simonvt.cathode.provider.helper.UserDatabaseHelper
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.settings.TraktTimestamps
import retrofit2.Call
import javax.inject.Inject

class SyncUserMovieComments @Inject constructor(
  private val context: Context,
  private val movieHelper: MovieDatabaseHelper,
  private val userHelper: UserDatabaseHelper,
  private val usersService: UsersService
) : PagedAction<Params, CommentItem>() {

  override fun key(params: Params): String = "SyncUserMovieComments"

  override fun getCall(params: Params, page: Int): Call<List<CommentItem>> =
    usersService.getUserComments(CommentType.ALL, ItemTypes.MOVIES, page, LIMIT)

  override suspend fun handleResponse(
    params: Params,
    pagedResponse: PagedResponse<Params, CommentItem>
  ) {
    val ops = arrayListOf<ContentProviderOperation>()
    val existingComments = mutableListOf<Long>()
    val addedLater = mutableMapOf<Long, CommentItem>()

    val localComments = context.contentResolver.query(
      Comments.COMMENTS,
      arrayOf(CommentColumns.ID),
      CommentColumns.ITEM_TYPE + "=? AND " + CommentColumns.IS_USER_COMMENT + "=1",
      arrayOf(ItemTypeString.MOVIE)
    )
    localComments.forEach { cursor -> existingComments.add(cursor.getLong(CommentColumns.ID)) }
    localComments.close()

    var profileId = -1L

    var page: PagedResponse<Params, CommentItem>? = pagedResponse
    do {
      for (commentItem in page!!.response) {
        val comment = commentItem.comment
        val commentId = comment.id

        // Old issue where two comments could have the same ID.
        if (addedLater[commentId] != null) {
          continue
        }

        val values = CommentsHelper.getValues(comment)
        values.put(CommentColumns.IS_USER_COMMENT, true)

        if (profileId == -1L) {
          val profile = commentItem.comment.user
          val result = userHelper.updateOrCreate(profile)
          profileId = result.id
        }
        values.put(CommentColumns.USER_ID, profileId)

        val movie = commentItem.movie
        val traktId = movie!!.ids.trakt!!
        val result = movieHelper.getIdOrCreate(traktId)
        val movieId = result.movieId

        values.put(CommentColumns.ITEM_TYPE, ItemType.MOVIE.toString())
        values.put(CommentColumns.ITEM_ID, movieId)

        var exists = existingComments.contains(commentId)
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
          existingComments.remove(commentId)
          val op = ContentProviderOperation.newUpdate(Comments.withId(commentId)).withValues(values)
          ops.add(op.build())
        } else {
          val op = ContentProviderOperation.newInsert(Comments.COMMENTS).withValues(values)
          ops.add(op.build())

          addedLater[commentId] = commentItem
        }
      }

      page = page.nextPage()
    } while (page != null)

    for (id in existingComments) {
      val op = ContentProviderOperation.newDelete(Comments.withId(id))
      ops.add(op.build())
    }

    context.contentResolver.batch(ops)

    if (params.userActivityTime > 0L) {
      TraktTimestamps.getSettings(context)
        .edit()
        .putLong(TraktTimestamps.MOVIE_COMMENT, params.userActivityTime)
        .apply()
    }
  }

  data class Params(val userActivityTime: Long = 0L)

  companion object {
    private const val LIMIT = 100
  }
}
