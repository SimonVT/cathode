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

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import net.simonvt.cathode.R.string
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.databinding.DialogListsBinding
import net.simonvt.cathode.databinding.DialogListsEmptyBinding
import net.simonvt.cathode.databinding.DialogListsLoadingBinding
import net.simonvt.cathode.databinding.RowDialogListsBinding
import net.simonvt.cathode.entity.UserList
import net.simonvt.cathode.sync.scheduler.ListsTaskScheduler
import net.simonvt.cathode.ui.lists.DialogListItemListMapper.DialogListItem
import timber.log.Timber
import java.util.ArrayList
import java.util.Locale
import javax.inject.Inject

class ListsDialog @Inject constructor(private val listScheduler: ListsTaskScheduler) :
  DialogFragment() {

  class Item(var listId: Long, var listName: String) {
    var checked = false
  }

  private var itemType: ItemType? = null
  private var itemId: Long = 0
  private val viewModel: ListsDialogViewModel by viewModels()
  private var userLists: List<UserList>? = null
  private var listItems: List<DialogListItem>? = null
  private val lists: MutableList<Item> = ArrayList()

  private var _binding: DialogListsBinding? = null
  private val binding get() = _binding!!
  private var _loadingBinding: DialogListsLoadingBinding? = null
  private val loadingBinding get() = _loadingBinding!!
  private var _emptyBinding: DialogListsEmptyBinding? = null
  private val emptyBinding get() = _emptyBinding!!

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    itemType = requireArguments().getSerializable(ARG_TYPE) as ItemType?
    itemId = requireArguments().getLong(ARG_ID)

    viewModel.setItemTypeAndId(itemType!!, itemId)
    viewModel.lists.observe(this, Observer { userLists -> setUserLists(userLists) })
    viewModel.listItems.observe(this, Observer { listItems -> setListItems(listItems) })
  }

  override fun onCreateDialog(inState: Bundle?): Dialog {
    val dialog = super.onCreateDialog(inState)
    dialog.setTitle(string.action_list_add)
    return dialog
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    inState: Bundle?
  ): View? {
    _binding = DialogListsBinding.inflate(inflater, container, false)
    _loadingBinding = DialogListsLoadingBinding.inflate(inflater, binding.container, false)
    _emptyBinding = DialogListsEmptyBinding.inflate(inflater, binding.container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, inState: Bundle?) {
    super.onViewCreated(view, inState)
    updateList()
  }

  override fun onDestroyView() {
    _binding = null
    _loadingBinding = null
    _emptyBinding = null
    super.onDestroyView()
  }

  private fun setUserLists(userLists: List<UserList>) {
    Timber.d("setUserLists")
    this.userLists = userLists
    updateList()
  }

  private fun setListItems(listItems: List<DialogListItem>) {
    this.listItems = listItems
    updateList()
  }

  private fun updateList() {
    if (view == null) {
      return
    }

    binding.container.removeAllViews()

    if (userLists == null || listItems == null) {
      binding.container.addView(loadingBinding.root)
      return
    }

    if (userLists!!.isEmpty()) {
      binding.container.addView(emptyBinding.root)
      return
    }

    for (list in userLists!!) {
      var item = getItem(list.id)
      if (item == null) {
        item = Item(list.id, list.name)
        lists.add(item)
      } else {
        item.listName = list.name
      }
      item.checked = false
    }

    for (listItem in listItems!!) {
      val listId = listItem.getListId()
      getItem(listId)!!.checked = true
    }

    lists.sortedBy { it.listName.toLowerCase(Locale.getDefault()) }
    for (item in lists) {
      val v = createView(item.listId, item.listName, item.checked)
      binding.container.addView(v)
    }
  }

  private fun getItem(id: Long): Item? {
    return lists.firstOrNull { id == it.listId }
  }

  private fun createView(listId: Long, listName: String, checked: Boolean): View {
    val binding = RowDialogListsBinding.inflate(
      LayoutInflater.from(requireContext()),
      binding.container,
      false
    )
    binding.name.text = listName
    binding.checkBox.isChecked = checked
    binding.root.setOnClickListener {
      if (binding.checkBox.isChecked) {
        binding.checkBox.isChecked = false
        listScheduler.removeItem(listId, itemType!!, itemId)
      } else {
        binding.checkBox.isChecked = true
        listScheduler.addItem(listId, itemType!!, itemId)
      }
    }
    return binding.root
  }

  companion object {
    private const val ARG_TYPE = "net.simonvt.cathode.ui.lists.ListsDialog.itemType"
    private const val ARG_ID = "net.simonvt.cathode.ui.lists.ListsDialog.itemId"

    fun getArgs(itemType: ItemType?, itemId: Long): Bundle {
      val args = Bundle()
      args.putSerializable(ARG_TYPE, itemType)
      args.putLong(ARG_ID, itemId)
      return args
    }
  }
}
