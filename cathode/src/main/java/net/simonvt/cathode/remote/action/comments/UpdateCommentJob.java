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
import javax.inject.Inject;
import net.simonvt.cathode.api.body.CommentBody;
import net.simonvt.cathode.api.entity.Comment;
import net.simonvt.cathode.api.service.CommentsService;
import net.simonvt.cathode.provider.CommentsHelper;
import net.simonvt.cathode.provider.ProviderSchematic.Comments;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import retrofit2.Call;

public class UpdateCommentJob extends CallJob<Comment> {

  @Inject transient CommentsService commentsService;

  private long commentId;

  private String comment;

  private Boolean spoiler;

  public UpdateCommentJob(long commentId, String comment, boolean spoiler) {
    super(Flags.REQUIRES_AUTH);
    this.commentId = commentId;
    this.comment = comment;
    this.spoiler = spoiler;
  }

  @Override public String key() {
    return "UpdateCommentJob&commentId="
        + commentId
        + "&comment="
        + comment
        + "&spoiler="
        + spoiler;
  }

  @Override public boolean allowDuplicates() {
    return true;
  }

  @Override public int getPriority() {
    return PRIORITY_ACTIONS;
  }

  @Override public Call<Comment> getCall() {
    // TODO: Catch 422
    CommentBody body = CommentBody.comment(comment);
    if (spoiler) {
      body.spoiler();
    }

    return commentsService.update(commentId, body);
  }

  @Override public void handleResponse(Comment comment) {
    ContentValues values = CommentsHelper.getValues(comment);
    getContentResolver().update(Comments.withId(commentId), values, null, null);
  }
}
