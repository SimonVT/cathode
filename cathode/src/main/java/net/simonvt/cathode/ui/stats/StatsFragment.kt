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
package net.simonvt.cathode.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import net.simonvt.cathode.R.layout
import net.simonvt.cathode.R.plurals
import net.simonvt.cathode.R.string
import net.simonvt.cathode.common.ui.fragment.BaseFragment
import net.simonvt.cathode.common.util.DateStringUtils
import net.simonvt.cathode.databinding.FragmentStatsBinding

class StatsFragment : BaseFragment() {

  private var _binding: FragmentStatsBinding? = null
  private val binding get() = _binding!!

  private lateinit var viewModel: StatsViewModel
  private var stats: Stats? = null

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    setTitle(string.navigation_stats)
    viewModel = ViewModelProviders.of(this).get(StatsViewModel::class.java)
    viewModel.stats.observe(this, Observer { stats ->
      this@StatsFragment.stats = stats
      updateViews()
    })
  }

  override fun displaysMenuIcon(): Boolean {
    return true
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    inState: Bundle?
  ): View? {
    _binding = FragmentStatsBinding.inflate(inflater, container, false)
    return inflater.inflate(layout.fragment_stats, container, false)
  }

  override fun onViewCreated(view: View, inState: Bundle?) {
    super.onViewCreated(view, inState)
    updateViews()
  }

  override fun onDestroyView() {
    _binding = null
    super.onDestroyView()
  }

  private fun updateViews() {
    if (stats != null && view != null) {
      binding.statsShows.visibility = View.VISIBLE
      binding.episodeTime.text =
        DateStringUtils.getRuntimeString(requireContext(), stats!!.episodeTime)
      binding.episodeCount.text = resources.getQuantityString(
        plurals.stats_episodes, stats!!.episodeCount,
        stats!!.episodeCount
      )
      binding.showCount.text = resources.getQuantityString(
        plurals.stats_shows, stats!!.showCount,
        stats!!.showCount
      )
      binding.statsMovies.visibility = View.VISIBLE
      binding.movieCount.text = resources.getQuantityString(
        plurals.stats_movies, stats!!.movieCount,
        stats!!.movieCount
      )
      binding.moviesTime.text =
        DateStringUtils.getRuntimeString(requireContext(), stats!!.moviesTime)
    }
  }

  companion object {
    const val TAG = "net.simonvt.cathode.ui.stats.StatsFragment"
  }
}
