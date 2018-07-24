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

package net.simonvt.cathode.ui;

import android.app.Application;
import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import net.simonvt.cathode.common.data.CursorLiveData;
import net.simonvt.cathode.provider.ProviderSchematic;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;

public class HomeViewModel extends AndroidViewModel {

  private LiveData<Cursor> watchingShow;
  private LiveData<Cursor> watchingMovie;

  public HomeViewModel(@NonNull Application application) {
    super(application);
    watchingShow =
        new CursorLiveData(application, Shows.SHOW_WATCHING, HomeActivity.SHOW_WATCHING_PROJECTION,
            null, null, null);
    watchingMovie =
        new CursorLiveData(application, ProviderSchematic.Movies.WATCHING, null, null, null, null);
  }

  public LiveData<Cursor> getWatchingShow() {
    return watchingShow;
  }

  public LiveData<Cursor> getWatchingMovie() {
    return watchingMovie;
  }
}
