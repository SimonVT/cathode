/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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

package net.simonvt.cathode.ui.comments;

import android.content.Context;
import android.database.Cursor;
import net.simonvt.cathode.common.data.ListenableLiveData;
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.ProviderSchematic.Comments;
import net.simonvt.cathode.common.database.DatabaseUtils;
import net.simonvt.cathode.common.database.SimpleMergeCursor;

public class CommentAndRepliesLiveData extends ListenableLiveData<Cursor> {

  private long commentId;

  public CommentAndRepliesLiveData(Context context, long commentId) {
    super(context);
    this.commentId = commentId;
  }

  @Override protected SimpleMergeCursor loadInBackground() {
    Cursor comment = getContext().getContentResolver()
        .query(Comments.COMMENTS_WITH_PROFILE, CommentsAdapter.PROJECTION,
            Tables.COMMENTS + "." + CommentColumns.ID + "=?", new String[] {
                String.valueOf(commentId),
            }, null);
    Cursor replies = getContext().getContentResolver()
        .query(Comments.withParent(commentId), CommentsAdapter.PROJECTION, null, null,
            CommentColumns.CREATED_AT + " DESC");

    addNotificationUri(DatabaseUtils.getNotificationUri(comment));
    addNotificationUri(DatabaseUtils.getNotificationUri(replies));

    return new SimpleMergeCursor(comment, replies);
  }
}
