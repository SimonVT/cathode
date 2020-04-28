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

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import net.simonvt.cathode.R
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.api.util.TraktUtils
import net.simonvt.cathode.common.ui.fragment.RefreshableAppBarFragment
import net.simonvt.cathode.common.ui.instantiate
import net.simonvt.cathode.common.util.DateStringUtils
import net.simonvt.cathode.common.util.Ids
import net.simonvt.cathode.common.util.Intents
import net.simonvt.cathode.common.util.Joiner
import net.simonvt.cathode.common.util.guava.Preconditions
import net.simonvt.cathode.common.widget.CircleTransformation
import net.simonvt.cathode.common.widget.RemoteImageView
import net.simonvt.cathode.databinding.FragmentMovieBinding
import net.simonvt.cathode.entity.CastMember
import net.simonvt.cathode.entity.Comment
import net.simonvt.cathode.entity.Movie
import net.simonvt.cathode.images.ImageType
import net.simonvt.cathode.images.ImageUri
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

class MovieFragment @Inject constructor(
  private val viewModelFactory: CathodeViewModelFactory,
  private val movieScheduler: MovieTaskScheduler
) : RefreshableAppBarFragment() {

  private val viewModel: MovieViewModel by viewModels { viewModelFactory }

  private var movie: Movie? = null
  private var genres: List<String>? = null
  private var cast: List<CastMember>? = null
  private var userComments: List<Comment>? = null
  private var comments: List<Comment>? = null
  private var related: List<Movie>? = null

  private var _binding: FragmentMovieBinding? = null
  private val binding get() = _binding!!

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

  override fun onAttach(context: Context) {
    super.onAttach(context)
    navigationListener = requireActivity() as NavigationListener
  }

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    val args = arguments
    movieId = args!!.getLong(ARG_ID)
    movieTitle = args.getString(ARG_TITLE)
    movieOverview = args.getString(ARG_OVERVIEW)

    setTitle(movieTitle)

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
    _binding = FragmentMovieBinding.inflate(inflater, container, false)
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

    val playDrawable =
      VectorDrawableCompat.create(resources, R.drawable.ic_play_arrow_black_24dp, null)
    binding.content.trailer.setCompoundDrawablesWithIntrinsicBounds(playDrawable, null, null, null)

    binding.content.overview.text = movieOverview

    if (TraktLinkSettings.isLinked(requireContext())) {
      binding.top.rating.setOnClickListener {
        parentFragmentManager.instantiate(
          RatingDialog::class.java,
          RatingDialog.getArgs(RatingDialog.Type.MOVIE, movieId, currentRating)
        ).show(parentFragmentManager, DIALOG_RATING)
      }
    }

    binding.content.cast.castHeader.setOnClickListener(displayCastClickListener)
    binding.content.comments.commentsHeader.setOnClickListener(displayCommentsClickListener)
    binding.content.related.relatedHeader.setOnClickListener(displayRelatedClickListener)

    updateView()
    updateGenreViews()
    updateCast()
    updateRelatedView()
    updateComments()
  }

  override fun onDestroyView() {
    _binding = null
    super.onDestroyView()
  }

  private val displayCastClickListener = View.OnClickListener {
    navigationListener.onDisplayCredits(ItemType.MOVIE, movieId, movieTitle)
  }

  private val displayCommentsClickListener = View.OnClickListener {
    navigationListener.onDisplayComments(ItemType.MOVIE, movieId)
  }

  private val displayRelatedClickListener = View.OnClickListener {
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
        parentFragmentManager.instantiate(
          AddToHistoryDialog::class.java,
          AddToHistoryDialog.getArgs(AddToHistoryDialog.Type.MOVIE, movieId, movieTitle)
        ).show(parentFragmentManager, AddToHistoryDialog.TAG)
        return true
      }

      R.id.action_history_remove -> {
        if (TraktLinkSettings.isLinked(requireContext())) {
          parentFragmentManager.instantiate(
            RemoveFromHistoryDialog::class.java,
            RemoveFromHistoryDialog.getArgs(
              RemoveFromHistoryDialog.Type.MOVIE,
              movieId,
              movieTitle,
              null
            )
          ).show(parentFragmentManager, RemoveFromHistoryDialog.TAG)
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
        parentFragmentManager.instantiate(
          ListsDialog::class.java,
          ListsDialog.getArgs(ItemType.MOVIE, movieId)
        ).show(parentFragmentManager, DIALOG_LISTS_ADD)
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

    val traktId = movie!!.traktId
    val title = movie!!.title
    if (title != movieTitle) {
      movieTitle = title
      setTitle(movieTitle)
    }

    val backdropUri = ImageUri.create(ImageUri.ITEM_MOVIE, ImageType.BACKDROP, movieId)
    setBackdrop(backdropUri, true)

    currentRating = movie!!.userRating
    binding.top.rating.setValue(movie!!.rating)

    movieOverview = movie!!.overview
    watched = movie!!.watched
    collected = movie!!.inCollection
    inWatchlist = movie!!.inWatchlist
    watching = movie!!.watching
    checkedIn = movie!!.checkedIn

    if (checkInDrawable != null) {
      checkInDrawable!!.setWatching(watching || checkedIn)
    }

    val hasCheckmark = watched || collected || inWatchlist
    binding.content.checkmarks.checkmarks.visibility = if (hasCheckmark) View.VISIBLE else View.GONE
    binding.content.checkmarks.isWatched.visibility = if (watched) View.VISIBLE else View.GONE
    binding.content.checkmarks.inCollection.visibility = if (collected) View.VISIBLE else View.GONE
    binding.content.checkmarks.inWatchlist.visibility = if (inWatchlist) View.VISIBLE else View.GONE

    var infoOneText = ""
    if (movie?.year ?: 0 > 0) {
      infoOneText = movie!!.year.toString()
      if (!TextUtils.isEmpty(movie!!.certification)) {
        infoOneText += ", " + movie!!.certification
      }
    } else {
      if (!movie!!.certification.isNullOrEmpty()) {
        infoOneText = movie!!.certification!!
      }
    }
    binding.top.info1.text = infoOneText

    if (movie?.runtime ?: 0 > 0) {
      val runtime = DateStringUtils.getRuntimeString(requireContext(), movie!!.runtime.toLong())
      binding.top.info2.text = runtime
    }

    binding.content.overview.text = movieOverview

    val trailer = movie!!.trailer
    if (!TextUtils.isEmpty(trailer)) {
      binding.content.trailer.visibility = View.VISIBLE
      binding.content.trailer.setOnClickListener { Intents.openUrl(requireContext(), trailer) }
    } else {
      binding.content.trailer.visibility = View.GONE
    }

    val website = movie!!.homepage
    if (!TextUtils.isEmpty(website)) {
      binding.content.website.visibility = View.VISIBLE
      binding.content.website.text = website
      binding.content.website.setOnClickListener { Intents.openUrl(requireContext(), website) }
    } else {
      binding.content.website.visibility = View.GONE
    }

    val imdbId = movie!!.imdbId
    val tmdbId = movie!!.tmdbId

    binding.content.viewOnTrakt.setOnClickListener {
      Intents.openUrl(
        requireContext(),
        TraktUtils.getTraktMovieUrl(traktId)
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

    if (tmdbId > 0) {
      binding.content.viewOnTmdb.visibility = View.VISIBLE
      binding.content.viewOnTmdb.setOnClickListener {
        Intents.openUrl(
          requireContext(),
          TraktUtils.getTmdbMovieUrl(tmdbId)
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
      binding.content.genresTitle.visibility = View.GONE
      binding.content.genresTitle.visibility = View.GONE
      binding.content.genres.visibility = View.GONE
    } else {
      val joinedGenres = Joiner.on(", ").join(genres)

      binding.content.genresTitle.visibility = View.VISIBLE
      binding.content.genresTitle.visibility = View.VISIBLE
      binding.content.genres.visibility = View.VISIBLE
      binding.content.genres.text = joinedGenres
    }
  }

  private fun updateCast() {
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

        val headshotUri =
          ImageUri.create(ImageUri.ITEM_PERSON, ImageType.PROFILE, castMember.person.id)

        val headshot = v.findViewById<RemoteImageView>(R.id.headshot)
        headshot.addTransformation(CircleTransformation())
        headshot.setImage(headshotUri)

        val name = v.findViewById<TextView>(R.id.person_name)
        name.text = castMember.person.name

        val character = v.findViewById<TextView>(R.id.person_job)
        character.text = castMember.character

        v.setOnClickListener { navigationListener.onDisplayPerson(castMember.person.id) }

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

      for (movie in related!!) {
        val v = LayoutInflater.from(requireContext()).inflate(
          R.layout.section_related_item,
          binding.content.related.container.container,
          false
        )

        val poster = ImageUri.create(ImageUri.ITEM_MOVIE, ImageType.POSTER, movie.id)

        val posterView = v.findViewById<RemoteImageView>(R.id.related_poster)
        posterView.addTransformation(CircleTransformation())
        posterView.setImage(poster)

        val titleView = v.findViewById<TextView>(R.id.related_title)
        titleView.text = movie.title

        val formattedRating = String.format(Locale.getDefault(), "%.1f", movie.rating)

        val ratingText: String
        if (movie.votes >= 1000) {
          val convertedVotes = movie.votes / 1000.0f
          val formattedVotes = String.format(Locale.getDefault(), "%.1f", convertedVotes)
          ratingText = getString(R.string.related_rating_thousands, formattedRating, formattedVotes)
        } else {
          ratingText = getString(R.string.related_rating, formattedRating, movie.votes)
        }

        val ratingView = v.findViewById<TextView>(R.id.related_rating)
        ratingView.text = ratingText

        v.setOnClickListener {
          navigationListener.onDisplayMovie(movie.id, movie.title, movie.overview)
        }
        binding.content.related.container.container.addView(v)
      }
    }
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
