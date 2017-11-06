/*
 * Copyright (C) 2018 Simon Vig Therkildsen
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

package net.simonvt.cathode.provider;

import android.content.Context;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper;
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper;
import net.simonvt.cathode.provider.helper.PersonDatabaseHelper;
import net.simonvt.cathode.provider.helper.SearchDatabaseHelper;
import net.simonvt.cathode.provider.helper.SeasonDatabaseHelper;
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper;
import net.simonvt.cathode.provider.helper.UserDatabaseHelper;

@Module public class DatabaseHelperModule {

  @Provides @Singleton EpisodeDatabaseHelper episodeDatabaseHelper(Context context,
      ShowDatabaseHelper showHelper, SeasonDatabaseHelper seasonHelper) {
    return new EpisodeDatabaseHelper(context, showHelper, seasonHelper);
  }

  @Provides @Singleton MovieDatabaseHelper movieDatabaseHelper(Context context) {
    return new MovieDatabaseHelper(context);
  }

  @Provides @Singleton PersonDatabaseHelper personDatabaseHelper(Context context) {
    return new PersonDatabaseHelper(context);
  }

  @Provides @Singleton SearchDatabaseHelper searchDatabaseHelper(Context context) {
    return new SearchDatabaseHelper(context);
  }

  @Provides @Singleton SeasonDatabaseHelper seasonDatabaseHelper(Context context,
      ShowDatabaseHelper showDatabaseHelper) {
    return new SeasonDatabaseHelper(context, showDatabaseHelper);
  }

  @Provides @Singleton ShowDatabaseHelper showDatabaseHelper(Context context) {
    return new ShowDatabaseHelper(context);
  }

  @Provides @Singleton UserDatabaseHelper userDatabaseHelper(Context context) {
    return new UserDatabaseHelper(context);
  }
}
