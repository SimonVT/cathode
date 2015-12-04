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
import android.content.Intent;
import com.squareup.otto.Bus;
import com.squareup.picasso.Picasso;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.api.TraktModule;
import net.simonvt.cathode.jobqueue.AuthJobService;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobInjector;
import net.simonvt.cathode.jobqueue.JobListener;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.jobqueue.JobModule;
import net.simonvt.cathode.jobqueue.JobService;
import net.simonvt.cathode.provider.EpisodeDatabaseHelper;
import net.simonvt.cathode.provider.MovieDatabaseHelper;
import net.simonvt.cathode.provider.SeasonDatabaseHelper;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import net.simonvt.cathode.provider.UserDatabaseHelper;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.cathode.remote.ForceUpdateJob;
import net.simonvt.cathode.remote.LogoutJob;
import net.simonvt.cathode.remote.UpdateShowCounts;
import net.simonvt.cathode.remote.action.CancelCheckin;
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
import net.simonvt.cathode.remote.action.lists.CreateList;
import net.simonvt.cathode.remote.action.lists.RemoveEpisode;
import net.simonvt.cathode.remote.action.lists.RemoveMovie;
import net.simonvt.cathode.remote.action.lists.RemovePerson;
import net.simonvt.cathode.remote.action.lists.RemoveSeason;
import net.simonvt.cathode.remote.action.lists.RemoveShow;
import net.simonvt.cathode.remote.action.movies.CalendarHideMovie;
import net.simonvt.cathode.remote.action.movies.CheckInMovie;
import net.simonvt.cathode.remote.action.movies.CollectMovie;
import net.simonvt.cathode.remote.action.movies.CollectedHideMovie;
import net.simonvt.cathode.remote.action.movies.DismissMovieRecommendation;
import net.simonvt.cathode.remote.action.movies.RateMovie;
import net.simonvt.cathode.remote.action.movies.WatchedHideMovie;
import net.simonvt.cathode.remote.action.movies.WatchedMovie;
import net.simonvt.cathode.remote.action.movies.WatchlistMovie;
import net.simonvt.cathode.remote.action.shows.CalendarHideShow;
import net.simonvt.cathode.remote.action.shows.CheckInEpisode;
import net.simonvt.cathode.remote.action.shows.CollectEpisode;
import net.simonvt.cathode.remote.action.shows.CollectSeason;
import net.simonvt.cathode.remote.action.shows.CollectedHideShow;
import net.simonvt.cathode.remote.action.shows.DismissShowRecommendation;
import net.simonvt.cathode.remote.action.shows.RateEpisode;
import net.simonvt.cathode.remote.action.shows.RateShow;
import net.simonvt.cathode.remote.action.shows.WatchedEpisode;
import net.simonvt.cathode.remote.action.shows.WatchedHideShow;
import net.simonvt.cathode.remote.action.shows.WatchedSeason;
import net.simonvt.cathode.remote.action.shows.WatchedShow;
import net.simonvt.cathode.remote.action.shows.WatchlistEpisode;
import net.simonvt.cathode.remote.action.shows.WatchlistShow;
import net.simonvt.cathode.remote.sync.PurgeDatabase;
import net.simonvt.cathode.remote.sync.SyncActivityStream;
import net.simonvt.cathode.remote.sync.SyncHiddenItems;
import net.simonvt.cathode.remote.sync.SyncHiddenSection;
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
import net.simonvt.cathode.remote.sync.movies.StartSyncUpdatedMovies;
import net.simonvt.cathode.remote.sync.movies.SyncMovie;
import net.simonvt.cathode.remote.sync.movies.SyncMovieCrew;
import net.simonvt.cathode.remote.sync.movies.SyncMovieRecommendations;
import net.simonvt.cathode.remote.sync.movies.SyncMoviesCollection;
import net.simonvt.cathode.remote.sync.movies.SyncMoviesRatings;
import net.simonvt.cathode.remote.sync.movies.SyncMoviesWatchlist;
import net.simonvt.cathode.remote.sync.movies.SyncTrendingMovies;
import net.simonvt.cathode.remote.sync.movies.SyncUpdatedMovies;
import net.simonvt.cathode.remote.sync.movies.SyncWatchedMovies;
import net.simonvt.cathode.remote.sync.shows.StartSyncUpdatedShows;
import net.simonvt.cathode.remote.sync.shows.SyncEpisodeWatchlist;
import net.simonvt.cathode.remote.sync.shows.SyncEpisodesRatings;
import net.simonvt.cathode.remote.sync.shows.SyncSeason;
import net.simonvt.cathode.remote.sync.shows.SyncSeasons;
import net.simonvt.cathode.remote.sync.shows.SyncSeasonsRatings;
import net.simonvt.cathode.remote.sync.shows.SyncShow;
import net.simonvt.cathode.remote.sync.shows.SyncShowCast;
import net.simonvt.cathode.remote.sync.shows.SyncShowCollectedStatus;
import net.simonvt.cathode.remote.sync.shows.SyncShowRecommendations;
import net.simonvt.cathode.remote.sync.shows.SyncShowWatchedStatus;
import net.simonvt.cathode.remote.sync.shows.SyncShowsCollection;
import net.simonvt.cathode.remote.sync.shows.SyncShowsRatings;
import net.simonvt.cathode.remote.sync.shows.SyncShowsWatchlist;
import net.simonvt.cathode.remote.sync.shows.SyncTrendingShows;
import net.simonvt.cathode.remote.sync.shows.SyncUpdatedShows;
import net.simonvt.cathode.remote.sync.shows.SyncWatchedShows;
import net.simonvt.cathode.scheduler.CommentsTaskScheduler;
import net.simonvt.cathode.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.scheduler.ListsTaskScheduler;
import net.simonvt.cathode.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.scheduler.SearchTaskScheduler;
import net.simonvt.cathode.scheduler.SeasonTaskScheduler;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.search.MovieSearchHandler;
import net.simonvt.cathode.search.ShowSearchHandler;
import net.simonvt.cathode.service.CathodeSyncAdapter;
import net.simonvt.cathode.service.SyncWatchingReceiver;
import net.simonvt.cathode.ui.HiddenItems;
import net.simonvt.cathode.ui.HomeActivity;
import net.simonvt.cathode.ui.LoginActivity;
import net.simonvt.cathode.ui.SettingsActivity;
import net.simonvt.cathode.ui.adapter.CommentsAdapter;
import net.simonvt.cathode.ui.adapter.HiddenItemsAdapter;
import net.simonvt.cathode.ui.adapter.MovieRecommendationsAdapter;
import net.simonvt.cathode.ui.adapter.MovieSearchAdapter;
import net.simonvt.cathode.ui.adapter.MoviesAdapter;
import net.simonvt.cathode.ui.adapter.SeasonAdapter;
import net.simonvt.cathode.ui.adapter.SeasonsAdapter;
import net.simonvt.cathode.ui.adapter.ShowDescriptionAdapter;
import net.simonvt.cathode.ui.adapter.ShowRecommendationsAdapter;
import net.simonvt.cathode.ui.adapter.ShowWatchlistAdapter;
import net.simonvt.cathode.ui.adapter.ShowsWithNextAdapter;
import net.simonvt.cathode.ui.adapter.UpcomingAdapter;
import net.simonvt.cathode.ui.dialog.AddCommentDialog;
import net.simonvt.cathode.ui.dialog.CheckInDialog;
import net.simonvt.cathode.ui.dialog.ListsDialog;
import net.simonvt.cathode.ui.dialog.LogoutDialog;
import net.simonvt.cathode.ui.dialog.RatingDialog;
import net.simonvt.cathode.ui.dialog.UpdateCommentDialog;
import net.simonvt.cathode.ui.fragment.CommentFragment;
import net.simonvt.cathode.ui.fragment.CommentsFragment;
import net.simonvt.cathode.ui.fragment.CreateListFragment;
import net.simonvt.cathode.ui.fragment.EpisodeFragment;
import net.simonvt.cathode.ui.fragment.ListFragment;
import net.simonvt.cathode.ui.fragment.ListsFragment;
import net.simonvt.cathode.ui.fragment.MovieCollectionFragment;
import net.simonvt.cathode.ui.fragment.MovieFragment;
import net.simonvt.cathode.ui.fragment.MovieRecommendationsFragment;
import net.simonvt.cathode.ui.fragment.MovieWatchlistFragment;
import net.simonvt.cathode.ui.fragment.SearchMovieFragment;
import net.simonvt.cathode.ui.fragment.SearchShowFragment;
import net.simonvt.cathode.ui.fragment.SeasonFragment;
import net.simonvt.cathode.ui.fragment.ShowFragment;
import net.simonvt.cathode.ui.fragment.ShowRecommendationsFragment;
import net.simonvt.cathode.ui.fragment.ShowsCollectionFragment;
import net.simonvt.cathode.ui.fragment.ShowsWatchlistFragment;
import net.simonvt.cathode.ui.fragment.TrendingMoviesFragment;
import net.simonvt.cathode.ui.fragment.TrendingShowsFragment;
import net.simonvt.cathode.ui.fragment.UpcomingShowsFragment;
import net.simonvt.cathode.ui.fragment.WatchedMoviesFragment;
import net.simonvt.cathode.ui.fragment.WatchedShowsFragment;
import net.simonvt.cathode.ui.setup.CalendarSetupActivity;
import net.simonvt.cathode.widget.PhoneEpisodeView;
import net.simonvt.cathode.widget.RemoteImageView;

