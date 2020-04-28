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
package net.simonvt.cathode.ui.suggestions.shows

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import net.simonvt.cathode.R
import net.simonvt.cathode.common.ui.fragment.SwipeRefreshRecyclerFragment
import net.simonvt.cathode.entity.Show
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.settings.Settings
import net.simonvt.cathode.sync.scheduler.ShowTaskScheduler
import net.simonvt.cathode.ui.CathodeViewModelFactory
import net.simonvt.cathode.ui.LibraryType
import net.simonvt.cathode.ui.NavigationListener
import net.simonvt.cathode.ui.ShowsNavigationListener
import net.simonvt.cathode.ui.lists.ListDialog
import net.simonvt.cathode.ui.shows.ShowDescriptionAdapter
import javax.inject.Inject

class ShowRecommendationsFragment @Inject constructor(
  private val viewModelFactory: CathodeViewModelFactory,
  private val showScheduler: ShowTaskScheduler
) : SwipeRefreshRecyclerFragment<ShowDescriptionAdapter.ViewHolder>(),
  ShowRecommendationsAdapter.DismissListener, ListDialog.Callback,
  ShowDescriptionAdapter.ShowCallbacks {

  private val viewModel: ShowRecommendationsViewModel by viewModels { viewModelFactory }

  private var showsAdapter: ShowRecommendationsAdapter? = null

  private lateinit var navigationListener: ShowsNavigationListener

  private var shows: List<Show>? = null

  private lateinit var sortBy: SortBy

  private var columnCount: Int = 0

  private var scrollToTop: Boolean = false

  enum class SortBy(val key: String, val sortOrder: String) {
    RELEVANCE("relevance", Shows.SORT_RECOMMENDED), RATING("rating", Shows.SORT_RATING);

    override fun toString(): String {
      return key
    }

    companion object {

      fun fromValue(value: String) = values().firstOrNull { it.key == value } ?: RELEVANCE
    }
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    navigationListener = requireActivity() as NavigationListener
  }

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    sortBy = SortBy.fromValue(
      Settings.get(requireContext())
        .getString(Settings.Sort.SHOW_RECOMMENDED, SortBy.RELEVANCE.key)!!
    )

    columnCount = resources.getInteger(R.integer.showsColumns)

    setTitle(R.string.title_shows_recommended)
    setEmptyText(R.string.recommendations_empty)

    viewModel.loading.observe(this, Observer { loading -> setRefreshing(loading) })
    viewModel.recommendations.observe(this, Observer { shows -> setShows(shows) })
  }

  override fun getColumnCount(): Int {
    return columnCount
  }

  override fun onRefresh() {
    viewModel.refresh()
  }

  override fun onMenuItemClick(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.sort_by -> {
        val items = arrayListOf<ListDialog.Item>()
        items.add(ListDialog.Item(R.id.sort_relevance, R.string.sort_relevance))
        items.add(ListDialog.Item(R.id.sort_rating, R.string.sort_rating))
        ListDialog.newInstance(parentFragmentManager, R.string.action_sort_by, items, this)
          .show(parentFragmentManager, DIALOG_SORT)
        return true
      }

      else -> return super.onMenuItemClick(item)
    }
  }

  override fun onItemSelected(id: Int) {
    when (id) {
      R.id.sort_relevance -> if (sortBy != SortBy.RELEVANCE) {
        sortBy = SortBy.RELEVANCE
        Settings.get(requireContext())
          .edit()
          .putString(Settings.Sort.SHOW_RECOMMENDED, SortBy.RELEVANCE.key)
          .apply()
        viewModel.setSortBy(sortBy)
        scrollToTop = true
      }

      R.id.sort_rating -> if (sortBy != SortBy.RATING) {
        sortBy = SortBy.RATING
        Settings.get(requireContext())
          .edit()
          .putString(Settings.Sort.SHOW_RECOMMENDED, SortBy.RATING.key)
          .apply()
        viewModel.setSortBy(sortBy)
        scrollToTop = true
      }
    }
  }

  override fun onShowClick(showId: Long, title: String?, overview: String?) {
    navigationListener.onDisplayShow(showId, title, overview, LibraryType.WATCHED)
  }

  override fun setIsInWatchlist(showId: Long, inWatchlist: Boolean) {
    showScheduler.setIsInWatchlist(showId, inWatchlist)
  }

  override fun onDismissItem(view: View, show: Show) {
    showScheduler.dismissRecommendation(show.id)
    showsAdapter!!.removeItem(show)
  }

  private fun setShows(shows: List<Show>) {
    this.shows = shows

    if (showsAdapter == null) {
      showsAdapter = ShowRecommendationsAdapter(requireContext(), this, this)
      adapter = showsAdapter
      return
    }

    showsAdapter!!.setList(shows)

    if (scrollToTop) {
      recyclerView.scrollToPosition(0)
      scrollToTop = false
    }
  }

  companion object {

    private const val DIALOG_SORT =
      "net.simonvt.cathode.ui.suggestions.shows.ShowRecommendationsFragment.sortDialog"
  }
}
