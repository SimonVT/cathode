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

import java.io.IOException;
import javax.inject.Inject;
import net.simonvt.cathode.api.body.CommentBody;
import net.simonvt.cathode.api.entity.Comment;
import net.simonvt.cathode.api.service.CommentsService;
import net.simonvt.cathode.common.event.RequestFailedEvent;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.sync.SyncUserActivity;
import net.simonvt.cathode.sync.R;
import retrofit2.Call;
import retrofit2.Response;

public class CommentReplyJob extends CallJob<Comment> {

  @Inject transient CommentsService commentsService;

  private long parentId;
  private String comment;
  private Boolean spoiler;

  public CommentReplyJob(long parentId, String comment, boolean spoiler) {

    this.parentId = parentId;
    this.comment = comment;
    this.spoiler = spoiler;
  }

  @Override public String key() {
    return "CommentReplyJob?parentId=" + parentId + "&comment=" + comment;
  }

  @Override public Call<Comment> getCall() {
    CommentBody body = new CommentBody(comment, spoiler, null, null, null);
    return commentsService.reply(parentId, body);
  }

  @Override protected boolean isError(Response<Comment> response) throws IOException {
    final int statusCode = response.code();
    if (statusCode == 422) {
      RequestFailedEvent.post(R.string.comment_submit_error);
      return false;
    }

    return super.isError(response);
  }

  @Override public boolean handleResponse(Comment comment) {
    queue(new SyncUserActivity());
    return true;
  }
}
