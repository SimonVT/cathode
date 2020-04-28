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
package net.simonvt.cathode.ui.shows.collected

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import net.simonvt.cathode.R
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.settings.Settings
import net.simonvt.cathode.settings.TraktLinkSettings
import net.simonvt.cathode.sync.scheduler.EpisodeTaskScheduler
import net.simonvt.cathode.sync.scheduler.ShowTaskScheduler
import net.simonvt.cathode.ui.CathodeViewModelFactory
import net.simonvt.cathode.ui.LibraryType
import net.simonvt.cathode.ui.lists.ListDialog
import net.simonvt.cathode.ui.shows.ShowsFragment
import javax.inject.Inject

class CollectedShowsFragment @Inject constructor(
  showScheduler: ShowTaskScheduler,
  episodeScheduler: EpisodeTaskScheduler
) : ShowsFragment(showScheduler, episodeScheduler), ListDialog.Callback {

  @Inject
  lateinit var viewModelFactory: CathodeViewModelFactory
  lateinit var viewModel: CollectedShowsViewModel

  private var sortBy: SortBy? = null

  enum class SortBy constructor(val key: String, val sortOrder: String) {
    TITLE("title", Shows.SORT_TITLE), COLLECTED("collected", Shows.SORT_COLLECTED);

    override fun toString(): String {
      return key
    }

    companion object {

      fun fromValue(value: String) = values().firstOrNull { it.key == value } ?: TITLE
    }
  }

  override fun onCreate(inState: Bundle?) {
    sortBy = SortBy.fromValue(
      Settings.get(requireContext())
        .getString(Settings.Sort.SHOW_COLLECTED, SortBy.TITLE.key)!!
    )

    super.onCreate(inState)

    setEmptyText(R.string.empty_show_collection)
    setTitle(R.string.title_shows_collection)

    viewModel =
      ViewModelProviders.of(this, viewModelFactory).get(CollectedShowsViewModel::class.java)
    viewModel.loading.observe(this, Observer { loading -> setRefreshing(loading) })
    viewModel.shows.observe(this, observer)
  }

  override fun onViewCreated(view: View, inState: Bundle?) {
    super.onViewCreated(view, inState)
    swipeRefreshLayout.isEnabled = TraktLinkSettings.isLinked(requireContext())
  }

  override fun createMenu(toolbar: Toolbar) {
    super.createMenu(toolbar)
    toolbar.inflateMenu(R.menu.fragment_shows_collected)
  }

  override fun onRefresh() {
    viewModel.refresh()
  }

  override fun onMenuItemClick(item: MenuItem): Boolean {
    if (item.itemId == R.id.menu_sort) {
      val items = arrayListOf<ListDialog.Item>()
      items.add(ListDialog.Item(R.id.sort_title, R.string.sort_title))
      items.add(ListDialog.Item(R.id.sort_collected, R.string.sort_collected))
      ListDialog.newInstance(parentFragmentManager, R.string.action_sort_by, items, this)
        .show(parentFragmentManager, DIALOG_SORT)
      return true
    }

    return super.onMenuItemClick(item)
  }

  override fun onItemSelected(id: Int) {
    when (id) {
      R.id.sort_title -> if (sortBy != SortBy.TITLE) {
        sortBy = SortBy.TITLE
        Settings.get(requireContext())
          .edit()
          .putString(Settings.Sort.SHOW_COLLECTED, SortBy.TITLE.key)
          .apply()
        viewModel.setSortBy(sortBy!!)
        scrollToTop = true
      }

      R.id.sort_collected -> if (sortBy != SortBy.COLLECTED) {
        sortBy = SortBy.COLLECTED
        Settings.get(requireContext())
          .edit()
          .putString(Settings.Sort.SHOW_COLLECTED, SortBy.COLLECTED.key)
          .apply()
        viewModel.setSortBy(sortBy!!)
        scrollToTop = true
      }
    }
  }

  override fun getLibraryType(): LibraryType {
    return LibraryType.COLLECTION
  }

  companion object {

    const val TAG = "net.simonvt.cathode.ui.shows.collected.ShowsCollectionFragment"

    private const val DIALOG_SORT =
      "net.simonvt.cathode.common.ui.fragment.ShowCollectionFragment.sortDialog"
  }
}
