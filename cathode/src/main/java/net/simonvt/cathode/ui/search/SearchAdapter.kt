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
package net.simonvt.cathode.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import net.simonvt.cathode.R
import net.simonvt.cathode.common.ui.adapter.NoLogicViewHolder
import net.simonvt.cathode.search.SearchHandler.SearchItem

class SearchAdapter(val listener: OnResultClickListener) :
  RecyclerView.Adapter<ViewHolder>() {

  private var recentQueries: List<String>? = null

  private var displaySearching: Boolean = false

  private var results: List<SearchItem>? = null

  interface OnResultClickListener {

    fun onShowClicked(showId: Long, title: String?, overview: String?)

    fun onMovieClicked(movieId: Long, title: String?, overview: String?)

    fun onQueryClicked(query: String)
  }

  fun setRecentQueries(recentQueries: List<String>) {
    val oldQueries = this.recentQueries
    this.recentQueries = recentQueries

    val hadItems = !oldQueries.isNullOrEmpty()
    val hasItems = recentQueries.isNotEmpty()
    if (hasItems) {
      if (!hadItems) {
        notifyItemInserted(0)
      } else {
        notifyItemChanged(0)
      }
    } else {
      if (hadItems) {
        notifyItemRemoved(0)
      } else {
        notifyItemChanged(0)
      }
    }
  }

  private fun hasRecentQueries(): Boolean {
    return !recentQueries.isNullOrEmpty()
  }

  fun setSearching(displaySearching: Boolean) {
    if (displaySearching != this.displaySearching) {
      this.displaySearching = displaySearching

      val hasRecentQueries = hasRecentQueries()
      val offset = if (hasRecentQueries) 1 else 0

      if (displaySearching) {
        if (results.isNullOrEmpty()) {
          notifyItemRemoved(offset)
        } else {
          notifyItemRangeRemoved(offset, results!!.size)
        }

        notifyItemInserted(offset)
      } else {
        notifyItemRemoved(offset)
        if (results.isNullOrEmpty()) {
          notifyItemInserted(offset)
        } else {
          notifyItemRangeInserted(offset, results!!.size)
        }
      }
    }
  }

  fun setResults(results: List<SearchItem>?) {
    if (displaySearching) {
      throw IllegalStateException("Results must now be set while in searching state.")
    }

    val oldResults = this.results
    this.results = results

    val hasRecentQueries = hasRecentQueries()
    val offset = if (hasRecentQueries) 1 else 0

    if (!displaySearching) {
      if (oldResults == null) {
        if (results != null) {
          notifyItemRemoved(offset)

          if (results.isEmpty()) {
            notifyItemInserted(offset)
          } else {
            notifyItemRangeInserted(offset, results.size)
          }
        }
      } else if (oldResults.isEmpty()) {
        if (results == null) {
          notifyItemRemoved(offset)
          notifyItemInserted(offset)
        } else if (results.isNotEmpty()) {
          notifyItemRemoved(offset)
          notifyItemRangeInserted(offset, results.size)
        }
      } else {
        if (results == null || results.isEmpty()) {
          notifyItemRangeRemoved(offset, oldResults.size)
          notifyItemInserted(offset)
        } else {
          diffLists(oldResults, results)
        }
      }
    }
  }

  private fun diffLists(oldResults: List<SearchItem>?, newResults: List<SearchItem>?) {
    val hasRecentQueries = hasRecentQueries()
    val offset = if (hasRecentQueries) 1 else 0

    DiffUtil.calculateDiff(object : DiffUtil.Callback() {
      override fun getOldListSize(): Int {
        var size = offset
        if (oldResults != null) {
          size += oldResults.size
        }
        return size
      }

      override fun getNewListSize(): Int {
        var size = offset
        if (newResults != null) {
          size += newResults.size
        }
        return size
      }

      override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        if (hasRecentQueries) {
          if (oldItemPosition == 0 || newItemPosition == 0) {
            return oldItemPosition == newItemPosition
          }
        }

        val (oldItemType, oldItemId) = oldResults!![oldItemPosition - offset]
        val (newItemType, newItemId) = newResults!![newItemPosition - offset]

        return newItemType == oldItemType && newItemId == oldItemId
      }

      override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        if (hasRecentQueries && oldItemPosition == 0 && newItemPosition == 0) {
          return true
        }

        val oldItem = oldResults!![oldItemPosition - offset]
        val newItem = newResults!![newItemPosition - offset]

        return newItem == oldItem
      }
    }).dispatchUpdatesTo(this)
  }

  override fun getItemCount(): Int {
    var size = if (!recentQueries.isNullOrEmpty()) 1 else 0

    if (!displaySearching && !results.isNullOrEmpty()) {
      size += results!!.size
    } else {
      size += 1
    }

    return size
  }

  override fun getItemViewType(position: Int): Int {
    if (position == 0 && hasRecentQueries()) {
      return TYPE_RECENT
    }

    if (displaySearching) {
      return TYPE_SEARCHING
    }

    if (results == null) {
      return TYPE_SEARCH
    }

    return if (results.isNullOrEmpty()) TYPE_NO_RESULTS else TYPE_RESULT
  }

  override fun onViewAttachedToWindow(holder: ViewHolder) {
    if (holder.itemViewType == TYPE_SEARCHING) {
      (holder as SearchingHolder).drawable.start()
    }
  }

  override fun onViewDetachedFromWindow(holder: ViewHolder) {
    if (holder.itemViewType == TYPE_SEARCHING) {
      (holder as SearchingHolder).drawable.stop()
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    when (viewType) {
      TYPE_SEARCHING -> {
        val view = LayoutInflater.from(parent.context)
          .inflate(R.layout.search_searching, parent, false)
        return SearchingHolder(view)
      }
      TYPE_SEARCH -> {
        val view = LayoutInflater.from(parent.context)
          .inflate(R.layout.search_something, parent, false)
        return NoLogicViewHolder(view)
      }
      TYPE_NO_RESULTS -> {
        val view = LayoutInflater.from(parent.context)
          .inflate(R.layout.search_no_results, parent, false)
        return NoLogicViewHolder(view)
      }
      TYPE_RECENT -> {
        val view =
          LayoutInflater.from(parent.context).inflate(R.layout.search_recents, parent, false)
        return RecentsViewHolder(view, listener)
      }
      else -> {
        val view =
          LayoutInflater.from(parent.context).inflate(R.layout.search_result, parent, false)
        return ResultViewHolder(view, listener)
      }
    }
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    when (holder) {
      is RecentsViewHolder -> holder.update(recentQueries!!)
      is ResultViewHolder -> {
        val offset = if (hasRecentQueries()) 1 else 0
        holder.update(results!![position - offset])
      }
    }
  }

  companion object {

    private const val TYPE_RECENT = 0
    private const val TYPE_RESULT = 1
    private const val TYPE_SEARCH = 2
    private const val TYPE_SEARCHING = 3
    private const val TYPE_NO_RESULTS = 4
  }
}
