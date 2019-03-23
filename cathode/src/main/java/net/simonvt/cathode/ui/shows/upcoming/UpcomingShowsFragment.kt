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
package net.simonvt.cathode.ui.shows.upcoming

import android.app.Activity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import dagger.android.support.AndroidSupportInjection
import net.simonvt.cathode.R
import net.simonvt.cathode.common.entity.ShowWithEpisode
import net.simonvt.cathode.common.ui.adapter.HeaderSpanLookup
import net.simonvt.cathode.common.ui.fragment.ToolbarSwipeRefreshRecyclerFragment
import net.simonvt.cathode.settings.TraktLinkSettings
import net.simonvt.cathode.sync.scheduler.EpisodeTaskScheduler
import net.simonvt.cathode.ui.CathodeViewModelFactory
import net.simonvt.cathode.ui.ShowsNavigationListener
import net.simonvt.cathode.ui.lists.ListDialog
import net.simonvt.cathode.ui.shows.upcoming.UpcomingSortByPreference.UpcomingSortByListener
import javax.inject.Inject

class UpcomingShowsFragment : ToolbarSwipeRefreshRecyclerFragment<RecyclerView.ViewHolder>(),
  ListDialog.Callback, UpcomingAdapter.Callbacks {

  @Inject
  lateinit var episodeScheduler: EpisodeTaskScheduler

  @Inject
  lateinit var upcomingSortByPreference: UpcomingSortByPreference

  private lateinit var sortBy: UpcomingSortBy

  private lateinit var navigationListener: ShowsNavigationListener

  @Inject
  lateinit var viewModelFactory: CathodeViewModelFactory
  private lateinit var viewModel: UpcomingViewModel

  private var columnCount: Int = 0

  private var adapter: UpcomingAdapter? = null

  private var scrollToTop: Boolean = false

  private val upcomingSortByListener = UpcomingSortByListener { sortBy ->
    this@UpcomingShowsFragment.sortBy = sortBy
    scrollToTop = true
  }

  override fun onAttach(activity: Activity) {
    super.onAttach(activity)
    navigationListener = activity as ShowsNavigationListener
  }

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    AndroidSupportInjection.inject(this)

    sortBy = upcomingSortByPreference.get()
    upcomingSortByPreference.registerListener(upcomingSortByListener)

    setTitle(R.string.title_shows_upcoming)
    setEmptyText(R.string.empty_show_upcoming)

    columnCount = resources.getInteger(R.integer.showsColumns)

    viewModel = ViewModelProviders.of(this, viewModelFactory).get(UpcomingViewModel::class.java)
    viewModel.loading.observe(this, Observer { loading -> setRefreshing(loading) })
    viewModel.shows.observe(this, Observer { shows -> setShows(shows) })
  }

  override fun onDestroy() {
    upcomingSortByPreference.unregisterListener(upcomingSortByListener)
    super.onDestroy()
  }

  override fun onViewCreated(view: View, inState: Bundle?) {
    super.onViewCreated(view, inState)
    swipeRefreshLayout.isEnabled = TraktLinkSettings.isLinked(requireContext())
  }

  override fun getColumnCount() = columnCount

  override fun getSpanSizeLookup() = HeaderSpanLookup(ensureAdapter(), columnCount)

  override fun displaysMenuIcon() = amITopLevel()

  override fun createMenu(toolbar: Toolbar) {
    super.createMenu(toolbar)
    toolbar.inflateMenu(R.menu.fragment_shows_upcoming)
  }

  override fun onMenuItemClick(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.sort_by -> {
        val items = arrayListOf<ListDialog.Item>()
        items.add(ListDialog.Item(R.id.sort_title, R.string.sort_title))
        items.add(ListDialog.Item(R.id.sort_next_episode, R.string.sort_next_episode))
        items.add(ListDialog.Item(R.id.sort_last_watched, R.string.sort_last_watched))
        ListDialog.newInstance(R.string.action_sort_by, items, this@UpcomingShowsFragment)
          .show(fragmentManager!!, DIALOG_SORT)
        return true
      }

      R.id.menu_search -> {
        navigationListener.onSearchClicked()
        return true
      }

      else -> return super.onMenuItemClick(item)
    }
  }

  override fun onEpisodeClicked(episodeId: Long, showTitle: String) {
    navigationListener.onDisplayEpisode(episodeId, showTitle)
  }

  override fun onCheckin(episodeId: Long) {
    episodeScheduler.checkin(episodeId, null, false, false, false)
  }

  override fun onCancelCheckin() {
    episodeScheduler.cancelCheckin()
  }

  override fun onItemSelected(id: Int) {
    when (id) {
      R.id.sort_title -> if (sortBy != UpcomingSortBy.TITLE) {
        upcomingSortByPreference.set(UpcomingSortBy.TITLE)
      }

      R.id.sort_next_episode -> if (sortBy != UpcomingSortBy.NEXT_EPISODE) {
        upcomingSortByPreference.set(UpcomingSortBy.NEXT_EPISODE)
      }

      R.id.sort_last_watched -> if (sortBy != UpcomingSortBy.LAST_WATCHED) {
        upcomingSortByPreference.set(UpcomingSortBy.LAST_WATCHED)
      }
    }
  }

  override fun onRefresh() {
    viewModel.refresh()
  }

  private fun ensureAdapter(): UpcomingAdapter {
    if (adapter == null) {
      adapter = UpcomingAdapter(requireActivity(), this)
      adapter!!.addHeader(R.string.header_aired)
      adapter!!.addHeader(R.string.header_upcoming)
      setAdapter(adapter)
    }

    return adapter!!
  }

  private fun setShows(shows: List<ShowWithEpisode>) {
    var adapter: UpcomingAdapter? = getAdapter() as UpcomingAdapter
    if (adapter == null) {
      adapter = ensureAdapter()
      setAdapter(adapter)
    }

    val currentTime = System.currentTimeMillis()

    val airedShows = shows.filter { show -> show.episode.firstAired!! <= currentTime }
    val unairedShows = shows.filter { show -> show.episode.firstAired!! > currentTime }

    adapter.updateHeaderItems(R.string.header_aired, airedShows)
    adapter.updateHeaderItems(R.string.header_upcoming, unairedShows)

    if (scrollToTop) {
      recyclerView.scrollToPosition(0)
      scrollToTop = false
    }
  }

  companion object {

    const val TAG = "net.simonvt.cathode.ui.shows.upcoming.UpcomingShowsFragment"

    private const val DIALOG_SORT =
      "net.simonvt.cathode.ui.shows.upcoming.UpcomingShowsFragment.sortDialog"
  }
}
