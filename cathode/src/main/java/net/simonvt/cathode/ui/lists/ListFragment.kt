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
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.common.entity.ListItem
import net.simonvt.cathode.common.entity.UserList
import net.simonvt.cathode.common.ui.fragment.ToolbarSwipeRefreshRecyclerFragment
import net.simonvt.cathode.common.util.guava.Preconditions
import net.simonvt.cathode.provider.DatabaseContract
import net.simonvt.cathode.settings.TraktLinkSettings
import net.simonvt.cathode.sync.scheduler.ListsTaskScheduler
import net.simonvt.cathode.ui.CathodeViewModelFactory
import net.simonvt.cathode.ui.LibraryType
import net.simonvt.cathode.ui.NavigationListener
import javax.inject.Inject

class ListFragment : ToolbarSwipeRefreshRecyclerFragment<ListAdapter.ListViewHolder>(),
  ListAdapter.ListListener {

  @Inject
  lateinit var listScheduler: ListsTaskScheduler

  private lateinit var navigationListener: NavigationListener

  var listId: Long = 0
    private set

  @Inject
  lateinit var viewModelFactory: CathodeViewModelFactory
  private lateinit var viewModel: ListViewModel

  private var adapter: ListAdapter? = null

  private var columnCount: Int = 0

  private var listInfo: UserList? = null

  override fun onAttach(activity: Activity) {
    super.onAttach(activity)
    navigationListener = activity as NavigationListener
  }

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    AndroidSupportInjection.inject(this)

    val args = arguments
    listId = args!!.getLong(ARG_LIST_ID)
    val listName = args.getString(ARG_LIST_NAME)
    setTitle(listName)

    columnCount = resources.getInteger(R.integer.listColumns)

    viewModel = ViewModelProviders.of(this, viewModelFactory).get(ListViewModel::class.java)
    viewModel.setListId(listId)
    viewModel.loading.observe(this, Observer { loading -> setRefreshing(loading) })
    viewModel.list.observe(this, Observer { userList ->
      listInfo = userList
      setTitle(userList.name)
    })
    viewModel.listItems.observe(this, Observer { listItems -> setListItems(listItems) })
  }

  override fun onViewCreated(view: View, inState: Bundle?) {
    super.onViewCreated(view, inState)
    swipeRefreshLayout.isEnabled = TraktLinkSettings.isLinked(requireContext())
  }

  override fun getColumnCount(): Int {
    return columnCount
  }

  override fun onRefresh() {
    viewModel.refresh()
  }

  override fun createMenu(toolbar: Toolbar) {
    toolbar.inflateMenu(R.menu.fragment_list)
  }

  override fun onMenuItemClick(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_list_edit -> {
        if (listInfo != null) {
          val updateFragment = UpdateListFragment()
          updateFragment.arguments = UpdateListFragment.getArgs(
            listId, listInfo!!.name, listInfo!!.description,
            listInfo!!.privacy, listInfo!!.displayNumbers!!,
            listInfo!!.allowComments!!
          )
          updateFragment.show(fragmentManager!!, DIALOG_UPDATE)
        }
        return true
      }

      R.id.menu_list_delete -> {
        DeleteListDialog.newInstance(listId).show(fragmentManager!!, DIALOG_DELETE)
        return true
      }
    }
    return super.onMenuItemClick(item)
  }

  override fun onShowClick(showId: Long, title: String, overview: String) {
    navigationListener.onDisplayShow(showId, title, overview, LibraryType.WATCHED)
  }

  override fun onSeasonClick(showId: Long, seasonId: Long, showTitle: String, seasonNumber: Int) {
    navigationListener.onDisplaySeason(
      showId,
      seasonId,
      showTitle,
      seasonNumber,
      LibraryType.WATCHED
    )
  }

  override fun onEpisodeClick(id: Long) {
    navigationListener.onDisplayEpisode(id, null)
  }

  override fun onMovieClicked(movieId: Long, title: String, overview: String) {
    navigationListener.onDisplayMovie(movieId, title, overview)
  }

  override fun onPersonClick(personId: Long) {
    navigationListener.onDisplayPerson(personId)
  }

  override fun onRemoveItem(position: Int, listItem: ListItem) {
    val itemType: Int
    when (listItem.type) {
      ItemType.SHOW -> itemType = DatabaseContract.ItemType.SHOW
      ItemType.SEASON -> itemType = DatabaseContract.ItemType.SEASON
      ItemType.EPISODE -> itemType = DatabaseContract.ItemType.EPISODE
      ItemType.MOVIE -> itemType = DatabaseContract.ItemType.MOVIE
      ItemType.PERSON -> itemType = DatabaseContract.ItemType.PERSON
      else -> throw IllegalStateException("Unknown item type: " + listItem.type.toString())
    }

    listScheduler.removeItem(listId, itemType, listItem.listItemId)
    adapter!!.removeItem(listItem)
  }

  private fun setListItems(items: List<ListItem>) {
    if (adapter == null) {
      adapter = ListAdapter(requireContext(), this)
      setAdapter(adapter)
    }

    adapter!!.setList(items)
  }

  companion object {

    const val TAG = "net.simonvt.cathode.ui.lists.ListFragment"

    private const val ARG_LIST_ID = "net.simonvt.cathode.ui.lists.ListFragment.lidtId"
    private const val ARG_LIST_NAME = "net.simonvt.cathode.ui.lists.ListFragment.listName"

    private const val DIALOG_UPDATE = "net.simonvt.cathode.ui.lists.ListFragment.updateListsDialog"
    private const val DIALOG_DELETE = "net.simonvt.cathode.ui.lists.ListFragment.deleteListsDialog"

    @JvmStatic
    fun getArgs(listId: Long, listName: String): Bundle {
      Preconditions.checkArgument(listId >= 0, "listId must be >= 0")

      val args = Bundle()
      args.putLong(ARG_LIST_ID, listId)
      args.putString(ARG_LIST_NAME, listName)
      return args
    }
  }
}
