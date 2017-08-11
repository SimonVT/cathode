/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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

package net.simonvt.cathode;

import android.content.Context;
import android.os.StrictMode;
import com.crashlytics.android.Crashlytics;
import dagger.ObjectGraph;
import io.fabric.sdk.android.Fabric;
import javax.inject.Inject;
import net.simonvt.cathode.common.InitProvider;
import net.simonvt.cathode.common.util.MainHandler;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.settings.FirstAiredOffsetPreference;
import net.simonvt.cathode.settings.UpcomingTimePreference;
import net.simonvt.cathode.ui.shows.upcoming.UpcomingSortByPreference;
import timber.log.Timber;

public class CathodeInitProvider extends InitProvider {

  @Inject JobManager jobManager;

  public static void ensureInjector(Context context) {
    if (!Injector.isInstalled()) {
      Injector.install(ObjectGraph.create(Modules.list(context)));
    }
  }

  @Override public boolean onCreate() {
    if (BuildConfig.DEBUG) {
      Timber.plant(new Timber.DebugTree());

      StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
      StrictMode.setThreadPolicy(
          new StrictMode.ThreadPolicy.Builder().detectAll().permitDiskReads().penaltyLog().build());
    } else {
      Fabric.with(getContext(), new Crashlytics());
      Timber.plant(new CrashlyticsTree());
    }

    UpcomingSortByPreference.init(getContext());
    UpcomingTimePreference.init(getContext());
    FirstAiredOffsetPreference.init(getContext());

    Upgrader.upgrade(getContext(), new Upgrader.JobQueue() {
      @Override public void add(final Job job) {
        MainHandler.post(new Runnable() {
          @Override public void run() {
            jobManager.addJob(job);
          }
        });
      }
    });

    ensureInjector(getContext());
    Injector.inject(this);

    return true;
  }
}
