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

import com.squareup.okhttp.ResponseBody;
import javax.inject.Inject;
import net.simonvt.cathode.api.service.CommentsService;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import retrofit.Call;

public class LikeCommentJob extends CallJob<ResponseBody> {

  @Inject transient CommentsService commentsService;

  private long commentId;

  public LikeCommentJob(long commentId) {
    super(Flags.REQUIRES_AUTH);
    this.commentId = commentId;
  }

  @Override public String key() {
    return "LikeCommentJob&commentId=" + commentId;
  }

  @Override public boolean allowDuplicates() {
    return true;
  }

  @Override public int getPriority() {
    return PRIORITY_ACTIONS;
  }

  @Override public Call<ResponseBody> getCall() {
    // TODO: Catch 422
    // TOOD: Can the empty body be made unnecessary
    return commentsService.like(commentId, "");
  }

  @Override public void handleResponse(ResponseBody response) {
  }
}
