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
package net.simonvt.cathode.ui.person

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import net.simonvt.cathode.api.enumeration.ItemType.SHOW
import net.simonvt.cathode.databinding.PersonItemCreditGridBinding
import net.simonvt.cathode.ui.person.PersonCreditsAdapter.ViewHolder

class PersonCreditsAdapter(
  private var credits: List<PersonCredit>?,
  private val listener: OnCreditClickListener
) : Adapter<ViewHolder>() {

  interface OnCreditClickListener {
    fun onShowClicked(showId: Long, title: String?, overview: String?)
    fun onMovieClicked(movieId: Long, title: String?, overview: String?)
  }

  fun setCredits(credits: List<PersonCredit>) {
    val oldCredits = this.credits
    this.credits = credits
    val oldSize = oldCredits?.size ?: 0
    val newSize = credits.size
    if (oldSize == 0) {
      if (credits.isNotEmpty()) {
        notifyItemRangeInserted(0, newSize)
      }
    } else {
      DiffUtil.calculateDiff(object : Callback() {
        override fun getOldListSize(): Int {
          return oldSize
        }

        override fun getNewListSize(): Int {
          return newSize
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
          val oldItem = oldCredits!![oldItemPosition]
          val newItem = credits[newItemPosition]
          return oldItem.getItemId() == newItem.getItemId()
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
          val oldItem = oldCredits!![oldItemPosition]
          val newItem = credits[newItemPosition]
          return oldItem == newItem
        }
      }).dispatchUpdatesTo(this)
    }
  }

  override fun getItemCount(): Int {
    return if (credits == null) 0 else credits!!.size
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val binding =
      PersonItemCreditGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    val holder = ViewHolder(binding)
    binding.root.setOnClickListener {
      val position = holder.adapterPosition
      if (position != RecyclerView.NO_POSITION) {
        val credit = credits!![position]
        if (credit.getItemType() === SHOW) {
          listener.onShowClicked(credit.getItemId(), credit.getTitle(), credit.getOverview())
        } else {
          listener.onMovieClicked(credit.getItemId(), credit.getTitle(), credit.getOverview())
        }
      }
    }
    return holder
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val credit = credits!![position]
    holder.poster.setImage(credit.getPoster())
    holder.title.text = credit.getTitle()
    if (credit.getJob() != null) {
      holder.job.text = credit.getJob()
    } else {
      holder.job.text = credit.getCharacter()
    }
  }

  class ViewHolder(binding: PersonItemCreditGridBinding) : RecyclerView.ViewHolder(binding.root) {
    var poster = binding.poster
    var title = binding.title
    var job = binding.job
  }
}
