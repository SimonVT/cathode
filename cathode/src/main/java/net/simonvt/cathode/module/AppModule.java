/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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
package net.simonvt.cathode.module;

import android.content.Context;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.DatabaseHelperModule;
import net.simonvt.cathode.JobsModule;
import net.simonvt.cathode.api.ApiModule;
import net.simonvt.cathode.api.ApiSettings;
import net.simonvt.cathode.api.TraktModule;
import net.simonvt.cathode.appwidget.UpcomingWidgetService.UpcomingRemoteViewsFactory;
import net.simonvt.cathode.images.EpisodeRequestHandler;
import net.simonvt.cathode.images.ImageModule;
import net.simonvt.cathode.images.ImageRequestHandler;
import net.simonvt.cathode.images.MovieRequestHandler;
import net.simonvt.cathode.images.PersonRequestHandler;
import net.simonvt.cathode.images.SeasonRequestHandler;
import net.simonvt.cathode.images.ShowRequestHandler;
import net.simonvt.cathode.jobqueue.AuthJobHandler;
import net.simonvt.cathode.jobqueue.AuthJobService;
import net.simonvt.cathode.jobqueue.DataJobHandler;
import net.simonvt.cathode.jobqueue.JobModule;
import net.simonvt.cathode.jobscheduler.AuthJobHandlerJob;
import net.simonvt.cathode.jobscheduler.DataJobHandlerJob;
import net.simonvt.cathode.jobscheduler.JobCreator;
import net.simonvt.cathode.notification.NotificationActionService;
import net.simonvt.cathode.provider.EpisodeDatabaseHelper;
import net.simonvt.cathode.provider.MovieDatabaseHelper;
import net.simonvt.cathode.provider.SearchDatabaseHelper;
import net.simonvt.cathode.provider.SeasonDatabaseHelper;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import net.simonvt.cathode.provider.UserDatabaseHelper;
import net.simonvt.cathode.remote.ForceUpdateJob;
import net.simonvt.cathode.remote.LogoutJob;
import net.simonvt.cathode.remote.UpdateShowCounts;
import net.simonvt.cathode.remote.action.RemoveHistoryItem;
import net.simonvt.cathode.remote.action.comments.AddCommentJob;
import net.simonvt.cathode.remote.action.comments.CommentReplyJob;
import net.simonvt.cathode.remote.action.comments.DeleteCommentJob;
import net.simonvt.cathode.remote.action.comments.LikeCommentJob;
import net.simonvt.cathode.remote.action.comments.UnlikeCommentJob;
import net.simonvt.cathode.remote.action.comments.UpdateCommentJob;
import net.simonvt.cathode.remote.action.lists.AddEpisode;
import net.simonvt.cathode.remote.action.lists.AddMovie;
import net.simonvt.cathode.remote.action.lists.AddPerson;
import net.simonvt.cathode.remote.action.lists.AddSeason;
import net.simonvt.cathode.remote.action.lists.AddShow;
import net.simonvt.cathode.remote.action.lists.RemoveEpisode;
import net.simonvt.cathode.remote.action.lists.RemoveMovie;
import net.simonvt.cathode.remote.action.lists.RemovePerson;
import net.simonvt.cathode.remote.action.lists.RemoveSeason;
import net.simonvt.cathode.remote.action.lists.RemoveShow;
import net.simonvt.cathode.remote.action.movies.AddMovieToHistory;
import net.simonvt.cathode.remote.action.movies.CalendarHideMovie;
import net.simonvt.cathode.remote.action.movies.CollectMovie;
import net.simonvt.cathode.remote.action.movies.DismissMovieRecommendation;
import net.simonvt.cathode.remote.action.movies.RateMovie;
import net.simonvt.cathode.remote.action.movies.RemoveMovieFromHistory;
import net.simonvt.cathode.remote.action.movies.WatchlistMovie;
import net.simonvt.cathode.remote.action.shows.AddEpisodeToHistory;
import net.simonvt.cathode.remote.action.shows.AddSeasonToHistory;
import net.simonvt.cathode.remote.action.shows.AddShowToHistory;
import net.simonvt.cathode.remote.action.shows.CalendarHideShow;
import net.simonvt.cathode.remote.action.shows.CollectEpisode;
import net.simonvt.cathode.remote.action.shows.CollectSeason;
import net.simonvt.cathode.remote.action.shows.CollectedHideShow;
import net.simonvt.cathode.remote.action.shows.DismissShowRecommendation;
import net.simonvt.cathode.remote.action.shows.RateEpisode;
import net.simonvt.cathode.remote.action.shows.RateShow;
import net.simonvt.cathode.remote.action.shows.RemoveEpisodeFromHistory;
import net.simonvt.cathode.remote.action.shows.RemoveSeasonFromHistory;
import net.simonvt.cathode.remote.action.shows.WatchedHideShow;
import net.simonvt.cathode.remote.action.shows.WatchlistEpisode;
import net.simonvt.cathode.remote.action.shows.WatchlistShow;
import net.simonvt.cathode.remote.sync.SyncHiddenCalendar;
import net.simonvt.cathode.remote.sync.SyncHiddenCollected;
import net.simonvt.cathode.remote.sync.SyncHiddenItems;
import net.simonvt.cathode.remote.sync.SyncHiddenRecommendations;
import net.simonvt.cathode.remote.sync.SyncHiddenWatched;
import net.simonvt.cathode.remote.sync.SyncJob;
import net.simonvt.cathode.remote.sync.SyncPerson;
import net.simonvt.cathode.remote.sync.SyncUserActivity;
import net.simonvt.cathode.remote.sync.SyncUserProfile;
import net.simonvt.cathode.remote.sync.SyncUserSettings;
import net.simonvt.cathode.remote.sync.SyncWatching;
import net.simonvt.cathode.remote.sync.comments.SyncCommentLikes;
import net.simonvt.cathode.remote.sync.comments.SyncComments;
import net.simonvt.cathode.remote.sync.comments.SyncUserComments;
import net.simonvt.cathode.remote.sync.lists.SyncList;
import net.simonvt.cathode.remote.sync.lists.SyncLists;
import net.simonvt.cathode.remote.sync.movies.SyncAnticipatedMovies;
import net.simonvt.cathode.remote.sync.movies.SyncMovie;
import net.simonvt.cathode.remote.sync.movies.SyncMovieCredits;
import net.simonvt.cathode.remote.sync.movies.SyncMovieRecommendations;
import net.simonvt.cathode.remote.sync.movies.SyncMoviesCollection;
import net.simonvt.cathode.remote.sync.movies.SyncMoviesRatings;
import net.simonvt.cathode.remote.sync.movies.SyncMoviesWatchlist;
import net.simonvt.cathode.remote.sync.movies.SyncPendingMovies;
import net.simonvt.cathode.remote.sync.movies.SyncRelatedMovies;
import net.simonvt.cathode.remote.sync.movies.SyncTrendingMovies;
import net.simonvt.cathode.remote.sync.movies.SyncUpdatedMovies;
import net.simonvt.cathode.remote.sync.movies.SyncWatchedMovies;
import net.simonvt.cathode.remote.sync.people.SyncPersonMovieCredits;
import net.simonvt.cathode.remote.sync.people.SyncPersonShowCredits;
import net.simonvt.cathode.remote.sync.shows.SyncAnticipatedShows;
import net.simonvt.cathode.remote.sync.shows.SyncEpisodeWatchlist;
import net.simonvt.cathode.remote.sync.shows.SyncEpisodesRatings;
import net.simonvt.cathode.remote.sync.shows.SyncPendingSeasons;
import net.simonvt.cathode.remote.sync.shows.SyncPendingShows;
import net.simonvt.cathode.remote.sync.shows.SyncRelatedShows;
import net.simonvt.cathode.remote.sync.shows.SyncSeason;
import net.simonvt.cathode.remote.sync.shows.SyncSeasons;
import net.simonvt.cathode.remote.sync.shows.SyncSeasonsRatings;
import net.simonvt.cathode.remote.sync.shows.SyncShow;
import net.simonvt.cathode.remote.sync.shows.SyncShowCollectedStatus;
import net.simonvt.cathode.remote.sync.shows.SyncShowCredits;
import net.simonvt.cathode.remote.sync.shows.SyncShowRecommendations;
import net.simonvt.cathode.remote.sync.shows.SyncShowWatchedStatus;
import net.simonvt.cathode.remote.sync.shows.SyncShowsCollection;
import net.simonvt.cathode.remote.sync.shows.SyncShowsRatings;
import net.simonvt.cathode.remote.sync.shows.SyncShowsWatchlist;
import net.simonvt.cathode.remote.sync.shows.SyncTrendingShows;
import net.simonvt.cathode.remote.sync.shows.SyncUpdatedShows;
import net.simonvt.cathode.remote.sync.shows.SyncWatchedShows;
import net.simonvt.cathode.remote.upgrade.EnsureSync;
import net.simonvt.cathode.remote.upgrade.UpperCaseGenres;
import net.simonvt.cathode.scheduler.CommentsTaskScheduler;
import net.simonvt.cathode.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.scheduler.ListsTaskScheduler;
import net.simonvt.cathode.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.scheduler.PersonTaskScheduler;
import net.simonvt.cathode.scheduler.SchedulerModule;
import net.simonvt.cathode.scheduler.SearchTaskScheduler;
import net.simonvt.cathode.scheduler.SeasonTaskScheduler;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.search.SearchHandler;
import net.simonvt.cathode.service.CathodeSyncAdapter;
import net.simonvt.cathode.service.SyncWatchingReceiver;
import net.simonvt.cathode.settings.LogoutDialog;
import net.simonvt.cathode.settings.NotificationSettingsActivity;
import net.simonvt.cathode.settings.SettingsActivity;
import net.simonvt.cathode.settings.UpcomingTimePreference;
import net.simonvt.cathode.settings.hidden.HiddenItems;
import net.simonvt.cathode.settings.hidden.HiddenItemsAdapter;
import net.simonvt.cathode.settings.login.LoginActivity;
import net.simonvt.cathode.settings.login.TokenActivity;
import net.simonvt.cathode.settings.login.TokenTask;
import net.simonvt.cathode.settings.setup.CalendarSetupActivity;
import net.simonvt.cathode.settings.setup.NotificationSetupActivity;
import net.simonvt.cathode.tmdb.TmdbModule;
import net.simonvt.cathode.tmdb.api.SyncConfiguration;
import net.simonvt.cathode.tmdb.api.movie.SyncMovieImages;
import net.simonvt.cathode.tmdb.api.people.SyncPersonBackdrop;
import net.simonvt.cathode.tmdb.api.people.SyncPersonHeadshot;
import net.simonvt.cathode.tmdb.api.show.SyncEpisodeImages;
import net.simonvt.cathode.tmdb.api.show.SyncShowImages;
import net.simonvt.cathode.trakt.CheckIn;
import net.simonvt.cathode.trakt.UserList;
import net.simonvt.cathode.ui.EpisodeDetailsActivity;
import net.simonvt.cathode.ui.HomeActivity;
import net.simonvt.cathode.ui.comments.AddCommentDialog;
import net.simonvt.cathode.ui.comments.CommentFragment;
import net.simonvt.cathode.ui.comments.CommentsAdapter;
import net.simonvt.cathode.ui.comments.CommentsFragment;
import net.simonvt.cathode.ui.comments.UpdateCommentDialog;
import net.simonvt.cathode.ui.credits.CreditsFragment;
import net.simonvt.cathode.ui.dashboard.DashboardFragment;
import net.simonvt.cathode.ui.dashboard.DashboardMoviesAdapter;
import net.simonvt.cathode.ui.dashboard.DashboardShowsAdapter;
import net.simonvt.cathode.ui.dashboard.DashboardShowsWatchlistAdapter;
import net.simonvt.cathode.ui.dashboard.DashboardUpcomingShowsAdapter;
import net.simonvt.cathode.ui.dialog.CheckInDialog;
import net.simonvt.cathode.ui.dialog.RatingDialog;
import net.simonvt.cathode.ui.history.AddToHistoryDialog;
import net.simonvt.cathode.ui.history.RemoveFromHistoryDialog;
import net.simonvt.cathode.ui.history.SelectHistoryDateFragment;
import net.simonvt.cathode.ui.lists.CreateListFragment;
import net.simonvt.cathode.ui.lists.DeleteListDialog;
import net.simonvt.cathode.ui.lists.ListAdapter;
import net.simonvt.cathode.ui.lists.ListFragment;
import net.simonvt.cathode.ui.lists.ListsDialog;
import net.simonvt.cathode.ui.lists.ListsFragment;
import net.simonvt.cathode.ui.lists.UpdateListFragment;
import net.simonvt.cathode.ui.movie.MovieFragment;
import net.simonvt.cathode.ui.movie.MovieHistoryFragment;
import net.simonvt.cathode.ui.movie.MovieHistoryLoader;
import net.simonvt.cathode.ui.movie.RelatedMoviesFragment;
import net.simonvt.cathode.ui.movies.MoviesAdapter;
import net.simonvt.cathode.ui.movies.collected.CollectedMoviesFragment;
import net.simonvt.cathode.ui.movies.watched.WatchedMoviesFragment;
import net.simonvt.cathode.ui.movies.watchlist.MovieWatchlistFragment;
import net.simonvt.cathode.ui.person.PersonCreditsAdapter;
import net.simonvt.cathode.ui.person.PersonFragment;
import net.simonvt.cathode.ui.search.MovieSearchAdapter;
import net.simonvt.cathode.ui.search.SearchAdapter;
import net.simonvt.cathode.ui.search.SearchFragment;
import net.simonvt.cathode.ui.show.EpisodeFragment;
import net.simonvt.cathode.ui.show.EpisodeHistoryFragment;
import net.simonvt.cathode.ui.show.EpisodeHistoryLoader;
import net.simonvt.cathode.ui.show.RelatedShowsFragment;
import net.simonvt.cathode.ui.show.SeasonAdapter;
import net.simonvt.cathode.ui.show.SeasonFragment;
import net.simonvt.cathode.ui.show.SeasonsAdapter;
import net.simonvt.cathode.ui.show.ShowFragment;
import net.simonvt.cathode.ui.shows.ShowDescriptionAdapter;
import net.simonvt.cathode.ui.shows.ShowsWithNextAdapter;
import net.simonvt.cathode.ui.shows.collected.CollectedShowsFragment;
import net.simonvt.cathode.ui.shows.upcoming.UpcomingAdapter;
import net.simonvt.cathode.ui.shows.upcoming.UpcomingShowsFragment;
import net.simonvt.cathode.ui.shows.upcoming.UpcomingSortByPreference;
import net.simonvt.cathode.ui.shows.watched.WatchedShowsFragment;
import net.simonvt.cathode.ui.shows.watchlist.ShowWatchlistAdapter;
import net.simonvt.cathode.ui.shows.watchlist.ShowsWatchlistFragment;
import net.simonvt.cathode.ui.stats.StatsFragment;
import net.simonvt.cathode.ui.suggestions.movies.AnticipatedMoviesFragment;
import net.simonvt.cathode.ui.suggestions.movies.MovieRecommendationsAdapter;
import net.simonvt.cathode.ui.suggestions.movies.MovieRecommendationsFragment;
import net.simonvt.cathode.ui.suggestions.movies.TrendingMoviesFragment;
import net.simonvt.cathode.ui.suggestions.shows.AnticipatedShowsFragment;
import net.simonvt.cathode.ui.suggestions.shows.ShowRecommendationsAdapter;
import net.simonvt.cathode.ui.suggestions.shows.ShowRecommendationsFragment;
import net.simonvt.cathode.ui.suggestions.shows.TrendingShowsFragment;
import net.simonvt.cathode.widget.PhoneEpisodeView;
import net.simonvt.cathode.widget.RemoteImageView;

