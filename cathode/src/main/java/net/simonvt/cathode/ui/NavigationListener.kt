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
package net.simonvt.cathode.ui

import net.simonvt.cathode.api.enumeration.Department
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.common.ui.FragmentCallbacks

interface NavigationListener : FragmentCallbacks, ShowsNavigationListener, MoviesNavigationListener,
  ListNavigationListener {

  fun onDisplayComments(type: ItemType, itemId: Long)

  fun onDisplayComment(commentId: Long)

  fun onDisplayPerson(personId: Long)

  fun onDisplayPersonCredit(personId: Long, department: Department)

  fun onDisplayCredit(itemType: ItemType, itemId: Long, department: Department)

  fun onDisplayCredits(itemType: ItemType, itemId: Long, title: String?)

  fun displayFragment(clazz: Class<*>, tag: String)

  fun upFromEpisode(showId: Long, showTitle: String?, seasonId: Long)
}
