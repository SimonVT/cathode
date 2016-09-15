/*
 * Copyright (C) 2014 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by contextlicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.simonvt.cathode.module;

import android.content.Context;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import net.simonvt.cathode.scheduler.CommentsTaskScheduler;
import net.simonvt.cathode.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.scheduler.ListsTaskScheduler;
import net.simonvt.cathode.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.scheduler.PersonTaskScheduler;
import net.simonvt.cathode.scheduler.SearchTaskScheduler;
import net.simonvt.cathode.scheduler.SeasonTaskScheduler;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;

@Module(
    complete = false,
    library = true)
public class SchedulerModule {

  @Provides @Singleton EpisodeTaskScheduler provideEpisodeScheduler(Context context) {
    return new EpisodeTaskScheduler(context);
  }

  @Provides @Singleton SeasonTaskScheduler provideSeasonScheduler(Context context) {
    return new SeasonTaskScheduler(context);
  }

  @Provides @Singleton ShowTaskScheduler provideShowScheduler(Context context) {
    return new ShowTaskScheduler(context);
  }

  @Provides @Singleton MovieTaskScheduler provideMovieScheduler(Context context) {
    return new MovieTaskScheduler(context);
  }

  @Provides @Singleton SearchTaskScheduler provideSearchScheduler(Context context) {
    return new SearchTaskScheduler(context);
  }

  @Provides @Singleton ListsTaskScheduler provideListsTaskSheduler(Context context) {
    return new ListsTaskScheduler(context);
  }

  @Provides @Singleton CommentsTaskScheduler provideCommentsTaskScheduler(Context context) {
    return new CommentsTaskScheduler(context);
  }

  @Provides @Singleton PersonTaskScheduler providePersonTaskScheduler(Context context) {
    return new PersonTaskScheduler(context);
  }
}
