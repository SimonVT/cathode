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

package net.simonvt.cathode.ui.shows.upcoming;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import javax.inject.Inject;
import net.simonvt.cathode.settings.UpcomingTimePreference;
import net.simonvt.cathode.ui.show.EpisodeHistoryViewModel;

public class UpcomingViewModelFactory extends ViewModelProvider.NewInstanceFactory {

  private Application application;
  private UpcomingTimePreference upcomingTimePreference;
  private UpcomingSortByPreference upcomingSortByPreference;

  @Inject public UpcomingViewModelFactory(Application application,
      UpcomingTimePreference upcomingTimePreference,
      UpcomingSortByPreference upcomingSortByPreference) {
    this.application = application;
    this.upcomingTimePreference = upcomingTimePreference;
    this.upcomingSortByPreference = upcomingSortByPreference;
  }

  @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
    if (modelClass.isAssignableFrom(UpcomingViewModel.class)) {
      return (T) new UpcomingViewModel(application, upcomingTimePreference,
          upcomingSortByPreference);
    }

    throw new RuntimeException("Unknown type " + modelClass.getCanonicalName());
  }
}
