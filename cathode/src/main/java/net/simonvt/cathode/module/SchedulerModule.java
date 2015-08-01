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

package net.simonvt.cathode.module;

import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.scheduler.ListsTaskScheduler;
import net.simonvt.cathode.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.scheduler.SearchTaskScheduler;
import net.simonvt.cathode.scheduler.SeasonTaskScheduler;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;

@Module(
    complete = false,
    library = true)
public class SchedulerModule {

  @Provides @Singleton EpisodeTaskScheduler provideEpisodeScheduler(CathodeApp app) {
    return new EpisodeTaskScheduler(app);
  }

  @Provides @Singleton SeasonTaskScheduler provideSeasonScheduler(CathodeApp app) {
    return new SeasonTaskScheduler(app);
  }

  @Provides @Singleton ShowTaskScheduler provideShowScheduler(CathodeApp app) {
    return new ShowTaskScheduler(app);
  }

  @Provides @Singleton MovieTaskScheduler provideMovieScheduler(CathodeApp app) {
    return new MovieTaskScheduler(app);
  }

  @Provides @Singleton SearchTaskScheduler provideSearchScheduler(CathodeApp app) {
    return new SearchTaskScheduler(app);
  }

  @Provides @Singleton ListsTaskScheduler provideListsTaskSheduler(CathodeApp app) {
    return new ListsTaskScheduler(app);
  }
}
