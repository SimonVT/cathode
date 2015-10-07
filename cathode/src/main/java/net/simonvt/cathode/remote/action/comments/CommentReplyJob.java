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

package net.simonvt.cathode.remote.action.comments;

import android.content.ContentValues;
import android.database.Cursor;
import javax.inject.Inject;
import net.simonvt.cathode.api.body.CommentBody;
import net.simonvt.cathode.api.entity.Comment;
import net.simonvt.cathode.api.service.CommentsService;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.provider.CommentsHelper;
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Comments;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.cathode.util.Cursors;
import timber.log.Timber;

public class CommentReplyJob extends Job {

  @Inject transient CommentsService commentsService;

  private long parentId;

  private String comment;

  private Boolean spoiler;

  public CommentReplyJob(long parentId, String comment, boolean spoiler) {
    super(Flags.REQUIRES_AUTH);
    this.parentId = parentId;
    this.comment = comment;
    this.spoiler = spoiler;
  }

  @Override public String key() {
    return "CommentReplyJob?parentId=" + parentId + "&comment=" + comment;
  }

  @Override public boolean allowDuplicates() {
    return true;
  }

  @Override public int getPriority() {
    return PRIORITY_ACTIONS;
  }

  @Override public void perform() {
    // TODO: Catch 422
    CommentBody body = CommentBody.comment(comment);
    if (spoiler) {
      body.spoiler();
    }

    Comment comment = commentsService.reply(parentId, body);
    ContentValues values = CommentsHelper.getValues(comment);
    values.put(CommentColumns.IS_USER_COMMENT, true);

    int itemType;
    long itemId;
    Cursor c = getContentResolver().query(Comments.withId(parentId), new String[] {
        CommentColumns.ITEM_TYPE, CommentColumns.ITEM_ID
    }, null, null, null);
    if (c.moveToFirst()) {
      itemType = Cursors.getInt(c, CommentColumns.ITEM_TYPE);
      itemId = Cursors.getLong(c, CommentColumns.ITEM_ID);
    } else {
      Timber.e(new RuntimeException(), "Comment parent not found");
      return;
    }
    c.close();

    values.put(CommentColumns.ITEM_TYPE, itemType);
    values.put(CommentColumns.ITEM_ID, itemId);

    getContentResolver().insert(Comments.COMMENTS, values);
  }
}
