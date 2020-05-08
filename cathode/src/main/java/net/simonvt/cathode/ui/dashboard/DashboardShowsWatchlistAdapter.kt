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
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import net.simonvt.cathode.common.ui.adapter.BaseAdapter
import net.simonvt.cathode.databinding.ListRowDashboardEpisodeBinding
import net.simonvt.cathode.databinding.ListRowDashboardShowBinding
import net.simonvt.cathode.entity.Episode
import net.simonvt.cathode.entity.Show
import net.simonvt.cathode.images.ImageType.POSTER
import net.simonvt.cathode.images.ImageType.STILL
import net.simonvt.cathode.images.ImageUri
import net.simonvt.cathode.provider.util.DataHelper
import net.simonvt.cathode.ui.dashboard.DashboardFragment.OverviewCallback
import java.util.ArrayList

class DashboardShowsWatchlistAdapter(
  context: Context,
  private val callback: OverviewCallback
) : BaseAdapter<Any, ViewHolder>(context) {

  private var shows: List<Show>? = null
  private var episodes: List<Episode>? = null

  override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
    if (oldItem is Show && newItem is Show) {
      return oldItem.id == newItem.id
    } else if (oldItem is Episode && newItem is Episode) {
      return oldItem.id == newItem.id
    }
    return false
  }

  fun changeShowList(shows: List<Show>?) {
    this.shows = shows
    updateItems()
  }

  fun changeEpisodeList(episodes: List<Episode>?) {
    this.episodes = episodes
    updateItems()
  }

  private fun updateItems() {
    val items: MutableList<Any> = ArrayList()
    if (shows != null) {
      items.addAll(shows!!)
    }
    if (episodes != null) {
      items.addAll(episodes!!)
    }

    list = items
  }

  override fun getItemViewType(position: Int): Int {
    return if (list[position] is Show) {
      TYPE_SHOW
    } else TYPE_EPISODE
  }

  override fun getItemId(position: Int): Long {
    // TODO: Better way?
    return if (getItemViewType(position) == TYPE_SHOW) {
      (list[position] as Show).tvdbId.toLong()
    } else {
      (list[position] as Episode).tvdbId.toLong()
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    if (viewType == TYPE_SHOW) {
      val binding =
        ListRowDashboardShowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
      val holder = ShowViewHolder(binding)
      binding.root.setOnClickListener {
        val position = holder.adapterPosition
        if (position != RecyclerView.NO_POSITION) {
          val show = list[position] as Show
          callback.onDisplayShow(show.id, show.title, show.overview)
        }
      }
      return holder
    } else {
      val binding =
        ListRowDashboardEpisodeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
      val holder = EpisodeViewHolder(binding)
      binding.root.setOnClickListener {
        val position = holder.adapterPosition
        if (position != RecyclerView.NO_POSITION) {
          val episode = list[position] as Episode
          callback.onDisplayEpisode(episode.id, episode.showTitle)
        }
      }
      return holder
    }
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    if (holder.itemViewType == TYPE_SHOW) {
      val showHolder = holder as ShowViewHolder
      val (id, title) = list[position] as Show
      val poster = ImageUri.create(ImageUri.ITEM_SHOW, POSTER, id)
      showHolder.poster.setImage(poster)
      showHolder.title.text = title
    } else {
      val episodeHolder =
        holder as EpisodeViewHolder
      val episode =
        list[position] as Episode
      val screenshotUri = ImageUri.create(ImageUri.ITEM_EPISODE, STILL, episode.id)
      episodeHolder.screenshot.setImage(screenshotUri)
      val title = DataHelper.getEpisodeTitle(
        context,
        episode.title,
        episode.season,
        episode.episode,
        episode.watched,
        true
      )
      episodeHolder.title.text = title
    }
  }

  class ShowViewHolder(binding: ListRowDashboardShowBinding) : ViewHolder(binding.root) {
    var poster = binding.poster
    var title = binding.title
  }

  class EpisodeViewHolder(binding: ListRowDashboardEpisodeBinding) : ViewHolder(binding.root) {
    val screenshot = binding.screenshot
    val title = binding.title
  }

  companion object {
    private const val TYPE_SHOW = 0
    private const val TYPE_EPISODE = 1
  }
}
