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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import dagger.android.AndroidInjection
import net.simonvt.cathode.R
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.common.ui.FragmentsUtils
import net.simonvt.cathode.common.util.FragmentStack.StackEntry
import net.simonvt.cathode.ui.comments.CommentsFragment
import net.simonvt.cathode.ui.history.SelectHistoryDateFragment
import net.simonvt.cathode.ui.show.EpisodeFragment
import net.simonvt.cathode.ui.show.EpisodeHistoryFragment
import net.simonvt.cathode.ui.show.EpisodeViewModel
import net.simonvt.cathode.ui.show.ShowFragment
import timber.log.Timber
import java.util.ArrayList
import javax.inject.Inject

class EpisodeDetailsActivity : NavigationListenerActivity() {

  private var id: Long = -1L
  private var showId: Long = -1L

  @Inject
  lateinit var viewModelFactory: CathodeViewModelFactory
  private val viewModel: EpisodeViewModel by viewModels { viewModelFactory }

  override fun onCreate(inState: Bundle?) {
    setTheme(R.style.Theme)
    super.onCreate(inState)
    AndroidInjection.inject(this)
    setContentView(R.layout.activity_details)

    id = intent.getLongExtra(EXTRA_ID, -1L)
    if (id == -1L) {
      Timber.e(Exception("Invalid episode ID"))
      finish()
    } else {
      if (inState == null) {
        val fragment = FragmentsUtils.instantiate(
          supportFragmentManager,
          EpisodeFragment::class.java,
          EpisodeFragment.getArgs(id, null)
        )
        supportFragmentManager.beginTransaction()
          .add(R.id.content, fragment, EpisodeFragment.getTag(id))
          .commit()
      }

      viewModel.setEpisodeId(id)
      viewModel.episode.observe(this, Observer { showId = it.showId })
    }
  }

  override fun upFromEpisode(showId: Long, showTitle: String?, seasonId: Long) {
    onHomeClicked()
  }

  override fun onHomeClicked() {
    if (showId == -1L) return

    val stack = ArrayList<StackEntry>()

    val showEntry = StackEntry(
      ShowFragment::class.java,
      ShowFragment.getTag(showId),
      ShowFragment.getArgs(showId, null, null, LibraryType.WATCHED)
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
      EpisodeFragment.getArgs(id, null)
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

    @JvmStatic
    fun createUri(episodeId: Long): Uri {
      return Uri.parse("cathode://episode/$episodeId")
    }

    fun createIntent(context: Context, uri: Uri): Intent? {
      val idSegment = uri.pathSegments[0]
      val id = if (!idSegment.isNullOrEmpty()) idSegment.toLong() else -1L
      if (id > -1L) {
        val intent = Intent(context, EpisodeDetailsActivity::class.java)
        intent.putExtra(EXTRA_ID, id)
        return intent
      } else {
        return null
      }
    }
  }
}
