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

package net.simonvt.cathode.provider;

import android.content.ContentValues;
import net.simonvt.cathode.api.entity.Comment;

public final class CommentsHelper {

  private CommentsHelper() {
  }

  public static ContentValues getValues(Comment comments) {
    ContentValues values = new ContentValues();

    values.put(DatabaseContract.CommentColumns.ID, comments.getId());
    values.put(DatabaseContract.CommentColumns.COMMENT, comments.getComment());
    values.put(DatabaseContract.CommentColumns.SPOILER, comments.getSpoiler());
    values.put(DatabaseContract.CommentColumns.REVIEW, comments.getReview());
    values.put(DatabaseContract.CommentColumns.PARENT_ID, comments.getParent_id());
    values.put(DatabaseContract.CommentColumns.CREATED_AT,
        comments.getCreated_at().getTimeInMillis());
    values.put(DatabaseContract.CommentColumns.REPLIES, comments.getReplies());
    values.put(DatabaseContract.CommentColumns.LIKES, comments.getLikes());
    values.put(DatabaseContract.CommentColumns.USER_RATING, comments.getUser_rating());

    return values;
  }
}
