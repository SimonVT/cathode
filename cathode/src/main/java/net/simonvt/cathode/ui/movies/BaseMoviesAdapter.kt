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
package net.simonvt.cathode.ui.movies

import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import net.simonvt.cathode.R
import net.simonvt.cathode.R.id
import net.simonvt.cathode.R.string
import net.simonvt.cathode.common.ui.FragmentsUtils
import net.simonvt.cathode.common.ui.adapter.BaseAdapter
import net.simonvt.cathode.common.widget.CircularProgressIndicator
import net.simonvt.cathode.common.widget.OverflowView
import net.simonvt.cathode.common.widget.RemoteImageView
import net.simonvt.cathode.common.widget.find
import net.simonvt.cathode.common.widget.findOrNull
import net.simonvt.cathode.entity.Movie
import net.simonvt.cathode.images.ImageType.POSTER
import net.simonvt.cathode.images.ImageUri
import net.simonvt.cathode.settings.TraktLinkSettings
import net.simonvt.cathode.ui.dialog.CheckInDialog
import net.simonvt.cathode.ui.history.AddToHistoryDialog
import net.simonvt.cathode.ui.history.AddToHistoryDialog.Type.MOVIE
import net.simonvt.cathode.ui.history.RemoveFromHistoryDialog
import net.simonvt.cathode.ui.history.RemoveFromHistoryDialog.Type
import net.simonvt.cathode.ui.movies.BaseMoviesAdapter.ViewHolder

abstract class BaseMoviesAdapter<T : ViewHolder?>(
  protected val activity: FragmentActivity,
  protected val callbacks: Callbacks
) : BaseAdapter<Movie, T>(activity) {

  interface Callbacks {
    fun onMovieClicked(movieId: Long, title: String?, overview: String?)
    fun onCheckin(movieId: Long)
    fun onCancelCheckin()
    fun onWatchlistAdd(movieId: Long)
    fun onWatchlistRemove(movieId: Long)
    fun onCollectionAdd(movieId: Long)
    fun onCollectionRemove(movieId: Long)
  }

  init {
    setHasStableIds(true)
  }

  override fun getItemId(position: Int): Long {
    return list[position]!!.id
  }

  override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean {
    return oldItem.id == newItem.id
  }

  override fun onViewRecycled(holder: T) {
    holder?.overflow?.dismiss()
  }

  override fun onBindViewHolder(holder: T, position: Int) {
    val movie = list[position]!!
    val poster = ImageUri.create(ImageUri.ITEM_MOVIE, POSTER, movie.id)
    holder!!.poster.setImage(poster)
    holder.title.text = movie.title
    holder.overview.text = movie.overview
    holder.rating?.setValue(movie.rating)
    holder.overflow.removeItems()
    setupOverflowItems(
      holder.overflow,
      movie.watched,
      movie.inCollection,
      movie.inWatchlist,
      movie.watching,
      movie.checkedIn
    )
  }

  protected open fun setupOverflowItems(
    overflow: OverflowView,
    watched: Boolean,
    collected: Boolean,
    inWatchlist: Boolean,
    watching: Boolean,
    checkedIn: Boolean
  ) {
    if (checkedIn) {
      overflow.addItem(id.action_checkin_cancel, string.action_checkin_cancel)
    } else if (watched) {
      overflow.addItem(id.action_history_remove, string.action_history_remove)
    } else if (inWatchlist) {
      overflow.addItem(id.action_checkin, string.action_checkin)
      overflow.addItem(id.action_watchlist_remove, string.action_watchlist_remove)
    } else {
      if (!watching) overflow.addItem(id.action_checkin, string.action_checkin)
      overflow.addItem(id.action_watchlist_add, string.action_watchlist_add)
    }
    if (collected) {
      overflow.addItem(id.action_collection_remove, string.action_collection_remove)
    } else {
      overflow.addItem(id.action_collection_add, string.action_collection_add)
    }
  }

  protected open fun onOverflowActionSelected(
    view: View?,
    id: Long,
    action: Int,
    position: Int,
    title: String?
  ) {
    when (action) {
      R.id.action_history_add -> FragmentsUtils.instantiate(
        activity.supportFragmentManager,
        AddToHistoryDialog::class.java,
        AddToHistoryDialog.getArgs(MOVIE, id, title)
      ).show(activity.supportFragmentManager, AddToHistoryDialog.TAG)
      R.id.action_history_remove -> if (TraktLinkSettings.isLinked(context)) {
        FragmentsUtils.instantiate(
          activity.supportFragmentManager,
          RemoveFromHistoryDialog::class.java,
          RemoveFromHistoryDialog.getArgs(Type.MOVIE, id, title, null)
        ).show(activity.supportFragmentManager, RemoveFromHistoryDialog.TAG)
      }
      R.id.action_checkin -> if (!CheckInDialog.showDialogIfNecessary(
          activity,
          CheckInDialog.Type.MOVIE,
          title,
          id
        )
      ) {
        callbacks.onCheckin(id)
      }
      R.id.action_checkin_cancel -> callbacks.onCancelCheckin()
      R.id.action_watchlist_add -> callbacks.onWatchlistAdd(id)
      R.id.action_watchlist_remove -> callbacks.onWatchlistRemove(id)
      R.id.action_collection_add -> callbacks.onCollectionAdd(id)
      R.id.action_collection_remove -> callbacks.onCollectionRemove(id)
    }
  }

  class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
    val poster: RemoteImageView = v.find(R.id.poster)
    val title: TextView = v.find(R.id.title)
    val overview: TextView = v.find(R.id.overview)
    val overflow: OverflowView = v.find(R.id.overflow)
    val rating: CircularProgressIndicator? = v.findOrNull(R.id.rating)
  }
}
