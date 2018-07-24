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
import androidx.lifecycle.AndroidViewModel;
import net.simonvt.cathode.common.data.CursorLiveData;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.settings.UpcomingTime;
import net.simonvt.cathode.settings.UpcomingTimePreference;

public class UpcomingViewModel extends AndroidViewModel {

  private CursorLiveData shows;

  private UpcomingTimePreference upcomingTimePreference;
  private UpcomingSortByPreference upcomingSortByPreference;

  public UpcomingViewModel(@NonNull Application application,
      UpcomingTimePreference upcomingTimePreference,
      UpcomingSortByPreference upcomingSortByPreference) {
    super(application);
    this.upcomingTimePreference = upcomingTimePreference;
    this.upcomingSortByPreference = upcomingSortByPreference;
    upcomingTimePreference.registerListener(upcomingTimeChangeListener);
    upcomingSortByPreference.registerListener(upcomingSortByListener);
    shows = new CursorLiveData(application, Shows.SHOWS_UPCOMING, UpcomingAdapter.PROJECTION, null,
        null, upcomingSortByPreference.get().getSortOrder());
  }

  @Override protected void onCleared() {
    upcomingTimePreference.unregisterListener(upcomingTimeChangeListener);
    upcomingSortByPreference.unregisterListener(upcomingSortByListener);
  }

  private UpcomingTimePreference.UpcomingTimeChangeListener upcomingTimeChangeListener =
      new UpcomingTimePreference.UpcomingTimeChangeListener() {
        @Override public void onUpcomingTimeChanged(UpcomingTime upcomingTime) {
          shows.loadData();
        }
      };

  private UpcomingSortByPreference.UpcomingSortByListener upcomingSortByListener =
      new UpcomingSortByPreference.UpcomingSortByListener() {
        @Override public void onUpcomingSortByChanged(UpcomingSortBy sortBy) {
          shows.setSortOrder(sortBy.getSortOrder());
        }
      };

  public CursorLiveData getShows() {
    return shows;
  }
}
