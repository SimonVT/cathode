/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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
package net.simonvt.cathode.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import butterknife.BindView
import butterknife.ButterKnife
import dagger.android.AndroidInjection
import net.simonvt.cathode.R
import net.simonvt.cathode.api.enumeration.Department
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.common.event.AuthFailedEvent
import net.simonvt.cathode.common.event.AuthFailedEvent.OnAuthFailedListener
import net.simonvt.cathode.common.event.ErrorEvent
import net.simonvt.cathode.common.event.ErrorEvent.ErrorListener
import net.simonvt.cathode.common.event.RequestFailedEvent
import net.simonvt.cathode.common.event.RequestFailedEvent.OnRequestFailedListener
import net.simonvt.cathode.common.event.SyncEvent
import net.simonvt.cathode.common.event.SyncEvent.OnSyncListener
import net.simonvt.cathode.common.ui.FragmentContract
import net.simonvt.cathode.common.util.FragmentStack
import net.simonvt.cathode.common.util.FragmentStack.StackEntry
import net.simonvt.cathode.common.util.MainHandler
import net.simonvt.cathode.common.widget.Crouton
import net.simonvt.cathode.entity.Movie
import net.simonvt.cathode.entity.ShowWithEpisode
import net.simonvt.cathode.images.ImageType
import net.simonvt.cathode.images.ImageUri
import net.simonvt.cathode.provider.util.DataHelper
import net.simonvt.cathode.settings.LinkPromptBottomSheet
import net.simonvt.cathode.settings.Settings
import net.simonvt.cathode.settings.SettingsActivity
import net.simonvt.cathode.settings.SetupPromptBottomSheet
import net.simonvt.cathode.settings.StartPage
import net.simonvt.cathode.settings.TraktLinkSettings
import net.simonvt.cathode.settings.login.LoginActivity
import net.simonvt.cathode.sync.scheduler.MovieTaskScheduler
import net.simonvt.cathode.sync.scheduler.ShowTaskScheduler
import net.simonvt.cathode.ui.comments.CommentFragment
import net.simonvt.cathode.ui.comments.CommentsFragment
import net.simonvt.cathode.ui.credits.CreditFragment
import net.simonvt.cathode.ui.credits.CreditsFragment
import net.simonvt.cathode.ui.dashboard.DashboardFragment
import net.simonvt.cathode.ui.history.SelectHistoryDateFragment
import net.simonvt.cathode.ui.lists.ListFragment
import net.simonvt.cathode.ui.lists.ListsFragment
import net.simonvt.cathode.ui.movie.MovieFragment
import net.simonvt.cathode.ui.movie.MovieHistoryFragment
import net.simonvt.cathode.ui.movie.RelatedMoviesFragment
import net.simonvt.cathode.ui.movies.collected.CollectedMoviesFragment
import net.simonvt.cathode.ui.movies.watched.WatchedMoviesFragment
import net.simonvt.cathode.ui.movies.watchlist.MovieWatchlistFragment
import net.simonvt.cathode.ui.navigation.NavigationFragment
import net.simonvt.cathode.ui.person.PersonCreditsFragment
import net.simonvt.cathode.ui.person.PersonFragment
import net.simonvt.cathode.ui.search.SearchFragment
import net.simonvt.cathode.ui.show.EpisodeFragment
import net.simonvt.cathode.ui.show.EpisodeHistoryFragment
import net.simonvt.cathode.ui.show.RelatedShowsFragment
import net.simonvt.cathode.ui.show.SeasonFragment
import net.simonvt.cathode.ui.show.ShowFragment
import net.simonvt.cathode.ui.shows.collected.CollectedShowsFragment
import net.simonvt.cathode.ui.shows.upcoming.UpcomingShowsFragment
import net.simonvt.cathode.ui.shows.watched.WatchedShowsFragment
import net.simonvt.cathode.ui.shows.watchlist.ShowsWatchlistFragment
import net.simonvt.cathode.ui.stats.StatsFragment
import net.simonvt.cathode.ui.suggestions.movies.MovieSuggestionsFragment
import net.simonvt.cathode.ui.suggestions.shows.ShowSuggestionsFragment
import net.simonvt.cathode.widget.WatchingView
import net.simonvt.cathode.widget.WatchingView.WatchingViewListener
import timber.log.Timber
import javax.inject.Inject

