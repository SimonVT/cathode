/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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

import androidx.fragment.app.Fragment
import net.simonvt.cathode.api.enumeration.Department
import net.simonvt.cathode.api.enumeration.ItemType

abstract class NavigationListenerActivity : BaseActivity(), NavigationListener {

  override fun onDisplayComments(type: ItemType, itemId: Long) {
    throw RuntimeException("Not implemented")
  }

  override fun onDisplayComment(commentId: Long) {
    throw RuntimeException("Not implemented")
  }

  override fun onDisplayPerson(personId: Long) {
    throw RuntimeException("Not implemented")
  }

  override fun onDisplayPersonCredit(personId: Long, department: Department) {
    throw RuntimeException("Not implemented")
  }

  override fun onDisplayCredit(itemType: ItemType, itemId: Long, department: Department) {
    throw RuntimeException("Not implemented")
  }

  override fun onDisplayCredits(itemType: ItemType, itemId: Long, title: String?) {
    throw RuntimeException("Not implemented")
  }

  override fun displayFragment(clazz: Class<*>, tag: String) {
    throw RuntimeException("Not implemented")
  }

  override fun upFromEpisode(showId: Long, showTitle: String?, seasonId: Long) {
    throw RuntimeException("Not implemented")
  }

  override fun popIfTop(fragment: Fragment) {
    throw RuntimeException("Not implemented")
  }

  override fun isFragmentTopLevel(fragment: Fragment): Boolean {
    throw RuntimeException("Not implemented")
  }

  override fun onShowList(listId: Long, listName: String) {
    throw RuntimeException("Not implemented")
  }

  override fun onListDeleted(listId: Long) {
    throw RuntimeException("Not implemented")
  }

  override fun onDisplayMovie(movieId: Long, title: String?, overview: String?) {
    throw RuntimeException("Not implemented")
  }

  override fun onDisplayRelatedMovies(movieId: Long, title: String?) {
    throw RuntimeException("Not implemented")
  }

  override fun onSelectMovieWatchedDate(movieId: Long, title: String?) {
    throw RuntimeException("Not implemented")
  }

  override fun onDisplayMovieHistory(movieId: Long, title: String?) {
    throw RuntimeException("Not implemented")
  }

  override fun onDisplayShow(showId: Long, title: String?, overview: String?, type: LibraryType) {
    throw RuntimeException("Not implemented")
  }

  override fun onDisplaySeason(
    showId: Long,
    seasonId: Long,
    showTitle: String?,
    seasonNumber: Int,
    type: LibraryType
  ) {
    throw RuntimeException("Not implemented")
  }

  override fun onDisplayEpisode(episodeId: Long, showTitle: String?) {
    throw RuntimeException("Not implemented")
  }

  override fun onDisplayEpisodeHistory(episodeId: Long, showTitle: String) {
    throw RuntimeException("Not implemented")
  }

  override fun onDisplayRelatedShows(showId: Long, title: String?) {
    throw RuntimeException("Not implemented")
  }

  override fun onSelectShowWatchedDate(showId: Long, title: String?) {
    throw RuntimeException("Not implemented")
  }

  override fun onSelectSeasonWatchedDate(seasonId: Long, title: String?) {
    throw RuntimeException("Not implemented")
  }

  override fun onSelectEpisodeWatchedDate(episodeId: Long, title: String?) {
    throw RuntimeException("Not implemented")
  }

  override fun onSelectOlderEpisodeWatchedDate(episodeId: Long, title: String?) {
    throw RuntimeException("Not implemented")
  }

  override fun onHomeClicked() {
    throw RuntimeException("Not implemented")
  }

  override fun onSearchClicked() {
    throw RuntimeException("Not implemented")
  }
}
