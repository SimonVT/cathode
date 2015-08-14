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

package net.simonvt.cathode.remote;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import javax.inject.Inject;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.ProviderSchematic.People;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.remote.sync.SyncJob;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.settings.TraktTimestamps;

public class InitialSyncJob extends Job {

  @Inject transient JobManager jobManager;

  @Override public String key() {
    return "InitialSyncJob";
  }

  @Override public int getPriority() {
    return 1000;
  }

  @Override public void perform() {
    jobManager.clear();

    Settings.clearProfile(getContext());
    TraktTimestamps.clear(getContext());

    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
    settings.edit().putBoolean(Settings.INITIAL_SYNC, true).apply();

    getContentResolver().delete(Shows.SHOWS, null, null);
    getContentResolver().delete(Movies.MOVIES, null, null);
    getContentResolver().delete(People.PEOPLE, null, null);

    queue(new SyncJob());
  }
}
