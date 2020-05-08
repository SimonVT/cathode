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

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.simonvt.cathode.R
import net.simonvt.cathode.common.ui.adapter.BaseAdapter
import net.simonvt.cathode.common.widget.CircularProgressIndicator
import net.simonvt.cathode.common.widget.OverflowView
import net.simonvt.cathode.common.widget.OverflowView.OverflowActionListener
import net.simonvt.cathode.common.widget.RemoteImageView
import net.simonvt.cathode.common.widget.find
import net.simonvt.cathode.common.widget.findOrNull
import net.simonvt.cathode.entity.Show
import net.simonvt.cathode.images.ImageType.POSTER
import net.simonvt.cathode.images.ImageUri
import net.simonvt.cathode.ui.shows.ShowDescriptionAdapter.ViewHolder
import net.simonvt.cathode.widget.IndicatorView

open class ShowDescriptionAdapter(
  context: Context,
  private var callbacks: ShowCallbacks,
  private val displayRating: Boolean
) : BaseAdapter<Show?, ViewHolder?>(context) {

  interface ShowCallbacks {
    fun onShowClick(showId: Long, title: String?, overview: String?)
    fun setIsInWatchlist(showId: Long, inWatchlist: Boolean)
  }

  constructor(context: Context, callbacks: ShowCallbacks) : this(context, callbacks, true) {
    this.callbacks = callbacks
  }

  override fun getItemId(position: Int): Long {
    return list[position]!!.id
  }

  override fun areItemsTheSame(oldItem: Show, newItem: Show): Boolean {
    return oldItem.id == newItem.id
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val v: View = if (displayRating) {
      LayoutInflater.from(context)
        .inflate(R.layout.list_row_show_description_rating, parent, false)
    } else {
      LayoutInflater.from(context)
        .inflate(R.layout.list_row_show_description, parent, false)
    }
    val holder = ViewHolder(v)
    v.setOnClickListener {
      val position = holder.adapterPosition
      if (position != RecyclerView.NO_POSITION) {
        val show = list[position]!!
        callbacks.onShowClick(show.id, show.title, show.overview)
      }
    }
    holder.overflow.setListener(object : OverflowActionListener {
      override fun onPopupShown() {}
      override fun onPopupDismissed() {}
      override fun onActionSelected(action: Int) {
        val position = holder.adapterPosition
        if (position != RecyclerView.NO_POSITION) {
          onOverflowActionSelected(holder.itemView, holder.itemId, action, position)
        }
      }
    })
    return holder
  }

  override fun onViewRecycled(holder: ViewHolder) {
    holder.overflow.dismiss()
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val show = list[position]!!
    val watched = show.watchedCount > 0
    val inCollection = show.inCollectionCount > 1
    val poster = ImageUri.create(ImageUri.ITEM_SHOW, POSTER, show.id)
    holder.indicator.setWatched(watched)
    holder.indicator.setCollected(inCollection)
    holder.indicator.setInWatchlist(show.inWatchlist)
    holder.poster.setImage(poster)
    holder.title.text = show.title
    holder.overview.text = show.overview
    if (displayRating) {
      holder.rating?.setValue(show.rating)
    }
    holder.overflow.removeItems()
    setupOverflowItems(holder.overflow, show.inWatchlist)
  }

  protected open fun setupOverflowItems(overflow: OverflowView, inWatchlist: Boolean) {
    if (inWatchlist) {
      overflow.addItem(R.id.action_watchlist_remove, R.string.action_watchlist_remove)
    } else {
      overflow.addItem(R.id.action_watchlist_add, R.string.action_watchlist_add)
    }
  }

  protected open fun onOverflowActionSelected(view: View?, id: Long, action: Int, position: Int) {
    when (action) {
      R.id.action_watchlist_add -> callbacks.setIsInWatchlist(id, true)
      R.id.action_watchlist_remove -> onWatchlistRemove(id)
    }
  }

  private fun onWatchlistRemove(showId: Long) {
    callbacks.setIsInWatchlist(showId, false)
  }

  class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
    var poster: RemoteImageView = v.find(R.id.poster)
    var indicator: IndicatorView = v.find(R.id.indicator)
    var title: TextView = v.find(R.id.title)
    var overview: TextView = v.find(R.id.overview)
    var overflow: OverflowView = v.find(R.id.overflow)
    var rating: CircularProgressIndicator? = v.findOrNull(R.id.rating)
  }

  init {
    setHasStableIds(true)
  }
}
