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
import net.simonvt.cathode.common.ui.instantiate
import net.simonvt.cathode.common.util.FragmentStack.StackEntry
import net.simonvt.cathode.ui.show.EpisodeFragment
import net.simonvt.cathode.ui.show.SeasonFragment
import net.simonvt.cathode.ui.show.SeasonViewModel
import net.simonvt.cathode.ui.show.ShowFragment
import timber.log.Timber
import java.util.ArrayList
import javax.inject.Inject

class SeasonDetailsActivity : NavigationListenerActivity() {

  private var seasonId: Long = -1L
  private var showId: Long = -1L
  private var seasonNumber: Int = -1

  @Inject
  lateinit var viewModelFactory: CathodeViewModelFactory
  private val viewModel: SeasonViewModel by viewModels { viewModelFactory }

  override fun onCreate(inState: Bundle?) {
    setTheme(R.style.Theme)
    super.onCreate(inState)
    AndroidInjection.inject(this)
    setContentView(R.layout.activity_details)

    seasonId = intent.getLongExtra(EXTRA_ID, -1L)

    if (seasonId == -1L) {
      Timber.e(Exception("Invalid season ID"))
      finish()
    } else {
      if (inState == null) {
        val fragment = supportFragmentManager.instantiate(
          SeasonFragment::class.java,
          SeasonFragment.getArgs(seasonId, null, seasonNumber, LibraryType.WATCHED)
        )

        supportFragmentManager.beginTransaction()
          .add(R.id.content, fragment, SeasonFragment.getTag(seasonId))
          .commit()
      }

      viewModel.setSeasonId(seasonId)
      viewModel.season.observe(this, Observer {
        showId = it.showId
        seasonNumber = it.season
      })
    }
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

  override fun onDisplayEpisode(episodeId: Long, showTitle: String?) {
    if (showId == -1L) return

    val stack = ArrayList<StackEntry>()

    val showEntry = StackEntry(
      ShowFragment::class.java,
      ShowFragment.getTag(showId),
      ShowFragment.getArgs(showId, null, null, LibraryType.WATCHED)
    )
    stack.add(showEntry)

    val seasonEntry = StackEntry(
      SeasonFragment::class.java,
      SeasonFragment.getTag(seasonId),
      SeasonFragment.getArgs(seasonId, null, -1, LibraryType.WATCHED)
    )
    stack.add(seasonEntry)

    val episodeEntry = StackEntry(
      EpisodeFragment::class.java,
      EpisodeFragment.getTag(episodeId),
      EpisodeFragment.getArgs(showId, null)
    )
    stack.add(episodeEntry)

    val i = Intent(this, HomeActivity::class.java)
    i.action = HomeActivity.ACTION_REPLACE_STACK
    i.putParcelableArrayListExtra(HomeActivity.EXTRA_STACK_ENTRIES, stack)

    startActivity(i)
    finish()
  }

  companion object {

    const val EXTRA_ID = "net.simonvt.cathode.ui.SeasonDetailsActivity.id"

    @JvmStatic
    fun createUri(seasonId: Long): Uri {
      return Uri.parse("cathode://season/$seasonId")
    }

    fun createIntent(context: Context, uri: Uri): Intent? {
      val idSegment = uri.pathSegments[0]
      val id = if (!idSegment.isNullOrEmpty()) idSegment.toLong() else -1L
      if (id > -1L) {
        val intent = Intent(context, SeasonDetailsActivity::class.java)
        intent.putExtra(EXTRA_ID, id)
        return intent
      }
      return null
    }
  }
}
