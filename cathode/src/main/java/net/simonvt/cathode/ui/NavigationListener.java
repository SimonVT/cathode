/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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

public interface NavigationListener
    extends ShowsNavigationListener, MoviesNavigationListener, ListNavigationListener {

  void onDisplayComments(ItemType type, long itemId);

  void onDisplayComment(long commentId);

  void onDisplayPerson(long personId);

  void onDisplayPersonCredit(long personId, Department department);

  void onDisplayCredit(ItemType itemType, long itemId, Department department);

  void onDisplayCredits(ItemType itemType, long itemId, String title);

  void displayFragment(Class clazz, String tag);

  void upFromEpisode(long showId, String showTitle, long seasonId);

  boolean isFragmentTopLevel(Fragment fragment);
}
