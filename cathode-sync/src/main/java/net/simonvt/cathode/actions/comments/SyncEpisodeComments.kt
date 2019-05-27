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
import net.simonvt.cathode.actions.PagedResponse
import net.simonvt.cathode.actions.comments.SyncEpisodeComments.Params
import net.simonvt.cathode.api.entity.Comment
import net.simonvt.cathode.api.enumeration.Extended
import net.simonvt.cathode.api.service.EpisodeService
import net.simonvt.cathode.common.database.forEach
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.ProviderSchematic.Comments
import net.simonvt.cathode.provider.ProviderSchematic.Episodes
import net.simonvt.cathode.provider.batch
import net.simonvt.cathode.provider.entity.ItemTypeString
import net.simonvt.cathode.provider.helper.CommentsHelper
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import net.simonvt.cathode.provider.helper.UserDatabaseHelper
import net.simonvt.cathode.provider.query
import retrofit2.Call
import javax.inject.Inject

class SyncEpisodeComments @Inject constructor(
  private val context: Context,
  private val showHelper: ShowDatabaseHelper,
  private val episodeHelper: EpisodeDatabaseHelper,
  private val userHelper: UserDatabaseHelper,
  private val episodeService: EpisodeService
) : PagedAction<Params, Comment>() {

  override fun key(params: Params): String =
    "SyncEpisodeComments&traktId=${params.traktId}&season=${params.season}&episode=${params.episode}"

  override fun getCall(params: Params, page: Int): Call<List<Comment>> {
    return episodeService.getComments(
      params.traktId,
      params.season,
      params.episode,
      page,
      LIMIT,
      Extended.FULL_IMAGES
    )
  }

  override suspend fun handleResponse(
    params: Params,
    pagedResponse: PagedResponse<Params, Comment>
  ) {
    val showId = showHelper.getId(params.traktId)
    val episodeId = episodeHelper.getId(showId, params.season, params.episode)

    val ops = arrayListOf<ContentProviderOperation>()
    val existingComments = mutableListOf<Long>()
    val deleteComments = mutableListOf<Long>()

    val localComments = context.contentResolver.query(
      Comments.COMMENTS,
      arrayOf(CommentColumns.ID),
      CommentColumns.ITEM_TYPE + "=? AND " + CommentColumns.ITEM_ID + "=?",
      arrayOf(ItemTypeString.EPISODE, episodeId.toString())
    )
    localComments.forEach { cursor ->
      val id = cursor.getLong(CommentColumns.ID)
      existingComments.add(id)
      deleteComments.add(id)
    }
    localComments.close()

    var page: PagedResponse<Params, Comment>? = pagedResponse
    do {
      for (comment in page!!.response) {
        val profile = comment.user
        val idResult = userHelper.updateOrCreate(profile)
        val userId = idResult.id

        val values = CommentsHelper.getValues(comment)
        values.put(CommentColumns.USER_ID, userId)

        values.put(CommentColumns.ITEM_TYPE, ItemTypeString.EPISODE)
        values.put(CommentColumns.ITEM_ID, episodeId)

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

      page = page.nextPage()
    } while (page != null)

    for (id in deleteComments) {
      val op = ContentProviderOperation.newDelete(Comments.withId(id))
      ops.add(op.build())
    }

    ops.add(
      ContentProviderOperation.newUpdate(Episodes.withId(episodeId)).withValue(
        EpisodeColumns.LAST_COMMENT_SYNC,
        System.currentTimeMillis()
      ).build()
    )

    context.contentResolver.batch(ops)
  }

  data class Params(val traktId: Long, val season: Int, val episode: Int)

  companion object {
    private const val LIMIT = 100
  }
}
