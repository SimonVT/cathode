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

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import net.simonvt.cathode.R
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.common.ui.fragment.ToolbarSwipeRefreshRecyclerFragment
import net.simonvt.cathode.common.ui.instantiate
import net.simonvt.cathode.common.util.Ids
import net.simonvt.cathode.common.util.guava.Preconditions
import net.simonvt.cathode.entity.Episode
import net.simonvt.cathode.settings.TraktLinkSettings
import net.simonvt.cathode.sync.scheduler.EpisodeTaskScheduler
import net.simonvt.cathode.sync.scheduler.SeasonTaskScheduler
import net.simonvt.cathode.ui.CathodeViewModelFactory
import net.simonvt.cathode.ui.LibraryType
import net.simonvt.cathode.ui.NavigationListener
import net.simonvt.cathode.ui.ShowsNavigationListener
import net.simonvt.cathode.ui.history.AddToHistoryDialog
import net.simonvt.cathode.ui.history.RemoveFromHistoryDialog
import net.simonvt.cathode.ui.lists.ListsDialog
import net.simonvt.cathode.ui.show.SeasonAdapter.ViewHolder
import javax.inject.Inject

class SeasonFragment @Inject constructor(
  private val viewModelFactory: CathodeViewModelFactory,
  private val seasonScheduler: SeasonTaskScheduler,
  private val episodeScheduler: EpisodeTaskScheduler
) : ToolbarSwipeRefreshRecyclerFragment<ViewHolder>(), SeasonAdapter.EpisodeCallbacks {

  var seasonId: Long = 0
    private set

  private lateinit var type: LibraryType

  private var title: String? = null

  private var seasonNumber = -1

  private val viewModel: SeasonViewModel by viewModels { viewModelFactory }

  private var seasonAdapter: SeasonAdapter? = null

  private lateinit var navigationListener: ShowsNavigationListener

  private var columnCount: Int = 0

  private var count = -1
  private var watchedCount = -1
  private var collectedCount = -1

  private val navigationClickListener = View.OnClickListener { navigationListener.onHomeClicked() }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    navigationListener = requireActivity() as NavigationListener
  }

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    val args = requireArguments()
    seasonId = args.getLong(ARG_SEASON_ID)
    title = args.getString(ARG_SHOW_TITLE)
    seasonNumber = args.getInt(ARG_SEASON_NUMBER)
    type = args.getSerializable(ARG_TYPE) as LibraryType

    setTitle(title)
    updateSubtitle()

    columnCount = resources.getInteger(R.integer.episodesColumns)

    viewModel.setSeasonId(seasonId)
    viewModel.loading.observe(this, Observer { loading -> setRefreshing(loading) })
    viewModel.season.observe(this, Observer { season ->
      title = season.showTitle
      setTitle(title)
      seasonNumber = season.season
      updateSubtitle()
    })
    viewModel.episodes.observe(this, Observer { episodes -> setEpisodes(episodes) })
  }

  override fun onRefresh() {
    viewModel.refresh()
  }

  fun updateSubtitle() {
    if (seasonNumber == -1) {
      return
    }
    val subtitle: String
    if (seasonNumber == 0) {
      subtitle = resources.getString(R.string.season_special)
    } else {
      subtitle = resources.getString(R.string.season_x, seasonNumber)
    }

    setSubtitle(subtitle)
  }

  override fun createMenu(toolbar: Toolbar) {
    super.createMenu(toolbar)
    val menu = toolbar.menu

    if (count > 0) {
      menu.add(0, R.id.action_history_add, 0, R.string.action_history_add)
      if (watchedCount > 0) {
        menu.add(0, R.id.action_history_remove, 0, R.string.action_history_remove)
      }
      if (collectedCount < count) {
        menu.add(0, R.id.action_collection_add, 0, R.string.action_collection_add)
      }
      if (collectedCount > 0) {
        menu.add(0, R.id.action_collection_remove, 0, R.string.action_collection_remove)
      }
    }

    menu.add(0, R.id.action_list_add, 0, R.string.action_list_add)
  }

  override fun onMenuItemClick(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.action_list_add -> {
        parentFragmentManager.instantiate(
          ListsDialog::class.java,
          ListsDialog.getArgs(ItemType.SEASON, seasonId)
        ).show(parentFragmentManager, DIALOG_LISTS_ADD)
        return true
      }

      R.id.action_history_add -> {
        parentFragmentManager.instantiate(
          AddToHistoryDialog::class.java,
          AddToHistoryDialog.getArgs(
            AddToHistoryDialog.Type.SEASON,
            seasonId,
            getString(R.string.season_x, seasonNumber)
          )
        ).show(parentFragmentManager, AddToHistoryDialog.TAG)
        return true
      }

      R.id.action_history_remove -> {
        if (TraktLinkSettings.isLinked(requireContext())) {
          parentFragmentManager.instantiate(
            RemoveFromHistoryDialog::class.java,
            RemoveFromHistoryDialog.getArgs(
              RemoveFromHistoryDialog.Type.SEASON,
              seasonId,
              requireContext().getString(R.string.season_x, seasonNumber),
              null
            )
          ).show(parentFragmentManager, RemoveFromHistoryDialog.TAG)
        } else {
          seasonScheduler.removeFromHistory(seasonId)
        }
        return true
      }

      R.id.action_collection_add -> {
        seasonScheduler.setInCollection(seasonId, true)
        return true
      }

      R.id.action_collection_remove -> {
        seasonScheduler.setInCollection(seasonId, false)
        return true
      }
    }

    return super.onMenuItemClick(item)
  }

  override fun getColumnCount(): Int {
    return columnCount
  }

  override fun displaysMenuIcon(): Boolean {
    return false
  }

  override fun onViewCreated(view: View, inState: Bundle?) {
    super.onViewCreated(view, inState)
    toolbar!!.setNavigationOnClickListener(navigationClickListener)
  }

  override fun onEpisodeClick(episodeId: Long) {
    navigationListener.onDisplayEpisode(episodeId, title)
  }

  override fun setEpisodeCollected(episodeId: Long, collected: Boolean) {
    episodeScheduler.setIsInCollection(episodeId, collected)
  }

  private fun setEpisodes(episodes: List<Episode>) {
    val count = episodes.size
    var watchedCount = 0
    var collectedCount = 0
    for (episode in episodes) {
      val watched = episode.watched
      val collected = episode.inCollection

      if (watched) {
        watchedCount++
      }
      if (collected) {
        collectedCount++
      }
    }

    if (count != this.count || watchedCount != this.watchedCount || collectedCount != this.collectedCount) {
      this.count = count
      this.watchedCount = watchedCount
      this.collectedCount = collectedCount

      invalidateMenu()
    }

    if (seasonAdapter == null) {
      seasonAdapter = SeasonAdapter(requireActivity(), this, type)
      adapter = seasonAdapter
    }

    seasonAdapter!!.setList(episodes)
  }

  companion object {

    const val TAG = "net.simonvt.cathode.ui.show.SeasonFragment"

    private const val ARG_SEASON_ID = "net.simonvt.cathode.ui.show.SeasonFragment.seasonId"
    private const val ARG_SHOW_TITLE = "net.simonvt.cathode.ui.show.SeasonFragment.showTitle"
    private const val ARG_SEASON_NUMBER = "net.simonvt.cathode.ui.show.SeasonFragment.seasonNumber"
    private const val ARG_TYPE = "net.simonvt.cathode.ui.show.SeasonFragment.type"

    private const val DIALOG_LISTS_ADD = "net.simonvt.cathode.ui.show.SeasonFragment.listsAddDialog"

    @JvmStatic
    fun getTag(episodeId: Long): String {
      return TAG + "/" + episodeId + "/" + Ids.newId()
    }

    @JvmStatic
    fun getArgs(
      seasonId: Long,
      showTitle: String?,
      seasonNumber: Int,
      type: LibraryType
    ): Bundle {
      Preconditions.checkArgument(seasonId >= 0, "seasonId must be >= 0")

      val args = Bundle()
      args.putLong(ARG_SEASON_ID, seasonId)
      args.putString(ARG_SHOW_TITLE, showTitle)
      args.putInt(ARG_SEASON_NUMBER, seasonNumber)
      args.putSerializable(ARG_TYPE, type)
      return args
    }
  }
}
