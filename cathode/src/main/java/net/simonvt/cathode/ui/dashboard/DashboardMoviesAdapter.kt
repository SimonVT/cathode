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
package net.simonvt.cathode.ui.dashboard

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.simonvt.cathode.common.ui.adapter.BaseAdapter
import net.simonvt.cathode.databinding.ListRowDashboardMovieBinding
import net.simonvt.cathode.entity.Movie
import net.simonvt.cathode.images.ImageType.POSTER
import net.simonvt.cathode.images.ImageUri
import net.simonvt.cathode.ui.dashboard.DashboardFragment.OverviewCallback
import net.simonvt.cathode.ui.dashboard.DashboardMoviesAdapter.ViewHolder

class DashboardMoviesAdapter(
  context: Context,
  private val callback: OverviewCallback
) : BaseAdapter<Movie, ViewHolder>(context) {
  override fun getItemId(position: Int): Long {
    return list[position]!!.id
  }

  override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean {
    return oldItem.id == newItem.id
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val binding =
      ListRowDashboardMovieBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    val holder = ViewHolder(binding)
    binding.root.setOnClickListener {
      val position = holder.adapterPosition
      if (position != RecyclerView.NO_POSITION) {
        val movie = list[position]!!
        callback.onDisplayMovie(holder.itemId, movie.title, movie.overview)
      }
    }
    return holder
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val movie = list[position]!!
    val poster = ImageUri.create(ImageUri.ITEM_MOVIE, POSTER, movie.id)
    holder.poster.setImage(poster)
    holder.title.text = movie.title
  }

  class ViewHolder(binding: ListRowDashboardMovieBinding) : RecyclerView.ViewHolder(binding.root) {
    var poster = binding.poster
    var title = binding.title
  }

  init {
    setHasStableIds(true)
  }
}
