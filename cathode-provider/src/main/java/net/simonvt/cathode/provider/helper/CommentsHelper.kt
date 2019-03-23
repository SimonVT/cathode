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

package net.simonvt.cathode.provider.helper

import android.content.ContentValues
import net.simonvt.cathode.api.entity.Comment
import net.simonvt.cathode.provider.DatabaseContract

object CommentsHelper {

  @JvmStatic
  fun getValues(comments: Comment): ContentValues {
    val values = ContentValues()

    values.put(DatabaseContract.CommentColumns.ID, comments.id)
    values.put(DatabaseContract.CommentColumns.COMMENT, comments.comment)
    values.put(DatabaseContract.CommentColumns.SPOILER, comments.spoiler)
    values.put(DatabaseContract.CommentColumns.REVIEW, comments.review)
    values.put(DatabaseContract.CommentColumns.PARENT_ID, comments.parent_id)
    values.put(
      DatabaseContract.CommentColumns.CREATED_AT,
      comments.created_at.timeInMillis
    )
    values.put(DatabaseContract.CommentColumns.REPLIES, comments.replies)
    values.put(DatabaseContract.CommentColumns.LIKES, comments.likes)
    values.put(DatabaseContract.CommentColumns.USER_RATING, comments.user_rating)

    return values
  }
}
