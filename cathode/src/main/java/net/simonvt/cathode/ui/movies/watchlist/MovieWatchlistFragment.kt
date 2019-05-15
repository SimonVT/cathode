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
package net.simonvt.cathode.ui.movies.watchlist

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import net.simonvt.cathode.R
import net.simonvt.cathode.jobqueue.JobManager
import net.simonvt.cathode.settings.TraktLinkSettings
import net.simonvt.cathode.sync.scheduler.MovieTaskScheduler
import net.simonvt.cathode.ui.CathodeViewModelFactory
import net.simonvt.cathode.ui.movies.MoviesFragment
import javax.inject.Inject

class MovieWatchlistFragment @Inject constructor(
  jobManager: JobManager,
  movieScheduler: MovieTaskScheduler
) : MoviesFragment(jobManager, movieScheduler) {

  @Inject
  lateinit var viewModelFactory: CathodeViewModelFactory
  private lateinit var viewModel: MovieWatchlistViewModel

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    setEmptyText(R.string.empty_movie_watchlist)
    setTitle(R.string.title_movies_watchlist)

    viewModel =
      ViewModelProviders.of(this, viewModelFactory).get(MovieWatchlistViewModel::class.java)
    viewModel.loading.observe(this, Observer { loading -> setRefreshing(loading) })
    viewModel.movies.observe(this, observer)
  }

  override fun onViewCreated(view: View, inState: Bundle?) {
    super.onViewCreated(view, inState)
    swipeRefreshLayout.isEnabled = TraktLinkSettings.isLinked(requireContext())
  }

  override fun onRefresh() {
    viewModel.refresh()
  }

  companion object {

    const val TAG = "net.simonvt.cathode.ui.movies.watchlist.MovieWatchlistFragment"
  }
}
