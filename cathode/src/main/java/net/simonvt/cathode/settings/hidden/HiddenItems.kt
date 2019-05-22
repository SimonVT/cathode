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
package net.simonvt.cathode.settings.hidden

import android.os.Bundle
import androidx.fragment.app.Fragment
import net.simonvt.cathode.R
import net.simonvt.cathode.api.enumeration.Department
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.common.ui.FragmentContract
import net.simonvt.cathode.common.util.FragmentStack
import net.simonvt.cathode.ui.BaseActivity
import net.simonvt.cathode.ui.LibraryType
import net.simonvt.cathode.ui.NavigationClickListener
import net.simonvt.cathode.ui.NavigationListener
import net.simonvt.cathode.ui.comments.CommentFragment
import net.simonvt.cathode.ui.comments.CommentsFragment
import net.simonvt.cathode.ui.credits.CreditFragment
import net.simonvt.cathode.ui.credits.CreditsFragment
import net.simonvt.cathode.ui.history.SelectHistoryDateFragment
import net.simonvt.cathode.ui.lists.ListFragment
import net.simonvt.cathode.ui.movie.MovieFragment
import net.simonvt.cathode.ui.movie.MovieHistoryFragment
import net.simonvt.cathode.ui.movie.RelatedMoviesFragment
import net.simonvt.cathode.ui.person.PersonCreditsFragment
import net.simonvt.cathode.ui.person.PersonFragment
import net.simonvt.cathode.ui.show.EpisodeFragment
import net.simonvt.cathode.ui.show.EpisodeHistoryFragment
import net.simonvt.cathode.ui.show.RelatedShowsFragment
import net.simonvt.cathode.ui.show.SeasonFragment
import net.simonvt.cathode.ui.show.ShowFragment

class HiddenItems : BaseActivity(), NavigationClickListener, NavigationListener {

  private lateinit var stack: FragmentStack

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    setContentView(R.layout.activity_hidden)

    stack = FragmentStack.forContainer(this, R.id.content)
    stack.setDefaultAnimation(
      R.anim.fade_in_front, R.anim.fade_out_back, R.anim.fade_in_back,
      R.anim.fade_out_front
    )
    if (inState != null) {
      stack.restoreState(inState.getBundle(STATE_STACK)!!)
    }
    if (stack.size() == 0) {
      stack.replace(HiddenItemsFragment::class.java, FRAGMENT_HIDDEN)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    outState.putBundle(STATE_STACK, stack.saveState())
    super.onSaveInstanceState(outState)
  }

  override fun onHomeClicked() {
    if (!stack.pop()) {
      finish()
    }
  }

  override fun onBackPressed() {
    val topFragment = stack.peek() as FragmentContract
    if (topFragment != null && topFragment.onBackPressed()) {
      return
    }

    if (stack.pop()) {
      return
    }

    super.onBackPressed()
  }

  override fun onSearchClicked() {
    throw RuntimeException("Searching from HiddenItems not supported")
  }

  override fun onDisplayShow(showId: Long, title: String?, overview: String?, type: LibraryType) {
    stack.push(
      ShowFragment::class.java,
      ShowFragment.getTag(showId),
      ShowFragment.getArgs(showId, title, overview, type)
    )
  }

  override fun onDisplayEpisode(episodeId: Long, showTitle: String?) {
    stack.push(
      EpisodeFragment::class.java,
      EpisodeFragment.getTag(episodeId),
      EpisodeFragment.getArgs(episodeId, showTitle)
    )
  }

  override fun onDisplayEpisodeHistory(episodeId: Long, showTitle: String) {
    stack.push(
      EpisodeHistoryFragment::class.java,
      EpisodeHistoryFragment.getTag(episodeId),
      EpisodeHistoryFragment.getArgs(episodeId, showTitle)
    )
  }

  override fun onDisplaySeason(
    showId: Long,
    seasonId: Long,
    showTitle: String?,
    seasonNumber: Int,
    type: LibraryType
  ) {
    stack.push(
      SeasonFragment::class.java,
      SeasonFragment.TAG,
      SeasonFragment.getArgs(seasonId, showTitle, seasonNumber, type)
    )
  }

  override fun onDisplayRelatedShows(showId: Long, title: String?) {
    stack.push(
      RelatedShowsFragment::class.java,
      RelatedShowsFragment.getTag(showId),
      RelatedShowsFragment.getArgs(showId)
    )
  }

  override fun onSelectShowWatchedDate(showId: Long, title: String?) {
    stack.push(
      SelectHistoryDateFragment::class.java,
      SelectHistoryDateFragment.TAG,
      SelectHistoryDateFragment.getArgs(SelectHistoryDateFragment.Type.SHOW, showId, title)
    )
  }

  override fun onSelectSeasonWatchedDate(seasonId: Long, title: String?) {
    stack.push(
      SelectHistoryDateFragment::class.java,
      SelectHistoryDateFragment.TAG,
      SelectHistoryDateFragment.getArgs(SelectHistoryDateFragment.Type.SEASON, seasonId, title)
    )
  }

