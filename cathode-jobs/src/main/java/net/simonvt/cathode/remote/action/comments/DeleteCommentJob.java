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
import net.simonvt.cathode.api.service.CommentsService;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.cathode.remote.sync.SyncUserActivity;
import net.simonvt.cathode.remote.sync.comments.SyncUserComments;
import okhttp3.ResponseBody;
import retrofit2.Call;

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
    return JobPriority.ACTIONS;
  }

  @Override public Call<ResponseBody> getCall() {
    return commentsService.delete(commentId);
  }

  @Override public boolean handleResponse(ResponseBody response) {
    queue(new SyncUserActivity());
    return true;
  }
}
