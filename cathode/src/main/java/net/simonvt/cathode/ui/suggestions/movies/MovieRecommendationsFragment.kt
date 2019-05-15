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

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import net.simonvt.cathode.R
import net.simonvt.cathode.common.ui.fragment.SwipeRefreshRecyclerFragment
import net.simonvt.cathode.entity.Movie
import net.simonvt.cathode.provider.ProviderSchematic.Movies
import net.simonvt.cathode.settings.Settings
import net.simonvt.cathode.sync.scheduler.MovieTaskScheduler
import net.simonvt.cathode.ui.CathodeViewModelFactory
import net.simonvt.cathode.ui.MoviesNavigationListener
import net.simonvt.cathode.ui.NavigationListener
import net.simonvt.cathode.ui.lists.ListDialog
import net.simonvt.cathode.ui.movies.BaseMoviesAdapter
import javax.inject.Inject

class MovieRecommendationsFragment @Inject constructor(
  private val viewModelFactory: CathodeViewModelFactory,
  private val movieScheduler: MovieTaskScheduler
) : SwipeRefreshRecyclerFragment<BaseMoviesAdapter.ViewHolder>(),
  BaseMoviesAdapter.Callbacks, MovieRecommendationsAdapter.DismissListener, ListDialog.Callback {

  private lateinit var navigationListener: MoviesNavigationListener

  private lateinit var viewModel: MovieRecommendationsViewModel

  private var movieAdapter: MovieRecommendationsAdapter? = null

  private lateinit var sortBy: SortBy

  private var columnCount: Int = 0

  private var scrollToTop: Boolean = false

  enum class SortBy(val key: String, val sortOrder: String) {
    RELEVANCE("relevance", Movies.SORT_RECOMMENDED), RATING("rating", Movies.SORT_RATING);

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
      Settings.get(requireContext()).getString(
        Settings.Sort.MOVIE_RECOMMENDED,
        SortBy.RELEVANCE.key
      )!!
    )

    columnCount = resources.getInteger(R.integer.movieColumns)

    setTitle(R.string.title_movies_recommended)
    setEmptyText(R.string.recommendations_empty)

    viewModel =
      ViewModelProviders.of(this, viewModelFactory).get(MovieRecommendationsViewModel::class.java)
    viewModel.loading.observe(this, Observer { loading -> setRefreshing(loading) })
    viewModel.recommendations.observe(this, Observer { movies -> setMovies(movies) })
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
        ListDialog.newInstance(requireFragmentManager(), R.string.action_sort_by, items, this)
          .show(requireFragmentManager(), DIALOG_SORT)
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
          .putString(Settings.Sort.MOVIE_RECOMMENDED, SortBy.RELEVANCE.key)
          .apply()
        viewModel.setSortBy(sortBy)
        scrollToTop = true
      }

      R.id.sort_rating -> if (sortBy != SortBy.RATING) {
        sortBy = SortBy.RATING
        Settings.get(requireContext())
          .edit()
          .putString(Settings.Sort.MOVIE_RECOMMENDED, SortBy.RATING.key)
          .apply()
        viewModel.setSortBy(sortBy)
        scrollToTop = true
      }
    }
  }

  override fun onMovieClicked(movieId: Long, title: String, overview: String) {
    navigationListener.onDisplayMovie(movieId, title, overview)
  }

  override fun onCheckin(movieId: Long) {
    movieScheduler.checkin(movieId, null, false, false, false)
  }

  override fun onCancelCheckin() {
    movieScheduler.cancelCheckin()
  }

  override fun onWatchlistAdd(movieId: Long) {
    movieScheduler.setIsInWatchlist(movieId, true)
  }

  override fun onWatchlistRemove(movieId: Long) {
    movieScheduler.setIsInWatchlist(movieId, false)
  }

  override fun onCollectionAdd(movieId: Long) {
    movieScheduler.setIsInCollection(movieId, true)
  }

  override fun onCollectionRemove(movieId: Long) {
    movieScheduler.setIsInCollection(movieId, false)
  }

  override fun onDismissItem(view: View, movie: Movie) {
    movieScheduler.dismissRecommendation(movie.id)
    movieAdapter!!.removeItem(movie)
  }

  private fun setMovies(movies: List<Movie>) {
    if (movieAdapter == null) {
      movieAdapter = MovieRecommendationsAdapter(requireActivity(), this, this)
      adapter = movieAdapter
    }

    movieAdapter!!.setList(movies)

    if (scrollToTop) {
      recyclerView.scrollToPosition(0)
      scrollToTop = false
    }
  }

  companion object {

    private const val DIALOG_SORT =
      "net.simonvt.cathode.common.ui.fragment.RecommendedMoviesFragment.sortDialog"
  }
}
