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

package net.simonvt.cathode.sync;

import android.content.Context;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import net.simonvt.cathode.jobqueue.JobInjector;
import net.simonvt.cathode.sync.trakt.CheckIn;
import net.simonvt.cathode.sync.trakt.UserList;

@Module(complete = false, library = true) public class JobsModule {

  @Provides @Singleton JobInjector provideJobInjector(final Context context) {
    return JobInjectorImpl.getInstance(context);
  }

  @Provides @Singleton CheckIn provideCheckIn() {
    return new CheckIn();
  }

  @Provides @Singleton UserList provideUserList() {
    return new UserList();
  }
}
