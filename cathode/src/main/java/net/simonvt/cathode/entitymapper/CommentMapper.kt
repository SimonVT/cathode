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

package net.simonvt.cathode.entitymapper

import android.database.Cursor
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.common.database.getBoolean
import net.simonvt.cathode.common.database.getInt
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.common.database.getString
import net.simonvt.cathode.common.database.getStringOrNull
import net.simonvt.cathode.entity.Comment
import net.simonvt.cathode.entity.User
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns
import net.simonvt.cathode.provider.DatabaseContract.UserColumns
import net.simonvt.cathode.provider.DatabaseSchematic.Tables
import net.simonvt.cathode.provider.util.SqlColumn

object CommentMapper : MappedCursorLiveData.CursorMapper<Comment> {

  override fun map(cursor: Cursor): Comment? {
    return if (cursor.moveToFirst()) mapComment(cursor) else null
  }

  fun mapComment(cursor: Cursor): Comment {
    val username = cursor.getString(UserColumns.USERNAME)
    val name = cursor.getStringOrNull(UserColumns.NAME)
    val userAvatar = cursor.getStringOrNull(UserColumns.AVATAR)
    val user =
      User(
        null,
        username,
        null,
        name,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        userAvatar
      )

    val id = cursor.getLong(CommentColumns.ID)
    val comment = cursor.getString(CommentColumns.COMMENT)
    val spoiler = cursor.getBoolean(CommentColumns.SPOILER)
    val review = cursor.getBoolean(CommentColumns.REVIEW)
    val createdAt = cursor.getLong(CommentColumns.CREATED_AT)
    val replies = cursor.getInt(CommentColumns.REPLIES)
    val likes = cursor.getInt(CommentColumns.LIKED)
    val userRating = cursor.getInt(CommentColumns.USER_RATING)
    val isUserComment = cursor.getBoolean(CommentColumns.IS_USER_COMMENT)
    val liked = cursor.getBoolean(CommentColumns.LIKED)
    val likedAt = cursor.getLong(CommentColumns.LIKED_AT)
    val lastSync = cursor.getLong(CommentColumns.LAST_SYNC)

    return Comment(
      id,
      comment,
      spoiler,
      review,
      -1L,
      createdAt,
      replies,
      likes,
      userRating,
      user,
      isUserComment,
      liked,
      likedAt,
      lastSync
    )
  }

  @JvmField
  val projection = arrayOf(
    SqlColumn.table(Tables.COMMENTS).column(CommentColumns.ID),
    SqlColumn.table(Tables.COMMENTS).column(CommentColumns.COMMENT),
    SqlColumn.table(Tables.COMMENTS).column(CommentColumns.SPOILER),
    SqlColumn.table(Tables.COMMENTS).column(CommentColumns.REVIEW),
    SqlColumn.table(Tables.COMMENTS).column(CommentColumns.CREATED_AT),
    SqlColumn.table(Tables.COMMENTS).column(CommentColumns.REPLIES),
    SqlColumn.table(Tables.COMMENTS).column(CommentColumns.LIKES),
    SqlColumn.table(Tables.COMMENTS).column(CommentColumns.USER_RATING),
    SqlColumn.table(Tables.COMMENTS).column(CommentColumns.IS_USER_COMMENT),
    SqlColumn.table(Tables.COMMENTS).column(CommentColumns.LIKED),
    SqlColumn.table(Tables.COMMENTS).column(CommentColumns.LIKED_AT),
    SqlColumn.table(Tables.COMMENTS).column(CommentColumns.LAST_SYNC),
    SqlColumn.table(Tables.USERS).column(UserColumns.USERNAME),
    SqlColumn.table(Tables.USERS).column(UserColumns.NAME),
    SqlColumn.table(Tables.USERS).column(UserColumns.AVATAR)
  )
}
