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

package net.simonvt.cathode.entitymapper;

import android.database.Cursor;
import net.simonvt.cathode.common.data.MappedCursorLiveData;
import net.simonvt.cathode.common.database.Cursors;
import net.simonvt.cathode.common.entity.Comment;
import net.simonvt.cathode.common.entity.User;
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns;
import net.simonvt.cathode.provider.DatabaseContract.UserColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.util.SqlColumn;

public class CommentMapper implements MappedCursorLiveData.CursorMapper<Comment> {

  public static final String[] PROJECTION = new String[] {
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
      SqlColumn.table(Tables.USERS).column(UserColumns.AVATAR),
  };

  @Override public Comment map(Cursor cursor) {
    if (cursor.moveToFirst()) {
      return mapComment(cursor);
    }

    return null;
  }

  public static Comment mapComment(Cursor cursor) {
    String username = Cursors.getString(cursor, UserColumns.USERNAME);
    String name = Cursors.getString(cursor, UserColumns.NAME);
    String userAvatar = Cursors.getString(cursor, UserColumns.AVATAR);
    final User user =
        new User(null, username, null, name, null, null, null, null, null, null, null, userAvatar);

    long id = Cursors.getLong(cursor, CommentColumns.ID);
    String comment = Cursors.getString(cursor, CommentColumns.COMMENT);
    boolean spoiler = Cursors.getBoolean(cursor, CommentColumns.SPOILER);
    boolean review = Cursors.getBoolean(cursor, CommentColumns.REVIEW);
    long createdAt = Cursors.getLong(cursor, CommentColumns.CREATED_AT);
    int replies = Cursors.getInt(cursor, CommentColumns.REPLIES);
    int likes = Cursors.getInt(cursor, CommentColumns.LIKED);
    int userRating = Cursors.getInt(cursor, CommentColumns.USER_RATING);
    boolean isUserComment = Cursors.getBoolean(cursor, CommentColumns.IS_USER_COMMENT);
    boolean liked = Cursors.getBoolean(cursor, CommentColumns.LIKED);
    long likedAt = Cursors.getLong(cursor, CommentColumns.LIKED_AT);
    long lastSync = Cursors.getLong(cursor, CommentColumns.LAST_SYNC);

    return new Comment(id, comment, spoiler, review, -1L, createdAt, replies, likes, userRating,
        user, isUserComment, liked, likedAt, lastSync);
  }
}
