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
package net.simonvt.cathode.ui.shows

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import net.simonvt.cathode.R.id
import net.simonvt.cathode.R.string
import net.simonvt.cathode.common.ui.FragmentsUtils
import net.simonvt.cathode.common.ui.adapter.BaseAdapter
import net.simonvt.cathode.common.widget.OverflowView
import net.simonvt.cathode.common.widget.OverflowView.OverflowActionListener
import net.simonvt.cathode.databinding.ListRowShowBinding
import net.simonvt.cathode.entity.ShowWithEpisode
import net.simonvt.cathode.images.ImageType.POSTER
import net.simonvt.cathode.images.ImageUri
import net.simonvt.cathode.provider.util.DataHelper
import net.simonvt.cathode.ui.LibraryType
import net.simonvt.cathode.ui.LibraryType.COLLECTION
import net.simonvt.cathode.ui.LibraryType.WATCHED
import net.simonvt.cathode.ui.LibraryType.WATCHLIST
import net.simonvt.cathode.ui.dialog.CheckInDialog
import net.simonvt.cathode.ui.dialog.CheckInDialog.Type.SHOW
import net.simonvt.cathode.ui.history.AddToHistoryDialog
import net.simonvt.cathode.ui.history.AddToHistoryDialog.Type.EPISODE
import net.simonvt.cathode.ui.shows.ShowsWithNextAdapter.ViewHolder

/**
 * A show adapter that displays the next episode as well.
 */