@Module(//
    includes = {
        ApiModule.class, SchedulerModule.class, JobModule.class, TraktModule.class,
        DatabaseHelperModule.class, TmdbModule.class, ImageModule.class, JobsModule.class,
    },

    injects = {
        CathodeApp.class,

        // Task schedulers
        EpisodeTaskScheduler.class, MovieTaskScheduler.class, SeasonTaskScheduler.class,
        ShowTaskScheduler.class, SearchTaskScheduler.class, ListsTaskScheduler.class,
        CommentsTaskScheduler.class, PersonTaskScheduler.class,

        // Database helpers
        ShowDatabaseHelper.class, SeasonDatabaseHelper.class, EpisodeDatabaseHelper.class,
        MovieDatabaseHelper.class, UserDatabaseHelper.class, SearchDatabaseHelper.class,

        // Activities
        HomeActivity.class, LoginActivity.class, TokenTask.class, CalendarSetupActivity.class,
        SettingsActivity.class, HiddenItems.class, HiddenItems.HiddenItemsFragment.class,
        TokenActivity.class, EpisodeDetailsActivity.class, NotificationSettingsActivity.class,
        NotificationSettingsActivity.NotificationSettingsFragment.class,
        NotificationSetupActivity.class,

        // Fragments
        EpisodeFragment.class, LogoutDialog.class, CollectedMoviesFragment.class,
        MovieFragment.class, MovieRecommendationsFragment.class, MovieWatchlistFragment.class,
        SeasonFragment.class, ShowFragment.class, CollectedShowsFragment.class,
        ShowRecommendationsFragment.class, ShowsWatchlistFragment.class,
        TrendingShowsFragment.class, TrendingMoviesFragment.class, UpcomingShowsFragment.class,
        WatchedMoviesFragment.class, WatchedShowsFragment.class, CreateListFragment.class,
        ListFragment.class, CommentsFragment.class, CommentFragment.class, ListsFragment.class,
        AnticipatedShowsFragment.class, AnticipatedMoviesFragment.class, StatsFragment.class,
        SearchFragment.class, UpdateListFragment.class, SettingsActivity.SettingsFragment.class,
        RelatedShowsFragment.class, RelatedMoviesFragment.class, PersonFragment.class,
        CreditsFragment.class, DashboardFragment.class, SelectHistoryDateFragment.class,
        EpisodeHistoryFragment.class, MovieHistoryFragment.class,

        // Dialogs
        RatingDialog.class, CheckInDialog.class, CheckInDialog.Injections.class, ListsDialog.class,
        AddCommentDialog.class, UpdateCommentDialog.class, DeleteListDialog.class,
        AddToHistoryDialog.class, RemoveFromHistoryDialog.class,

        // ListAdapters
        SeasonAdapter.class, SeasonsAdapter.class, ShowDescriptionAdapter.class,
        MoviesAdapter.class, MovieSearchAdapter.class, ShowRecommendationsAdapter.class,
        MovieRecommendationsAdapter.class, ShowsWithNextAdapter.class, ShowWatchlistAdapter.class,
        UpcomingAdapter.class, CommentsAdapter.class, HiddenItemsAdapter.class,
        DashboardShowsWatchlistAdapter.class, ListAdapter.class, DashboardMoviesAdapter.class,
        PersonCreditsAdapter.class, DashboardShowsAdapter.class,
        DashboardUpcomingShowsAdapter.class, SearchAdapter.class,

        // Views
        PhoneEpisodeView.class, RemoteImageView.class,

        // Images
        ShowRequestHandler.class, SeasonRequestHandler.class, EpisodeRequestHandler.class,
        MovieRequestHandler.class, PersonRequestHandler.class, ImageRequestHandler.class,

        // Services
        CathodeSyncAdapter.class, AuthJobService.class, NotificationActionService.class,

        // Receivers
        SyncWatchingReceiver.class,

        // Appwidget
        UpcomingRemoteViewsFactory.class,

        // Jobs
        DismissMovieRecommendation.class, CollectMovie.class, RateMovie.class, WatchlistMovie.class,
        DismissShowRecommendation.class, CollectEpisode.class, RateEpisode.class,
        WatchlistEpisode.class, CollectSeason.class, RateShow.class, WatchlistShow.class,
        SyncJob.class, SyncUserActivity.class, SyncUserSettings.class, SyncWatching.class,
        SyncMovieCredits.class, SyncMovieRecommendations.class, SyncMoviesCollection.class,
        SyncMoviesRatings.class, SyncWatchedMovies.class, SyncMoviesWatchlist.class,
        SyncMovie.class, SyncTrendingMovies.class, SyncEpisodesRatings.class,
        SyncEpisodeWatchlist.class, SyncSeasonsRatings.class, SyncSeasons.class, SyncSeason.class,
        SyncShowCredits.class, SyncShowCollectedStatus.class, SyncShowRecommendations.class,
        SyncShowsCollection.class, SyncShowsRatings.class, SyncWatchedShows.class,
        SyncShowsWatchlist.class, SyncShow.class, SyncShowWatchedStatus.class,
        SyncTrendingShows.class, SyncPerson.class, SyncUpdatedShows.class, SyncUpdatedMovies.class,
        ForceUpdateJob.class, UpdateShowCounts.class, SyncLists.class, SyncList.class,
        RemoveShow.class, RemoveSeason.class, RemoveEpisode.class, RemoveMovie.class,
        RemovePerson.class, AddShow.class, AddSeason.class, AddEpisode.class, AddMovie.class,
        AddPerson.class, LogoutJob.class, SyncHiddenItems.class, SyncHiddenCalendar.class,
        SyncHiddenCollected.class, SyncHiddenRecommendations.class, SyncHiddenWatched.class,
        SyncUserComments.class, AddCommentJob.class, UpdateCommentJob.class, DeleteCommentJob.class,
        SyncComments.class, CommentReplyJob.class, SyncUserProfile.class, LikeCommentJob.class,
        UnlikeCommentJob.class, SyncCommentLikes.class, CalendarHideShow.class,
        WatchedHideShow.class, CollectedHideShow.class, CalendarHideMovie.class,
        SyncAnticipatedShows.class, SyncAnticipatedMovies.class, SyncRelatedShows.class,
        SyncRelatedMovies.class, SyncPersonShowCredits.class, SyncPersonMovieCredits.class,
        SyncMovieImages.class, SyncConfiguration.class, SyncShowImages.class,
        SyncEpisodeImages.class, SyncPersonHeadshot.class, SyncPersonBackdrop.class,
        AddShowToHistory.class, AddSeasonToHistory.class, AddEpisodeToHistory.class,
        AddMovieToHistory.class, RemoveSeasonFromHistory.class, RemoveEpisodeFromHistory.class,
        RemoveMovieFromHistory.class, RemoveHistoryItem.class, SyncPendingShows.class,
        SyncPendingSeasons.class, SyncPendingMovies.class,

        // Scheduler jobs
        AuthJobHandlerJob.class, DataJobHandlerJob.class,

        // Upgrade tasks
        EnsureSync.class, UpperCaseGenres.class,

        // Misc
        SearchHandler.class, ApiSettings.class, EpisodeHistoryLoader.class, CheckIn.class,
        MovieHistoryLoader.class, AuthJobHandler.class, DataJobHandler.class, JobCreator.class,
        UserList.class,
    }) //
public class AppModule {

  private final Context app;

  public AppModule(Context app) {
    this.app = app;
  }

  @Provides Context provideContext() {
    return app;
  }

  @Provides @Singleton SearchHandler provideSearchHandler() {
    return new SearchHandler(app);
  }

  @Provides @Singleton UpcomingTimePreference provideUpcomingTimePreference() {
    return UpcomingTimePreference.getInstance();
  }

  @Provides @Singleton UpcomingSortByPreference provideUpcomingSortByPreference() {
    return UpcomingSortByPreference.getInstance();
  }
}