class HomeActivity : BaseActivity(), NavigationFragment.OnMenuClickListener, NavigationListener,
  LinkPromptBottomSheet.LinkPromptDismissListener {

  @Inject
  lateinit var showScheduler: ShowTaskScheduler
  @Inject
  lateinit var movieScheduler: MovieTaskScheduler

  @BindView(R.id.progress_top)
  lateinit var progressTop: ProgressBar

  @BindView(R.id.crouton)
  lateinit var crouton: Crouton

  private lateinit var stack: FragmentStack

  @BindView(R.id.drawer)
  lateinit var drawer: DrawerLayout
  private lateinit var navigation: NavigationFragment

  @BindView(R.id.watching_parent)
  lateinit var watchingParent: ViewGroup
  @BindView(R.id.watchingView)
  lateinit var watchingView: WatchingView

  @BindView(R.id.authFailedView)
  lateinit var authFailedView: View
  @BindView(R.id.authFailedAction)
  lateinit var authFailedAction: View

  private lateinit var viewModel: HomeViewModel

  private var watchingShow: ShowWithEpisode? = null
  private var watchingMovie: Movie? = null

  private var pendingReplacement: PendingReplacement? = null

  private var isSyncing = false

  private val drawerListener = object : DrawerLayout.DrawerListener {
    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

    override fun onDrawerOpened(drawerView: View) {
      pendingReplacement = null
    }

    override fun onDrawerClosed(drawerView: View) {
      if (pendingReplacement != null) {
        stack.replace(pendingReplacement!!.fragment, pendingReplacement!!.tag)
        pendingReplacement = null
      }
    }

    override fun onDrawerStateChanged(newState: Int) {
      if (newState == DrawerLayout.STATE_DRAGGING) {
        pendingReplacement = null
      }
    }
  }

  private val watchingTouchListener = View.OnTouchListener { _, event ->
    if (watchingView.isExpanded) {
      val action = event.actionMasked
      if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
        watchingView.collapse()
      }

      return@OnTouchListener true
    }

    false
  }

  private val watchingListener = object : WatchingViewListener {
    override fun onExpand(view: WatchingView) {
      Timber.d("onExpand")
    }

    override fun onCollapse(view: WatchingView) {
      Timber.d("onCollapse")
    }

    override fun onEpisodeClicked(view: WatchingView, episodeId: Long, showTitle: String?) {
      watchingView.collapse()

      val top = stack.peek()
      if (top is EpisodeFragment) {
        if (episodeId == top.episodeId) {
          return
        }
      }

      onDisplayEpisode(episodeId, showTitle)
    }

    override fun onMovieClicked(view: WatchingView, id: Long, title: String, overview: String) {
      watchingView.collapse()

      val top = stack.peek()
      if (top is MovieFragment) {
        if (id == top.movieId) {
          return
        }
      }

      onDisplayMovie(id, title, overview)
    }

    override fun onAnimatingIn(view: WatchingView) {}

    override fun onAnimatingOut(view: WatchingView) {}
  }

  private val onSyncEvent = OnSyncListener { syncing ->
    if (syncing != this@HomeActivity.isSyncing) {
      this@HomeActivity.isSyncing = syncing

      val progressVisibility = progressTop.visibility
      val progressAnimator = progressTop.animate()
      if (syncing) {
        if (progressVisibility == View.GONE) {
          progressTop.alpha = 0.0f
          progressTop.visibility = View.VISIBLE
        }

        progressAnimator.alpha(1.0f)
      } else {
        progressAnimator.alpha(0.0f).withEndAction { progressTop.visibility = View.GONE }
      }
    }
  }

  private val requestFailedListener = OnRequestFailedListener { event ->
    crouton.show(
      getString(event.errorMessage),
      ContextCompat.getColor(this, android.R.color.holo_red_dark)
    )
  }

  private val checkInFailedListener = ErrorListener { error ->
    crouton.show(error, ContextCompat.getColor(this, android.R.color.holo_red_dark))
  }

  private val onAuthFailedListener =
    OnAuthFailedListener { authFailedView.visibility = View.VISIBLE }

  private class PendingReplacement(var fragment: Class<*>, var tag: String)

  override fun onCreate(inState: Bundle?) {
    setTheme(R.style.Theme)
    super.onCreate(inState)
    AndroidInjection.inject(this)

    setContentView(R.layout.activity_home)
    ButterKnife.bind(this)

    drawer.addDrawerListener(drawerListener)
    watchingParent.setOnTouchListener(watchingTouchListener)
    watchingView.setWatchingViewListener(watchingListener)
    authFailedAction.setOnClickListener { startLoginActivity() }

    navigation =
      supportFragmentManager.findFragmentByTag(NavigationFragment.TAG) as NavigationFragment

    stack = FragmentStack.forContainer(this, R.id.content)
    stack.setDefaultAnimation(
      R.anim.fade_in_front,
      R.anim.fade_out_back,
      R.anim.fade_in_back,
      R.anim.fade_out_front
    )
    if (inState != null) {
      stack.restoreState(inState.getBundle(STATE_STACK))
    }

    val intent = intent

    if (isShowStartPageIntent(intent)) {
      var startPage: StartPage? = intent.getSerializableExtra(EXTRA_START_PAGE) as StartPage
      if (startPage == null) {
        startPage = StartPage.SHOWS_UPCOMING
      }
      navigation.setSelectedId(startPage.menuId.toLong())
      stack.replace(startPage.pageClass, startPage.tag)
    } else if (isShowUpcomingAction(intent)) {
      navigation.setSelectedId(StartPage.SHOWS_UPCOMING.menuId.toLong())
      stack.replace(StartPage.SHOWS_UPCOMING.pageClass, StartPage.SHOWS_UPCOMING.tag)
    } else {
      if (stack.size() == 0) {
        val startPagePref = Settings.get(this).getString(Settings.START_PAGE, null)
        val startPage = StartPage.fromValue(startPagePref, StartPage.DASHBOARD)
        navigation.setSelectedId(startPage.menuId.toLong())
        stack.replace(startPage.pageClass, startPage.tag)
      }

      if (isSearchAction(intent)) {
        onSearchClicked()
      }
    }

    if (!TraktLinkSettings.isLinkPrompted(this)) {
      displayLinkPrompt()
    } else if (isLoginAction(getIntent())) {
      startLoginActivity()
    } else {
      if (isReplaceStackAction(intent)) {
        val stackEntries = getIntent().getParcelableArrayListExtra<StackEntry>(EXTRA_STACK_ENTRIES)
        replaceStack(stackEntries)
      }

      if (!Settings.get(this).getBoolean(Settings.SETUP_PROMPTED, false)) {
        displaySetupPrompt()
      }
    }

    intent.action = ACTION_CONSUMED

    SyncEvent.registerListener(onSyncEvent)
    RequestFailedEvent.registerListener(requestFailedListener)
    ErrorEvent.registerListener(checkInFailedListener)
    AuthFailedEvent.registerListener(onAuthFailedListener)

    viewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java)
    viewModel.watchingShow.observe(this, Observer { showWithEpisode ->
      watchingShow = showWithEpisode
      updateWatching()
    })
    viewModel.watchingMovie.observe(this, Observer { movie ->
      watchingMovie = movie
      updateWatching()
    })
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    when {
      isLoginAction(intent) -> MainHandler.post { startLoginActivity() }
      isShowStartPageIntent(intent) -> {
        val startPage = intent.getSerializableExtra(EXTRA_START_PAGE) as StartPage
        MainHandler.post { showStartPage(startPage) }
      }
      isReplaceStackAction(intent) -> {
        val stackEntries = intent.getParcelableArrayListExtra<StackEntry>(EXTRA_STACK_ENTRIES)
        MainHandler.post { replaceStack(stackEntries) }
      }
    }

    intent.action = ACTION_CONSUMED
  }

  private fun isShowStartPageIntent(intent: Intent): Boolean {
    return ACTION_SHOW_START_PAGE == intent.action
  }

  private fun showStartPage(startPage: StartPage) {
    navigation.setSelectedId(startPage.menuId.toLong())
    onMenuItemClicked(startPage.menuId)

    pendingReplacement?.apply {
      stack.replace(fragment, tag)
      pendingReplacement = null
    }
  }

  private fun replaceStack(stackEntries: MutableList<StackEntry>) {
    val f = stack.peekFirst()
    val entry = StackEntry(f.javaClass, f.tag, f.arguments)
    stackEntries.add(0, entry)
    stack.replaceStack(stackEntries)
  }

  private fun isReplaceStackAction(intent: Intent): Boolean {
    return ACTION_REPLACE_STACK == intent.action
  }

  private fun isLoginAction(intent: Intent): Boolean {
    return ACTION_LOGIN == intent.action
  }

  private fun isSearchAction(intent: Intent): Boolean {
    return ACTION_SEARCH == intent.action
  }

  private fun isShowUpcomingAction(intent: Intent): Boolean {
    return ACTION_UPCOMING == intent.action
  }

  private fun displayLinkPrompt() {
    if (supportFragmentManager.findFragmentByTag(PROMPT_LINK) == null) {
      val linkPrompt = LinkPromptBottomSheet()
      linkPrompt.show(supportFragmentManager, PROMPT_LINK)
    }
  }

  override fun onDismissLinkPrompt() {
    if (!Settings.get(this).getBoolean(Settings.SETUP_PROMPTED, false)) {
      displaySetupPrompt()
    }
  }

  private fun displaySetupPrompt() {
    if (supportFragmentManager.findFragmentByTag(PROMPT_SETUP) == null) {
      val setupPrompt = SetupPromptBottomSheet()
      setupPrompt.show(supportFragmentManager, PROMPT_SETUP)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    outState.putBundle(STATE_STACK, stack.saveState())
    super.onSaveInstanceState(outState)
  }

  override fun onResume() {
    super.onResume()
    if (TraktLinkSettings.hasAuthFailed(this)) {
      authFailedView.visibility = View.VISIBLE
    } else {
      authFailedView.visibility = View.GONE
    }
  }

  override fun onDestroy() {
    Timber.d("onDestroy")
    SyncEvent.unregisterListener(onSyncEvent)
    RequestFailedEvent.unregisterListener(requestFailedListener)
    ErrorEvent.unregisterListener(checkInFailedListener)
    super.onDestroy()
  }

  override fun onBackPressed() {
    if (watchingView.isExpanded) {
      watchingView.collapse()
      return
    }

    if (drawer.isDrawerVisible(Gravity.LEFT)) {
      drawer.closeDrawer(Gravity.LEFT)
      return
    }

    val topFragment = stack.peek() as FragmentContract?
    if (topFragment?.onBackPressed() == true) {
      return
    }

    if (stack.pop()) {
      return
    }

    super.onBackPressed()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      android.R.id.home -> {
        onHomeClicked()
        return true
      }
    }

    return super.onOptionsItemSelected(item)
  }

  override fun onMenuItemClicked(id: Int): Boolean {
    when (id) {
      R.id.menu_dashboard -> pendingReplacement =
        PendingReplacement(DashboardFragment::class.java, DashboardFragment.TAG)

      R.id.menu_shows_upcoming -> pendingReplacement =
        PendingReplacement(UpcomingShowsFragment::class.java, UpcomingShowsFragment.TAG)

      R.id.menu_shows_watched -> pendingReplacement =
        PendingReplacement(WatchedShowsFragment::class.java, WatchedShowsFragment.TAG)

      R.id.menu_shows_collection -> pendingReplacement =
        PendingReplacement(CollectedShowsFragment::class.java, CollectedShowsFragment.TAG)

      R.id.menu_shows_watchlist -> pendingReplacement =
        PendingReplacement(ShowsWatchlistFragment::class.java, ShowsWatchlistFragment.TAG)

      R.id.menu_shows_suggestions -> pendingReplacement =
        PendingReplacement(ShowSuggestionsFragment::class.java, ShowSuggestionsFragment.TAG)

      R.id.menu_movies_watched -> pendingReplacement =
        PendingReplacement(WatchedMoviesFragment::class.java, WatchedMoviesFragment.TAG)

      R.id.menu_movies_collection -> pendingReplacement =
        PendingReplacement(CollectedMoviesFragment::class.java, CollectedMoviesFragment.TAG)

      R.id.menu_movies_watchlist -> pendingReplacement =
        PendingReplacement(MovieWatchlistFragment::class.java, MovieWatchlistFragment.TAG)

      R.id.menu_movies_suggestions -> pendingReplacement =
        PendingReplacement(MovieSuggestionsFragment::class.java, MovieSuggestionsFragment.TAG)

      R.id.menu_lists -> pendingReplacement =
        PendingReplacement(ListsFragment::class.java, ListsFragment.TAG)

      R.id.menu_stats -> pendingReplacement =
        PendingReplacement(StatsFragment::class.java, StatsFragment.TAG)

      R.id.menu_settings -> {
        val settings = Intent(this, SettingsActivity::class.java)
        startActivity(settings)
        return false
      }

      else -> throw IllegalArgumentException("Unknown id $id")
    }

    drawer.closeDrawer(Gravity.LEFT)
    return true
  }

  private fun startLoginActivity() {
    val login = Intent(this, LoginActivity::class.java)
    login.putExtra(LoginActivity.EXTRA_TASK, LoginActivity.TASK_TOKEN_REFRESH)
    startActivity(login)
    finish()
  }

  ///////////////////////////////////////////////////////////////////////////
  // Navigation callbacks
  ///////////////////////////////////////////////////////////////////////////

  override fun onHomeClicked() {
    if (stack.size() == 1) {
      drawer.openDrawer(Gravity.LEFT)
      return
    }

    stack.pop()
  }

  override fun onSearchClicked() {
    stack.push(SearchFragment::class.java, SearchFragment.TAG)
  }

  override fun onDisplayShow(showId: Long, title: String?, overview: String?, type: LibraryType) {
    stack.push(
      ShowFragment::class.java,
      ShowFragment.getTag(showId),
      ShowFragment.getArgs(showId, title, overview, type)
    )
  }

  override fun onDisplayEpisode(episodeId: Long, showTitle: String?) {
    stack.push(
      EpisodeFragment::class.java,
      EpisodeFragment.getTag(episodeId),
      EpisodeFragment.getArgs(episodeId, showTitle)
    )
  }

  override fun onDisplayEpisodeHistory(episodeId: Long, showTitle: String) {
    stack.push(
      EpisodeHistoryFragment::class.java,
      EpisodeHistoryFragment.getTag(episodeId),
      EpisodeHistoryFragment.getArgs(episodeId, showTitle)
    )
  }

  override fun onDisplaySeason(
    showId: Long,
    seasonId: Long,
    showTitle: String?,
    seasonNumber: Int,
    type: LibraryType
  ) {
    stack.push(
      SeasonFragment::class.java,
      SeasonFragment.TAG,
      SeasonFragment.getArgs(showId, seasonId, showTitle, seasonNumber, type)
    )
  }

  override fun onDisplayRelatedShows(showId: Long, title: String?) {
    stack.push(
      RelatedShowsFragment::class.java,
      RelatedShowsFragment.getTag(showId),
      RelatedShowsFragment.getArgs(showId)
    )
  }

  override fun onSelectShowWatchedDate(showId: Long, title: String?) {
    stack.push(
      SelectHistoryDateFragment::class.java,
      SelectHistoryDateFragment.TAG,
      SelectHistoryDateFragment.getArgs(SelectHistoryDateFragment.Type.SHOW, showId, title)
    )
  }

  override fun onSelectSeasonWatchedDate(seasonId: Long, title: String?) {
    stack.push(
      SelectHistoryDateFragment::class.java,
      SelectHistoryDateFragment.TAG,
      SelectHistoryDateFragment.getArgs(SelectHistoryDateFragment.Type.SEASON, seasonId, title)
    )
  }

  override fun onSelectEpisodeWatchedDate(episodeId: Long, title: String?) {
    stack.push(
      SelectHistoryDateFragment::class.java,
      SelectHistoryDateFragment.TAG,
      SelectHistoryDateFragment.getArgs(SelectHistoryDateFragment.Type.EPISODE, episodeId, title)
    )
  }

  override fun onSelectOlderEpisodeWatchedDate(episodeId: Long, title: String?) {
    stack.push(
      SelectHistoryDateFragment::class.java,
      SelectHistoryDateFragment.TAG,
      SelectHistoryDateFragment.getArgs(
        SelectHistoryDateFragment.Type.EPISODE_OLDER,
        episodeId,
        title
      )
    )
  }

  override fun onDisplayMovie(movieId: Long, title: String?, overview: String?) {
    stack.push(
      MovieFragment::class.java,
      MovieFragment.getTag(movieId),
      MovieFragment.getArgs(movieId, title, overview)
    )
  }

  override fun onDisplayRelatedMovies(movieId: Long, title: String?) {
    stack.push(
      RelatedMoviesFragment::class.java,
      RelatedMoviesFragment.getTag(movieId),
      RelatedMoviesFragment.getArgs(movieId)
    )
  }

  override fun onSelectMovieWatchedDate(movieId: Long, title: String?) {
    stack.push(
      SelectHistoryDateFragment::class.java,
      SelectHistoryDateFragment.TAG,
      SelectHistoryDateFragment.getArgs(SelectHistoryDateFragment.Type.MOVIE, movieId, title)
    )
  }

  override fun onDisplayMovieHistory(movieId: Long, title: String?) {
    stack.push(
      MovieHistoryFragment::class.java,
      MovieHistoryFragment.getTag(movieId),
      MovieHistoryFragment.getArgs(movieId, title)
    )
  }

  override fun onShowList(listId: Long, listName: String) {
    stack.push(ListFragment::class.java, ListFragment.TAG, ListFragment.getArgs(listId, listName))
  }

  override fun onListDeleted(listId: Long) {
    val top = stack.peek()
    if (top is ListFragment) {
      if (listId == top.listId) {
        stack.pop()
      }
    }
  }

  override fun onDisplayComments(type: ItemType, itemId: Long) {
    stack.push(
      CommentsFragment::class.java,
      CommentsFragment.TAG,
      CommentsFragment.getArgs(type, itemId)
    )
  }

  override fun onDisplayComment(commentId: Long) {
    stack.push(CommentFragment::class.java, CommentFragment.TAG, CommentFragment.getArgs(commentId))
  }

  override fun onDisplayPerson(personId: Long) {
    stack.push(
      PersonFragment::class.java,
      PersonFragment.getTag(personId),
      PersonFragment.getArgs(personId)
    )
  }

  override fun onDisplayPersonCredit(personId: Long, department: Department) {
    stack.push(
      PersonCreditsFragment::class.java,
      PersonCreditsFragment.getTag(personId),
      PersonCreditsFragment.getArgs(personId, department)
    )
  }

  override fun onDisplayCredit(itemType: ItemType, itemId: Long, department: Department) {
    stack.push(
      CreditFragment::class.java,
      CreditFragment.getTag(itemId),
      CreditFragment.getArgs(itemType, itemId, department)
    )
  }

  override fun onDisplayCredits(itemType: ItemType, itemId: Long, title: String?) {
    stack.push(
      CreditsFragment::class.java,
      CreditsFragment.getTag(itemId),
      CreditsFragment.getArgs(itemType, itemId, title)
    )
  }

  override fun displayFragment(clazz: Class<*>, tag: String) {
    stack.push(clazz, tag, null)
  }

  override fun upFromEpisode(showId: Long, showTitle: String?, seasonId: Long) {
    if (stack.removeTop()) {
      val f = stack.peek()
      if (f is ShowFragment && f.showId == showId) {
        stack.attachTop()
      } else if (seasonId >= 0 && f is SeasonFragment && f.seasonId == seasonId) {
        stack.attachTop()
      } else {
        stack.putFragment(
          ShowFragment::class.java,
          ShowFragment.getTag(showId),
          ShowFragment.getArgs(showId, showTitle, null, LibraryType.WATCHED)
        )
      }
    }
  }

  override fun popIfTop(fragment: Fragment) {
    if (fragment === stack.peek()) {
      stack.pop()
    }
  }

  override fun isFragmentTopLevel(fragment: Fragment): Boolean {
    return stack.positionInStack(fragment) == 0
  }

  ///////////////////////////////////////////////////////////////////////////
  // Watching view
  ///////////////////////////////////////////////////////////////////////////

  private fun updateWatching() {
    if (watchingShow != null) {
      watchingShow?.let {
        val showId = it.show.id
        val showTitle = it.show.title
        val season = it.episode.season
        val episode = it.episode.episode

        val episodeId = it.episode.id
        val episodeTitle =
          DataHelper.getEpisodeTitle(this, it.episode.title, season, episode, false)
        val startTime = it.episode.checkinStartedAt
        val endTime = it.episode.checkinExpiresAt

        val poster = ImageUri.create(ImageUri.ITEM_SHOW, ImageType.POSTER, showId)

        watchingView.watchingShow(
          showId,
          showTitle,
          episodeId,
          episodeTitle,
          poster,
          startTime,
          endTime
        )
      }
    } else if (watchingMovie != null) {
      watchingMovie?.let {
        val id = it.id
        val title = it.title
        val overview = it.overview
        val startTime = it.checkinStartedAt
        val endTime = it.checkinExpiresAt

        val poster = ImageUri.create(ImageUri.ITEM_MOVIE, ImageType.POSTER, id)

        watchingView.watchingMovie(id, title, overview, poster, startTime, endTime)
      }
    } else {
      watchingView.clearWatching()
    }
  }

  companion object {

    private const val PROMPT_LINK = "net.simonvt.cathode.ui.HomeActivity.linkPrompt"
    private const val PROMPT_SETUP = "net.simonvt.cathode.ui.HomeActivity.setupPrompt"

    private const val STATE_STACK = "net.simonvt.cathode.ui.HomeActivity.stack"

    const val EXTRA_START_PAGE = "net.simonvt.cathode.ui.HomeActivity.startPage"
    const val EXTRA_STACK_ENTRIES = "net.simonvt.cathode.ui.HomeActivity.stackEntries"

    const val ACTION_CONSUMED = "consumed"
    const val ACTION_LOGIN = "net.simonvt.cathode.intent.action.LOGIN"
    const val ACTION_SHOW_START_PAGE = "net.simonvt.cathode.intent.action.showStartPage"
    const val ACTION_REPLACE_STACK = "replaceStack"
    const val ACTION_SEARCH = "net.simonvt.cathode.SEARCH"
    const val ACTION_UPCOMING = "net.simonvt.cathode.UPCOMING"
  }
}
