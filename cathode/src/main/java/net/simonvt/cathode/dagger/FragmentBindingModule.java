package net.simonvt.cathode.dagger;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import net.simonvt.cathode.settings.LogoutDialog;
import net.simonvt.cathode.settings.SettingsActivity;
import net.simonvt.cathode.ui.comments.AddCommentDialog;
import net.simonvt.cathode.ui.comments.CommentFragment;
import net.simonvt.cathode.ui.comments.CommentsFragment;
import net.simonvt.cathode.ui.comments.UpdateCommentDialog;
import net.simonvt.cathode.ui.credits.CreditsFragment;
import net.simonvt.cathode.ui.dashboard.DashboardFragment;
import net.simonvt.cathode.ui.dialog.CheckInDialog;
import net.simonvt.cathode.ui.dialog.RatingDialog;
import net.simonvt.cathode.ui.history.AddToHistoryDialog;
import net.simonvt.cathode.ui.history.RemoveFromHistoryDialog;
import net.simonvt.cathode.ui.history.SelectHistoryDateFragment;
import net.simonvt.cathode.ui.lists.CreateListFragment;
import net.simonvt.cathode.ui.lists.DeleteListDialog;
import net.simonvt.cathode.ui.lists.ListFragment;
import net.simonvt.cathode.ui.lists.ListsDialog;
import net.simonvt.cathode.ui.lists.ListsFragment;
import net.simonvt.cathode.ui.lists.UpdateListFragment;
import net.simonvt.cathode.ui.movie.MovieFragment;
import net.simonvt.cathode.ui.movie.MovieHistoryFragment;
import net.simonvt.cathode.ui.movie.RelatedMoviesFragment;
import net.simonvt.cathode.ui.movies.collected.CollectedMoviesFragment;
import net.simonvt.cathode.ui.movies.watched.WatchedMoviesFragment;
import net.simonvt.cathode.ui.movies.watchlist.MovieWatchlistFragment;
import net.simonvt.cathode.ui.person.PersonFragment;
import net.simonvt.cathode.ui.search.SearchFragment;
import net.simonvt.cathode.ui.show.EpisodeFragment;
import net.simonvt.cathode.ui.show.EpisodeHistoryFragment;
import net.simonvt.cathode.ui.show.RelatedShowsFragment;
import net.simonvt.cathode.ui.show.SeasonFragment;
import net.simonvt.cathode.ui.show.ShowFragment;
import net.simonvt.cathode.ui.shows.collected.CollectedShowsFragment;
import net.simonvt.cathode.ui.shows.upcoming.UpcomingShowsFragment;
import net.simonvt.cathode.ui.shows.watched.WatchedShowsFragment;
import net.simonvt.cathode.ui.shows.watchlist.ShowsWatchlistFragment;
import net.simonvt.cathode.ui.stats.StatsFragment;
import net.simonvt.cathode.ui.suggestions.movies.AnticipatedMoviesFragment;
import net.simonvt.cathode.ui.suggestions.movies.MovieRecommendationsFragment;
import net.simonvt.cathode.ui.suggestions.movies.TrendingMoviesFragment;
import net.simonvt.cathode.ui.suggestions.shows.AnticipatedShowsFragment;
import net.simonvt.cathode.ui.suggestions.shows.ShowRecommendationsFragment;
import net.simonvt.cathode.ui.suggestions.shows.TrendingShowsFragment;

@Module public abstract class FragmentBindingModule {

  @ContributesAndroidInjector abstract EpisodeFragment episodeFragment();

  @ContributesAndroidInjector abstract LogoutDialog logoutDialog();

  @ContributesAndroidInjector abstract CollectedMoviesFragment collectedMoviesFragment();

  @ContributesAndroidInjector abstract MovieFragment movieFragment();

  @ContributesAndroidInjector abstract MovieRecommendationsFragment movieRecommendationsFragment();

  @ContributesAndroidInjector abstract MovieWatchlistFragment movieWatchlistFragment();

  @ContributesAndroidInjector abstract SeasonFragment seasonFragment();

  @ContributesAndroidInjector abstract ShowFragment showFragment();

  @ContributesAndroidInjector abstract CollectedShowsFragment collectedShowsFragment();

  @ContributesAndroidInjector abstract ShowRecommendationsFragment showRecommendationsFragment();

  @ContributesAndroidInjector abstract ShowsWatchlistFragment showsWatchlistFragment();

  @ContributesAndroidInjector abstract TrendingShowsFragment trendingShowsFragment();

  @ContributesAndroidInjector abstract TrendingMoviesFragment trendingMoviesFragment();

  @ContributesAndroidInjector abstract UpcomingShowsFragment upcomingShowsFragment();

  @ContributesAndroidInjector abstract WatchedMoviesFragment watchedMoviesFragment();

  @ContributesAndroidInjector abstract WatchedShowsFragment watchedShowsFragment();

  @ContributesAndroidInjector abstract CreateListFragment createListFragment();

  @ContributesAndroidInjector abstract ListFragment listFragment();

  @ContributesAndroidInjector abstract CommentsFragment commentsFragment();

  @ContributesAndroidInjector abstract CommentFragment commentFragment();

  @ContributesAndroidInjector abstract ListsFragment listsFragment();

  @ContributesAndroidInjector abstract AnticipatedShowsFragment anticipatedShowsFragment();

  @ContributesAndroidInjector abstract AnticipatedMoviesFragment anticipatedMoviesFragment();

  @ContributesAndroidInjector abstract StatsFragment statsFragment();

  @ContributesAndroidInjector abstract SearchFragment searchFragment();

  @ContributesAndroidInjector abstract UpdateListFragment updateListFragment();

  @ContributesAndroidInjector abstract SettingsActivity.SettingsFragment settingsFragment();

  @ContributesAndroidInjector abstract RelatedShowsFragment relatedShowsFragment();

  @ContributesAndroidInjector abstract RelatedMoviesFragment relatedMoviesFragment();

  @ContributesAndroidInjector abstract PersonFragment personFragment();

  @ContributesAndroidInjector abstract CreditsFragment creditsFragment();

  @ContributesAndroidInjector abstract DashboardFragment dashboardFragment();

  @ContributesAndroidInjector abstract SelectHistoryDateFragment selectHistoryDateFragment();

  @ContributesAndroidInjector abstract EpisodeHistoryFragment episodeHistoryFragment();

  @ContributesAndroidInjector abstract MovieHistoryFragment movieHistoryFragment();

  @ContributesAndroidInjector abstract RatingDialog ratingDialog();

  @ContributesAndroidInjector abstract CheckInDialog checkInDialog();

  @ContributesAndroidInjector abstract ListsDialog listsDialog();

  @ContributesAndroidInjector abstract AddCommentDialog addCommentDialog();

  @ContributesAndroidInjector abstract UpdateCommentDialog updateCommentDialog();

  @ContributesAndroidInjector abstract DeleteListDialog deleteListDialog();

  @ContributesAndroidInjector abstract AddToHistoryDialog addToHistoryDialog();

  @ContributesAndroidInjector abstract RemoveFromHistoryDialog removeFromHistoryDialog();
}
