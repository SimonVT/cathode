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

package net.simonvt.cathode.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.entity.Movie
import net.simonvt.cathode.entity.ShowWithEpisode
import net.simonvt.cathode.entitymapper.MovieMapper
import net.simonvt.cathode.entitymapper.ShowWithEpisodeMapper
import net.simonvt.cathode.provider.ProviderSchematic.Movies
import net.simonvt.cathode.provider.ProviderSchematic.Shows

class HomeViewModel(application: Application) : AndroidViewModel(application) {

  val watchingShow: LiveData<ShowWithEpisode>
  val watchingMovie: LiveData<Movie>

  init {
    watchingShow = MappedCursorLiveData(
      application,
      Shows.SHOW_WATCHING,
      ShowWithEpisodeMapper.projection,
      mapper = ShowWithEpisodeMapper,
      allowNulls = true
    )
    watchingMovie = MappedCursorLiveData(
      application,
      Movies.WATCHING,
      MovieMapper.projection,
      mapper = MovieMapper,
      allowNulls = true
    )
  }
}