class ShowsWithNextAdapter(
  private val activity: FragmentActivity,
  private var callbacks: Callbacks,
  private val libraryType: LibraryType
) : BaseAdapter<ShowWithEpisode?, ViewHolder?>(activity) {

  interface Callbacks {
    fun onShowClick(showId: Long, title: String?, overview: String?)
    fun onRemoveFromWatchlist(showId: Long)
    fun onCheckin(episodeId: Long)
    fun onCancelCheckin()
    fun onCollectNext(showId: Long)
    fun onHideFromWatched(showId: Long)
    fun onHideFromCollection(showId: Long)
  }

  override fun getItemId(position: Int): Long {
    return list[position]!!.show.id
  }

  override fun areItemsTheSame(oldItem: ShowWithEpisode, newItem: ShowWithEpisode): Boolean {
    return oldItem.show.id == newItem.show.id
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val binding = ListRowShowBinding.inflate(LayoutInflater.from(context), parent, false)
    val holder = ViewHolder(binding)
    binding.root.setOnClickListener {
      val position = holder.adapterPosition
      if (position != RecyclerView.NO_POSITION) {
        val (show) = list[position]!!
        callbacks.onShowClick(holder.itemId, show.title, show.overview)
      }
    }
    holder.overflow.setListener(object : OverflowActionListener {
      override fun onPopupShown() {}
      override fun onPopupDismissed() {}
      override fun onActionSelected(action: Int) {
        val position = holder.adapterPosition
        if (position != RecyclerView.NO_POSITION) {
          when (action) {
            id.action_watchlist_remove -> callbacks.onRemoveFromWatchlist(holder.itemId)
            id.action_history_add -> FragmentsUtils.instantiate(
              activity.supportFragmentManager,
              AddToHistoryDialog::class.java,
              AddToHistoryDialog.getArgs(EPISODE, holder.episodeId, holder.episodeTitle)
            ).show(activity.supportFragmentManager, AddToHistoryDialog.TAG)
            id.action_checkin -> if (!CheckInDialog.showDialogIfNecessary(
                activity,
                SHOW,
                holder.episodeTitle,
                holder.episodeId
              )
            ) {
              callbacks.onCheckin(holder.episodeId)
            }
            id.action_checkin_cancel -> callbacks.onCancelCheckin()
            id.action_collection_add -> callbacks.onCollectNext(holder.itemId)
            id.action_watched_hide -> callbacks.onHideFromWatched(holder.itemId)
            id.action_collection_hide -> callbacks.onHideFromCollection(holder.itemId)
          }
        }
      }
    })
    return holder
  }

  override fun onViewRecycled(holder: ViewHolder) {
    holder.overflow.dismiss()
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val (show, episode) = list[position]!!
    val showPosterUri = ImageUri.create(ImageUri.ITEM_SHOW, POSTER, show.id)
    val showAiredCount = show.airedCount
    val count = when (libraryType) {
      WATCHED, WATCHLIST -> show.watchedCount
      COLLECTION -> show.inCollectionCount
    }
    val showTypeCount = count
    var episodeTitle: String? = null
    if (episode.season > 0) {
      episodeTitle = DataHelper.getEpisodeTitle(
        context,
        episode.title,
        episode.season,
        episode.episode,
        episode.watched,
        true
      )
    }
    holder.title.text = show.title
    holder.progressBar.max = showAiredCount
    holder.progressBar.progress = showTypeCount
    val typeCount = context.getString(string.x_of_y, showTypeCount, showAiredCount)
    holder.watched.text = typeCount
    var episodeText: String? = null
    if (episodeTitle == null) {
      if (show.status != null) {
        episodeText = show.status.toString()
      }
      holder.firstAired.visibility = View.GONE
    } else {
      episodeText = if (show.watching) {
        context.getString(string.show_watching)
      } else {
        context.getString(string.episode_next, episodeTitle)
      }
      holder.firstAired.visibility = View.VISIBLE
      holder.firstAired.setTimeInMillis(episode.firstAired)
    }
    holder.nextEpisode.text = episodeText
    holder.overflow.visibility = if (showAiredCount > 0) View.VISIBLE else View.INVISIBLE
    holder.overflow.removeItems()
    setupOverflowItems(
      holder.overflow,
      showTypeCount,
      showAiredCount,
      episodeTitle != null,
      show.watching
    )
    holder.poster.setImage(showPosterUri)
    holder.showTypeCount = showTypeCount
    holder.showAiredCount = showAiredCount
    holder.episodeTitle = episodeTitle
    holder.episodeId = episode.id
  }

  private fun setupOverflowItems(
    overflow: OverflowView,
    typeCount: Int,
    airedCount: Int,
    hasNext: Boolean,
    watching: Boolean
  ) {
    when (libraryType) {
      WATCHLIST -> {
        overflow.addItem(id.action_watchlist_remove, string.action_watchlist_remove)
        if (airedCount - typeCount > 0) {
          if (!watching && hasNext) {
            overflow.addItem(id.action_checkin, string.action_checkin)
            overflow.addItem(id.action_history_add, string.action_history_add)
          } else if (watching) {
            overflow.addItem(id.action_checkin_cancel, string.action_checkin_cancel)
          }
        }
        overflow.addItem(id.action_watched_hide, string.action_watched_hide)
      }
      WATCHED -> {
        if (airedCount - typeCount > 0) {
          if (!watching && hasNext) {
            overflow.addItem(id.action_checkin, string.action_checkin)
            overflow.addItem(id.action_history_add, string.action_history_add)
          } else if (watching) {
            overflow.addItem(id.action_checkin_cancel, string.action_checkin_cancel)
          }
        }
        overflow.addItem(id.action_watched_hide, string.action_watched_hide)
      }
      COLLECTION -> {
        if (airedCount - typeCount > 0) {
          overflow.addItem(id.action_collection_add, string.action_collect_next)
        }
        overflow.addItem(id.action_collection_hide, string.action_collection_hide)
      }
    }
  }

  class ViewHolder(binding: ListRowShowBinding) : RecyclerView.ViewHolder(binding.root) {
    val title = binding.title
    val watched = binding.watched
    val progressBar = binding.progress
    val nextEpisode = binding.nextEpisode
    val firstAired = binding.firstAired
    val overflow = binding.overflow
    val poster = binding.poster

    var showTypeCount = 0
    var showAiredCount = 0
    var episodeTitle: String? = null
    var episodeId: Long = 0
  }

  init {
    setHasStableIds(true)
  }
}
