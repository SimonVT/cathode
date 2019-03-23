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
package net.simonvt.cathode.ui.movie

import android.app.Activity
import android.os.Bundle
import android.text.TextUtils
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
import dagger.android.support.AndroidSupportInjection
import net.simonvt.cathode.R
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.api.util.TraktUtils
import net.simonvt.cathode.common.entity.CastMember
import net.simonvt.cathode.common.entity.Comment
import net.simonvt.cathode.common.entity.Movie
import net.simonvt.cathode.common.ui.fragment.RefreshableAppBarFragment
import net.simonvt.cathode.common.util.DateStringUtils
import net.simonvt.cathode.common.util.Ids
import net.simonvt.cathode.common.util.Intents
import net.simonvt.cathode.common.util.Joiner
import net.simonvt.cathode.common.util.guava.Preconditions
import net.simonvt.cathode.common.widget.CircleTransformation
import net.simonvt.cathode.common.widget.CircularProgressIndicator
import net.simonvt.cathode.common.widget.RemoteImageView
import net.simonvt.cathode.images.ImageType
import net.simonvt.cathode.images.ImageUri
import net.simonvt.cathode.provider.DatabaseContract
import net.simonvt.cathode.settings.TraktLinkSettings
import net.simonvt.cathode.sync.scheduler.MovieTaskScheduler
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
import java.util.Locale
import javax.inject.Inject

class MovieFragment : RefreshableAppBarFragment() {

  @Inject
  lateinit var movieScheduler: MovieTaskScheduler

  @Inject
  lateinit var viewModelFactory: CathodeViewModelFactory
  lateinit var viewModel: MovieViewModel

  private var movie: Movie? = null
  private var genres: List<String>? = null
  private var cast: List<CastMember>? = null
  private var userComments: List<Comment>? = null
  private var comments: List<Comment>? = null
  private var related: List<Movie>? = null

  @BindView(R.id.info1)
  @JvmField
  var info1: TextView? = null
  @BindView(R.id.info2)
  @JvmField
  var info2: TextView? = null
  //@BindView(R.id.poster) RemoteImageView poster;
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
  var isWatched: TextView? = null
  @BindView(R.id.inCollection)
  @JvmField
  var collection: TextView? = null
  @BindView(R.id.inWatchlist)
  @JvmField
  var watchlist: TextView? = null
  @BindView(R.id.rating)
  @JvmField
  var rating: CircularProgressIndicator? = null

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
  @BindView(R.id.viewOnTmdb)
  @JvmField
  var viewOnTmdb: TextView? = null

  var movieId: Long = 0
    private set

  private var movieTitle: String? = null

  private var movieOverview: String? = null

  private var currentRating: Int = 0

  private var loaded: Boolean = false

  private var watched: Boolean = false

  private var collected: Boolean = false

  private var inWatchlist: Boolean = false

  private var watching: Boolean = false

  private var checkedIn: Boolean = false

  lateinit var navigationListener: NavigationListener

  private var checkInDrawable: CheckInDrawable? = null

  override fun onAttach(activity: Activity) {
    super.onAttach(activity)
    navigationListener = activity as NavigationListener
  }

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    AndroidSupportInjection.inject(this)

    val args = arguments
    movieId = args!!.getLong(ARG_ID)
    movieTitle = args.getString(ARG_TITLE)
    movieOverview = args.getString(ARG_OVERVIEW)

    setTitle(movieTitle)

