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
package net.simonvt.cathode.ui.shows.upcoming

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import net.simonvt.cathode.R.id
import net.simonvt.cathode.R.layout
import net.simonvt.cathode.R.string
import net.simonvt.cathode.common.ui.FragmentsUtils
import net.simonvt.cathode.common.ui.adapter.HeaderAdapter
import net.simonvt.cathode.common.widget.OverflowView.OverflowActionListener
import net.simonvt.cathode.databinding.ListRowUpcomingBinding
import net.simonvt.cathode.entity.ShowWithEpisode
import net.simonvt.cathode.images.ImageType.POSTER
import net.simonvt.cathode.images.ImageUri
import net.simonvt.cathode.provider.util.DataHelper
import net.simonvt.cathode.ui.dialog.CheckInDialog
import net.simonvt.cathode.ui.dialog.CheckInDialog.Type.SHOW
import net.simonvt.cathode.ui.history.AddToHistoryDialog
import net.simonvt.cathode.ui.history.AddToHistoryDialog.Type.EPISODE

class UpcomingAdapter(
  private val activity: FragmentActivity,
  private val callbacks: Callbacks
) : HeaderAdapter<ShowWithEpisode, ViewHolder>(activity) {

  interface Callbacks {
    fun onEpisodeClicked(episodeId: Long, showTitle: String?)
    fun onCheckin(episodeId: Long)
    fun onCancelCheckin()
  }

  override fun getItemViewType(headerRes: Int, item: ShowWithEpisode): Int {
    return TYPE_ITEM
  }

  override fun getItemId(item: ShowWithEpisode): Long {
    return item.show.id
  }

  override fun onCreateItemHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val binding = ListRowUpcomingBinding.inflate(LayoutInflater.from(activity), parent, false)
    val holder = ItemViewHolder(binding)
    holder.itemView.setOnClickListener {
      val position = holder.adapterPosition
      if (position != RecyclerView.NO_POSITION) {
        val showWithEpisode = getItem(position)
        if (holder.watching) {
          callbacks.onEpisodeClicked(holder.watchingId!!, showWithEpisode!!.show.title)
        } else {
          callbacks.onEpisodeClicked(showWithEpisode!!.episode.id, showWithEpisode.show.title)
        }
      }
    }
    return holder
  }

  override fun onCreateHeaderHolder(parent: ViewGroup): ViewHolder {
    val v = LayoutInflater.from(activity).inflate(layout.list_row_upcoming_header, parent, false)
    return HeaderViewHolder(v as TextView)
  }

  override fun onViewRecycled(holder: ViewHolder) {
    if (holder is ItemViewHolder) {
      holder.checkIn.dismiss()
      holder.checkIn.reset()
      holder.watching = false
      holder.watchingId = null
    }
  }

  override fun onBindHeader(holder: ViewHolder, headerRes: Int) {
    val vh = holder as HeaderViewHolder
    vh.header.setText(headerRes)
  }

  override fun onBindViewHolder(
    holder: ViewHolder,
    showWithEpisode: ShowWithEpisode,
    position: Int
  ) {
    val vh = holder as ItemViewHolder
    val showId = showWithEpisode.show.id
    val watching = showWithEpisode.show.watching
    val episodeId = showWithEpisode.episode.id
    val episodeFirstAired = showWithEpisode.episode.firstAired
    val episodeSeasonNumber = showWithEpisode.episode.season
    val episodeNumber = showWithEpisode.episode.episode
    val watched = showWithEpisode.episode.watched
    val episodeTitle = DataHelper.getEpisodeTitle(
      activity,
      showWithEpisode.episode.title,
      episodeSeasonNumber,
      episodeNumber,
      watched,
      true
    )
    val watchingEpisodeId = showWithEpisode.show.watchingEpisodeId
    val showPosterUri = ImageUri.create(ImageUri.ITEM_SHOW, POSTER, showId)
    vh.title.text = showWithEpisode.show.title
    vh.poster.setImage(showPosterUri)
    vh.watching = watching
    vh.watchingId = watchingEpisodeId
    if (watching) {
      vh.nextEpisode.setText(string.show_watching)
    } else {
      vh.nextEpisode.text = episodeTitle
    }
    vh.firstAired.visibility = View.VISIBLE
    vh.firstAired.setTimeInMillis(episodeFirstAired)
    vh.checkIn.setWatching(watching)
    vh.checkIn.setId(showId)
    vh.checkIn.setListener(object : OverflowActionListener {
      override fun onPopupShown() {}
      override fun onPopupDismissed() {}
      override fun onActionSelected(action: Int) {
        if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
          when (action) {
            id.action_checkin_cancel -> {
              callbacks.onCancelCheckin()
              vh.checkIn.setWatching(false)
            }
            id.action_checkin -> if (!CheckInDialog.showDialogIfNecessary(
                activity,
                SHOW,
                episodeTitle,
                episodeId
              )
            ) {
              callbacks.onCheckin(episodeId)
              vh.checkIn.setWatching(true)
            }
            id.action_history_add -> FragmentsUtils.instantiate(
              activity.supportFragmentManager,
              AddToHistoryDialog::class.java,
              AddToHistoryDialog.getArgs(EPISODE, episodeId, episodeTitle)
            ).show(activity.supportFragmentManager, AddToHistoryDialog.TAG)
          }
        }
      }
    })
  }

  class HeaderViewHolder(var header: TextView) : ViewHolder(header)

  class ItemViewHolder(binding: ListRowUpcomingBinding) : ViewHolder(binding.root) {
    var title = binding.title
    var nextEpisode = binding.nextEpisode
    var firstAired = binding.firstAired
    var checkIn = binding.checkIn
    var poster = binding.poster

    var watching = false
    var watchingId: Long? = null
  }

  companion object {
    private const val TYPE_ITEM = 0
  }

  init {
    setHasStableIds(true)
  }
}
