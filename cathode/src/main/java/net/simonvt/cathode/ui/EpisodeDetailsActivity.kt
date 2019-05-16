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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import net.simonvt.cathode.R
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.common.ui.FragmentsUtils
import net.simonvt.cathode.common.util.FragmentStack.StackEntry
import net.simonvt.cathode.ui.comments.CommentsFragment
import net.simonvt.cathode.ui.history.SelectHistoryDateFragment
import net.simonvt.cathode.ui.show.EpisodeFragment
import net.simonvt.cathode.ui.show.EpisodeHistoryFragment
import net.simonvt.cathode.ui.show.ShowFragment
import timber.log.Timber
import java.util.ArrayList

class EpisodeDetailsActivity : NavigationListenerActivity() {

  private var id: Long = 0
  private var showId: Long = 0
  private var showTitle: String? = null
  private var showOverview: String? = null

  override fun onCreate(inState: Bundle?) {
    setTheme(R.style.Theme)
    super.onCreate(inState)
    setContentView(R.layout.activity_details)

    val intent = intent

    if (CalendarContract.ACTION_HANDLE_CUSTOM_EVENT == intent.action) {
      val uriString = intent.getStringExtra(CalendarContract.EXTRA_CUSTOM_APP_URI)
      if (uriString.isNullOrEmpty()) {
        finish()
      } else {
        val uri = Uri.parse(uriString)
        val idSegment = uri.pathSegments[0]
        if (!idSegment.isNullOrEmpty()) {
          id = java.lang.Long.parseLong(idSegment)
        } else {
          finish()
        }
      }
    } else {
      id = intent.getLongExtra(EXTRA_ID, -1L)
      showId = intent.getLongExtra(EXTRA_SHOW_ID, -1L)
      showTitle = intent.getStringExtra(EXTRA_SHOW_TITLE)
      showOverview = intent.getStringExtra(EXTRA_SHOW_OVERVIEW)
    }

    if (id == -1L) {
      Timber.e(Exception("ID was $id"))
      finish()
    } else {
      if (inState == null) {
        val fragment = FragmentsUtils.instantiate(
          supportFragmentManager,
          EpisodeFragment::class.java,
          EpisodeFragment.getArgs(id, showTitle!!)
        )
        supportFragmentManager.beginTransaction()
          .add(R.id.content, fragment, EpisodeFragment.getTag(id)).commitNow()
      }
    }
  }

  override fun upFromEpisode(showId: Long, showTitle: String?, seasonId: Long) {
    this.showId = showId
    this.showTitle = showTitle
    onHomeClicked()
  }

  override fun onHomeClicked() {
    val stack = ArrayList<StackEntry>()

    val showEntry = StackEntry(
      ShowFragment::class.java,
      ShowFragment.getTag(showId),
      ShowFragment.getArgs(showId, showTitle!!, showOverview, LibraryType.WATCHED)
    )
    stack.add(showEntry)

    val i = Intent(this, HomeActivity::class.java)
    i.action = HomeActivity.ACTION_REPLACE_STACK
    i.putParcelableArrayListExtra(HomeActivity.EXTRA_STACK_ENTRIES, stack)

    startActivity(i)
    finish()
  }

  override fun onDisplayComments(type: ItemType, itemId: Long) {
    displayOnTop(
      CommentsFragment::class.java,
      CommentsFragment.TAG,
      CommentsFragment.getArgs(ItemType.EPISODE, itemId)
    )
  }

  override fun onDisplayEpisodeHistory(episodeId: Long, showTitle: String) {
    displayOnTop(
      EpisodeHistoryFragment::class.java,
      EpisodeHistoryFragment.getTag(episodeId),
      EpisodeHistoryFragment.getArgs(episodeId, showTitle)
    )
  }

  override fun onSelectEpisodeWatchedDate(episodeId: Long, title: String?) {
    displayOnTop(
      SelectHistoryDateFragment::class.java,
      SelectHistoryDateFragment.TAG,
      SelectHistoryDateFragment.getArgs(SelectHistoryDateFragment.Type.EPISODE, episodeId, title)
    )
  }

  private fun displayOnTop(fragment: Class<*>, tag: String, args: Bundle) {
    val stack = ArrayList<StackEntry>()

    val episodeEntry = StackEntry(
      EpisodeFragment::class.java,
      EpisodeFragment.getTag(id),
      EpisodeFragment.getArgs(id, showTitle)
    )
    stack.add(episodeEntry)

    val entryOnTop = StackEntry(fragment, tag, args)
    stack.add(entryOnTop)

    val i = Intent(this, HomeActivity::class.java)
    i.action = HomeActivity.ACTION_REPLACE_STACK
    i.putParcelableArrayListExtra(HomeActivity.EXTRA_STACK_ENTRIES, stack)

    startActivity(i)
    finish()
  }

  companion object {

    const val EXTRA_ID = "net.simonvt.cathode.ui.DetailsActivity.id"
    const val EXTRA_SHOW_ID = "net.simonvt.cathode.ui.DetailsActivity.showId"
    const val EXTRA_SHOW_TITLE = "net.simonvt.cathode.ui.DetailsActivity.showTitle"
    const val EXTRA_SHOW_OVERVIEW = "net.simonvt.cathode.ui.DetailsActivity.showOverview"

    @JvmStatic
    fun createUri(episodeId: Long): Uri {
      return Uri.parse("cathode://episode/$episodeId")
    }
  }
}
