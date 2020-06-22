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

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import net.simonvt.cathode.R
import net.simonvt.cathode.common.ui.FragmentsUtils
import net.simonvt.cathode.common.ui.adapter.BaseAdapter
import net.simonvt.cathode.databinding.ListRowEpisodeBinding
import net.simonvt.cathode.entity.Episode
import net.simonvt.cathode.images.ImageType.STILL
import net.simonvt.cathode.images.ImageUri
import net.simonvt.cathode.provider.util.DataHelper
import net.simonvt.cathode.settings.TraktLinkSettings
import net.simonvt.cathode.ui.LibraryType
import net.simonvt.cathode.ui.LibraryType.COLLECTION
import net.simonvt.cathode.ui.history.AddToHistoryDialog
import net.simonvt.cathode.ui.history.AddToHistoryDialog.Type
import net.simonvt.cathode.ui.history.RemoveFromHistoryDialog
import net.simonvt.cathode.ui.history.RemoveFromHistoryDialog.Type.EPISODE
import net.simonvt.cathode.ui.show.SeasonAdapter.ViewHolder

class SeasonAdapter(
  private val activity: FragmentActivity,
  private val callbacks: EpisodeCallbacks,
  private val type: LibraryType
) : BaseAdapter<Episode?, ViewHolder?>(activity) {

  interface EpisodeCallbacks {
    fun onEpisodeClick(episodeId: Long)
    fun setEpisodeCollected(episodeId: Long, collected: Boolean)
  }

  private val resources: Resources = activity.resources

  init {
    setHasStableIds(true)
  }

  override fun getItemId(position: Int): Long {
    return list[position]!!.id
  }

  override fun areItemsTheSame(oldItem: Episode, newItem: Episode): Boolean {
    return oldItem.id == newItem.id
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val binding = ListRowEpisodeBinding.inflate(LayoutInflater.from(context), parent, false)
    val holder = ViewHolder(binding)
    binding.root.setOnClickListener {
      val position = holder.adapterPosition
      if (position != RecyclerView.NO_POSITION) {
        callbacks.onEpisodeClick(holder.itemId)
      }
    }
    holder.number.setOnClickListener {
      if (holder.adapterPosition != RecyclerView.NO_POSITION) {
        val activated = holder.number.isActivated
        if (type === COLLECTION) {
          holder.number.isActivated = !activated
          callbacks.setEpisodeCollected(holder.itemId, !activated)
        } else {
          if (activated) {
            if (TraktLinkSettings.isLinked(context)) {
              FragmentsUtils.instantiate(
                activity.supportFragmentManager,
                RemoveFromHistoryDialog::class.java,
                RemoveFromHistoryDialog.getArgs(
                  EPISODE,
                  holder.itemId, holder.episodeTitle, holder.showTitle
                )
              )
                .show(activity.supportFragmentManager, RemoveFromHistoryDialog.TAG)
            }
          } else {
            FragmentsUtils.instantiate(
              activity.supportFragmentManager,
              AddToHistoryDialog::class.java,
              AddToHistoryDialog.getArgs(
                Type.EPISODE, holder.itemId,
                holder.episodeTitle
              )
            )
              .show(activity.supportFragmentManager, AddToHistoryDialog.TAG)
          }
        }
      }
    }
    return holder
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val episode = list[position]!!
    val id = episode.id
    val title = DataHelper.getEpisodeTitle(
      context,
      episode.title,
      episode.season,
      episode.episode,
      episode.watched
    )
    val showTitle = episode.showTitle
    val screenshotUri =
      ImageUri.create(ImageUri.ITEM_EPISODE, STILL, id)
    holder.episodeTitle = title
    holder.showTitle = showTitle
    holder.screen.setImage(screenshotUri)
    holder.title.text = title
    holder.firstAired.setTimeInMillis(episode.firstAired)
    holder.firstAired.setExtended(true)
    holder.number.text = episode.episode.toString()
    if (type === COLLECTION) {
      ContextCompat.getColorStateList(context, R.color.episode_number_collected)
      holder.number.setTextColor(
        ContextCompat.getColorStateList(
          context,
          R.color.episode_number_collected
        )
      )
      holder.number.isActivated = episode.inCollection
    } else {
      holder.number.setTextColor(
        ContextCompat.getColorStateList(
          context,
          R.color.episode_number_watched
        )
      )
      holder.number.isActivated = episode.watched
    }
  }

  class ViewHolder constructor(binding: ListRowEpisodeBinding) :
    RecyclerView.ViewHolder(binding.root) {

    val screen = binding.screen
    val title = binding.title
    val firstAired = binding.firstAired
    val number = binding.number

    var episodeTitle: String? = null
    var showTitle: String? = null
  }
}