@Module(
    includes = {
        ApiModule.class, SchedulerModule.class, JobModule.class, TraktModule.class,
        DatabaseHelperModule.class
    },

    injects = {
        CathodeApp.class,

        // Task schedulers
        EpisodeTaskScheduler.class, MovieTaskScheduler.class, SeasonTaskScheduler.class,
        ShowTaskScheduler.class, SearchTaskScheduler.class, ListsTaskScheduler.class,
        CommentsTaskScheduler.class,

        // Database helpers
        ShowDatabaseHelper.class, SeasonDatabaseHelper.class, EpisodeDatabaseHelper.class,
        MovieDatabaseHelper.class, UserDatabaseHelper.class,

        // Activities
        HomeActivity.class, LoginActivity.class, LoginActivity.TokenTask.class,
        CalendarSetupActivity.class, SettingsActivity.class, HiddenItems.class,

        // Fragments
        EpisodeFragment.class, LogoutDialog.class, MovieCollectionFragment.class,
        MovieFragment.class, MovieRecommendationsFragment.class, MovieWatchlistFragment.class,
        SeasonFragment.class, ShowFragment.class, ShowsCollectionFragment.class,
        ShowRecommendationsFragment.class, ShowsWatchlistFragment.class,
        TrendingShowsFragment.class, TrendingMoviesFragment.class, UpcomingShowsFragment.class,
        WatchedMoviesFragment.class, WatchedShowsFragment.class, CreateListFragment.class,
        ListFragment.class, CommentsFragment.class, CommentFragment.class, ListsFragment.class,
        SearchShowFragment.class, SearchMovieFragment.class,

        // Dialogs
        RatingDialog.class, CheckInDialog.class, CheckInDialog.Injections.class, ListsDialog.class,
        AddCommentDialog.class, UpdateCommentDialog.class,

        // ListAdapters
        SeasonAdapter.class, SeasonsAdapter.class, ShowDescriptionAdapter.class,
        MoviesAdapter.class, MovieSearchAdapter.class, ShowRecommendationsAdapter.class,
        MovieRecommendationsAdapter.class, ShowsWithNextAdapter.class, ShowWatchlistAdapter.class,
        UpcomingAdapter.class, CommentsAdapter.class, HiddenItemsAdapter.class,

        // Views
        PhoneEpisodeView.class, RemoteImageView.class,

        // Services
        JobService.class, CathodeSyncAdapter.class, AuthJobService.class,

        // Receivers
        SyncWatchingReceiver.class,

        // Tasks
        CancelCheckin.class, CheckInMovie.class, DismissMovieRecommendation.class,
        CollectMovie.class, RateMovie.class, WatchedMovie.class, WatchlistMovie.class,
        CheckInEpisode.class, DismissShowRecommendation.class, CollectEpisode.class,
        RateEpisode.class, WatchedEpisode.class, WatchlistEpisode.class, CollectSeason.class,
        WatchedSeason.class, RateShow.class, WatchedShow.class, WatchlistShow.class,
        PurgeDatabase.class, SyncActivityStream.class, SyncJob.class, SyncUserActivity.class,
        SyncUserSettings.class, SyncWatching.class, SyncMovieCrew.class,
        SyncMovieRecommendations.class, SyncMoviesCollection.class, SyncMoviesRatings.class,
        SyncWatchedMovies.class, SyncMoviesWatchlist.class, SyncMovie.class,
        SyncTrendingMovies.class, StartSyncUpdatedMovies.class, SyncEpisodesRatings.class,
        SyncEpisodeWatchlist.class, SyncSeasonsRatings.class, SyncSeasons.class,
        SyncSeason.class, SyncShowCast.class, SyncShowCollectedStatus.class,
        SyncShowRecommendations.class, SyncShowsCollection.class, SyncShowsRatings.class,
        SyncWatchedShows.class, SyncShowsWatchlist.class, SyncShow.class,
        SyncShowWatchedStatus.class, SyncTrendingShows.class, StartSyncUpdatedShows.class,
        SyncPerson.class, SyncUpdatedShows.class, SyncUpdatedMovies.class, ForceUpdateJob.class,
        UpdateShowCounts.class, SyncLists.class, SyncList.class, CreateList.class,
        RemoveShow.class, RemoveSeason.class, RemoveEpisode.class, RemoveMovie.class,
        RemovePerson.class, AddShow.class, AddSeason.class, AddEpisode.class, AddMovie.class,
        AddPerson.class, LogoutJob.class, SyncHiddenItems.class, SyncHiddenSection.class,
        SyncUserComments.class, AddCommentJob.class, UpdateCommentJob.class, DeleteCommentJob.class,
        SyncComments.class, CommentReplyJob.class, SyncUserProfile.class, LikeCommentJob.class,
        UnlikeCommentJob.class, SyncCommentLikes.class, CalendarHideShow.class,
        WatchedHideShow.class, CollectedHideShow.class, CalendarHideMovie.class,
        WatchedHideMovie.class, CollectedHideMovie.class,

        // Misc
        ShowSearchHandler.class, ShowSearchHandler.SearchThread.class, MovieSearchHandler.class,
        MovieSearchHandler.SearchThread.class, ApiSettings.class
    })
