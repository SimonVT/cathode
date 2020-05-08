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
package net.simonvt.cathode.ui.show

import android.R.attr
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import net.simonvt.cathode.R.plurals
import net.simonvt.cathode.R.string
import net.simonvt.cathode.common.ui.adapter.BaseAdapter
import net.simonvt.cathode.databinding.ListRowSeasonBinding
import net.simonvt.cathode.entity.Season
import net.simonvt.cathode.images.ImageType.POSTER
import net.simonvt.cathode.images.ImageUri
import net.simonvt.cathode.ui.LibraryType
import net.simonvt.cathode.ui.LibraryType.COLLECTION
import net.simonvt.cathode.ui.LibraryType.WATCHED
import net.simonvt.cathode.ui.LibraryType.WATCHLIST
import net.simonvt.cathode.ui.show.SeasonsAdapter.ViewHolder

class SeasonsAdapter(
  private val activity: FragmentActivity,
  private val clickListener: SeasonClickListener,
  private val type: LibraryType
) : BaseAdapter<Season, ViewHolder>(activity) {

  interface SeasonClickListener {
    fun onSeasonClick(showId: Long, seasonId: Long, showTitle: String?, seasonNumber: Int)
  }

  private val resources = activity.resources

  private val primaryColor: ColorStateList?
  private val secondaryColor: ColorStateList?

  override fun getItemId(position: Int): Long {
    return list[position]!!.id
  }

  override fun areItemsTheSame(oldItem: Season, newItem: Season): Boolean {
    return oldItem.id == newItem.id
  }

  override fun areContentsTheSame(oldItem: Season, newItem: Season): Boolean {
    return oldItem == newItem
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewGroup: Int): ViewHolder {
    val binding = ListRowSeasonBinding.inflate(LayoutInflater.from(context), parent, false)
    val holder = ViewHolder(binding)
    binding.root.setOnClickListener {
      val position = holder.adapterPosition
      if (position != RecyclerView.NO_POSITION) {
        val season = list[position]!!
        clickListener.onSeasonClick(
          season.showId, season.id, season.showTitle,
          season.season
        )
      }
    }
    return holder
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val season = list[position]!!
    val seasonId = season.id
    val seasonNumber = season.season
    when (type) {
      WATCHLIST, WATCHED -> bindWatched(context, holder, season)
      COLLECTION -> bindCollection(context, holder, season)
    }
    if (seasonNumber == 0) {
      holder.title.setText(string.season_special)
    } else {
      holder.title.text = resources.getString(string.season_x, seasonNumber)
    }
    val posterUri = ImageUri.create(ImageUri.ITEM_SEASON, POSTER, seasonId)
    holder.poster.setImage(posterUri)
  }

  private fun bindWatched(context: Context, holder: ViewHolder, season: Season) {
    val unairedCount = season.unairedCount!!
    val airedCount = season.airedCount!!
    val watchedAiredCount = season.watchedAiredCount!!
    val toWatch = airedCount - watchedAiredCount
    holder.progress.max = airedCount
    holder.progress.progress = watchedAiredCount
    val summary: String
    summary = if (toWatch > 0 && unairedCount > 0) {
      resources.getQuantityString(
        plurals.x_unwatched_x_unaired, toWatch, toWatch,
        unairedCount
      )
    } else if (toWatch > 0 && unairedCount == 0) {
      resources.getQuantityString(plurals.x_unwatched, toWatch, toWatch)
    } else if (toWatch == 0 && unairedCount > 0) {
      resources.getQuantityString(plurals.x_unaired, unairedCount, unairedCount)
    } else {
      resources.getString(string.all_watched)
    }
    holder.summary.text = summary
  }

  private fun bindCollection(context: Context, holder: ViewHolder, season: Season) {
    val unairedCount = season.unairedCount!!
    val airedCount = season.airedCount!!
    val collectedAiredCount = season.collectedAiredCount!!
    val toCollect = airedCount - collectedAiredCount
    holder.progress.max = airedCount
    holder.progress.progress = collectedAiredCount
    val summary: String
    summary = if (toCollect > 0 && unairedCount > 0) {
      resources.getQuantityString(
        plurals.x_uncollected_x_unaired, toCollect, toCollect,
        unairedCount
      )
    } else if (toCollect > 0 && unairedCount == 0) {
      resources.getQuantityString(plurals.x_uncollected, toCollect, toCollect)
    } else if (toCollect == 0 && unairedCount > 0) {
      resources.getQuantityString(plurals.x_unaired, unairedCount, unairedCount)
    } else {
      resources.getString(string.all_collected)
    }
    holder.summary.text = summary
  }

  class ViewHolder(binding: ListRowSeasonBinding) : RecyclerView.ViewHolder(binding.root) {
    var title = binding.title
    var progress = binding.progress
    var summary = binding.summary
    var poster = binding.poster
  }

  init {
    setHasStableIds(true)

    val a =
      activity.obtainStyledAttributes(intArrayOf(attr.textColorPrimary, attr.textColorSecondary))
    primaryColor = a.getColorStateList(0)
    secondaryColor = a.getColorStateList(1)
    a.recycle()
  }
}
