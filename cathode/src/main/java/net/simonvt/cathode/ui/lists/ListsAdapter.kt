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
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.simonvt.cathode.common.ui.adapter.BaseAdapter
import net.simonvt.cathode.databinding.ListRowListBinding
import net.simonvt.cathode.entity.UserList
import net.simonvt.cathode.ui.lists.ListsAdapter.ViewHolder

class ListsAdapter(private val listener: OnListClickListener, context: Context?) :
  BaseAdapter<UserList?, ViewHolder?>(context) {

  interface OnListClickListener {
    fun onListClicked(listId: Long, listName: String)
  }

  override fun getItemId(position: Int): Long {
    return list[position]!!.id
  }

  override fun areItemsTheSame(oldItem: UserList, newItem: UserList): Boolean {
    return oldItem.id == newItem.id
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val binding = ListRowListBinding.inflate(LayoutInflater.from(context), parent, false)
    val holder = ViewHolder(binding)
    binding.root.setOnClickListener {
      listener.onListClicked(
        holder.itemId,
        holder.name.text.toString()
      )
    }
    return holder
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val list = list[position]!!
    holder.name.text = list.name
    holder.description.text = list.description
  }

  class ViewHolder(binding: ListRowListBinding) : RecyclerView.ViewHolder(binding.root) {
    val name = binding.name
    val description = binding.description
  }

  init {
    setHasStableIds(true)
  }
}
