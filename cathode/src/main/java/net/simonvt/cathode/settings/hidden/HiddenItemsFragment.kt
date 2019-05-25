package net.simonvt.cathode.settings.hidden

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import net.simonvt.cathode.R
import net.simonvt.cathode.common.ui.fragment.ToolbarSwipeRefreshRecyclerFragment
import net.simonvt.cathode.entity.Movie
import net.simonvt.cathode.entity.Show
import net.simonvt.cathode.settings.TraktLinkSettings
import net.simonvt.cathode.sync.scheduler.MovieTaskScheduler
import net.simonvt.cathode.sync.scheduler.ShowTaskScheduler
import net.simonvt.cathode.ui.CathodeViewModelFactory
import net.simonvt.cathode.ui.LibraryType
import net.simonvt.cathode.ui.NavigationListener
import java.util.ArrayList
import javax.inject.Inject

class HiddenItemsFragment @Inject constructor(
  private val viewModelFactory: CathodeViewModelFactory,
  private val showScheduler: ShowTaskScheduler,
  private val movieScheduler: MovieTaskScheduler
) : ToolbarSwipeRefreshRecyclerFragment<RecyclerView.ViewHolder>(),
  HiddenItemsAdapter.ItemCallbacks {

  private lateinit var viewModel: HiddenViewModel

  private var adapter: HiddenItemsAdapter? = null

  private var hiddenShowsCalendar: List<Show>? = null
  private var hiddenShowsWatched: List<Show>? = null
  private var hiddenShowsCollected: List<Show>? = null
  private var hiddenMoviesCalendar: List<Movie>? = null

  private lateinit var navigationListener: NavigationListener

  override fun onAttach(context: Context) {
    super.onAttach(context)
    navigationListener = requireActivity() as NavigationListener
  }

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    setTitle(R.string.preference_hidden_items)
    setEmptyText(R.string.preference_hidden_empty)

    viewModel = ViewModelProviders.of(this, viewModelFactory).get(HiddenViewModel::class.java)
    viewModel.loading.observe(this, Observer { loading -> setRefreshing(loading) })
    viewModel.showsCalendar.observe(this, Observer { shows ->
      hiddenShowsCalendar = shows
      ensureAdapter()
      adapter!!.updateHeaderItems(
        R.string.header_hidden_calendar_shows,
        ArrayList<Any>(shows)
      )
    })
    viewModel.showsWatched.observe(this, Observer { shows ->
      hiddenShowsWatched = shows
      ensureAdapter()
      adapter!!.updateHeaderItems(
        R.string.header_hidden_watched_shows,
        ArrayList<Any>(shows)
      )
    })
    viewModel.showsCollected.observe(this, Observer { shows ->
      hiddenShowsCollected = shows
      ensureAdapter()
      adapter!!.updateHeaderItems(
        R.string.header_hidden_collected_shows,
        ArrayList<Any>(shows)
      )
    })
    viewModel.moviesCalendar.observe(this, Observer { movies ->
      hiddenMoviesCalendar = movies
      ensureAdapter()
      adapter!!.updateHeaderItems(
        R.string.header_hidden_calendar_movies,
        ArrayList<Any>(movies)
      )
    })
  }

  override fun onViewCreated(view: View, inState: Bundle?) {
    super.onViewCreated(view, inState)
    swipeRefreshLayout.isEnabled = TraktLinkSettings.isLinked(requireContext())
  }

  override fun onRefresh() {
    viewModel.refresh()
  }

  override fun getColumnCount(): Int {
    return resources.getInteger(R.integer.hiddenColumns)
  }

  override fun onShowClicked(showId: Long, title: String, overview: String) {
    navigationListener.onDisplayShow(showId, title, overview, LibraryType.WATCHED)
  }

  override fun displayShowInCalendar(showId: Long) {
    showScheduler.hideFromCalendar(showId, false)
  }

  override fun displayShowInWatched(showId: Long) {
    showScheduler.hideFromWatched(showId, false)
  }

  override fun displayShowInCollection(showId: Long) {
    showScheduler.hideFromCollected(showId, false)
  }

  override fun onMovieClicked(movieId: Long, title: String?, overview: String?) {
    navigationListener.onDisplayMovie(movieId, title, overview)
  }

  override fun displayMovieInCalendar(movieId: Long) {
    movieScheduler.hideFromCalendar(movieId, false)
  }

  private fun ensureAdapter() {
    if (adapter == null) {
      adapter = HiddenItemsAdapter(requireContext(), this)
      adapter!!.addHeader(R.string.header_hidden_calendar_shows)
      adapter!!.addHeader(R.string.header_hidden_watched_shows)
      adapter!!.addHeader(R.string.header_hidden_collected_shows)
      adapter!!.addHeader(R.string.header_hidden_calendar_movies)
      setAdapter(adapter)
    }
  }
}
