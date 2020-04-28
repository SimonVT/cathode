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
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import net.simonvt.cathode.R
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.api.enumeration.ShowStatus
import net.simonvt.cathode.api.util.TraktUtils
import net.simonvt.cathode.common.ui.fragment.RefreshableAppBarFragment
import net.simonvt.cathode.common.ui.instantiate
import net.simonvt.cathode.common.util.DateStringUtils
import net.simonvt.cathode.common.util.Ids
import net.simonvt.cathode.common.util.Intents
import net.simonvt.cathode.common.util.Joiner
import net.simonvt.cathode.common.util.guava.Preconditions
import net.simonvt.cathode.common.widget.CircleTransformation
import net.simonvt.cathode.common.widget.OverflowView
import net.simonvt.cathode.common.widget.RemoteImageView
import net.simonvt.cathode.databinding.FragmentShowBinding
import net.simonvt.cathode.databinding.ShowInfoEpisodeBinding
import net.simonvt.cathode.entity.CastMember
import net.simonvt.cathode.entity.Comment
import net.simonvt.cathode.entity.Episode
import net.simonvt.cathode.entity.Show
import net.simonvt.cathode.images.ImageType
import net.simonvt.cathode.images.ImageUri
import net.simonvt.cathode.provider.util.DataHelper
import net.simonvt.cathode.settings.TraktLinkSettings
import net.simonvt.cathode.sync.scheduler.EpisodeTaskScheduler
import net.simonvt.cathode.sync.scheduler.ShowTaskScheduler
import net.simonvt.cathode.ui.CathodeViewModelFactory
import net.simonvt.cathode.ui.LibraryType
import net.simonvt.cathode.ui.NavigationListener
import net.simonvt.cathode.ui.comments.LinearCommentsAdapter
import net.simonvt.cathode.ui.dialog.CheckInDialog
import net.simonvt.cathode.ui.dialog.CheckInDialog.Type
import net.simonvt.cathode.ui.dialog.RatingDialog
import net.simonvt.cathode.ui.history.AddToHistoryDialog
import net.simonvt.cathode.ui.lists.ListsDialog
import net.simonvt.cathode.widget.AdapterCountDataObserver
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

