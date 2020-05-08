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
package net.simonvt.cathode.ui.lists

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import net.simonvt.cathode.R
import net.simonvt.cathode.api.enumeration.ItemType.EPISODE
import net.simonvt.cathode.api.enumeration.ItemType.MOVIE
import net.simonvt.cathode.api.enumeration.ItemType.PERSON
import net.simonvt.cathode.api.enumeration.ItemType.SEASON
import net.simonvt.cathode.api.enumeration.ItemType.SHOW
import net.simonvt.cathode.common.ui.adapter.BaseAdapter
import net.simonvt.cathode.common.widget.OverflowView
import net.simonvt.cathode.common.widget.OverflowView.OverflowActionListener
import net.simonvt.cathode.databinding.RowListEpisodeBinding
import net.simonvt.cathode.databinding.RowListMovieBinding
import net.simonvt.cathode.databinding.RowListPersonBinding
import net.simonvt.cathode.databinding.RowListSeasonBinding
import net.simonvt.cathode.databinding.RowListShowBinding
import net.simonvt.cathode.entity.ListItem
import net.simonvt.cathode.images.ImageType.POSTER
import net.simonvt.cathode.images.ImageType.PROFILE
import net.simonvt.cathode.images.ImageType.STILL
import net.simonvt.cathode.images.ImageUri
import net.simonvt.cathode.provider.util.DataHelper
import net.simonvt.cathode.ui.lists.ListAdapter.ListViewHolder

