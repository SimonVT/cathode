/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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
package net.simonvt.cathode.settings.hidden

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import net.simonvt.cathode.R.id
import net.simonvt.cathode.R.string
import net.simonvt.cathode.common.ui.adapter.HeaderAdapter
import net.simonvt.cathode.common.widget.OverflowView
import net.simonvt.cathode.common.widget.OverflowView.OverflowActionListener
import net.simonvt.cathode.databinding.ListRowUpcomingHeaderBinding
import net.simonvt.cathode.databinding.RowListMovieBinding
import net.simonvt.cathode.databinding.RowListShowBinding
import net.simonvt.cathode.entity.Movie
import net.simonvt.cathode.entity.Show
import net.simonvt.cathode.images.ImageType.POSTER
import net.simonvt.cathode.images.ImageUri

class HiddenItemsAdapter(
  context: Context,
  private val itemCallbacks: ItemCallbacks
) : HeaderAdapter<Any?, ViewHolder>(context) {

  interface ItemCallbacks {
    fun onShowClicked(showId: Long, title: String?, overview: String?)
    fun displayShowInCalendar(showId: Long)
    fun displayShowInWatched(showId: Long)
    fun displayShowInCollection(showId: Long)
    fun onMovieClicked(movieId: Long, title: String?, overview: String?)
    fun displayMovieInCalendar(movieId: Long)
  }

  override fun getItemViewType(headerRes: Int, item: Any?): Int {
    return when (headerRes) {
      string.header_hidden_calendar_shows, string.header_hidden_watched_shows, string.header_hidden_collected_shows -> TYPE_SHOW
      else -> TYPE_MOVIE
    }
  }

  override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
    if (oldItem.javaClass == newItem.javaClass) {
      if (oldItem is Show) {
        return oldItem.id == (newItem as Show).id
      } else if (oldItem is Movie) {
        return oldItem.id == (newItem as Movie).id
      }
    }
    return false
  }

  override fun onCreateItemHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val holder: ListViewHolder
    if (viewType == TYPE_SHOW) {
      val binding = RowListShowBinding.inflate(LayoutInflater.from(context), parent, false)
      val showHolder = ShowViewHolder(binding)
      holder = showHolder
      showHolder.overflow.addItem(id.action_unhide, string.action_unhide)
      showHolder.overflow.setListener(object : OverflowActionListener {
        override fun onPopupShown() {}
        override fun onPopupDismissed() {}
        override fun onActionSelected(action: Int) {
          val position = showHolder.adapterPosition
          if (position != RecyclerView.NO_POSITION) {
            val show =
              getItem(position) as Show?
            val itemId = show!!.id
            when (action) {
              id.action_unhide -> {
                val headerRes = getHeaderRes(position)
                if (headerRes == string.header_hidden_calendar_shows) {
                  itemCallbacks.displayShowInCalendar(itemId)
                } else if (headerRes == string.header_hidden_watched_shows) {
                  itemCallbacks.displayShowInWatched(itemId)
                } else {
                  itemCallbacks.displayShowInCollection(itemId)
                }
              }
            }
          }
        }
      })
      binding.root.setOnClickListener {
        val position = showHolder.adapterPosition
        if (position != RecyclerView.NO_POSITION) {
          val show =
            getItem(position) as Show?
          val itemId = show!!.id
          itemCallbacks.onShowClicked(itemId, show.title, show.overview)
        }
      }
    } else {
      val binding = RowListMovieBinding.inflate(LayoutInflater.from(context), parent, false)
      val movieHolder = MovieViewHolder(binding)
      holder = movieHolder
      movieHolder.overflow.addItem(id.action_unhide, string.action_unhide)
      movieHolder.overflow.setListener(object : OverflowActionListener {
        override fun onPopupShown() {}
        override fun onPopupDismissed() {}
        override fun onActionSelected(action: Int) {
          val position = movieHolder.adapterPosition
          if (position.toLong() != RecyclerView.NO_ID) {
            val movie =
              getItem(position) as Movie?
            val itemId = movie!!.id
            when (action) {
              id.action_unhide -> {
                val headerRes = getHeaderRes(position)
                if (headerRes == string.header_hidden_calendar_movies) {
                  itemCallbacks.displayMovieInCalendar(itemId)
                }
              }
            }
          }
        }
      })
      binding.root.setOnClickListener {
        val position = movieHolder.adapterPosition
        if (position != RecyclerView.NO_POSITION) {
          val movie =
            getItem(position) as Movie?
          val itemId = movie!!.id
          itemCallbacks.onMovieClicked(itemId, movie.title, movie.overview)
        }
      }
    }
    return holder
  }

  override fun onCreateHeaderHolder(parent: ViewGroup): ViewHolder {
    val binding = ListRowUpcomingHeaderBinding.inflate(LayoutInflater.from(context), parent, false)
    return HeaderViewHolder(binding.root as TextView)
  }

  override fun onViewRecycled(holder: ViewHolder) {
    if (holder is ListViewHolder) {
      holder.overflow.dismiss()
    }
  }

  override fun onBindHeader(holder: ViewHolder, headerRes: Int) {
    (holder as HeaderViewHolder).header.setText(headerRes)
  }

  override fun onBindViewHolder(holder: ViewHolder, type: Any?, position: Int) {
    if (holder.itemViewType == TYPE_SHOW) {
      val vh = holder as ShowViewHolder
      val show = getItem(position) as Show
      val poster = ImageUri.create(ImageUri.ITEM_SHOW, POSTER, show.id)
      vh.poster.setImage(poster)
      vh.title.text = show.title
      vh.overview.text = show.overview
    } else {
      val vh = holder as MovieViewHolder
      val movie = getItem(position) as Movie
      val poster = ImageUri.create(ImageUri.ITEM_MOVIE, POSTER, movie.id)
      vh.poster.setImage(poster)
      vh.title.text = movie.title
      vh.overview.text = movie.overview
    }
  }

  class HeaderViewHolder(var header: TextView) : ViewHolder(header)

  open class ListViewHolder(v: View, val overflow: OverflowView) : ViewHolder(v)

  class ShowViewHolder(binding: RowListShowBinding) :
    ListViewHolder(binding.root, binding.overflow) {
    val poster = binding.poster
    val title = binding.title
    val overview = binding.overview
  }

  class MovieViewHolder(binding: RowListMovieBinding) :
    ListViewHolder(binding.root, binding.overflow) {
    val poster = binding.poster
    val title = binding.title
    val overview = binding.overview
  }

  companion object {
    private const val TYPE_SHOW = 0
    private const val TYPE_MOVIE = 1
  }
}
