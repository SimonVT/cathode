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
package net.simonvt.cathode.ui.movie

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import dagger.android.support.AndroidSupportInjection
import net.simonvt.cathode.R
import net.simonvt.cathode.common.entity.Movie
import net.simonvt.cathode.common.ui.fragment.ToolbarSwipeRefreshRecyclerFragment
import net.simonvt.cathode.common.util.Ids
import net.simonvt.cathode.common.util.guava.Preconditions
import net.simonvt.cathode.sync.scheduler.MovieTaskScheduler
import net.simonvt.cathode.ui.CathodeViewModelFactory
import net.simonvt.cathode.ui.MoviesNavigationListener
import net.simonvt.cathode.ui.movies.BaseMoviesAdapter
import net.simonvt.cathode.ui.movies.MoviesAdapter
import javax.inject.Inject

class RelatedMoviesFragment : ToolbarSwipeRefreshRecyclerFragment<BaseMoviesAdapter.ViewHolder>(),
  BaseMoviesAdapter.Callbacks {

  @Inject
  lateinit var movieScheduler: MovieTaskScheduler

  private lateinit var navigationListener: MoviesNavigationListener

  private var movieId: Long = -1L

  @Inject
  lateinit var viewModelFactory: CathodeViewModelFactory
  lateinit var viewModel: RelatedMoviesViewModel

  private var movieAdapter: MoviesAdapter? = null

  private var columnCount: Int = 0

  override fun onAttach(activity: Activity) {
    super.onAttach(activity)
    navigationListener = activity as MoviesNavigationListener
  }

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    AndroidSupportInjection.inject(this)

    movieId = arguments!!.getLong(ARG_MOVIE_ID)

    columnCount = resources.getInteger(R.integer.movieColumns)

    setTitle(R.string.title_related)
    setEmptyText(R.string.empty_movie_related)

    viewModel =
      ViewModelProviders.of(this, viewModelFactory).get(RelatedMoviesViewModel::class.java)
    viewModel.loading.observe(this, Observer { loading -> setRefreshing(loading) })
    viewModel.setMovieId(movieId)
  }

  override fun onViewCreated(view: View, inState: Bundle?) {
    super.onViewCreated(view, inState)
    viewModel.movies.observe(this, Observer { movies -> setMovies(movies) })
  }

  override fun getColumnCount(): Int {
    return columnCount
  }

  override fun onRefresh() {
    viewModel.refresh()
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

  protected fun setMovies(movies: List<Movie>) {
    if (movieAdapter == null) {
      movieAdapter = MoviesAdapter(requireActivity(), this)
      adapter = movieAdapter
    }

    movieAdapter!!.setList(movies)
  }

  companion object {

    private const val TAG = "net.simonvt.cathode.ui.movie.RelatedMoviesFragment"

    private const val ARG_MOVIE_ID = "net.simonvt.cathode.ui.movie.RelatedMoviesFragment.movieId"

    @JvmStatic
    fun getTag(movieId: Long): String {
      return TAG + "/" + movieId + "/" + Ids.newId()
    }

    @JvmStatic
    fun getArgs(movieId: Long): Bundle {
      Preconditions.checkArgument(movieId >= 0, "movieId must be >= 0")

      val args = Bundle()
      args.putLong(ARG_MOVIE_ID, movieId)
      return args
    }
  }
}