public class AppModule {

  private final Context app;

  public AppModule(Context app) {
    this.app = app;
  }

  @Provides Context provideContext() {
    return app;
  }

  @Provides @Singleton JobInjector provideJobInjector(final Context context) {
    return JobInjectorImpl.getInstance(context);
  }

  @Provides @Singleton JobListener provideJobListener(final Context context) {
    return new JobListener() {
      @Override public void onJobsLoaded(JobManager jobManager) {
        if (jobManager.hasJobs(Flags.REQUIRES_AUTH, 0)) {
          Intent i = new Intent(context, AuthJobService.class);
          context.startService(i);
        } else if (jobManager.hasJobs(0, Flags.REQUIRES_AUTH)) {
          Intent i = new Intent(context, JobService.class);
          context.startService(i);
        }
      }

      @Override public void onJobAdded(JobManager jobManager, Job job) {
        if (job.hasFlags(Flags.REQUIRES_AUTH)) {
          Intent i = new Intent(context, AuthJobService.class);
          context.startService(i);
        } else {
          Intent i = new Intent(context, JobService.class);
          context.startService(i);
        }
      }

      @Override public void onJobRemoved(JobManager jobManager, Job job) {
      }
    };
  }

  @Provides @Singleton ShowSearchHandler provideShowSearchHandler(Bus bus) {
    return new ShowSearchHandler(app);
  }

  @Provides @Singleton MovieSearchHandler provideMovieSearchHandler(Bus bus) {
    return new MovieSearchHandler(app);
  }

  @Provides @Singleton Picasso providePicasso() {
    return new Picasso.Builder(app).build();
  }

  @Provides @Singleton Bus provideBus() {
    return new Bus();
  }
}
