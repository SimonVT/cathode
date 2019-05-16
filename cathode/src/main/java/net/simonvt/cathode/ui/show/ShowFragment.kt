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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import butterknife.BindView
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
import net.simonvt.cathode.common.widget.CircularProgressIndicator
import net.simonvt.cathode.common.widget.OverflowView
import net.simonvt.cathode.common.widget.RemoteImageView
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

  lateinit var navigationListener: NavigationListener

  var showId: Long = 0
    private set

  lateinit var viewModel: ShowViewModel

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

  @BindView(R.id.seasonsDivider)
  @JvmField
  var seasonsDivider: View? = null
  @BindView(R.id.seasonsTitle)
  @JvmField
  var seasonsTitle: View? = null
  @BindView(R.id.seasons)
  @JvmField
  var seasonsView: RecyclerView? = null
  lateinit var seasonsAdapter: SeasonsAdapter

  @BindView(R.id.rating)
  @JvmField
  var rating: CircularProgressIndicator? = null
  @BindView(R.id.airtime)
  @JvmField
  var airTime: TextView? = null
  @BindView(R.id.status)
  @JvmField
  var status: TextView? = null
  @BindView(R.id.overview)
  @JvmField
  var overview: TextView? = null

  @BindView(R.id.genresDivider)
  @JvmField
  var genresDivider: View? = null
  @BindView(R.id.genresTitle)
  @JvmField
  var genresTitle: View? = null
  @BindView(R.id.genres)
  @JvmField
  var genresView: TextView? = null

  @BindView(R.id.checkmarks)
  @JvmField
  var checkmarks: View? = null
  @BindView(R.id.isWatched)
  @JvmField
  var watched: TextView? = null
  @BindView(R.id.inCollection)
  @JvmField
  var collection: TextView? = null
  @BindView(R.id.inWatchlist)
  @JvmField
  var watchlist: TextView? = null

  @BindView(R.id.castParent)
  @JvmField
  var castParent: View? = null
  @BindView(R.id.castHeader)
  @JvmField
  var castHeader: View? = null
  @BindView(R.id.castContainer)
  @JvmField
  var castContainer: LinearLayout? = null

  @BindView(R.id.commentsParent)
  @JvmField
  var commentsParent: View? = null
  @BindView(R.id.commentsHeader)
  @JvmField
  var commentsHeader: View? = null
  @BindView(R.id.commentsContainer)
  @JvmField
  var commentsContainer: LinearLayout? = null

  @BindView(R.id.relatedParent)
  @JvmField
  var relatedParent: View? = null
  @BindView(R.id.relatedHeader)
  @JvmField
  var relatedHeader: View? = null
  @BindView(R.id.relatedContainer)
  @JvmField
  var relatedContainer: LinearLayout? = null

  @BindView(R.id.trailer)
  @JvmField
  var trailer: TextView? = null
  @BindView(R.id.website)
  @JvmField
  var website: TextView? = null
  @BindView(R.id.viewOnTrakt)
  @JvmField
  var viewOnTrakt: TextView? = null
  @BindView(R.id.viewOnImdb)
  @JvmField
  var viewOnImdb: TextView? = null
  @BindView(R.id.viewOnTvdb)
  @JvmField
  var viewOnTvdb: TextView? = null
  @BindView(R.id.viewOnTmdb)
  @JvmField
  var viewOnTmdb: TextView? = null

  @BindView(R.id.episodes)
  @JvmField
  var episodes: LinearLayout? = null

  @BindView(R.id.toWatch)
  @JvmField
  var toWatchView: View? = null
  private var toWatchHolder: EpisodeHolder? = null
  private var toWatchId: Long = -1
  private var toWatchTitle: String? = null

  @BindView(R.id.lastWatched)
  @JvmField
  var lastWatchedView: View? = null
  private var lastWatchedHolder: EpisodeHolder? = null
  private var lastWatchedId: Long = -1

  @BindView(R.id.toCollect)
  @JvmField
  var toCollectView: View? = null
  private var toCollectHolder: EpisodeHolder? = null
  private var toCollectId: Long = -1

  @BindView(R.id.lastCollected)
  @JvmField
  var lastCollectedView: View? = null
  private var lastCollectedHolder: EpisodeHolder? = null
  private var lastCollectedId: Long = -1

  private var showTitle: String? = null

  private var showOverview: String? = null

  private var inWatchlist: Boolean = false

  private var currentRating: Int = 0

  private var calendarHidden: Boolean = false

  private lateinit var type: LibraryType

  private class EpisodeHolder(v: View) {

    val episodeScreenshot: RemoteImageView = v.findViewById(R.id.episodeScreenshot)
    val episodeTitle: TextView = v.findViewById(R.id.episodeTitle)
    val episodeAirTime: TextView = v.findViewById(R.id.episodeAirTime)
    val episodeEpisode: TextView = v.findViewById(R.id.episodeEpisode)
    val episodeOverflow: OverflowView = v.findViewById(R.id.episodeOverflow)
  }

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

    viewModel = ViewModelProviders.of(this, viewModelFactory).get(ShowViewModel::class.java)
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
    return inflater.inflate(R.layout.fragment_show, container, false)
  }

  override fun onViewCreated(view: View, inState: Bundle?) {
    super.onViewCreated(view, inState)
    val linkDrawable = VectorDrawableCompat.create(resources, R.drawable.ic_link_black_24dp, null)
    website!!.setCompoundDrawablesWithIntrinsicBounds(linkDrawable, null, null, null)
    viewOnTrakt!!.setCompoundDrawablesWithIntrinsicBounds(linkDrawable, null, null, null)
    viewOnImdb!!.setCompoundDrawablesWithIntrinsicBounds(linkDrawable, null, null, null)
    viewOnTmdb!!.setCompoundDrawablesWithIntrinsicBounds(linkDrawable, null, null, null)
    viewOnTvdb!!.setCompoundDrawablesWithIntrinsicBounds(linkDrawable, null, null, null)

    val playDrawable =
      VectorDrawableCompat.create(resources, R.drawable.ic_play_arrow_black_24dp, null)
    trailer!!.setCompoundDrawablesWithIntrinsicBounds(playDrawable, null, null, null)

    overview!!.text = showOverview

    val decoration = DividerItemDecoration(requireContext(), LinearLayoutManager.HORIZONTAL)
    decoration.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.divider_4dp)!!)
    seasonsView!!.addItemDecoration(decoration)
    seasonsView!!.layoutManager =
      LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    seasonsView!!.adapter = seasonsAdapter
    (seasonsView!!.itemAnimator as DefaultItemAnimator).supportsChangeAnimations = false
    seasonsAdapter.registerAdapterDataObserver(object : AdapterCountDataObserver(seasonsAdapter) {
      override fun onCountChanged(itemCount: Int) {
        if (seasonsTitle != null) {
          if (itemCount == 0) {
            seasonsDivider!!.visibility = View.GONE
            seasonsTitle!!.visibility = View.GONE
            seasonsView!!.visibility = View.GONE
          } else {
            seasonsDivider!!.visibility = View.VISIBLE
            seasonsTitle!!.visibility = View.VISIBLE
            seasonsView!!.visibility = View.VISIBLE
          }
        }
      }
    })
    if (seasonsAdapter.itemCount > 0) {
      seasonsDivider!!.visibility = View.VISIBLE
      seasonsTitle!!.visibility = View.VISIBLE
      seasonsView!!.visibility = View.VISIBLE
    } else {
      seasonsDivider!!.visibility = View.GONE
      seasonsTitle!!.visibility = View.GONE
      seasonsView!!.visibility = View.GONE
    }

    if (TraktLinkSettings.isLinked(requireContext())) {
      rating!!.setOnClickListener {
        requireFragmentManager().instantiate(
          RatingDialog::class.java,
          RatingDialog.getArgs(RatingDialog.Type.SHOW, showId, currentRating)
        ).show(requireFragmentManager(), DIALOG_RATING)
      }
    }

    castHeader!!.setOnClickListener {
      navigationListener.onDisplayCredits(
        ItemType.SHOW,
        showId,
        showTitle
      )
    }

    commentsHeader!!.setOnClickListener {
      navigationListener.onDisplayComments(
        ItemType.SHOW,
        showId
      )
    }

    relatedHeader!!.setOnClickListener {
      navigationListener.onDisplayRelatedShows(
        showId,
        showTitle
      )
    }

    toWatchHolder = EpisodeHolder(toWatchView!!)
    toWatchView!!.setOnClickListener {
      if (toWatchId != -1L) navigationListener.onDisplayEpisode(
        toWatchId,
        showTitle
      )
    }

    toWatchHolder!!.episodeOverflow.setListener(object : OverflowView.OverflowActionListener {
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
            requireFragmentManager().instantiate(
              AddToHistoryDialog::class.java,
              AddToHistoryDialog.getArgs(AddToHistoryDialog.Type.EPISODE, toWatchId, null)
            ).show(requireFragmentManager(), AddToHistoryDialog.TAG)
          }
        }
      }
    })

    if (lastWatchedView != null) {
      lastWatchedHolder = EpisodeHolder(lastWatchedView!!)
      lastWatchedView!!.setOnClickListener {
        if (lastWatchedId != -1L) {
          navigationListener.onDisplayEpisode(lastWatchedId, showTitle)
        }
      }
    }

    toCollectHolder = EpisodeHolder(toCollectView!!)
    toCollectView!!.setOnClickListener {
      if (toCollectId != -1L) navigationListener.onDisplayEpisode(
        toCollectId,
        showTitle
      )
    }

    toCollectHolder!!.episodeOverflow.addItem(
      R.id.action_collection_add,
      R.string.action_collection_add
    )
    toCollectHolder!!.episodeOverflow.setListener(object : OverflowView.OverflowActionListener {
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

    if (lastCollectedView != null) {
      lastCollectedHolder = EpisodeHolder(lastCollectedView!!)
      lastCollectedView!!.setOnClickListener {
        if (lastCollectedId != -1L) {
          navigationListener.onDisplayEpisode(lastCollectedId, showTitle)
        }
      }

      lastCollectedHolder!!.episodeOverflow.addItem(
        R.id.action_collection_remove,
        R.string.action_collection_remove
      )
      lastCollectedHolder!!.episodeOverflow.setListener(object :
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
        requireFragmentManager().instantiate(
          ListsDialog::class.java,
          ListsDialog.getArgs(ItemType.SHOW, showId)
        ).show(requireFragmentManager(), DIALOG_LISTS_ADD)
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
    rating!!.setValue(show!!.rating)

    calendarHidden = show!!.hiddenCalendar

    val isWatched = show!!.watchedCount > 0
    val isCollected = show!!.inCollectionCount > 0
    inWatchlist = show!!.inWatchlist
    val hasCheckmark = isWatched || isCollected || inWatchlist
    checkmarks!!.visibility = if (hasCheckmark) View.VISIBLE else View.GONE
    watched!!.visibility = if (isWatched) View.VISIBLE else View.GONE
    collection!!.visibility = if (isCollected) View.VISIBLE else View.GONE
    watchlist!!.visibility = if (inWatchlist) View.VISIBLE else View.GONE

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

    this.airTime!!.text = airTimeString

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

    this.status!!.text = statusString

    this.overview!!.text = showOverview

    val trailer = show!!.trailer
    if (!TextUtils.isEmpty(trailer)) {
      this.trailer!!.visibility = View.VISIBLE
      this.trailer!!.setOnClickListener { Intents.openUrl(requireContext(), trailer) }
    } else {
      this.trailer!!.visibility = View.GONE
    }

    val website = show!!.homepage
    if (!TextUtils.isEmpty(website)) {
      this.website!!.visibility = View.VISIBLE
      this.website!!.text = website
      this.website!!.setOnClickListener { Intents.openUrl(requireContext(), website) }
    } else {
      this.website!!.visibility = View.GONE
    }

    val traktId = show!!.traktId
    val imdbId = show!!.imdbId
    val tvdbId = show!!.tvdbId
    val tmdbId = show!!.tmdbId

    viewOnTrakt!!.visibility = View.VISIBLE
    viewOnTrakt!!.setOnClickListener {
      Intents.openUrl(
        requireContext(),
        TraktUtils.getTraktShowUrl(traktId)
      )
    }

    if (!imdbId.isNullOrEmpty()) {
      viewOnImdb!!.visibility = View.VISIBLE
      viewOnImdb!!.setOnClickListener {
        Intents.openUrl(
          requireContext(),
          TraktUtils.getImdbUrl(imdbId)
        )
      }
    } else {
      viewOnImdb!!.visibility = View.GONE
    }

    if (tvdbId > 0) {
      viewOnTvdb!!.visibility = View.VISIBLE
      viewOnTvdb!!.setOnClickListener {
        Intents.openUrl(
          requireContext(),
          TraktUtils.getTvdbUrl(tvdbId)
        )
      }
    } else {
      viewOnTvdb!!.visibility = View.GONE
    }

    if (tmdbId > 0) {
      viewOnTmdb!!.visibility = View.VISIBLE
      viewOnTmdb!!.setOnClickListener {
        Intents.openUrl(
          requireContext(),
          TraktUtils.getTmdbTvUrl(tmdbId)
        )
      }
    } else {
      viewOnTmdb!!.visibility = View.GONE
    }

    invalidateMenu()
  }

  private fun updateGenreViews() {
    if (view == null) {
      return
    }

    if (genres == null || genres!!.isEmpty()) {
      genresDivider!!.visibility = View.GONE
      genresTitle!!.visibility = View.GONE
      genresView!!.visibility = View.GONE
    } else {
      genresDivider!!.visibility = View.VISIBLE
      genresTitle!!.visibility = View.VISIBLE
      genresView!!.visibility = View.VISIBLE
      genresView!!.text = Joiner.on(", ").join(genres)
    }
  }

  private fun updateCastViews() {
    if (view == null) {
      return
    }

    castContainer!!.removeAllViews()
    if (cast.isNullOrEmpty()) {
      castParent!!.visibility = View.GONE
    } else {
      castParent!!.visibility = View.VISIBLE

      for (castMember in cast!!) {
        val v =
          LayoutInflater.from(requireContext())
            .inflate(R.layout.section_people_item, castContainer, false)

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

        castContainer!!.addView(v)
      }
    }
  }

  private fun updateRelatedView() {
    if (view == null) {
      return
    }

    relatedContainer!!.removeAllViews()
    if (related.isNullOrEmpty()) {
      relatedParent!!.visibility = View.GONE
    } else {
      relatedParent!!.visibility = View.VISIBLE

      for (show in related!!) {
        val v = LayoutInflater.from(requireContext())
          .inflate(R.layout.section_related_item, this.relatedContainer, false)

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
        relatedContainer!!.addView(v)
      }
    }
  }

  private fun updateEpisodesContainer() {
    if (view == null) {
      return
    }

    if (toWatchId == -1L && lastWatchedId == -1L && toCollectId == -1L && lastCollectedId == -1L) {
      episodes!!.visibility = View.GONE
    } else {
      episodes!!.visibility = View.VISIBLE
    }
  }

  private fun updateToWatch() {
    if (view == null) {
      return
    }

    if (toWatch == null) {
      toWatchView!!.visibility = View.GONE
      toWatchId = -1
    } else {
      toWatchView!!.visibility = View.VISIBLE
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

      toWatchHolder!!.episodeTitle.text = toWatchTitle
      toWatchHolder!!.episodeEpisode.text = toWatchEpisodeText

      val screenshotUri = ImageUri.create(ImageUri.ITEM_EPISODE, ImageType.STILL, toWatchId)
      toWatchHolder!!.episodeScreenshot.setImage(screenshotUri)

      var firstAiredString =
        DateStringUtils.getAirdateInterval(requireContext(), toWatch!!.firstAired, false)

      toWatchHolder!!.episodeOverflow.removeItems()
      if (toWatch!!.checkedIn) {
        toWatchHolder!!.episodeOverflow.addItem(
          R.id.action_checkin_cancel,
          R.string.action_checkin_cancel
        )
        firstAiredString = resources.getString(R.string.show_watching)
      } else if (!toWatch!!.watching) {
        toWatchHolder!!.episodeOverflow.addItem(R.id.action_checkin, R.string.action_checkin)
        toWatchHolder!!.episodeOverflow.addItem(
          R.id.action_history_add,
          R.string.action_history_add
        )
      }

      toWatchHolder!!.episodeAirTime.text = firstAiredString
    }

    updateEpisodesContainer()
  }

  private fun updateLastWatched() {
    if (lastWatchedView == null) {
      return
    }

    if (lastWatched != null) {
      lastWatchedView!!.visibility = View.VISIBLE
      lastWatchedId = lastWatched!!.id

      val title = DataHelper.getEpisodeTitle(
        requireContext(),
        lastWatched!!.title,
        lastWatched!!.season,
        lastWatched!!.episode,
        lastWatched!!.watched
      )
      lastWatchedHolder!!.episodeTitle.text = title

      val firstAiredString =
        DateStringUtils.getAirdateInterval(requireContext(), lastWatched!!.firstAired, false)
      lastWatchedHolder!!.episodeAirTime.text = firstAiredString

      val lastWatchedEpisodeText =
        getString(R.string.season_x_episode_y, lastWatched!!.season, lastWatched!!.episode)
      lastWatchedHolder!!.episodeEpisode.text = lastWatchedEpisodeText

      val screenshotUri = ImageUri.create(ImageUri.ITEM_EPISODE, ImageType.STILL, lastWatchedId)
      lastWatchedHolder!!.episodeScreenshot.setImage(screenshotUri)
    } else {
      lastWatchedView!!.visibility = if (toWatchId == -1L) View.GONE else View.INVISIBLE
      lastWatchedId = -1
    }

    updateEpisodesContainer()
  }

  private fun updateToCollect() {
    if (view == null) {
      return
    }

    if (toCollect == null) {
      toCollectView!!.visibility = View.GONE
      toCollectId = -1
    } else {
      toCollectView!!.visibility = View.VISIBLE
      toCollectId = toCollect!!.id

      val title = DataHelper.getEpisodeTitle(
        requireContext(),
        toCollect!!.title,
        toCollect!!.season,
        toCollect!!.episode,
        toCollect!!.watched
      )
      toCollectHolder!!.episodeTitle.text = title

      val firstAiredString =
        DateStringUtils.getAirdateInterval(requireContext(), toCollect!!.firstAired, false)
      toCollectHolder!!.episodeAirTime.text = firstAiredString

      val toCollectEpisodeText =
        getString(R.string.season_x_episode_y, toCollect!!.season, toCollect!!.episode)
      toCollectHolder!!.episodeEpisode.text = toCollectEpisodeText

      val screenshotUri = ImageUri.create(ImageUri.ITEM_EPISODE, ImageType.STILL, toCollectId)
      toCollectHolder!!.episodeScreenshot.setImage(screenshotUri)
    }

    updateEpisodesContainer()
  }

  private fun updateLastCollected() {
    if (lastCollectedView == null) {
      return
    }

    if (lastCollected == null) {
      lastCollectedId = -1
      lastCollectedView!!.visibility = View.INVISIBLE
    } else {
      lastCollectedView!!.visibility = View.VISIBLE
      lastCollectedId = lastCollected!!.id

      val title = DataHelper.getEpisodeTitle(
        requireContext(),
        lastCollected!!.title,
        lastCollected!!.season,
        lastCollected!!.episode,
        lastCollected!!.watched
      )
      lastCollectedHolder!!.episodeTitle.text = title

      val firstAiredString =
        DateStringUtils.getAirdateInterval(requireContext(), lastCollected!!.firstAired, false)
      lastCollectedHolder!!.episodeAirTime.text = firstAiredString

      val lastCollectedEpisodeText = getString(
        R.string.season_x_episode_y, lastCollected!!.season,
        lastCollected!!.episode
      )
      lastCollectedHolder!!.episodeEpisode.text = lastCollectedEpisodeText

      val screenshotUri = ImageUri.create(ImageUri.ITEM_EPISODE, ImageType.STILL, lastCollectedId)
      lastCollectedHolder!!.episodeScreenshot.setImage(screenshotUri)
    }

    updateEpisodesContainer()
  }

  private fun updateComments() {
    if (view == null) {
      return
    }

    LinearCommentsAdapter.updateComments(
      requireContext(),
      commentsContainer!!,
      userComments,
      comments
    )
    commentsParent!!.visibility = View.VISIBLE
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
