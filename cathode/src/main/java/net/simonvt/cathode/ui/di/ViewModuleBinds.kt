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

import androidx.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import net.simonvt.cathode.settings.hidden.HiddenViewModel
import net.simonvt.cathode.settings.link.TraktLinkSyncViewModel
import net.simonvt.cathode.ui.comments.CommentViewModel
import net.simonvt.cathode.ui.credits.CreditsViewModel
import net.simonvt.cathode.ui.dashboard.DashboardViewModel
import net.simonvt.cathode.ui.lists.ListViewModel
import net.simonvt.cathode.ui.lists.ListsViewModel
import net.simonvt.cathode.ui.movie.MovieViewModel
import net.simonvt.cathode.ui.movie.RelatedMoviesViewModel
import net.simonvt.cathode.ui.movies.collected.CollectedMoviesViewModel
import net.simonvt.cathode.ui.movies.watched.WatchedMoviesViewModel
import net.simonvt.cathode.ui.movies.watchlist.MovieWatchlistViewModel
import net.simonvt.cathode.ui.person.PersonViewModel
import net.simonvt.cathode.ui.search.SearchViewModel
import net.simonvt.cathode.ui.show.EpisodeViewModel
import net.simonvt.cathode.ui.show.RelatedShowsViewModel
import net.simonvt.cathode.ui.show.SeasonViewModel
import net.simonvt.cathode.ui.show.ShowViewModel
import net.simonvt.cathode.ui.shows.collected.CollectedShowsViewModel
import net.simonvt.cathode.ui.shows.upcoming.UpcomingViewModel
import net.simonvt.cathode.ui.shows.watched.WatchedShowsViewModel
import net.simonvt.cathode.ui.shows.watchlist.ShowsWatchlistViewModel
import net.simonvt.cathode.ui.suggestions.movies.AnticipatedMoviesViewModel
import net.simonvt.cathode.ui.suggestions.movies.MovieRecommendationsViewModel
import net.simonvt.cathode.ui.suggestions.movies.TrendingMoviesViewModel
import net.simonvt.cathode.ui.suggestions.shows.AnticipatedShowsViewModel
import net.simonvt.cathode.ui.suggestions.shows.ShowRecommendationsViewModel
import net.simonvt.cathode.ui.suggestions.shows.TrendingShowsViewModel

@Module
abstract class ViewModuleBinds {

  @Binds
  @IntoMap
  @ViewModelKey(ShowViewModel::class)
  abstract fun showViewModel(viewModel: ShowViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(AnticipatedMoviesViewModel::class)
  abstract fun anticipatedMoviesViewModel(viewModel: AnticipatedMoviesViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(AnticipatedShowsViewModel::class)
  abstract fun anticipatedShowsViewModel(viewModel: AnticipatedShowsViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(CollectedMoviesViewModel::class)
  abstract fun collectedMoviesViewModel(viewModel: CollectedMoviesViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(CollectedShowsViewModel::class)
  abstract fun collectedShowsViewModel(viewModel: CollectedShowsViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(CommentViewModel::class)
  abstract fun commentViewModel(viewModel: CommentViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(CreditsViewModel::class)
  abstract fun creditsViewModel(viewModel: CreditsViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(DashboardViewModel::class)
  abstract fun dashboardViewModel(viewModel: DashboardViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(EpisodeViewModel::class)
  abstract fun episodeViewModel(viewModel: EpisodeViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(HiddenViewModel::class)
  abstract fun hiddenViewModel(viewModel: HiddenViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(ListsViewModel::class)
  abstract fun listsViewModel(viewModel: ListsViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(ListViewModel::class)
  abstract fun listViewModel(viewModel: ListViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(MovieRecommendationsViewModel::class)
  abstract fun movieRecommendationsViewModel(viewModel: MovieRecommendationsViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(MovieViewModel::class)
  abstract fun movieViewModel(viewModel: MovieViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(MovieWatchlistViewModel::class)
  abstract fun movieWatchlistViewModel(viewModel: MovieWatchlistViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(PersonViewModel::class)
  abstract fun personViewModel(viewModel: PersonViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(RelatedMoviesViewModel::class)
  abstract fun relatedMoviesViewModel(viewModel: RelatedMoviesViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(RelatedShowsViewModel::class)
  abstract fun relatedShowsViewModel(viewModel: RelatedShowsViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(SearchViewModel::class)
  abstract fun searchViewModel(viewModel: SearchViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(SeasonViewModel::class)
  abstract fun seasonViewModel(viewModel: SeasonViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(ShowRecommendationsViewModel::class)
  abstract fun showRecommendationsViewModel(viewModel: ShowRecommendationsViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(ShowsWatchlistViewModel::class)
  abstract fun showsWatchlistViewModel(viewModel: ShowsWatchlistViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(TrendingMoviesViewModel::class)
  abstract fun trendingMoviesViewModel(viewModel: TrendingMoviesViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(TrendingShowsViewModel::class)
  abstract fun trendingShowsViewModel(viewModel: TrendingShowsViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(UpcomingViewModel::class)
  abstract fun upcomingViewModel(viewModel: UpcomingViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(WatchedMoviesViewModel::class)
  abstract fun watchedMoviesViewModel(viewModel: WatchedMoviesViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(WatchedShowsViewModel::class)
  abstract fun watchedShowsViewModel(viewModel: WatchedShowsViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(TraktLinkSyncViewModel::class)
  abstract fun traktLinkSyncViewModel(viewModel: TraktLinkSyncViewModel): ViewModel
}
