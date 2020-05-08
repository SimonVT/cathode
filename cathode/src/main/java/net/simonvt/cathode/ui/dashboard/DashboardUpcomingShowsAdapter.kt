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
package net.simonvt.cathode.ui.dashboard

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.simonvt.cathode.R.string
import net.simonvt.cathode.common.ui.adapter.BaseAdapter
import net.simonvt.cathode.databinding.ListRowDashboardShowUpcomingBinding
import net.simonvt.cathode.entity.ShowWithEpisode
import net.simonvt.cathode.images.ImageType.POSTER
import net.simonvt.cathode.images.ImageUri
import net.simonvt.cathode.provider.util.DataHelper
import net.simonvt.cathode.ui.dashboard.DashboardFragment.OverviewCallback
import net.simonvt.cathode.ui.dashboard.DashboardUpcomingShowsAdapter.ViewHolder

class DashboardUpcomingShowsAdapter(
  context: Context?,
  private val callback: OverviewCallback
) : BaseAdapter<ShowWithEpisode, ViewHolder>(context) {

  override fun getItemId(position: Int): Long {
    return list[position]!!.show.id
  }

  override fun areItemsTheSame(oldItem: ShowWithEpisode, newItem: ShowWithEpisode): Boolean {
    return oldItem.show.id == newItem.show.id
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val binding = ListRowDashboardShowUpcomingBinding.inflate(
      LayoutInflater.from(parent.context),
      parent,
      false
    )
    val holder = ViewHolder(binding)
    binding.root.setOnClickListener {
      val position = holder.adapterPosition
      if (position != RecyclerView.NO_POSITION) {
        val (show, episode) = list[position]!!
        callback.onDisplayEpisode(episode.id, show.title)
      }
    }
    return holder
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val (show, episode) = list[position]!!
    val season = episode.season
    val episodeNumber = episode.episode
    val watched = episode.watched
    val episodeTitle =
      DataHelper.getEpisodeTitle(context, episode.title, season, episodeNumber, watched, true)
    val poster = ImageUri.create(ImageUri.ITEM_SHOW, POSTER, show.id)
    holder.poster.setImage(poster)
    holder.title.text = show.title
    if (show.watching) {
      holder.nextEpisode.setText(string.show_watching)
    } else {
      holder.nextEpisode.text = episodeTitle
    }
  }

  class ViewHolder(binding: ListRowDashboardShowUpcomingBinding) :
    RecyclerView.ViewHolder(binding.root) {
    var poster = binding.poster
    var title = binding.title
    var nextEpisode = binding.nextEpisode
  }

  init {
    setHasStableIds(true)
  }
}