  override fun onSelectEpisodeWatchedDate(episodeId: Long, title: String?) {
    stack.push(
      SelectHistoryDateFragment::class.java,
      SelectHistoryDateFragment.TAG,
      SelectHistoryDateFragment.getArgs(SelectHistoryDateFragment.Type.EPISODE, episodeId, title)
    )
  }

  override fun onSelectOlderEpisodeWatchedDate(episodeId: Long, title: String?) {
    stack.push(
      SelectHistoryDateFragment::class.java,
      SelectHistoryDateFragment.TAG,
      SelectHistoryDateFragment.getArgs(
        SelectHistoryDateFragment.Type.EPISODE_OLDER,
        episodeId,
        title
      )
    )
  }

  override fun onDisplayMovie(movieId: Long, title: String?, overview: String?) {
    stack.push(
      MovieFragment::class.java,
      MovieFragment.getTag(movieId),
      MovieFragment.getArgs(movieId, title, overview)
    )
  }

  override fun onDisplayRelatedMovies(movieId: Long, title: String?) {
    stack.push(
      RelatedMoviesFragment::class.java,
      RelatedMoviesFragment.getTag(movieId),
      RelatedMoviesFragment.getArgs(movieId)
    )
  }

  override fun onSelectMovieWatchedDate(movieId: Long, title: String?) {
    stack.push(
      SelectHistoryDateFragment::class.java,
      SelectHistoryDateFragment.TAG,
      SelectHistoryDateFragment.getArgs(SelectHistoryDateFragment.Type.MOVIE, movieId, title)
    )
  }

  override fun onDisplayMovieHistory(movieId: Long, title: String?) {
    stack.push(
      MovieHistoryFragment::class.java,
      MovieHistoryFragment.getTag(movieId),
      MovieHistoryFragment.getArgs(movieId, title)
    )
  }

  override fun onShowList(listId: Long, listName: String) {
    stack.push(ListFragment::class.java, ListFragment.TAG, ListFragment.getArgs(listId, listName))
  }

  override fun onListDeleted(listId: Long) {
    val top = stack.peek()
    if (top is ListFragment) {
      if (listId == top.listId) {
        stack.pop()
      }
    }
  }

  override fun onDisplayComments(type: ItemType, itemId: Long) {
    stack.push(
      CommentsFragment::class.java,
      CommentsFragment.TAG,
      CommentsFragment.getArgs(type, itemId)
    )
  }

  override fun onDisplayComment(commentId: Long) {
    stack.push(CommentFragment::class.java, CommentFragment.TAG, CommentFragment.getArgs(commentId))
  }

  override fun onDisplayPerson(personId: Long) {
    stack.push(
      PersonFragment::class.java,
      PersonFragment.getTag(personId),
      PersonFragment.getArgs(personId)
    )
  }

  override fun onDisplayPersonCredit(personId: Long, department: Department) {
    stack.push(
      PersonCreditsFragment::class.java,
      PersonCreditsFragment.getTag(personId),
      PersonCreditsFragment.getArgs(personId, department)
    )
  }

  override fun onDisplayCredit(itemType: ItemType, itemId: Long, department: Department) {
    stack.push(
      CreditFragment::class.java,
      CreditFragment.getTag(itemId),
      CreditFragment.getArgs(itemType, itemId, department)
    )
  }

  override fun onDisplayCredits(itemType: ItemType, itemId: Long, title: String?) {
    stack.push(
      CreditsFragment::class.java,
      CreditsFragment.getTag(itemId),
      CreditsFragment.getArgs(itemType, itemId, title)
    )
  }

  override fun displayFragment(clazz: Class<*>, tag: String) {
    stack.push(clazz, tag, null)
  }

  override fun upFromEpisode(showId: Long, showTitle: String?, seasonId: Long) {
    if (stack.removeTop()) {
      val f = stack.peek()
      if (f is ShowFragment && f.showId == showId) {
        stack.attachTop()
      } else if (seasonId >= 0 && f is SeasonFragment && f.seasonId == seasonId) {
        stack.attachTop()
      } else {
        stack.putFragment(
          ShowFragment::class.java,
          ShowFragment.getTag(showId),
          ShowFragment.getArgs(showId, showTitle, null!!, LibraryType.WATCHED)
        )
      }
    }
  }

  override fun popIfTop(fragment: Fragment) {
    if (fragment === stack.peek()) {
      stack.pop()
    }
  }

  override fun isFragmentTopLevel(fragment: Fragment): Boolean {
    return stack.positionInStack(fragment) == 0
  }

  companion object {

    private const val FRAGMENT_HIDDEN =
      "net.simonvt.cathode.settings.hidden.HiddenItems.HiddenItemsFragment"

    private const val STATE_STACK = "net.simonvt.cathode.ui.HomeActivity.stack"
  }
}
