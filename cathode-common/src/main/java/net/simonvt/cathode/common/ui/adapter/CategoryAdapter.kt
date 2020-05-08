/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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
package net.simonvt.cathode.common.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import net.simonvt.cathode.common.databinding.CategoryAdapterCategoryBinding
import net.simonvt.cathode.common.databinding.CategoryAdapterRecyclerviewBinding

class CategoryAdapter(
  private val context: Context,
  private val clickListener: CategoryClickListener
) : Adapter<ViewHolder>() {

  interface CategoryClickListener {
    fun onCategoryClick(category: Int)
  }

  class Category internal constructor(
    private val parentAdapter: CategoryAdapter,
    val category: Int,
    val categoryId: Long,
    val adapterId: Long
  ) : AdapterDataObserver() {

    private var _adapter: Adapter<*>? = null
    var adapter: Adapter<*>?
      get() = _adapter
      set(value) {
        _adapter?.unregisterAdapterDataObserver(this)
        _adapter = value
        _adapter?.registerAdapterDataObserver(this)
        onAdapterChanged()
      }

    var adapterCount = 0

    private fun onAdapterChanged() {
      val adapterCount = adapter!!.itemCount
      if (adapterCount == 0 && this.adapterCount > 0) {
        parentAdapter.onHideCategory(this)
      } else if (this.adapterCount == 0 && adapterCount > 0) {
        parentAdapter.onShowCategory(this)
      }
      this.adapterCount = adapterCount
    }

    override fun onChanged() {
      onAdapterChanged()
    }

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
      onAdapterChanged()
    }

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
      onAdapterChanged()
    }

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
      onAdapterChanged()
    }

    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
      onAdapterChanged()
    }

    override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
      onAdapterChanged()
    }
  }

  private var categories = mutableListOf<Category>()
  private var nextId: Long = 0
  private var allCategoriesLoaded = false

  fun initCategory(category: Int) {
    categories.add(Category(this, category, nextId++, nextId++))
  }

  fun setAdapter(category: Int, adapter: Adapter<*>) {
    for (c in categories) {
      if (category == c.category) {
        c.adapter = adapter
        break
      }
    }

    if (allCategoriesLoaded) {
      return
    }

    for (c in categories) {
      if (c.adapter == null) {
        return
      }
    }

    allCategoriesLoaded = true
    notifyItemRangeInserted(0, itemCount)
  }

  private fun onShowCategory(category: Category) {
    if (allCategoriesLoaded) {
      val position = getPositionOfCategory(category)
      notifyItemRangeInserted(position, 2)
    }
  }

  private fun onHideCategory(category: Category) {
    if (allCategoriesLoaded) {
      val position = getPositionOfCategory(category)
      notifyItemRangeRemoved(position, 2)
    }
  }

  override fun getItemCount(): Int {
    if (!allCategoriesLoaded) {
      return 0
    }

    var count = 0
    for (category in categories) {
      if (category.adapterCount > 0) {
        count += 2
      }
    }
    return count
  }

  private fun getPositionOfCategory(category: Category): Int {
    var position = 0
    for (c in categories) {
      if (category === c) {
        return position
      }
      if (c.adapterCount > 0) {
        position += 2
      }
    }
    throw IllegalStateException("No category found for category")
  }

  private fun getCategoryForPosition(position: Int): Category {
    var offset = 0
    for (category in categories) {
      if (category.adapterCount > 0) {
        offset += 2
        if (position < offset) {
          return category
        }
      }
    }

    throw IllegalStateException("No category found for position $position")
  }

  override fun getItemViewType(position: Int): Int {
    return if (position % 2 == 0) TYPE_CATEGORY else TYPE_ITEMS
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return if (viewType == TYPE_CATEGORY) {
      val binding =
        CategoryAdapterCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
      val holder = CategoryViewHolder(binding)
      binding.root.setOnClickListener {
        val position = holder.adapterPosition
        if (position != RecyclerView.NO_POSITION) {
          val category =
            getCategoryForPosition(position)
          clickListener.onCategoryClick(category.category)
        }
      }
      holder
    } else {
      val binding = CategoryAdapterRecyclerviewBinding.inflate(
        LayoutInflater.from(parent.context),
        parent,
        false
      )
      binding.list.layoutManager =
        LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
      ItemsViewHolder(binding)
    }
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val category = getCategoryForPosition(position)
    if (holder.itemViewType == TYPE_CATEGORY) {
      val cHolder = holder as CategoryViewHolder
      cHolder.category.setText(category.category)
    } else {
      val iHolder = holder as ItemsViewHolder
      iHolder.recyclerView.adapter = category.adapter
    }
  }

  class CategoryViewHolder(binding: CategoryAdapterCategoryBinding) : ViewHolder(binding.root) {
    val category = binding.category
  }

  class ItemsViewHolder(binding: CategoryAdapterRecyclerviewBinding) : ViewHolder(binding.root) {
    val recyclerView = binding.list
  }

  companion object {
    private const val TYPE_CATEGORY = 0
    private const val TYPE_ITEMS = 1
  }
}
