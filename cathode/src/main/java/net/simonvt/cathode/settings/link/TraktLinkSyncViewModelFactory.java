/*
 * Copyright (C) 2019 Simon Vig Therkildsen
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

package net.simonvt.cathode.settings.link;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import javax.inject.Inject;
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper;
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper;
import net.simonvt.cathode.provider.helper.PersonDatabaseHelper;
import net.simonvt.cathode.provider.helper.SeasonDatabaseHelper;
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper;

public class TraktLinkSyncViewModelFactory extends ViewModelProvider.NewInstanceFactory {

  private Application application;
  private ShowDatabaseHelper showHelper;
  private SeasonDatabaseHelper seasonHelper;
  private EpisodeDatabaseHelper episodeHelper;
  private MovieDatabaseHelper movieHelper;
  private PersonDatabaseHelper personHelper;

  @Inject
  public TraktLinkSyncViewModelFactory(Application application, ShowDatabaseHelper showHelper,
      SeasonDatabaseHelper seasonHelper, EpisodeDatabaseHelper episodeHelper,
      MovieDatabaseHelper movieHelper, PersonDatabaseHelper personHelper) {
    this.application = application;
    this.showHelper = showHelper;
    this.seasonHelper = seasonHelper;
    this.episodeHelper = episodeHelper;
    this.movieHelper = movieHelper;
    this.personHelper = personHelper;
  }

  @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
    if (modelClass.isAssignableFrom(TraktLinkSyncViewModel.class)) {
      return (T) new TraktLinkSyncViewModel(application, showHelper, seasonHelper, episodeHelper,
          movieHelper, personHelper);
    }

    throw new RuntimeException("Unknown type " + modelClass.getCanonicalName());
  }
}
