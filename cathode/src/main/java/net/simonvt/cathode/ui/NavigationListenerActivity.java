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
package net.simonvt.cathode.ui;

import android.support.v4.app.Fragment;
import net.simonvt.cathode.api.enumeration.Department;
import net.simonvt.cathode.api.enumeration.ItemType;

public class NavigationListenerActivity extends BaseActivity implements NavigationListener {

  @Override public void onDisplayComments(ItemType type, long itemId) {
    throw new RuntimeException("Not implemented");
  }

  @Override public void onDisplayComment(long commentId) {
    throw new RuntimeException("Not implemented");
  }

  @Override public void onDisplayPerson(long personId) {
    throw new RuntimeException("Not implemented");
  }

  @Override public void onDisplayPersonCredit(long personId, Department department) {
    throw new RuntimeException("Not implemented");
  }

  @Override public void onDisplayCredit(ItemType itemType, long itemId, Department department) {
    throw new RuntimeException("Not implemented");
  }

  @Override public void onDisplayCredits(ItemType itemType, long itemId, String title) {
    throw new RuntimeException("Not implemented");
  }

  @Override public void displayFragment(Class clazz, String tag) {
    throw new RuntimeException("Not implemented");
  }

  @Override public void upFromEpisode(long showId, String showTitle, long seasonId) {
    throw new RuntimeException("Not implemented");
  }

  @Override public void popIfTop(Fragment fragment) {
    throw new RuntimeException("Not implemented");
  }

  @Override public boolean isFragmentTopLevel(Fragment fragment) {
    throw new RuntimeException("Not implemented");
  }

  @Override public void onShowList(long listId, String listName) {
    throw new RuntimeException("Not implemented");
  }

  @Override public void onListDeleted(long listId) {
    throw new RuntimeException("Not implemented");
  }

  @Override public void onDisplayMovie(long movieId, String title, String overview) {
    throw new RuntimeException("Not implemented");
  }

  @Override public void onDisplayRelatedMovies(long movieId, String title) {
    throw new RuntimeException("Not implemented");
  }

  @Override public void onSelectMovieWatchedDate(long movieId, String title) {
    throw new RuntimeException("Not implemented");
  }

  @Override public void onDisplayMovieHistory(long movieId, String title) {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public void onDisplayShow(long showId, String title, String overview, LibraryType type) {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public void onDisplaySeason(long showId, long seasonId, String showTitle, int seasonNumber,
      LibraryType type) {
    throw new RuntimeException("Not implemented");
  }

  @Override public void onDisplayEpisode(long episodeId, String showTitle) {
    throw new RuntimeException("Not implemented");
  }

  @Override public void onDisplayEpisodeHistory(long episodeId, String showTitle) {
    throw new RuntimeException("Not implemented");
  }

  @Override public void onDisplayRelatedShows(long showId, String title) {
    throw new RuntimeException("Not implemented");
  }

  @Override public void onSelectShowWatchedDate(long showId, String title) {
    throw new RuntimeException("Not implemented");
  }

  @Override public void onSelectSeasonWatchedDate(long seasonId, String title) {
    throw new RuntimeException("Not implemented");
  }

  @Override public void onSelectEpisodeWatchedDate(long episodeId, String title) {
    throw new RuntimeException("Not implemented");
  }

  @Override public void onHomeClicked() {
    throw new RuntimeException("Not implemented");
  }

  @Override public void onSearchClicked() {
    throw new RuntimeException("Not implemented");
  }
}
