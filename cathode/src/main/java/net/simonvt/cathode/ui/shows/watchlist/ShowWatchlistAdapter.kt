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

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import net.simonvt.cathode.R
import net.simonvt.cathode.R.layout
import net.simonvt.cathode.R.string
import net.simonvt.cathode.common.ui.FragmentsUtils
import net.simonvt.cathode.common.ui.adapter.HeaderAdapter
import net.simonvt.cathode.common.widget.OverflowView.OverflowActionListener
import net.simonvt.cathode.databinding.ListRowShowDescriptionRatingBinding
import net.simonvt.cathode.databinding.ListRowWatchlistEpisodeBinding
import net.simonvt.cathode.entity.Episode
import net.simonvt.cathode.entity.Show
import net.simonvt.cathode.images.ImageType.POSTER
import net.simonvt.cathode.images.ImageType.STILL
import net.simonvt.cathode.images.ImageUri
import net.simonvt.cathode.provider.util.DataHelper
import net.simonvt.cathode.ui.history.AddToHistoryDialog
import net.simonvt.cathode.ui.history.AddToHistoryDialog.Type.EPISODE

class ShowWatchlistAdapter(
  private val activity: FragmentActivity,
  private val itemCallbacks: ItemCallbacks,
  private val onRemoveListener: RemoveListener
) : HeaderAdapter<Any, ViewHolder>(activity) {

  interface RemoveListener {
    fun onRemoveItem(item: Any)
  }

  interface ItemCallbacks {
    fun onShowClicked(showId: Long, title: String?, overview: String?)
    fun onRemoveShowFromWatchlist(showId: Long)
    fun onEpisodeClicked(episodeId: Long, showTitle: String?)
    fun onRemoveEpisodeFromWatchlist(episodeId: Long)
  }

  override fun getItemViewType(headerRes: Int, item: Any): Int {
    return if (headerRes == string.header_shows) TYPE_SHOW else TYPE_EPISODE
  }

  override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
    if (oldItem.javaClass == newItem.javaClass) {
      if (oldItem is Show) {
        return oldItem.id == (newItem as Show).id
      } else if (oldItem is Episode) {
        return oldItem.id == (newItem as Episode).id
      }
    }
    return false
  }

  override fun onCreateItemHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    if (viewType == TYPE_SHOW) {
      val binding = ListRowShowDescriptionRatingBinding.inflate(
        LayoutInflater.from(parent.context),
        parent,
        false
      )
      val holder = ShowViewHolder(binding)
      binding.root.setOnClickListener {
        val position = holder.adapterPosition
        if (position != RecyclerView.NO_POSITION) {
          val show = getItem(position) as Show
          itemCallbacks.onShowClicked(show.id, show.title, show.overview)
        }
      }
      return holder
    } else {
      val binding =
        ListRowWatchlistEpisodeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
      val holder = EpisodeViewHolder(binding)
      binding.root.setOnClickListener {
        val position = holder.adapterPosition
        if (position != RecyclerView.NO_POSITION) {
          val episode = getItem(position) as Episode
          itemCallbacks.onEpisodeClicked(episode.id, episode.showTitle)
        }
      }
      return holder
    }
  }

  override fun onCreateHeaderHolder(parent: ViewGroup): ViewHolder {
    val v =
      LayoutInflater.from(parent.context).inflate(layout.list_row_upcoming_header, parent, false)
    return HeaderViewHolder(v as TextView)
  }

  override fun onViewRecycled(holder: ViewHolder) {
    if (holder is ShowViewHolder) {
      holder.overflow.dismiss()
    } else if (holder is EpisodeViewHolder) {
      holder.overflow.dismiss()
    }
  }

  override fun onBindHeader(holder: ViewHolder, headerRes: Int) {
    (holder as HeaderViewHolder).header.setText(headerRes)
  }

  override fun onBindViewHolder(holder: ViewHolder, item: Any?, position: Int) {
    if (holder.itemViewType == TYPE_SHOW) {
      val vh = holder as ShowViewHolder
      val show = item as Show
      val poster = ImageUri.create(ImageUri.ITEM_SHOW, POSTER, item.id)
      vh.indicator.setWatched(show.watchedCount > 0)
      vh.indicator.setCollected(show.inCollectionCount > 1)
      vh.indicator.setInWatchlist(show.inWatchlist)
      vh.poster.setImage(poster)
      vh.title.text = show.title
      vh.overview.text = show.overview
      vh.rating.setValue(show.rating)
      vh.overflow.setListener(object : OverflowActionListener {
        override fun onPopupShown() {}
        override fun onPopupDismissed() {}
        override fun onActionSelected(action: Int) {
          if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
            when (action) {
              R.id.action_watchlist_remove -> {
                itemCallbacks.onRemoveShowFromWatchlist(item.id)
                onRemoveListener.onRemoveItem(item)
              }
            }
          }
        }
      })
    } else {
      val vh = holder as EpisodeViewHolder
      val episode = item as Episode
      val id = episode.id
      val firstAired = episode.firstAired
      val season = episode.season
      val number = episode.episode
      val watched = episode.watched
      val title = DataHelper.getEpisodeTitle(activity, episode.title, season, number, watched)
      val screenshotUri = ImageUri.create(ImageUri.ITEM_EPISODE, STILL, id)
      vh.screen.setImage(screenshotUri)
      vh.title.text = title
      vh.firstAired.setTimeInMillis(firstAired)
      val episodeNumber = activity.getString(string.season_x_episode_y, season, number)
      vh.episode.text = episodeNumber
      vh.overflow.setListener(object : OverflowActionListener {
        override fun onPopupShown() {}
        override fun onPopupDismissed() {}
        override fun onActionSelected(action: Int) {
          if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
            when (action) {
              R.id.action_history_add -> FragmentsUtils.instantiate(
                activity.supportFragmentManager,
                AddToHistoryDialog::class.java,
                AddToHistoryDialog.getArgs(EPISODE, id, title)
              ).show(activity.supportFragmentManager, AddToHistoryDialog.TAG)
              R.id.action_watchlist_remove -> {
                itemCallbacks.onRemoveEpisodeFromWatchlist(id)
                onRemoveListener.onRemoveItem(item)
              }
            }
          }
        }
      })
    }
  }

  class HeaderViewHolder(var header: TextView) : ViewHolder(header)

  class ShowViewHolder(binding: ListRowShowDescriptionRatingBinding) : ViewHolder(binding.root) {
    var poster = binding.poster
    var indicator = binding.indicator
    var title = binding.title
    var overview = binding.overview
    var overflow = binding.overflow
    var rating = binding.rating
  }

  class EpisodeViewHolder(binding: ListRowWatchlistEpisodeBinding) : ViewHolder(binding.root) {
    var screen = binding.screen
    var title = binding.title
    var firstAired = binding.firstAired
    var episode = binding.episode
    var overflow = binding.overflow
  }

  companion object {
    private const val TYPE_SHOW = 0
    private const val TYPE_EPISODE = 1
  }
}
