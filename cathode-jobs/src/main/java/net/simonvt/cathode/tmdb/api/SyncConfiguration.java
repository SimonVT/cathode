/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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

package net.simonvt.cathode.tmdb.api;

import com.uwetrottmann.tmdb2.entities.Configuration;
import com.uwetrottmann.tmdb2.services.ConfigurationService;
import javax.inject.Inject;
import net.simonvt.cathode.images.ImageSettings;
import net.simonvt.cathode.jobqueue.JobPriority;
import retrofit2.Call;

public class SyncConfiguration extends TmdbCallJob<Configuration> {

  @Inject transient ConfigurationService configurationService;

  @Override public String key() {
    return "SyncConfiguration";
  }

  @Override public int getPriority() {
    return JobPriority.CONFIGURATION;
  }

  @Override public Call<Configuration> getCall() {
    return configurationService.configuration();
  }

  @Override public boolean handleResponse(Configuration configuration) {
    ImageSettings.updateTmdbConfiguration(getContext(), configuration);
    return true;
  }
}
