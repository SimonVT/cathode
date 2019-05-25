/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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
package net.simonvt.cathode.remote.action.shows;

import javax.inject.Inject;
import net.simonvt.cathode.api.service.RecommendationsService;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.sync.SyncUserActivity;
import okhttp3.ResponseBody;
import retrofit2.Call;

public class DismissShowRecommendation extends CallJob<ResponseBody> {

  @Inject transient RecommendationsService recommendationsService;

  private long traktId;

  public DismissShowRecommendation(long traktId) {

    this.traktId = traktId;
  }

  @Override public String key() {
    return "DismissShowRecommendation" + "&traktId=" + traktId;
  }

  @Override public Call<ResponseBody> getCall() {
    return recommendationsService.dismissShow(traktId);
  }

  @Override public boolean handleResponse(ResponseBody response) {
    queue(new SyncUserActivity());
    return true;
  }
}
