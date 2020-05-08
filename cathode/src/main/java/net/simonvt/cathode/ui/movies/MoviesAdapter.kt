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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import net.simonvt.cathode.R.layout
import net.simonvt.cathode.common.widget.OverflowView.OverflowActionListener
import net.simonvt.cathode.ui.movies.BaseMoviesAdapter.ViewHolder

open class MoviesAdapter @JvmOverloads constructor(
  activity: FragmentActivity,
  callbacks: Callbacks,
  private val rowLayout: Int = layout.list_row_movie
) : BaseMoviesAdapter<ViewHolder>(activity, callbacks) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val v = LayoutInflater.from(context).inflate(rowLayout, parent, false)
    val holder = ViewHolder(v)
    v.setOnClickListener {
      val position = holder.adapterPosition
      if (position != RecyclerView.NO_POSITION) {
        val movie = list[position]!!
        callbacks.onMovieClicked(movie.id, movie.title, movie.overview)
      }
    }
    holder.overflow.setListener(object : OverflowActionListener {
      override fun onPopupShown() {}
      override fun onPopupDismissed() {}
      override fun onActionSelected(action: Int) {
        val position = holder.adapterPosition
        if (position != RecyclerView.NO_POSITION) {
          onOverflowActionSelected(
            holder.itemView,
            holder.itemId,
            action,
            position,
            holder.title.text.toString()
          )
        }
      }
    })
    return holder
  }
}