class ListAdapter(context: Context?, var listener: ListListener) :
  BaseAdapter<ListItem?, ListViewHolder?>(context) {

  interface ListListener {
    fun onShowClick(showId: Long, title: String?, overview: String?)
    fun onSeasonClick(showId: Long, seasonId: Long, showTitle: String?, seasonNumber: Int)
    fun onEpisodeClick(id: Long)
    fun onMovieClicked(movieId: Long, title: String?, overview: String?)
    fun onPersonClick(personId: Long)
    fun onRemoveItem(position: Int, listItem: ListItem)
  }

  override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
    return oldItem.listItemId == newItem.listItemId
  }

  override fun getItemViewType(position: Int): Int {
    val item = list[position]!!
    return when (item.type) {
      SHOW -> TYPE_SHOW
      SEASON -> TYPE_SEASON
      EPISODE -> TYPE_EPISODE
      MOVIE -> TYPE_MOVIE
      PERSON -> TYPE_PERSON
      else -> throw IllegalStateException("Unsupported item type ${item.type}")
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
    val holder: ListViewHolder
    if (viewType == TYPE_SHOW) {
      val binding = RowListShowBinding.inflate(LayoutInflater.from(context), parent, false)
      val showHolder = ShowViewHolder(binding)
      holder = showHolder
      binding.root.setOnClickListener {
        val position = showHolder.adapterPosition
        if (position != RecyclerView.NO_POSITION) {
          val show = list[position]!!.show!!
          listener.onShowClick(show.id, show.title, show.overview)
        }
      }
    } else if (viewType == TYPE_SEASON) {
      val binding = RowListSeasonBinding.inflate(LayoutInflater.from(context), parent, false)
      val seasonHolder = SeasonViewHolder(binding)
      holder = seasonHolder
      binding.root.setOnClickListener {
        val position = seasonHolder.adapterPosition
        if (position != RecyclerView.NO_POSITION) {
          val season = list[position]!!.season!!
          listener.onSeasonClick(season.showId, season.id, season.showTitle, season.season)
        }
      }
    } else if (viewType == TYPE_EPISODE) {
      val binding = RowListEpisodeBinding.inflate(LayoutInflater.from(context), parent, false)
      val episodeHolder = EpisodeViewHolder(binding)
      holder = episodeHolder
      binding.root.setOnClickListener {
        val position = episodeHolder.adapterPosition
        if (position != RecyclerView.NO_POSITION) {
          val episode = list[position]!!.episode!!
          listener.onEpisodeClick(episode.id)
        }
      }
    } else if (viewType == TYPE_MOVIE) {
      val binding = RowListMovieBinding.inflate(LayoutInflater.from(context), parent, false)
      val movieHolder = MovieViewHolder(binding)
      holder = movieHolder
      binding.root.setOnClickListener {
        val position = movieHolder.adapterPosition
        if (position != RecyclerView.NO_POSITION) {
          val movie = list[position]!!.movie!!
          listener.onMovieClicked(movie.id, movie.title, movie.overview)
        }
      }
    } else {
      val binding = RowListPersonBinding.inflate(LayoutInflater.from(context), parent, false)
      val personHolder = PersonViewHolder(binding)
      holder = personHolder
      binding.root.setOnClickListener {
        val position = personHolder.adapterPosition
        if (position != RecyclerView.NO_POSITION) {
          val item = list[position]!!
          listener.onPersonClick(item.person!!.id)
        }
      }
    }
    holder.overflow.addItem(R.id.action_list_remove, R.string.action_list_remove)
    holder.overflow.setListener(object : OverflowActionListener {
      override fun onPopupShown() {}
      override fun onPopupDismissed() {}
      override fun onActionSelected(action: Int) {
        val position = holder.adapterPosition
        if (position != RecyclerView.NO_POSITION) {
          val item = list[position]!!
          when (action) {
            R.id.action_list_remove -> listener.onRemoveItem(position, item)
          }
        }
      }
    })
    return holder
  }

  override fun onViewRecycled(holder: ListViewHolder) {
    holder.overflow.dismiss()
  }

  override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
    val (_, _, _, _, _, show, season, episode, movie, person) = list[position]!!
    if (holder.itemViewType == TYPE_SHOW) {
      val showHolder = holder as ShowViewHolder
      val poster = ImageUri.create(ImageUri.ITEM_SHOW, POSTER, show!!.id)
      showHolder.poster.setImage(poster)
      showHolder.title.text = show.title
      showHolder.overview.text = show.overview
    } else if (holder.itemViewType == TYPE_SEASON) {
      val seasonHolder = holder as SeasonViewHolder
      val showPoster = ImageUri.create(ImageUri.ITEM_SHOW, POSTER, season!!.showId)
      seasonHolder.poster.setImage(showPoster)
      seasonHolder.season.text = context.resources.getString(R.string.season_x, season.season)
      seasonHolder.show.text = season.showTitle
    } else if (holder.itemViewType == TYPE_EPISODE) {
      val episodeHolder = holder as EpisodeViewHolder
      val title = DataHelper.getEpisodeTitle(
        context,
        episode!!.title,
        episode.season,
        episode.episode,
        episode.watched
      )
      val screenshotUri = ImageUri.create(ImageUri.ITEM_EPISODE, STILL, episode.id)
      episodeHolder.screen.setImage(screenshotUri)
      episodeHolder.title.text = title
      episodeHolder.showTitle.text = episode.showTitle
    } else if (holder.itemViewType == TYPE_MOVIE) {
      val movieHolder = holder as MovieViewHolder
      val poster = ImageUri.create(ImageUri.ITEM_MOVIE, POSTER, movie!!.id)
      movieHolder.poster.setImage(poster)
      movieHolder.title.text = movie.title
      movieHolder.overview.text = movie.overview
    } else {
      val personHolder = holder as PersonViewHolder
      val headshot = ImageUri.create(ImageUri.ITEM_PERSON, PROFILE, person!!.id)
      personHolder.headshot.setImage(headshot)
      personHolder.name.text = person.name
    }
  }

  open class ListViewHolder(v: View, val overflow: OverflowView) : ViewHolder(v)

  class ShowViewHolder(binding: RowListShowBinding) :
    ListViewHolder(binding.root, binding.overflow) {
    val poster = binding.poster
    val title = binding.title
    val overview = binding.overview
  }

  class SeasonViewHolder(binding: RowListSeasonBinding) :
    ListViewHolder(binding.root, binding.overflow) {
    val poster = binding.poster
    val season = binding.season
    val show = binding.show
  }

  class EpisodeViewHolder(binding: RowListEpisodeBinding) :
    ListViewHolder(binding.root, binding.overflow) {
    val screen = binding.screen
    val title = binding.title
    val showTitle = binding.showTitle
  }

  class MovieViewHolder(binding: RowListMovieBinding) :
    ListViewHolder(binding.root, binding.overflow) {
    val poster = binding.poster
    val title = binding.title
    val overview = binding.overview
  }

  class PersonViewHolder(binding: RowListPersonBinding) :
    ListViewHolder(binding.root, binding.overflow) {
    val headshot = binding.headshot
    val name = binding.personName
  }

  companion object {
    private const val TYPE_SHOW = 1
    private const val TYPE_SEASON = 2
    private const val TYPE_EPISODE = 3
    private const val TYPE_MOVIE = 4
    private const val TYPE_PERSON = 5
  }
}