class ShowFragment @Inject constructor(
  private val viewModelFactory: CathodeViewModelFactory,
  private val showScheduler: ShowTaskScheduler,
  private val episodeScheduler: EpisodeTaskScheduler
) : RefreshableAppBarFragment() {

  private var _binding: FragmentShowBinding? = null
  private val binding get() = _binding!!

  lateinit var navigationListener: NavigationListener

  var showId: Long = 0
    private set

  private val viewModel: ShowViewModel by viewModels { viewModelFactory }

  private var show: Show? = null
  private var genres: List<String>? = null
  private var cast: List<CastMember>? = null
  private var userComments: List<Comment>? = null
  private var comments: List<Comment>? = null
  private var related: List<Show>? = null
  private var toWatch: Episode? = null
  private var lastWatched: Episode? = null
  private var toCollect: Episode? = null
  private var lastCollected: Episode? = null

  lateinit var seasonsAdapter: SeasonsAdapter

  private var toWatchBinding: ShowInfoEpisodeBinding? = null
  private var toWatchId: Long = -1
  private var toWatchTitle: String? = null

  private var lastWatchedBinding: ShowInfoEpisodeBinding? = null
  private var lastWatchedId: Long = -1

  private var toCollectBinding: ShowInfoEpisodeBinding? = null
  private var toCollectId: Long = -1

  private var lastCollectedBinding: ShowInfoEpisodeBinding? = null
  private var lastCollectedId: Long = -1

  private var showTitle: String? = null

  private var showOverview: String? = null

  private var inWatchlist: Boolean = false

  private var currentRating: Int = 0

  private var calendarHidden: Boolean = false

  private lateinit var type: LibraryType

  override fun onAttach(context: Context) {
    super.onAttach(context)
    navigationListener = requireActivity() as NavigationListener
  }

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    showId = requireArguments().getLong(ARG_SHOWID)
    showTitle = requireArguments().getString(ARG_TITLE)
    showOverview = requireArguments().getString(ARG_OVERVIEW)
    type = requireArguments().getSerializable(ARG_TYPE) as LibraryType

    setTitle(showTitle)

    seasonsAdapter = SeasonsAdapter(
      requireActivity(),
      SeasonsAdapter.SeasonClickListener { showId, seasonId, showTitle, seasonNumber ->
        navigationListener.onDisplaySeason(
          showId,
          seasonId,
          showTitle,
          seasonNumber,
          type
        )
      },
      type
    )

    viewModel.setShowId(showId)
    viewModel.loading.observe(this, Observer { loading -> setRefreshing(loading) })
    viewModel.show.observe(this, Observer { show ->
      this@ShowFragment.show = show
      updateShowView()
    })
    viewModel.genres.observe(this, Observer { genres ->
      this@ShowFragment.genres = genres
      updateGenreViews()
    })
    viewModel.seasons.observe(this, Observer { seasons -> seasonsAdapter.setList(seasons) })
    viewModel.cast.observe(this, Observer { castMembers ->
      cast = castMembers
      updateCastViews()
    })
    viewModel.userComments.observe(this, Observer { userComments ->
      this@ShowFragment.userComments = userComments
      updateComments()
    })
    viewModel.comments.observe(this, Observer { comments ->
      this@ShowFragment.comments = comments
      updateComments()
    })
    viewModel.related.observe(this, Observer { relatedShows ->
      Timber.d("Related shows: %d", relatedShows?.size ?: 0)
      related = relatedShows
      updateRelatedView()
    })
    viewModel.toWatch.observe(this, Observer { episode ->
      toWatch = episode
      updateToWatch()
    })
    viewModel.lastWatched.observe(this, Observer { episode ->
      lastWatched = episode
      updateLastWatched()
    })
    viewModel.toCollect.observe(this, Observer { episode ->
      toCollect = episode
      updateToCollect()
    })
    viewModel.lastCollected.observe(this, Observer { episode ->
      lastCollected = episode
      updateLastCollected()
    })
  }

  override fun createView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View {
    _binding = FragmentShowBinding.inflate(inflater, container, false)
    toWatchBinding = binding.content.episodes.toWatch
    lastWatchedBinding = binding.content.episodes.lastWatched
    toCollectBinding = binding.content.episodes.toCollect
    lastCollectedBinding = binding.content.episodes.lastCollected
    return binding.root
  }

  override fun onViewCreated(view: View, inState: Bundle?) {
    super.onViewCreated(view, inState)
    val linkDrawable = VectorDrawableCompat.create(resources, R.drawable.ic_link_black_24dp, null)
    binding.content.website.setCompoundDrawablesWithIntrinsicBounds(linkDrawable, null, null, null)
    binding.content.viewOnTrakt.setCompoundDrawablesWithIntrinsicBounds(
      linkDrawable,
      null,
      null,
      null
    )
    binding.content.viewOnImdb.setCompoundDrawablesWithIntrinsicBounds(
      linkDrawable,
      null,
      null,
      null
    )
    binding.content.viewOnTmdb.setCompoundDrawablesWithIntrinsicBounds(
      linkDrawable,
      null,
      null,
      null
    )
    binding.content.viewOnTvdb.setCompoundDrawablesWithIntrinsicBounds(
      linkDrawable,
      null,
      null,
      null
    )

    val playDrawable =
      VectorDrawableCompat.create(resources, R.drawable.ic_play_arrow_black_24dp, null)
    binding.content.trailer.setCompoundDrawablesWithIntrinsicBounds(playDrawable, null, null, null)

    binding.content.overview.text = showOverview

    val decoration = DividerItemDecoration(requireContext(), LinearLayoutManager.HORIZONTAL)
    decoration.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.divider_4dp)!!)
    binding.content.seasons.addItemDecoration(decoration)
    binding.content.seasons.layoutManager =
      LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    binding.content.seasons.adapter = seasonsAdapter
    (binding.content.seasons.itemAnimator as DefaultItemAnimator).supportsChangeAnimations = false
    seasonsAdapter.registerAdapterDataObserver(object : AdapterCountDataObserver(seasonsAdapter) {
      override fun onCountChanged(itemCount: Int) {
        if (_binding != null) {
          if (itemCount == 0) {
            binding.content.seasonsDivider.visibility = View.GONE
            binding.content.seasonsTitle.visibility = View.GONE
            binding.content.seasons.visibility = View.GONE
          } else {
            binding.content.seasonsDivider.visibility = View.VISIBLE
            binding.content.seasonsTitle.visibility = View.VISIBLE
            binding.content.seasons.visibility = View.VISIBLE
          }
        }
      }
    })
    if (seasonsAdapter.itemCount > 0) {
      binding.content.seasonsDivider.visibility = View.VISIBLE
      binding.content.seasonsTitle.visibility = View.VISIBLE
      binding.content.seasons.visibility = View.VISIBLE
    } else {
      binding.content.seasonsDivider.visibility = View.GONE
      binding.content.seasonsTitle.visibility = View.GONE
      binding.content.seasons.visibility = View.GONE
    }

    if (TraktLinkSettings.isLinked(requireContext())) {
      binding.top.rating.setOnClickListener {
        parentFragmentManager.instantiate(
          RatingDialog::class.java,
          RatingDialog.getArgs(RatingDialog.Type.SHOW, showId, currentRating)
        ).show(parentFragmentManager, DIALOG_RATING)
      }
    }

    binding.content.cast.castHeader.setOnClickListener {
      navigationListener.onDisplayCredits(
        ItemType.SHOW,
        showId,
        showTitle
      )
    }

    binding.content.comments.commentsHeader.setOnClickListener {
      navigationListener.onDisplayComments(
        ItemType.SHOW,
        showId
      )
    }

    binding.content.related.relatedHeader.setOnClickListener {
      navigationListener.onDisplayRelatedShows(
        showId,
        showTitle
      )
    }

    toWatchBinding?.root?.setOnClickListener {
      if (toWatchId != -1L) navigationListener.onDisplayEpisode(
        toWatchId,
        showTitle
      )
    }

    toWatchBinding?.episodeOverflow?.setListener(object : OverflowView.OverflowActionListener {
      override fun onPopupShown() {}

      override fun onPopupDismissed() {}

      override fun onActionSelected(action: Int) {
        when (action) {
          R.id.action_checkin -> if (toWatchId != -1L) {
            CheckInDialog.showDialogIfNecessary(
              requireActivity(), Type.SHOW, toWatchTitle,
              toWatchId
            )
          }
          R.id.action_checkin_cancel -> if (toWatchId != -1L) {
            episodeScheduler.cancelCheckin()
          }
          R.id.action_history_add -> if (toWatchId != -1L) {
            parentFragmentManager.instantiate(
              AddToHistoryDialog::class.java,
              AddToHistoryDialog.getArgs(AddToHistoryDialog.Type.EPISODE, toWatchId, null)
            ).show(parentFragmentManager, AddToHistoryDialog.TAG)
          }
        }
      }
    })

    if (lastWatchedBinding != null) {
      lastWatchedBinding?.root?.setOnClickListener {
        if (lastWatchedId != -1L) {
          navigationListener.onDisplayEpisode(lastWatchedId, showTitle)
        }
      }
    }

    toCollectBinding?.root?.setOnClickListener {
      if (toCollectId != -1L) navigationListener.onDisplayEpisode(
        toCollectId,
        showTitle
      )
    }

    toCollectBinding?.episodeOverflow?.addItem(
      R.id.action_collection_add,
      R.string.action_collection_add
    )
    toCollectBinding?.episodeOverflow?.setListener(object : OverflowView.OverflowActionListener {
      override fun onPopupShown() {}

      override fun onPopupDismissed() {}

      override fun onActionSelected(action: Int) {
        when (action) {
          R.id.action_collection_add -> if (toCollectId != -1L) {
            episodeScheduler.setIsInCollection(toCollectId, true)
          }
        }
      }
    })

    if (lastCollectedBinding != null) {
      lastCollectedBinding?.root?.setOnClickListener {
        if (lastCollectedId != -1L) {
          navigationListener.onDisplayEpisode(lastCollectedId, showTitle)
        }
      }

      lastCollectedBinding?.episodeOverflow?.addItem(
        R.id.action_collection_remove,
        R.string.action_collection_remove
      )
      lastCollectedBinding?.episodeOverflow?.setListener(object :
        OverflowView.OverflowActionListener {
        override fun onPopupShown() {}

        override fun onPopupDismissed() {}

        override fun onActionSelected(action: Int) {
          when (action) {
            R.id.action_collection_add -> if (lastCollectedId != -1L) {
              episodeScheduler.setIsInCollection(lastCollectedId, true)
            }
          }
        }
      })
    }

    updateShowView()
    updateGenreViews()
    updateCastViews()
    updateRelatedView()
    updateToWatch()
    updateLastWatched()
    updateToCollect()
    updateLastCollected()
    updateComments()
  }

  override fun onDestroyView() {
    _binding = null
    super.onDestroyView()
  }

  override fun onRefresh() {
    viewModel.refresh()
  }

  override fun createMenu(toolbar: Toolbar) {
    super.createMenu(toolbar)
    val menu = toolbar.menu

    if (inWatchlist) {
      menu.add(0, R.id.action_watchlist_remove, 1, R.string.action_watchlist_remove)
    } else {
      menu.add(0, R.id.action_watchlist_add, 1, R.string.action_watchlist_add)
    }

    if (calendarHidden) {
      menu.add(0, R.id.action_calendar_unhide, 2, R.string.action_calendar_unhide)
    } else {
      menu.add(0, R.id.action_calendar_hide, 2, R.string.action_calendar_hide)
    }

    menu.add(0, R.id.action_list_add, 3, R.string.action_list_add)
  }

  override fun onMenuItemClick(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.action_watchlist_remove -> {
        showScheduler.setIsInWatchlist(showId, false)
        return true
      }

      R.id.action_watchlist_add -> {
        showScheduler.setIsInWatchlist(showId, true)
        return true
      }

      R.id.action_list_add -> {
        parentFragmentManager.instantiate(
          ListsDialog::class.java,
          ListsDialog.getArgs(ItemType.SHOW, showId)
        ).show(parentFragmentManager, DIALOG_LISTS_ADD)
        return true
      }

      R.id.action_calendar_hide -> {
        showScheduler.hideFromCalendar(showId, true)
        return true
      }

      R.id.action_calendar_unhide -> {
        showScheduler.hideFromCalendar(showId, false)
        return true
      }
    }

    return super.onMenuItemClick(item)
  }

  private fun updateShowView() {
    if (view == null || show == null) {
      return
    }

    showTitle = show!!.title
    setTitle(show!!.title)
    val backdropUri = ImageUri.create(ImageUri.ITEM_SHOW, ImageType.BACKDROP, showId)
    setBackdrop(backdropUri, true)
    showOverview = show!!.overview

    currentRating = show!!.userRating
    binding.top.rating.setValue(show!!.rating)

    calendarHidden = show!!.hiddenCalendar

    val isWatched = show!!.watchedCount > 0
    val isCollected = show!!.inCollectionCount > 0
    inWatchlist = show!!.inWatchlist
    val hasCheckmark = isWatched || isCollected || inWatchlist
    binding.content.checkmarks.checkmarks.visibility = if (hasCheckmark) View.VISIBLE else View.GONE
    binding.content.checkmarks.isWatched.visibility = if (isWatched) View.VISIBLE else View.GONE
    binding.content.checkmarks.inCollection.visibility =
      if (isCollected) View.VISIBLE else View.GONE
    binding.content.checkmarks.inWatchlist.visibility = if (inWatchlist) View.VISIBLE else View.GONE

    val airDay = show!!.airDay
    val airTime = show!!.airTime
    val network = show!!.network
    val certification = show!!.certification

    var airTimeString: String? = null
    if (airDay != null && airTime != null) {
      airTimeString = "$airDay $airTime"
    }
    if (network != null) {
      if (airTimeString != null) {
        airTimeString += ", $network"
      } else {
        airTimeString = network
      }
    }
    if (certification != null) {
      if (airTimeString != null) {
        airTimeString += ", $certification"
      } else {
        airTimeString = certification
      }
    }

    binding.top.airtime.text = airTimeString

    val status = show!!.status
    var statusString: String? = null
    if (status != null) {
      when (status) {
        ShowStatus.ENDED -> statusString = getString(R.string.show_status_ended)

        ShowStatus.RETURNING -> statusString = getString(R.string.show_status_returning)

        ShowStatus.CANCELED -> statusString = getString(R.string.show_status_canceled)

        ShowStatus.IN_PRODUCTION -> statusString = getString(R.string.show_status_in_production)

        ShowStatus.PLANNED -> statusString = getString(R.string.show_status_planned)
      }
    }

    binding.top.status.text = statusString

    binding.content.overview.text = showOverview

    val trailer = show!!.trailer
    if (!TextUtils.isEmpty(trailer)) {
      binding.content.trailer.visibility = View.VISIBLE
      binding.content.trailer.setOnClickListener { Intents.openUrl(requireContext(), trailer) }
    } else {
      binding.content.trailer.visibility = View.GONE
    }

    val website = show!!.homepage
    if (!TextUtils.isEmpty(website)) {
      binding.content.website.visibility = View.VISIBLE
      binding.content.website.text = website
      binding.content.website.setOnClickListener { Intents.openUrl(requireContext(), website) }
    } else {
      binding.content.website.visibility = View.GONE
    }

    val traktId = show!!.traktId
    val imdbId = show!!.imdbId
    val tvdbId = show!!.tvdbId
    val tmdbId = show!!.tmdbId

    binding.content.viewOnTrakt.visibility = View.VISIBLE
    binding.content.viewOnTrakt.setOnClickListener {
      Intents.openUrl(
        requireContext(),
        TraktUtils.getTraktShowUrl(traktId)
      )
    }

    if (!imdbId.isNullOrEmpty()) {
      binding.content.viewOnImdb.visibility = View.VISIBLE
      binding.content.viewOnImdb.setOnClickListener {
        Intents.openUrl(
          requireContext(),
          TraktUtils.getImdbUrl(imdbId)
        )
      }
    } else {
      binding.content.viewOnImdb.visibility = View.GONE
    }

    if (tvdbId > 0) {
      binding.content.viewOnTvdb.visibility = View.VISIBLE
      binding.content.viewOnTvdb.setOnClickListener {
        Intents.openUrl(
          requireContext(),
          TraktUtils.getTvdbUrl(tvdbId)
        )
      }
    } else {
      binding.content.viewOnTvdb.visibility = View.GONE
    }

    if (tmdbId > 0) {
      binding.content.viewOnTmdb.visibility = View.VISIBLE
      binding.content.viewOnTmdb.setOnClickListener {
        Intents.openUrl(
          requireContext(),
          TraktUtils.getTmdbTvUrl(tmdbId)
        )
      }
    } else {
      binding.content.viewOnTmdb.visibility = View.GONE
    }

    invalidateMenu()
  }

  private fun updateGenreViews() {
    if (view == null) {
      return
    }

    if (genres == null || genres!!.isEmpty()) {
      binding.content.genresDivider.visibility = View.GONE
      binding.content.genresTitle.visibility = View.GONE
      binding.content.genres.visibility = View.GONE
    } else {
      binding.content.genresDivider.visibility = View.VISIBLE
      binding.content.genresTitle.visibility = View.VISIBLE
      binding.content.genres.visibility = View.VISIBLE
      binding.content.genres.text = Joiner.on(", ").join(genres)
    }
  }

  private fun updateCastViews() {
    if (view == null) {
      return
    }

    binding.content.cast.container.container.removeAllViews()
    if (cast.isNullOrEmpty()) {
      binding.content.cast.cast.visibility = View.GONE
    } else {
      binding.content.cast.cast.visibility = View.VISIBLE

      for (castMember in cast!!) {
        val v = LayoutInflater.from(requireContext()).inflate(
          R.layout.section_people_item,
          binding.content.cast.container.container,
          false
        )

        val personId = castMember.person.id
        val headshotUrl = ImageUri.create(ImageUri.ITEM_PERSON, ImageType.PROFILE, personId)

        val headshot = v.findViewById<RemoteImageView>(R.id.headshot)
        headshot.addTransformation(CircleTransformation())
        headshot.setImage(headshotUrl)

        val name = v.findViewById<TextView>(R.id.person_name)
        name.text = castMember.person.name

        val character = v.findViewById<TextView>(R.id.person_job)
        character.text = castMember.character

        v.setOnClickListener { navigationListener.onDisplayPerson(personId) }

        binding.content.cast.container.container.addView(v)
      }
    }
  }

  private fun updateRelatedView() {
    if (view == null) {
      return
    }

    binding.content.related.container.container.removeAllViews()
    if (related.isNullOrEmpty()) {
      binding.content.related.related.visibility = View.GONE
    } else {
      binding.content.related.related.visibility = View.VISIBLE

      for (show in related!!) {
        val v = LayoutInflater.from(requireContext()).inflate(
          R.layout.section_related_item,
          binding.content.related.container.container,
          false
        )

        val poster = ImageUri.create(ImageUri.ITEM_SHOW, ImageType.POSTER, show.id)

        val posterView = v.findViewById<RemoteImageView>(R.id.related_poster)
        posterView.addTransformation(CircleTransformation())
        posterView.setImage(poster)

        val titleView = v.findViewById<TextView>(R.id.related_title)
        titleView.text = show.title

        val formattedRating = String.format(Locale.getDefault(), "%.1f", show.rating)

        val ratingText: String
        if (show.votes >= 1000) {
          val convertedVotes = show.votes / 1000.0f
          val formattedVotes = String.format(Locale.getDefault(), "%.1f", convertedVotes)
          ratingText = getString(R.string.related_rating_thousands, formattedRating, formattedVotes)
        } else {
          ratingText = getString(R.string.related_rating, formattedRating, show.votes)
        }

        val ratingView = v.findViewById<TextView>(R.id.related_rating)
        ratingView.text = ratingText

        v.setOnClickListener {
          navigationListener.onDisplayShow(show.id, show.title, show.overview, LibraryType.WATCHED)
        }
        binding.content.related.container.container.addView(v)
      }
    }
  }

  private fun updateEpisodesContainer() {
    if (view == null) {
      return
    }

    if (toWatchId == -1L && lastWatchedId == -1L && toCollectId == -1L && lastCollectedId == -1L) {
      binding.content.episodes.episodes.visibility = View.GONE
    } else {
      binding.content.episodes.episodes.visibility = View.VISIBLE
    }
  }

  private fun updateToWatch() {
    if (view == null) {
      return
    }

    if (toWatch == null) {
      toWatchBinding?.root?.visibility = View.GONE
      toWatchId = -1
    } else {
      toWatchBinding?.root?.visibility = View.VISIBLE
      toWatchId = toWatch!!.id

      toWatchTitle = DataHelper.getEpisodeTitle(
        requireContext(),
        toWatch!!.title,
        toWatch!!.season,
        toWatch!!.episode,
        toWatch!!.watched
      )
      val toWatchEpisodeText =
        getString(R.string.season_x_episode_y, toWatch!!.season, toWatch!!.episode)

      toWatchBinding?.episodeTitle?.text = toWatchTitle
      toWatchBinding?.episodeEpisode?.text = toWatchEpisodeText

      val screenshotUri = ImageUri.create(ImageUri.ITEM_EPISODE, ImageType.STILL, toWatchId)
      toWatchBinding?.episodeScreenshot?.setImage(screenshotUri)

      var firstAiredString =
        DateStringUtils.getAirdateInterval(requireContext(), toWatch!!.firstAired, false)

      toWatchBinding?.episodeOverflow?.removeItems()
      if (toWatch!!.checkedIn) {
        toWatchBinding?.episodeOverflow?.addItem(
          R.id.action_checkin_cancel,
          R.string.action_checkin_cancel
        )
        firstAiredString = resources.getString(R.string.show_watching)
      } else if (!toWatch!!.watching) {
        toWatchBinding?.episodeOverflow?.addItem(R.id.action_checkin, R.string.action_checkin)
        toWatchBinding?.episodeOverflow?.addItem(
          R.id.action_history_add,
          R.string.action_history_add
        )
      }

      toWatchBinding?.episodeAirTime?.text = firstAiredString
    }

    updateEpisodesContainer()
  }

  private fun updateLastWatched() {
    if (lastWatchedBinding == null) {
      return
    }

    if (lastWatched != null) {
      lastWatchedBinding?.root?.visibility = View.VISIBLE
      lastWatchedId = lastWatched!!.id

      val title = DataHelper.getEpisodeTitle(
        requireContext(),
        lastWatched!!.title,
        lastWatched!!.season,
        lastWatched!!.episode,
        lastWatched!!.watched
      )
      lastWatchedBinding?.episodeTitle?.text = title

      val firstAiredString =
        DateStringUtils.getAirdateInterval(requireContext(), lastWatched!!.firstAired, false)
      lastWatchedBinding?.episodeAirTime?.text = firstAiredString

      val lastWatchedEpisodeText =
        getString(R.string.season_x_episode_y, lastWatched!!.season, lastWatched!!.episode)
      lastWatchedBinding?.episodeEpisode?.text = lastWatchedEpisodeText

      val screenshotUri = ImageUri.create(ImageUri.ITEM_EPISODE, ImageType.STILL, lastWatchedId)
      lastWatchedBinding?.episodeScreenshot?.setImage(screenshotUri)
    } else {
      lastWatchedBinding?.root?.visibility = if (toWatchId == -1L) View.GONE else View.INVISIBLE
      lastWatchedId = -1
    }

    updateEpisodesContainer()
  }

  private fun updateToCollect() {
    if (toCollectBinding == null) {
      return
    }

    if (toCollect == null) {
      toCollectBinding?.root?.visibility = View.GONE
      toCollectId = -1
    } else {
      toCollectBinding?.root?.visibility = View.VISIBLE
      toCollectId = toCollect!!.id

      val title = DataHelper.getEpisodeTitle(
        requireContext(),
        toCollect!!.title,
        toCollect!!.season,
        toCollect!!.episode,
        toCollect!!.watched
      )
      toCollectBinding?.episodeTitle?.text = title

      val firstAiredString =
        DateStringUtils.getAirdateInterval(requireContext(), toCollect!!.firstAired, false)
      toCollectBinding?.episodeAirTime?.text = firstAiredString

      val toCollectEpisodeText =
        getString(R.string.season_x_episode_y, toCollect!!.season, toCollect!!.episode)
      toCollectBinding?.episodeEpisode?.text = toCollectEpisodeText

      val screenshotUri = ImageUri.create(ImageUri.ITEM_EPISODE, ImageType.STILL, toCollectId)
      toCollectBinding?.episodeScreenshot?.setImage(screenshotUri)
    }

    updateEpisodesContainer()
  }

  private fun updateLastCollected() {
    if (lastCollectedBinding == null) {
      return
    }

    if (lastCollected == null) {
      lastCollectedId = -1
      lastCollectedBinding?.root?.visibility = View.INVISIBLE
    } else {
      lastCollectedBinding?.root?.visibility = View.VISIBLE
      lastCollectedId = lastCollected!!.id

      val title = DataHelper.getEpisodeTitle(
        requireContext(),
        lastCollected!!.title,
        lastCollected!!.season,
        lastCollected!!.episode,
        lastCollected!!.watched
      )
      lastCollectedBinding?.episodeTitle?.text = title

      val firstAiredString =
        DateStringUtils.getAirdateInterval(requireContext(), lastCollected!!.firstAired, false)
      lastCollectedBinding?.episodeAirTime?.text = firstAiredString

      val lastCollectedEpisodeText = getString(
        R.string.season_x_episode_y, lastCollected!!.season,
        lastCollected!!.episode
      )
      lastCollectedBinding?.episodeEpisode?.text = lastCollectedEpisodeText

      val screenshotUri = ImageUri.create(ImageUri.ITEM_EPISODE, ImageType.STILL, lastCollectedId)
      lastCollectedBinding?.episodeScreenshot?.setImage(screenshotUri)
    }

    updateEpisodesContainer()
  }

  private fun updateComments() {
    if (view == null) {
      return
    }

    LinearCommentsAdapter.updateComments(
      requireContext(),
      binding.content.comments.container.container,
      userComments,
      comments
    )
    binding.content.comments.comments.visibility = View.VISIBLE
  }

  companion object {

    private const val TAG = "net.simonvt.cathode.ui.show.ShowFragment"

    private const val ARG_SHOWID = "net.simonvt.cathode.ui.show.ShowFragment.showId"
    private const val ARG_TITLE = "net.simonvt.cathode.ui.show.ShowFragment.title"
    private const val ARG_OVERVIEW = "net.simonvt.cathode.ui.show.ShowFragment.overview"
    private const val ARG_TYPE = "net.simonvt.cathode.ui.show.ShowFragment.type"

    private const val DIALOG_RATING = "net.simonvt.cathode.ui.show.ShowFragment.ratingDialog"
    private const val DIALOG_LISTS_ADD = "net.simonvt.cathode.ui.show.ShowFragment.listsAddDialog"

    @JvmStatic
    fun getTag(showId: Long): String {
      return TAG + "/" + showId + "/" + Ids.newId()
    }

    @JvmStatic
    fun getArgs(showId: Long, title: String?, overview: String?, type: LibraryType): Bundle {
      Preconditions.checkArgument(showId >= 0, "showId must be >= 0")

      val args = Bundle()
      args.putLong(ARG_SHOWID, showId)
      args.putString(ARG_TITLE, title)
      args.putString(ARG_OVERVIEW, overview)
      args.putSerializable(ARG_TYPE, type)
      return args
    }
  }
}
