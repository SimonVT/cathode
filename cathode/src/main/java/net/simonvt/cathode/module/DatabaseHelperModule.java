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

package net.simonvt.cathode.module;

import android.content.Context;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import net.simonvt.cathode.provider.EpisodeDatabaseHelper;
import net.simonvt.cathode.provider.MovieDatabaseHelper;
import net.simonvt.cathode.provider.PersonDatabaseHelper;
import net.simonvt.cathode.provider.SearchDatabaseHelper;
import net.simonvt.cathode.provider.SeasonDatabaseHelper;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import net.simonvt.cathode.provider.UserDatabaseHelper;

@Module(
    complete = false,
    library = true)
public class DatabaseHelperModule {

  @Provides @Singleton ShowDatabaseHelper provideShowDatabaseHelper(Context context) {
    return ShowDatabaseHelper.getInstance(context);
  }

  @Provides @Singleton SeasonDatabaseHelper provideSeasonDatabaseHelper(Context context) {
    return SeasonDatabaseHelper.getInstance(context);
  }

  @Provides @Singleton EpisodeDatabaseHelper provideEpisodeDatabaseHelper(Context context) {
    return EpisodeDatabaseHelper.getInstance(context);
  }

  @Provides @Singleton MovieDatabaseHelper provideMovieDatabaseHelper(Context context) {
    return MovieDatabaseHelper.getInstance(context);
  }

  @Provides @Singleton UserDatabaseHelper provideUserDatabaseHelper(Context context) {
    return UserDatabaseHelper.getInstance(context);
  }

  @Provides @Singleton SearchDatabaseHelper provideSearchDatabaseHelper(Context context) {
    return SearchDatabaseHelper.getInstance(context);
  }

  @Provides @Singleton PersonDatabaseHelper providePersonDatabaseHelper(Context context) {
    return PersonDatabaseHelper.getInstance(context);
  }
}
