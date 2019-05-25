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
package net.simonvt.cathode.ui.shows.watchlist

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.simonvt.cathode.R
import net.simonvt.cathode.common.ui.adapter.HeaderSpanLookup
import net.simonvt.cathode.common.ui.fragment.ToolbarSwipeRefreshRecyclerFragment
import net.simonvt.cathode.entity.Episode
import net.simonvt.cathode.entity.Show
import net.simonvt.cathode.settings.TraktLinkSettings
import net.simonvt.cathode.sync.scheduler.EpisodeTaskScheduler
import net.simonvt.cathode.sync.scheduler.ShowTaskScheduler
import net.simonvt.cathode.ui.CathodeViewModelFactory
import net.simonvt.cathode.ui.LibraryType
import net.simonvt.cathode.ui.NavigationListener
import net.simonvt.cathode.ui.ShowsNavigationListener
import javax.inject.Inject

class ShowsWatchlistFragment @Inject constructor(
  private val viewModelFactory: CathodeViewModelFactory,
  private val showScheduler: ShowTaskScheduler,
  private val episodeScheduler: EpisodeTaskScheduler
) : ToolbarSwipeRefreshRecyclerFragment<RecyclerView.ViewHolder>(),
  ShowWatchlistAdapter.RemoveListener, ShowWatchlistAdapter.ItemCallbacks {

  lateinit var navigationListener: ShowsNavigationListener

  lateinit var viewModel: ShowsWatchlistViewModel

  private var columnCount: Int = 0

  private var adapter: ShowWatchlistAdapter? = null

  private val navigationClickListener = View.OnClickListener { navigationListener.onHomeClicked() }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    navigationListener = requireActivity() as NavigationListener
  }

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    columnCount = resources.getInteger(R.integer.showsColumns)

    setEmptyText(R.string.empty_show_watchlist)
    setTitle(R.string.title_shows_watchlist)

    viewModel =
      ViewModelProviders.of(this, viewModelFactory).get(ShowsWatchlistViewModel::class.java)
    viewModel.loading.observe(this, Observer { loading -> setRefreshing(loading) })
    viewModel.shows.observe(this, Observer { shows -> setShows(shows) })
    viewModel.episodes.observe(this, Observer { episodes -> setEpisodes(episodes) })
  }

  override fun displaysMenuIcon(): Boolean {
    return amITopLevel()
  }

  override fun getColumnCount(): Int {
    return columnCount
  }

  override fun getSpanSizeLookup(): GridLayoutManager.SpanSizeLookup? {
    return HeaderSpanLookup(ensureAdapter(), columnCount)
  }

  override fun onViewCreated(view: View, inState: Bundle?) {
    super.onViewCreated(view, inState)
    toolbar!!.setNavigationOnClickListener(navigationClickListener)
    swipeRefreshLayout.isEnabled = TraktLinkSettings.isLinked(requireContext())
  }

  override fun onRefresh() {
    viewModel.refresh()
  }

  override fun createMenu(toolbar: Toolbar) {
    super.createMenu(toolbar)
    toolbar.inflateMenu(R.menu.fragment_shows)
  }

  override fun onMenuItemClick(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_search -> {
        navigationListener.onSearchClicked()
        return true
      }

      else -> return super.onMenuItemClick(item)
    }
  }

  override fun onShowClicked(showId: Long, title: String, overview: String) {
    navigationListener.onDisplayShow(showId, title, overview, LibraryType.WATCHED)
  }

  override fun onRemoveShowFromWatchlist(showId: Long) {
    showScheduler.setIsInWatchlist(showId, false)
  }

  override fun onEpisodeClicked(episodeId: Long, showTitle: String?) {
    navigationListener.onDisplayEpisode(episodeId, showTitle)
  }

  override fun onRemoveEpisodeFromWatchlist(episodeId: Long) {
    episodeScheduler.setIsInWatchlist(episodeId, false)
  }

  override fun onRemoveItem(item: Any) {
    adapter!!.removeItem(item)
  }

  private fun ensureAdapter(): ShowWatchlistAdapter {
    if (adapter == null) {
      adapter = ShowWatchlistAdapter(requireActivity(), this, this)
      adapter!!.addHeader(R.string.header_shows)
      adapter!!.addHeader(R.string.header_episodes)
    }

    return adapter!!
  }

  private fun setShows(shows: List<Show>) {
    if (getAdapter() == null) {
      setAdapter(ensureAdapter())
    }

    (getAdapter() as ShowWatchlistAdapter).updateHeaderItems(
      R.string.header_shows,
      shows.toMutableList<Any>()
    )
  }

  private fun setEpisodes(episodes: List<Episode>) {
    if (getAdapter() == null) {
      setAdapter(ensureAdapter())
    }

    (getAdapter() as ShowWatchlistAdapter).updateHeaderItems(
      R.string.header_episodes,
      episodes.toMutableList<Any>()
    )
  }

  companion object {

    const val TAG = "net.simonvt.cathode.ui.shows.watchlist.ShowsWatchlistFragment"
  }
}
