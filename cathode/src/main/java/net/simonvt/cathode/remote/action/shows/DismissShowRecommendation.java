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
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.remote.Flags;

public class DismissShowRecommendation extends Job {

  @Inject transient RecommendationsService recommendationsService;

  private long traktId;

  public DismissShowRecommendation(long traktId) {
    super(Flags.REQUIRES_AUTH);
    this.traktId = traktId;
  }

  @Override public String key() {
    return "DismissShowRecommendation" + "&traktId=" + traktId;
  }

  @Override public int getPriority() {
    return PRIORITY_5;
  }

  @Override public boolean requiresWakelock() {
    return true;
  }

  @Override public void perform() {
    recommendationsService.dismissShow(traktId);
  }
}
