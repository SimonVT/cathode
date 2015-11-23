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

package net.simonvt.cathode.remote.action.movies;

import net.simonvt.cathode.api.entity.HideResponse;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import retrofit2.Call;

public class CalendarHideMovie extends CallJob<HideResponse> {

  private long movieId;

  private boolean hidden;

  public CalendarHideMovie(long movieId, boolean hidden) {
    super(Flags.REQUIRES_AUTH);
    this.movieId = movieId;
    this.hidden = hidden;
  }

  @Override public String key() {
    return "CalendarHideMovie?movieId=" + movieId + "&hidden" + hidden;
  }

  @Override public boolean allowDuplicates() {
    return true;
  }

  @Override public int getPriority() {
    return PRIORITY_ACTIONS;
  }

  @Override public Call<HideResponse> getCall() {
    return null;
  }

  @Override public void handleResponse(HideResponse response) {
    // TODO: Wait for trakt support
  }
}
