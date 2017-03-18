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

import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.entity.Comment;
import net.simonvt.cathode.api.service.CommentsService;
import net.simonvt.cathode.event.RequestFailedEvent;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class DeleteCommentJob extends CallJob<ResponseBody> {

  @Inject transient CommentsService commentsService;

  private long commentId;

  public DeleteCommentJob(long commentId) {
    super(Flags.REQUIRES_AUTH);
    this.commentId = commentId;
  }

  @Override public String key() {
    return "DeleteCommentJob&commentId=" + commentId;
  }

  @Override public boolean allowDuplicates() {
    return true;
  }

  @Override public int getPriority() {
    return PRIORITY_ACTIONS;
  }

  @Override public Call<ResponseBody> getCall() {
    return commentsService.delete(commentId);
  }

  @Override public void handleResponse(ResponseBody response) {
  }
}