    viewModel = ViewModelProviders.of(this, viewModelFactory).get(MovieViewModel::class.java)
    viewModel.setMovieId(movieId)
    viewModel.loading.observe(this, Observer { loading -> setRefreshing(loading) })
    viewModel.movie.observe(this, Observer { movie ->
      this@MovieFragment.movie = movie
      updateView()
    })
    viewModel.genres.observe(this, Observer { genres ->
      this@MovieFragment.genres = genres
      updateGenreViews()
    })
    viewModel.cast.observe(this, Observer { castMembers ->
      cast = castMembers
      updateCast()
    })
    viewModel.userComments.observe(this, Observer { userComments ->
      this@MovieFragment.userComments = userComments
      updateComments()
    })
    viewModel.comments.observe(this, Observer { comments ->
      this@MovieFragment.comments = comments
      updateComments()
    })
    viewModel.relatedMovies.observe(this, Observer { movies ->
      related = movies
      updateRelatedView()
    })
  }

  override fun createView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View {
    return inflater.inflate(R.layout.fragment_movie, container, false)
  }

  override fun onViewCreated(view: View, inState: Bundle?) {
    super.onViewCreated(view, inState)
    val linkDrawable = VectorDrawableCompat.create(resources, R.drawable.ic_link_black_24dp, null)
    website!!.setCompoundDrawablesWithIntrinsicBounds(linkDrawable, null, null, null)
    viewOnTrakt!!.setCompoundDrawablesWithIntrinsicBounds(linkDrawable, null, null, null)
    viewOnImdb!!.setCompoundDrawablesWithIntrinsicBounds(linkDrawable, null, null, null)
    viewOnTmdb!!.setCompoundDrawablesWithIntrinsicBounds(linkDrawable, null, null, null)

    val playDrawable =
      VectorDrawableCompat.create(resources, R.drawable.ic_play_arrow_black_24dp, null)
    trailer!!.setCompoundDrawablesWithIntrinsicBounds(playDrawable, null, null, null)

    overview!!.text = movieOverview

    if (TraktLinkSettings.isLinked(requireContext())) {
      rating!!.setOnClickListener {
        RatingDialog.newInstance(RatingDialog.Type.MOVIE, movieId, currentRating)
          .show(fragmentManager!!, DIALOG_RATING)
      }
    }

    updateView()
    updateGenreViews()
    updateCast()
    updateRelatedView()
    updateComments()
  }

  @OnClick(R.id.castHeader)
  fun onDisplayCast() {
    navigationListener.onDisplayCredits(ItemType.MOVIE, movieId, movieTitle)
  }

  @OnClick(R.id.commentsHeader)
  fun onShowComments() {
    navigationListener.onDisplayComments(ItemType.MOVIE, movieId)
  }

  @OnClick(R.id.relatedHeader)
  fun onShowRelated() {
    navigationListener.onDisplayRelatedMovies(movieId, movieTitle)
  }

  override fun onRefresh() {
    viewModel.refresh()
  }

  override fun createMenu(toolbar: Toolbar) {
    if (loaded) {
      val menu = toolbar.menu

      if (checkInDrawable == null) {
        checkInDrawable = CheckInDrawable(toolbar.context)
        checkInDrawable!!.setWatching(watching || checkedIn)
        checkInDrawable!!.setId(movieId)
      }

      val checkInItem: MenuItem

      if (watching || checkedIn) {
        checkInItem = menu.add(0, R.id.action_checkin, 1, R.string.action_checkin_cancel)
      } else {
        checkInItem = menu.add(0, R.id.action_checkin, 1, R.string.action_checkin)
      }

      checkInItem.setIcon(checkInDrawable).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
      checkInItem.isEnabled = !watching

      menu.add(0, R.id.action_history_add, 3, R.string.action_history_add)

      if (watched) {
        menu.add(0, R.id.action_history_remove, 4, R.string.action_history_remove)
      } else {
        if (inWatchlist) {
          menu.add(0, R.id.action_watchlist_remove, 7, R.string.action_watchlist_remove)
        } else {
          menu.add(0, R.id.action_watchlist_add, 8, R.string.action_watchlist_add)
        }
      }

      if (collected) {
        menu.add(0, R.id.action_collection_remove, 5, R.string.action_collection_remove)
      } else {
        menu.add(0, R.id.action_collection_add, 6, R.string.action_collection_add)
      }

      menu.add(0, R.id.action_list_add, 9, R.string.action_list_add)
    }
  }

  override fun onMenuItemClick(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.action_history_add -> {
        AddToHistoryDialog.newInstance(AddToHistoryDialog.Type.MOVIE, movieId, movieTitle)
          .show(fragmentManager!!, AddToHistoryDialog.TAG)
        return true
      }

      R.id.action_history_remove -> {
        if (TraktLinkSettings.isLinked(requireContext())) {
          RemoveFromHistoryDialog.newInstance(
            RemoveFromHistoryDialog.Type.MOVIE, movieId,
            movieTitle
          ).show(fragmentManager!!, RemoveFromHistoryDialog.TAG)
        } else {
          movieScheduler.removeFromHistory(movieId)
        }
        return true
      }

      R.id.action_checkin -> {
        if (!watching) {
          if (checkedIn) {
            movieScheduler.cancelCheckin()
            if (checkInDrawable != null) {
              checkInDrawable!!.setWatching(false)
            }
          } else {
            if (!CheckInDialog.showDialogIfNecessary(
                requireActivity(), Type.MOVIE, movieTitle,
                movieId
              )
            ) {
              movieScheduler.checkin(movieId, null, false, false, false)
              checkInDrawable!!.setWatching(true)
            }
          }
        }
        return true
      }

      R.id.action_checkin_cancel -> {
        movieScheduler.cancelCheckin()
        return true
      }

      R.id.action_watchlist_add -> {
        movieScheduler.setIsInWatchlist(movieId, true)
        return true
      }

      R.id.action_watchlist_remove -> {
        movieScheduler.setIsInWatchlist(movieId, false)
        return true
      }

      R.id.action_collection_add -> {
        movieScheduler.setIsInCollection(movieId, true)
        return true
      }

      R.id.action_collection_remove -> {
        movieScheduler.setIsInCollection(movieId, false)
        return true
      }

      R.id.action_list_add -> {
        ListsDialog.newInstance(DatabaseContract.ItemType.MOVIE, movieId)
          .show(fragmentManager!!, DIALOG_LISTS_ADD)
        return true
      }
    }

    return super.onMenuItemClick(item)
  }

  private fun updateView() {
    if (view == null || movie == null) {
      return
    }

    loaded = true

    val traktId = movie!!.traktId!!
    val title = movie!!.title
    if (title != null && title != movieTitle) {
      movieTitle = title
      setTitle(movieTitle)
    }

    val backdropUri = ImageUri.create(ImageUri.ITEM_MOVIE, ImageType.BACKDROP, movieId)
    setBackdrop(backdropUri, true)

    currentRating = movie!!.userRating!!
    rating!!.setValue(movie!!.rating!!)

    movieOverview = movie!!.overview
    watched = movie!!.watched!!
    collected = movie!!.inCollection!!
    inWatchlist = movie!!.inWatchlist!!
    watching = movie!!.watching!!
    checkedIn = movie!!.checkedIn!!

    if (checkInDrawable != null) {
      checkInDrawable!!.setWatching(watching || checkedIn)
    }

    val hasCheckmark = watched || collected || inWatchlist
    checkmarks!!.visibility = if (hasCheckmark) View.VISIBLE else View.GONE
    isWatched!!.visibility = if (watched) View.VISIBLE else View.GONE
    collection!!.visibility = if (collected) View.VISIBLE else View.GONE
    watchlist!!.visibility = if (inWatchlist) View.VISIBLE else View.GONE

    var infoOneText = ""
    if (movie!!.year != null && movie!!.year > 0) {
      infoOneText = movie!!.year!!.toString()
      if (!TextUtils.isEmpty(movie!!.certification)) {
        infoOneText += ", " + movie!!.certification
      }
    } else {
      if (!movie!!.certification.isNullOrEmpty()) {
        infoOneText = movie!!.certification
      }
    }
    info1!!.text = infoOneText

    if (movie!!.runtime != null && movie!!.runtime > 0) {
      val runtime = DateStringUtils.getRuntimeString(requireContext(), movie!!.runtime!!.toLong())
      info2!!.text = runtime
    }

    this.overview!!.text = movieOverview

    val trailer = movie!!.trailer
    if (!TextUtils.isEmpty(trailer)) {
      this.trailer!!.visibility = View.VISIBLE
      this.trailer!!.setOnClickListener { Intents.openUrl(requireContext(), trailer) }
    } else {
      this.trailer!!.visibility = View.GONE
    }

    val website = movie!!.homepage
    if (!TextUtils.isEmpty(website)) {
      this.website!!.visibility = View.VISIBLE
      this.website!!.text = website
      this.website!!.setOnClickListener { Intents.openUrl(requireContext(), website) }
    } else {
      this.website!!.visibility = View.GONE
    }

    val imdbId = movie!!.imdbId
    val tmdbId = movie!!.tmdbId!!

    viewOnTrakt!!.setOnClickListener {
      Intents.openUrl(
        requireContext(),
        TraktUtils.getTraktMovieUrl(traktId)
      )
    }

    val hasImdbId = !TextUtils.isEmpty(imdbId)
    if (hasImdbId) {
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

    if (tmdbId > 0) {
      viewOnTmdb!!.visibility = View.VISIBLE
      viewOnTmdb!!.setOnClickListener {
        Intents.openUrl(
          requireContext(),
          TraktUtils.getTmdbMovieUrl(tmdbId)
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
      genresTitle!!.visibility = View.GONE
      genresTitle!!.visibility = View.GONE
      genresView!!.visibility = View.GONE
    } else {
      val joinedGenres = Joiner.on(", ").join(genres)

      genresTitle!!.visibility = View.VISIBLE
      genresTitle!!.visibility = View.VISIBLE
      genresView!!.visibility = View.VISIBLE
      genresView!!.text = joinedGenres
    }
  }

  private fun updateCast() {
    if (view == null) {
      return
    }

    castContainer!!.removeAllViews()
    if (cast.isNullOrEmpty()) {
      castParent!!.visibility = View.GONE
    } else {
      castParent!!.visibility = View.VISIBLE

      for (castMember in cast!!) {
        val v = LayoutInflater.from(requireContext())
          .inflate(R.layout.section_people_item, castContainer, false)

        val headshotUri =
          ImageUri.create(ImageUri.ITEM_PERSON, ImageType.PROFILE, castMember.person.id!!)

        val headshot = v.findViewById<RemoteImageView>(R.id.headshot)
        headshot.addTransformation(CircleTransformation())
        headshot.setImage(headshotUri)

        val name = v.findViewById<TextView>(R.id.person_name)
        name.text = castMember.person.name

        val character = v.findViewById<TextView>(R.id.person_job)
        character.text = castMember.character

        v.setOnClickListener { navigationListener.onDisplayPerson(castMember.person.id!!) }

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

      for (movie in related!!) {
        val v = LayoutInflater.from(requireContext())
          .inflate(R.layout.section_related_item, relatedContainer, false)

        val poster = ImageUri.create(ImageUri.ITEM_MOVIE, ImageType.POSTER, movie.id)

        val posterView = v.findViewById<RemoteImageView>(R.id.related_poster)
        posterView.addTransformation(CircleTransformation())
        posterView.setImage(poster)

        val titleView = v.findViewById<TextView>(R.id.related_title)
        titleView.text = movie.title

        val formattedRating = String.format(Locale.getDefault(), "%.1f", movie.rating)

        val ratingText: String
        if (movie.votes >= 1000) {
          val convertedVotes = movie.votes!! / 1000.0f
          val formattedVotes = String.format(Locale.getDefault(), "%.1f", convertedVotes)
          ratingText = getString(R.string.related_rating_thousands, formattedRating, formattedVotes)
        } else {
          ratingText = getString(R.string.related_rating, formattedRating, movie.votes)
        }

        val ratingView = v.findViewById<TextView>(R.id.related_rating)
        ratingView.text = ratingText

        v.setOnClickListener {
          navigationListener.onDisplayMovie(
            movie.id,
            movie.title,
            movie.overview
          )
        }
        relatedContainer!!.addView(v)
      }
    }
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

    private const val TAG = "net.simonvt.cathode.ui.movie.MovieFragment"

    private const val ARG_ID = "net.simonvt.cathode.ui.movie.MovieFragment.id"
    private const val ARG_TITLE = "net.simonvt.cathode.ui.movie.MovieFragment.title"
    private const val ARG_OVERVIEW = "net.simonvt.cathode.ui.movie.MovieFragment.overview"

    private const val DIALOG_RATING = "net.simonvt.cathode.ui.movie.MovieFragment.ratingDialog"
    private const val DIALOG_LISTS_ADD = "net.simonvt.cathode.ui.movie.MovieFragment.listsAddDialog"

    @JvmStatic
    fun getTag(movieId: Long): String {
      return TAG + "/" + movieId + "/" + Ids.newId()
    }

    @JvmStatic
    fun getArgs(movieId: Long, movieTitle: String?, overview: String?): Bundle {
      Preconditions.checkArgument(movieId >= 0, "movieId must be >= 0")

      val args = Bundle()
      args.putLong(ARG_ID, movieId)
      args.putString(ARG_TITLE, movieTitle)
      args.putString(ARG_OVERVIEW, overview)
      return args
    }
  }
}
