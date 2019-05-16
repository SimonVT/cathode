/*
 * Copyright (C) 2019 Simon Vig Therkildsen
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

package net.simonvt.cathode.ui.di

import androidx.fragment.app.Fragment
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import net.simonvt.cathode.settings.LogoutDialog
import net.simonvt.cathode.settings.SettingsFragment
import net.simonvt.cathode.settings.hidden.HiddenItemsFragment
import net.simonvt.cathode.ui.comments.AddCommentDialog
import net.simonvt.cathode.ui.comments.CommentFragment
import net.simonvt.cathode.ui.comments.CommentsFragment
import net.simonvt.cathode.ui.comments.UpdateCommentDialog
import net.simonvt.cathode.ui.credits.CreditsFragment
import net.simonvt.cathode.ui.dashboard.DashboardFragment
import net.simonvt.cathode.ui.dialog.CheckInDialog
import net.simonvt.cathode.ui.dialog.RatingDialog
import net.simonvt.cathode.ui.history.AddToHistoryDialog
import net.simonvt.cathode.ui.history.RemoveFromHistoryDialog
import net.simonvt.cathode.ui.history.SelectHistoryDateFragment
import net.simonvt.cathode.ui.lists.CreateListFragment
import net.simonvt.cathode.ui.lists.DeleteListDialog
import net.simonvt.cathode.ui.lists.ListFragment
import net.simonvt.cathode.ui.lists.ListsDialog
import net.simonvt.cathode.ui.lists.ListsFragment
import net.simonvt.cathode.ui.lists.UpdateListFragment
import net.simonvt.cathode.ui.movie.MovieFragment
import net.simonvt.cathode.ui.movie.MovieHistoryFragment
import net.simonvt.cathode.ui.movie.RelatedMoviesFragment
import net.simonvt.cathode.ui.movies.collected.CollectedMoviesFragment
import net.simonvt.cathode.ui.movies.watched.WatchedMoviesFragment
import net.simonvt.cathode.ui.movies.watchlist.MovieWatchlistFragment
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
import net.simonvt.cathode.ui.suggestions.movies.AnticipatedMoviesFragment
import net.simonvt.cathode.ui.suggestions.movies.MovieRecommendationsFragment
import net.simonvt.cathode.ui.suggestions.movies.TrendingMoviesFragment
import net.simonvt.cathode.ui.suggestions.shows.AnticipatedShowsFragment
import net.simonvt.cathode.ui.suggestions.shows.ShowRecommendationsFragment
import net.simonvt.cathode.ui.suggestions.shows.TrendingShowsFragment

@Module
abstract class FragmentModuleBinds {

  @Binds
  @IntoMap
  @FragmentKey(UpcomingShowsFragment::class)
  abstract fun upcomingShowsFragment(fragment: UpcomingShowsFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(DashboardFragment::class)
  abstract fun dashboardFragment(fragment: DashboardFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(AddCommentDialog::class)
  abstract fun addCommentDialog(fragment: AddCommentDialog): Fragment

  @Binds
  @IntoMap
  @FragmentKey(AddToHistoryDialog::class)
  abstract fun addToHistoryDialog(fragment: AddToHistoryDialog): Fragment

  @Binds
  @IntoMap
  @FragmentKey(CheckInDialog::class)
  abstract fun checkInDialog(fragment: CheckInDialog): Fragment

  @Binds
  @IntoMap
  @FragmentKey(CommentsFragment::class)
  abstract fun commentsFragment(fragment: CommentsFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(DeleteListDialog::class)
  abstract fun deleteListDialog(fragment: DeleteListDialog): Fragment

  @Binds
  @IntoMap
  @FragmentKey(EpisodeHistoryFragment::class)
  abstract fun episodeHistoryFragment(fragment: EpisodeHistoryFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(ListsDialog::class)
  abstract fun legalistsDialog(fragment: ListsDialog): Fragment

  @Binds
  @IntoMap
  @FragmentKey(LogoutDialog::class)
  abstract fun logoutDialog(fragment: LogoutDialog): Fragment

  @Binds
  @IntoMap
  @FragmentKey(MovieHistoryFragment::class)
  abstract fun movieHistoryFragment(fragment: MovieHistoryFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(AnticipatedMoviesFragment::class)
  abstract fun anticipatedMoviesFragment(fragment: AnticipatedMoviesFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(CollectedMoviesFragment::class)
  abstract fun collectedMoviesFragment(fragment: CollectedMoviesFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(MovieWatchlistFragment::class)
  abstract fun movieWatchlistFragment(fragment: MovieWatchlistFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(TrendingMoviesFragment::class)
  abstract fun trendingMoviesFragment(fragment: TrendingMoviesFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(WatchedMoviesFragment::class)
  abstract fun watchedMoviesFragment(fragment: WatchedMoviesFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(PersonCreditsFragment::class)
  abstract fun personCreditsFragment(fragment: PersonCreditsFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(RatingDialog::class)
  abstract fun ratingDialog(fragment: RatingDialog): Fragment

  @Binds
  @IntoMap
  @FragmentKey(RemoveFromHistoryDialog::class)
  abstract fun removeFromHistoryDialog(fragment: RemoveFromHistoryDialog): Fragment

  @Binds
  @IntoMap
  @FragmentKey(SelectHistoryDateFragment::class)
  abstract fun selectHistoryDateFragment(fragment: SelectHistoryDateFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(SettingsFragment::class)
  abstract fun settingsFragment(fragment: SettingsFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(CollectedShowsFragment::class)
  abstract fun collectedShowsFragment(fragment: CollectedShowsFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(WatchedShowsFragment::class)
  abstract fun watchedShowsFragment(fragment: WatchedShowsFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(UpdateCommentDialog::class)
  abstract fun updateCommentDialog(fragment: UpdateCommentDialog): Fragment

  @Binds
  @IntoMap
  @FragmentKey(AnticipatedShowsFragment::class)
  abstract fun anticipatedShowsFragment(fragment: AnticipatedShowsFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(CommentFragment::class)
  abstract fun commentFragment(fragment: CommentFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(CreateListFragment::class)
  abstract fun createListFragment(fragment: CreateListFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(CreditsFragment::class)
  abstract fun creditsFragment(fragment: CreditsFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(EpisodeFragment::class)
  abstract fun episodeFragment(fragment: EpisodeFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(HiddenItemsFragment::class)
  abstract fun hiddenItemsFragment(fragment: HiddenItemsFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(ListFragment::class)
  abstract fun listFragment(fragment: ListFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(ListsFragment::class)
  abstract fun listsFragment(fragment: ListsFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(MovieFragment::class)
  abstract fun movieFragment(fragment: MovieFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(MovieRecommendationsFragment::class)
  abstract fun movieRecommendationsFragment(fragment: MovieRecommendationsFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(PersonFragment::class)
  abstract fun personFragment(fragment: PersonFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(RelatedMoviesFragment::class)
  abstract fun relatedMoviesFragment(fragment: RelatedMoviesFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(RelatedShowsFragment::class)
  abstract fun relatedShowsFragment(fragment: RelatedShowsFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(SearchFragment::class)
  abstract fun searchFragment(fragment: SearchFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(SeasonFragment::class)
  abstract fun seasonFragment(fragment: SeasonFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(ShowFragment::class)
  abstract fun showFragment(fragment: ShowFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(ShowRecommendationsFragment::class)
  abstract fun showRecommendationsFragment(fragment: ShowRecommendationsFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(ShowsWatchlistFragment::class)
  abstract fun showsWatchlistFragment(fragment: ShowsWatchlistFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(TrendingShowsFragment::class)
  abstract fun trendingShowsFragment(fragment: TrendingShowsFragment): Fragment

  @Binds
  @IntoMap
  @FragmentKey(UpdateListFragment::class)
  abstract fun updateListFragment(fragment: UpdateListFragment): Fragment
}
