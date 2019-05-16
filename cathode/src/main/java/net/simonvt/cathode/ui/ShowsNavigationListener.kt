/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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

import net.simonvt.cathode.common.ui.FragmentCallbacks

interface ShowsNavigationListener : NavigationClickListener, FragmentCallbacks {

  fun onDisplayShow(showId: Long, title: String?, overview: String?, type: LibraryType)

  fun onDisplaySeason(
    showId: Long,
    seasonId: Long,
    showTitle: String?,
    seasonNumber: Int,
    type: LibraryType
  )

  fun onDisplayEpisode(episodeId: Long, showTitle: String?)

  fun onDisplayEpisodeHistory(episodeId: Long, showTitle: String)

  fun onDisplayRelatedShows(showId: Long, title: String?)

  fun onSelectShowWatchedDate(showId: Long, title: String?)

  fun onSelectSeasonWatchedDate(seasonId: Long, title: String?)

  fun onSelectEpisodeWatchedDate(episodeId: Long, title: String?)

  fun onSelectOlderEpisodeWatchedDate(episodeId: Long, title: String?)
}
