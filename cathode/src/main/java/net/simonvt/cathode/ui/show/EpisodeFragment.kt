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
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import butterknife.BindView
import butterknife.OnClick
import net.simonvt.cathode.R
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.api.util.TraktUtils
import net.simonvt.cathode.common.ui.fragment.RefreshableAppBarFragment
import net.simonvt.cathode.common.ui.instantiate
import net.simonvt.cathode.common.util.DateStringUtils
import net.simonvt.cathode.common.util.Ids
import net.simonvt.cathode.common.util.Intents
import net.simonvt.cathode.common.util.guava.Preconditions
import net.simonvt.cathode.common.widget.CircularProgressIndicator
import net.simonvt.cathode.entity.Comment
import net.simonvt.cathode.entity.Episode
import net.simonvt.cathode.images.ImageType
import net.simonvt.cathode.images.ImageUri
import net.simonvt.cathode.provider.util.DataHelper
import net.simonvt.cathode.settings.TraktLinkSettings
import net.simonvt.cathode.sync.scheduler.EpisodeTaskScheduler
import net.simonvt.cathode.ui.CathodeViewModelFactory
import net.simonvt.cathode.ui.NavigationListener
import net.simonvt.cathode.ui.comments.LinearCommentsAdapter
import net.simonvt.cathode.ui.dialog.CheckInDialog
import net.simonvt.cathode.ui.dialog.CheckInDialog.Type
import net.simonvt.cathode.ui.dialog.RatingDialog
import net.simonvt.cathode.ui.history.AddToHistoryDialog
import net.simonvt.cathode.ui.history.RemoveFromHistoryDialog
import net.simonvt.cathode.ui.lists.ListsDialog
import net.simonvt.cathode.widget.CheckInDrawable
import javax.inject.Inject

