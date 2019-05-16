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

package net.simonvt.cathode.settings.link

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper
import net.simonvt.cathode.provider.helper.PersonDatabaseHelper
import net.simonvt.cathode.provider.helper.SeasonDatabaseHelper
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import javax.inject.Inject

class TraktLinkSyncViewModel @Inject constructor(
  application: Application,
  showHelper: ShowDatabaseHelper,
  seasonHelper: SeasonDatabaseHelper,
  episodeHelper: EpisodeDatabaseHelper,
  movieHelper: MovieDatabaseHelper,
  personHelper: PersonDatabaseHelper
) : AndroidViewModel(application) {

  val localState = LocalStateLiveData(
    application,
    showHelper,
    seasonHelper,
    episodeHelper,
    movieHelper,
    personHelper
  )
}
