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
package net.simonvt.cathode.ui.show

import android.app.Activity
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import dagger.android.support.AndroidSupportInjection
import net.simonvt.cathode.R
import net.simonvt.cathode.common.ui.fragment.ToolbarSwipeRefreshRecyclerFragment
import net.simonvt.cathode.common.util.Ids
import net.simonvt.cathode.common.util.guava.Preconditions
import net.simonvt.cathode.entity.Show
import net.simonvt.cathode.sync.scheduler.ShowTaskScheduler
import net.simonvt.cathode.ui.CathodeViewModelFactory
import net.simonvt.cathode.ui.LibraryType
import net.simonvt.cathode.ui.ShowsNavigationListener
import net.simonvt.cathode.ui.shows.ShowDescriptionAdapter
import javax.inject.Inject

class RelatedShowsFragment :
  ToolbarSwipeRefreshRecyclerFragment<ShowDescriptionAdapter.ViewHolder>(),
  ShowDescriptionAdapter.ShowCallbacks {

  @Inject
  lateinit var showScheduler: ShowTaskScheduler

  lateinit var navigationListener: ShowsNavigationListener

  private var showId: Long = -1L

  @Inject
  lateinit var viewModelFactory: CathodeViewModelFactory
  lateinit var viewModel: RelatedShowsViewModel

  private var showsAdapter: ShowDescriptionAdapter? = null

  override fun onAttach(activity: Activity) {
    super.onAttach(activity)
    navigationListener = activity as ShowsNavigationListener
  }

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    AndroidSupportInjection.inject(this)

    showId = arguments!!.getLong(ARG_SHOW_ID)

    setEmptyText(R.string.empty_show_related)
    setTitle(R.string.title_related)

    viewModel = ViewModelProviders.of(this, viewModelFactory).get(RelatedShowsViewModel::class.java)
    viewModel.setShowId(showId)
    viewModel.loading.observe(this, Observer { loading -> setRefreshing(loading) })
    viewModel.shows.observe(this, Observer { shows -> setShows(shows) })
  }

  override fun onRefresh() {
    viewModel.refresh()
  }

  override fun onShowClick(showId: Long, title: String, overview: String) {
    navigationListener.onDisplayShow(showId, title, overview, LibraryType.WATCHED)
  }

  override fun setIsInWatchlist(showId: Long, inWatchlist: Boolean) {
    showScheduler.setIsInWatchlist(showId, inWatchlist)
  }

  private fun setShows(shows: List<Show>) {
    if (showsAdapter == null) {
      showsAdapter = ShowDescriptionAdapter(requireContext(), this, false)
      adapter = showsAdapter
    }

    showsAdapter!!.setList(shows)
  }

  companion object {

    private const val TAG = "net.simonvt.cathode.ui.show.RelatedShowsFragment"

    private const val ARG_SHOW_ID = "net.simonvt.cathode.ui.show.RelatedShowsFragment.showId"

    @JvmStatic
    fun getTag(showId: Long): String {
      return TAG + "/" + showId + "/" + Ids.newId()
    }

    @JvmStatic
    fun getArgs(showId: Long): Bundle {
      Preconditions.checkArgument(showId >= 0, "showId must be >= 0")

      val args = Bundle()
      args.putLong(ARG_SHOW_ID, showId)
      return args
    }
  }
}