class EpisodeFragment @Inject constructor(
  private val viewModelFactory: CathodeViewModelFactory,
  private val episodeScheduler: EpisodeTaskScheduler
) : RefreshableAppBarFragment() {

  @BindView(R.id.title)
  @JvmField
  var title: TextView? = null
  @BindView(R.id.overview)
  @JvmField
  var overview: TextView? = null
  @BindView(R.id.episodeNumber)
  @JvmField
  var episodeNumber: TextView? = null
  @BindView(R.id.firstAired)
  @JvmField
  var firstAired: TextView? = null

  @BindView(R.id.rating)
  @JvmField
  var rating: CircularProgressIndicator? = null

  @BindView(R.id.checkmarks)
  @JvmField
  var checkmarks: View? = null
  @BindView(R.id.isWatched)
  @JvmField
  var watchedView: View? = null
  @BindView(R.id.inCollection)
  @JvmField
  var inCollectionView: View? = null
  @BindView(R.id.inWatchlist)
  @JvmField
  var inWatchlistView: View? = null

  @BindView(R.id.commentsParent)
  @JvmField
  var commentsParent: View? = null
  @BindView(R.id.commentsHeader)
  @JvmField
  var commentsHeader: View? = null
  @BindView(R.id.commentsContainer)
  @JvmField
  var commentsContainer: LinearLayout? = null

  @BindView(R.id.viewOnTrakt)
  @JvmField
  var viewOnTrakt: TextView? = null

  private lateinit var viewModel: EpisodeViewModel

  private var userComments: List<Comment>? = null
  private var comments: List<Comment>? = null

  var episodeId: Long = 0
    private set
  private var episode: Episode? = null

  private var episodeTitle: String? = null

  private var showTitle: String? = null
  private var showId = -1L
  private var season = -1
  private var seasonId = -1L

  private var currentRating: Int = 0

  private var loaded: Boolean = false

  private var watched: Boolean = false

  private var collected: Boolean = false

  private var inWatchlist: Boolean = false

  private var watching: Boolean = false

  private var checkedIn: Boolean = false

  lateinit var navigationListener: NavigationListener

  private var checkInDrawable: CheckInDrawable? = null

  override fun onAttach(context: Context) {
    super.onAttach(context)
    navigationListener = requireActivity() as NavigationListener
  }

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    val args = arguments
    episodeId = args!!.getLong(ARG_EPISODEID)
    showTitle = args.getString(ARG_SHOW_TITLE)
    setTitle(showTitle)

    viewModel = ViewModelProviders.of(this, viewModelFactory).get(EpisodeViewModel::class.java)
    viewModel.setEpisodeId(episodeId)
    viewModel.loading.observe(this, Observer { loading -> setRefreshing(loading) })
    viewModel.episode.observe(this, Observer { episode -> updateEpisodeViews(episode) })
    viewModel.userComments.observe(this, Observer { userComments ->
      this@EpisodeFragment.userComments = userComments
      updateComments()
    })
    viewModel.comments.observe(this, Observer { comments ->
      this@EpisodeFragment.comments = comments
      updateComments()
    })
  }

  override fun onHomeClicked() {
    if (showId >= 0L && seasonId >= 0L) {
      navigationListener.upFromEpisode(showId, showTitle, seasonId)
    } else {
      navigationListener.onHomeClicked()
    }
  }

  public override fun createView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    inState: Bundle?
  ): View {
    return inflater.inflate(R.layout.fragment_episode, container, false)
  }

  override fun onViewCreated(view: View, inState: Bundle?) {
    super.onViewCreated(view, inState)
    val linkDrawable = VectorDrawableCompat.create(resources, R.drawable.ic_link_black_24dp, null)
    viewOnTrakt!!.setCompoundDrawablesWithIntrinsicBounds(linkDrawable, null, null, null)

    if (TraktLinkSettings.isLinked(requireContext())) {
      rating!!.setOnClickListener {
        requireFragmentManager().instantiate(
          RatingDialog::class.java,
          RatingDialog.getArgs(RatingDialog.Type.EPISODE, episodeId, currentRating)
        ).show(requireFragmentManager(), DIALOG_RATING)
      }
    }
  }

  override fun onRefresh() {
    viewModel.refresh()
  }

  @OnClick(R.id.commentsHeader)
  fun onShowComments() {
    navigationListener.onDisplayComments(ItemType.EPISODE, episodeId)
  }

  override fun createMenu(toolbar: Toolbar) {
    if (loaded) {
      val menu = toolbar.menu
      if (TraktLinkSettings.isLinked(requireContext())) {
        if (checkInDrawable == null) {
          checkInDrawable = CheckInDrawable(toolbar.context)
          checkInDrawable!!.setWatching(watching || checkedIn)
          checkInDrawable!!.setId(episodeId)
        }

        val checkInItem: MenuItem

        if (watching || checkedIn) {
          checkInItem = menu.add(0, R.id.action_checkin, 1, R.string.action_checkin_cancel)
        } else {
          checkInItem = menu.add(0, R.id.action_checkin, 1, R.string.action_checkin)
        }

        checkInItem.setIcon(checkInDrawable).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)

        checkInItem.isEnabled = !watching
      }

      menu.add(0, R.id.action_history_add, 3, R.string.action_history_add)

      if (watched) {
        menu.add(0, R.id.action_history_remove, 4, R.string.action_history_remove)
      } else {
        if (inWatchlist) {
          menu.add(0, R.id.action_watchlist_remove, 5, R.string.action_watchlist_remove)
        } else {
          menu.add(0, R.id.action_watchlist_add, 6, R.string.action_watchlist_add)
        }
      }

      menu.add(0, R.id.action_history_add_older, 7, R.string.action_history_add_older)

      if (collected) {
        menu.add(0, R.id.action_collection_remove, 8, R.string.action_collection_remove)
      } else {
        menu.add(0, R.id.action_collection_add, 9, R.string.action_collection_add)
      }

      menu.add(0, R.id.action_list_add, 10, R.string.action_list_add)
    }
  }

  override fun onMenuItemClick(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.action_history_add -> {
        requireFragmentManager().instantiate(
          AddToHistoryDialog::class.java,
          AddToHistoryDialog.getArgs(AddToHistoryDialog.Type.EPISODE, episodeId, episodeTitle)
        ).show(requireFragmentManager(), AddToHistoryDialog.TAG)
        return true
      }

      R.id.action_history_remove -> {
        if (TraktLinkSettings.isLinked(requireContext())) {
          requireFragmentManager().instantiate(
            RemoveFromHistoryDialog::class.java,
            RemoveFromHistoryDialog.getArgs(
              RemoveFromHistoryDialog.Type.EPISODE,
              episodeId,
              episodeTitle,
              showTitle
            )
          ).show(requireFragmentManager(), RemoveFromHistoryDialog.TAG)
        } else {
          episodeScheduler.removeFromHistory(episodeId)
        }
        return true
      }

      R.id.action_history_add_older -> {
        requireFragmentManager().instantiate(
          AddToHistoryDialog::class.java,
          AddToHistoryDialog.getArgs(AddToHistoryDialog.Type.EPISODE_OLDER, episodeId, episodeTitle)
        ).show(requireFragmentManager(), AddToHistoryDialog.TAG)
        return true
      }

      R.id.action_checkin -> {
        if (!watching) {
          if (checkedIn) {
            episodeScheduler.cancelCheckin()
            if (checkInDrawable != null) {
              checkInDrawable!!.setWatching(false)
            }
          } else {
            if (!CheckInDialog.showDialogIfNecessary(
                requireActivity(), Type.SHOW, episodeTitle,
                episodeId
              )
            ) {
              episodeScheduler.checkin(episodeId, null, false, false, false)
              checkInDrawable!!.setWatching(true)
            }
          }
        }
        return true
      }

      R.id.action_checkin_cancel -> {
        episodeScheduler.cancelCheckin()
        return true
      }

      R.id.action_collection_add -> {
        episodeScheduler.setIsInCollection(episodeId, true)
        return true
      }

      R.id.action_collection_remove -> {
        episodeScheduler.setIsInCollection(episodeId, false)
        return true
      }

      R.id.action_watchlist_add -> {
        episodeScheduler.setIsInWatchlist(episodeId, true)
        return true
      }

      R.id.action_watchlist_remove -> {
        episodeScheduler.setIsInWatchlist(episodeId, false)
        return true
      }

      R.id.action_list_add -> {
        requireFragmentManager().instantiate(
          ListsDialog::class.java,
          ListsDialog.getArgs(ItemType.EPISODE, episodeId)
        ).show(requireFragmentManager(), DIALOG_LISTS_ADD)
        return true
      }

      else -> return super.onMenuItemClick(item)
    }
  }

  private fun updateEpisodeViews(episode: Episode) {
    this.episode = episode

    loaded = true

    showId = episode.showId
    seasonId = episode.seasonId
    showTitle = episode.showTitle

    watched = episode.watched
    collected = episode.inCollection
    inWatchlist = episode.inWatchlist
    watching = episode.watching
    checkedIn = episode.checkedIn

    season = episode.season
    episodeTitle = DataHelper.getEpisodeTitle(
      requireContext(), episode.title, season,
      episode.episode, episode.watched
    )

    title!!.text = episodeTitle
    episodeNumber!!.text = getString(
      R.string.season_x_episode, season,
      episode.episode
    )

    overview!!.text = episode.overview

    val screenshotUri = ImageUri.create(ImageUri.ITEM_EPISODE, ImageType.STILL, episodeId)

    setBackdrop(screenshotUri, true)

    firstAired!!.text =
      DateStringUtils.getAirdateInterval(requireContext(), episode.firstAired, true)

    if (checkInDrawable != null) {
      checkInDrawable!!.setWatching(watching || checkedIn)
    }

    val hasCheckmark = watched || collected || inWatchlist
    checkmarks!!.visibility = if (hasCheckmark) View.VISIBLE else View.GONE
    watchedView!!.visibility = if (watched) View.VISIBLE else View.GONE
    inCollectionView!!.visibility = if (collected) View.VISIBLE else View.GONE
    inWatchlistView!!.visibility = if (inWatchlist) View.VISIBLE else View.GONE

    currentRating = episode.userRating
    val ratingAll = episode.rating
    rating!!.setValue(ratingAll)

    viewOnTrakt!!.setOnClickListener {
      Intents.openUrl(
        requireContext(),
        TraktUtils.getTraktEpisodeUrl(episode.traktId)
      )
    }

    invalidateMenu()
  }

  private fun updateComments() {
    LinearCommentsAdapter.updateComments(
      requireContext(),
      commentsContainer!!,
      userComments,
      comments
    )
    commentsParent!!.visibility = View.VISIBLE
  }

  companion object {

    private const val TAG = "net.simonvt.cathode.ui.show.EpisodeFragment"

    private const val ARG_EPISODEID = "net.simonvt.cathode.ui.show.EpisodeFragment.episodeId"
    private const val ARG_SHOW_TITLE = "net.simonvt.cathode.ui.show.EpisodeFragment.showTitle"

    private const val DIALOG_RATING = "net.simonvt.cathode.ui.show.EpisodeFragment.ratingDialog"
    private const val DIALOG_LISTS_ADD =
      "net.simonvt.cathode.ui.show.EpisodeFragment.listsAddDialog"

    @JvmStatic
    fun getTag(episodeId: Long): String {
      return TAG + "/" + episodeId + "/" + Ids.newId()
    }

    @JvmStatic
    fun getArgs(episodeId: Long, showTitle: String): Bundle {
      Preconditions.checkArgument(episodeId >= 0, "episodeId must be >= 0, was $episodeId")

      val args = Bundle()
      args.putLong(ARG_EPISODEID, episodeId)
      args.putString(ARG_SHOW_TITLE, showTitle)
      return args
    }
  }
}
