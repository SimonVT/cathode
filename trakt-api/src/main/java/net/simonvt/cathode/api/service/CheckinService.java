/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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

package net.simonvt.cathode.api.service;

import com.squareup.okhttp.ResponseBody;
import net.simonvt.cathode.api.body.CheckinItem;
import net.simonvt.cathode.api.entity.CheckinResponse;
import retrofit.Call;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.POST;

public interface CheckinService {

  @POST("/checkin") Call<CheckinResponse> checkin(@Body CheckinItem item);

  @DELETE("/checkin") Call<ResponseBody> deleteCheckin();
}
