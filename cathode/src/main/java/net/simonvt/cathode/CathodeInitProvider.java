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

import android.os.StrictMode;
import dagger.android.AndroidInjection;
import net.simonvt.cathode.common.InitProvider;
import net.simonvt.cathode.settings.Accounts;
import net.simonvt.cathode.settings.FirstAiredOffsetPreference;
import net.simonvt.cathode.settings.UpcomingTimePreference;
import net.simonvt.cathode.ui.shows.upcoming.UpcomingSortByPreference;
import timber.log.Timber;

public class CathodeInitProvider extends InitProvider {

  @Override public boolean onCreate() {
    if (BuildConfig.DEBUG) {
      Timber.plant(new Timber.DebugTree());

      StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
      StrictMode.setThreadPolicy(
          new StrictMode.ThreadPolicy.Builder().detectAll().permitDiskReads().penaltyLog().build());
    } else {
      Timber.plant(new CrashlyticsTree());
    }

    ((CathodeApp) getContext().getApplicationContext()).ensureInjection();
    AndroidInjection.inject(this);

    UpcomingSortByPreference.init(getContext());
    UpcomingTimePreference.init(getContext());
    FirstAiredOffsetPreference.init(getContext());

    Upgrader.upgrade(getContext());

    Accounts.setupAccount(getContext());

    return true;
  }
}
