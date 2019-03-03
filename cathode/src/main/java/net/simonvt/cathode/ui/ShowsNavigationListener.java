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
package net.simonvt.cathode.ui;

import net.simonvt.cathode.common.ui.FragmentCallbacks;

public interface ShowsNavigationListener extends NavigationClickListener, FragmentCallbacks {

  void onDisplayShow(long showId, String title, String overview, LibraryType type);

  void onDisplaySeason(long showId, long seasonId, String showTitle, int seasonNumber,
      LibraryType type);

  void onDisplayEpisode(long episodeId, String showTitle);

  void onDisplayEpisodeHistory(long episodeId, String showTitle);

  void onDisplayRelatedShows(long showId, String title);

  void onSelectShowWatchedDate(long showId, String title);

  void onSelectSeasonWatchedDate(long seasonId, String title);

  void onSelectEpisodeWatchedDate(long episodeId, String title);

  void onSelectOlderEpisodeWatchedDate(long episodeId, String title);
}
