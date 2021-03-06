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
package net.simonvt.cathode.ui.suggestions.movies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import net.simonvt.cathode.R
import net.simonvt.cathode.common.ui.adapter.BaseAdapter
import net.simonvt.cathode.entity.Movie
import net.simonvt.cathode.jobqueue.JobManager
import net.simonvt.cathode.provider.ProviderSchematic.Movies
import net.simonvt.cathode.settings.Settings
import net.simonvt.cathode.sync.scheduler.MovieTaskScheduler
import net.simonvt.cathode.ui.CathodeViewModelFactory
import net.simonvt.cathode.ui.lists.ListDialog
import net.simonvt.cathode.ui.movies.BaseMoviesAdapter
import net.simonvt.cathode.ui.movies.MoviesAdapter
import net.simonvt.cathode.ui.movies.MoviesFragment
import javax.inject.Inject

class TrendingMoviesFragment @Inject constructor(
  private val viewModelFactory: CathodeViewModelFactory,
  jobManager: JobManager,
  movieScheduler: MovieTaskScheduler
) : MoviesFragment(jobManager, movieScheduler), ListDialog.Callback {

  private val viewModel: TrendingMoviesViewModel by viewModels { viewModelFactory }

  private lateinit var sortBy: SortBy

  enum class SortBy(val key: String, val sortOrder: String) {
    VIEWERS("viewers", Movies.SORT_VIEWERS), RATING("rating", Movies.SORT_RATING);

    override fun toString(): String {
      return key
    }

    companion object {

      fun fromValue(value: String) = values().firstOrNull { it.key == value } ?: VIEWERS
    }
  }

  override fun onCreate(inState: Bundle?) {
    sortBy = SortBy.fromValue(
      Settings.get(requireContext())
        .getString(Settings.Sort.MOVIE_TRENDING, SortBy.VIEWERS.key)!!
    )
    super.onCreate(inState)

    setTitle(R.string.title_movies_trending)
    setEmptyText(R.string.movies_loading_trending)

    viewModel.loading.observe(this, Observer { loading -> setRefreshing(loading) })
    viewModel.trending.observe(this, observer)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    inState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_swiperefresh_recyclerview, container, false)
  }

  override fun onRefresh() {
    viewModel.refresh()
  }

  override fun onMenuItemClick(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.sort_by -> {
        val items = arrayListOf<ListDialog.Item>()
        items.add(ListDialog.Item(R.id.sort_viewers, R.string.sort_viewers))
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
      R.id.sort_viewers -> if (sortBy != SortBy.VIEWERS) {
        sortBy = SortBy.VIEWERS
        Settings.get(requireContext())
          .edit()
          .putString(Settings.Sort.MOVIE_TRENDING, SortBy.VIEWERS.key)
          .apply()
        viewModel.setSortBy(sortBy)
        scrollToTop = true
      }

      R.id.sort_rating -> if (sortBy != SortBy.RATING) {
        sortBy = SortBy.RATING
        Settings.get(requireContext())
          .edit()
          .putString(Settings.Sort.MOVIE_TRENDING, SortBy.RATING.key)
          .apply()
        viewModel.setSortBy(sortBy)
        scrollToTop = true
      }
    }
  }

  override fun createAdapter(): BaseAdapter<Movie, BaseMoviesAdapter.ViewHolder> {
    return MoviesAdapter(requireActivity(), this, R.layout.list_row_movie_rating)
  }

  companion object {

    private const val DIALOG_SORT =
      "net.simonvt.cathode.ui.suggestions.movies.TrendingMoviesFragment.sortDialog"
  }
}
