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

import android.app.Activity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import dagger.android.support.AndroidSupportInjection
import net.simonvt.cathode.R
import net.simonvt.cathode.common.ui.fragment.ToolbarSwipeRefreshRecyclerFragment
import net.simonvt.cathode.entity.UserList
import net.simonvt.cathode.settings.TraktLinkSettings
import net.simonvt.cathode.ui.CathodeViewModelFactory
import net.simonvt.cathode.ui.ListNavigationListener
import javax.inject.Inject

class ListsFragment : ToolbarSwipeRefreshRecyclerFragment<ListsAdapter.ViewHolder>(),
  ListsAdapter.OnListClickListener {

  private lateinit var listener: ListNavigationListener

  @Inject
  lateinit var viewModelFactory: CathodeViewModelFactory
  private lateinit var viewModel: ListsViewModel

  private var adapter: ListsAdapter? = null

  override fun onAttach(activity: Activity) {
    super.onAttach(activity)
    listener = activity as ListNavigationListener
  }

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    AndroidSupportInjection.inject(this)
    setTitle(R.string.navigation_lists)
    setEmptyText(R.string.empty_lists)

    viewModel = ViewModelProviders.of(this, viewModelFactory).get(ListsViewModel::class.java)
    viewModel.loading.observe(this, Observer { loading -> setRefreshing(loading) })
    viewModel.lists.observe(this, Observer { userLists -> setLists(userLists) })
  }

  override fun onViewCreated(view: View, inState: Bundle?) {
    super.onViewCreated(view, inState)
    swipeRefreshLayout.isEnabled = TraktLinkSettings.isLinked(requireContext())
  }

  override fun getColumnCount(): Int {
    return 1
  }

  override fun onRefresh() {
    viewModel.refresh()
  }

  override fun onListClicked(listId: Long, listName: String) {
    listener.onShowList(listId, listName)
  }

  override fun displaysMenuIcon(): Boolean {
    return true
  }

  override fun createMenu(toolbar: Toolbar) {
    super.createMenu(toolbar)
    toolbar.inflateMenu(R.menu.fragment_lists)
  }

  override fun onMenuItemClick(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_list_create -> {
        val dialog = CreateListFragment()
        dialog.show(fragmentManager!!, DIALOG_LIST_CREATE)
        return true
      }
    }

    return super.onMenuItemClick(item)
  }

  private fun setLists(userLists: List<UserList>) {
    if (adapter == null) {
      adapter = ListsAdapter(this, requireContext())
      setAdapter(adapter)
    }

    adapter!!.setList(userLists)
  }

  companion object {

    const val TAG = "net.simonvt.cathode.ui.lists.ListsFragment"

    private const val DIALOG_LIST_CREATE = "net.simonvt.cathode.ui.HomeActivity.createListFragment"
  }
}
