package net.simonvt.trakt;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.otto.Bus;
import com.squareup.picasso.Picasso;
import com.squareup.tape.ObjectQueue;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import javax.inject.Singleton;
import net.simonvt.trakt.api.ApiKey;
import net.simonvt.trakt.api.ResponseParser;
import net.simonvt.trakt.api.TraktModule;
import net.simonvt.trakt.api.UserCredentials;
import net.simonvt.trakt.remote.PriorityTraktTaskQueue;
import net.simonvt.trakt.remote.TraktTask;
import net.simonvt.trakt.remote.TraktTaskQueue;
import net.simonvt.trakt.remote.TraktTaskSerializer;
import net.simonvt.trakt.remote.TraktTaskService;
import net.simonvt.trakt.remote.action.EpisodeCollectionTask;
import net.simonvt.trakt.remote.action.EpisodeRateTask;
import net.simonvt.trakt.remote.action.EpisodeWatchedTask;
import net.simonvt.trakt.remote.action.EpisodeWatchlistTask;
import net.simonvt.trakt.remote.action.MovieCollectionTask;
import net.simonvt.trakt.remote.action.MovieRateTask;
import net.simonvt.trakt.remote.action.MovieWatchedTask;
import net.simonvt.trakt.remote.action.MovieWatchlistTask;
import net.simonvt.trakt.remote.action.ShowCollectionTask;
import net.simonvt.trakt.remote.action.ShowRateTask;
import net.simonvt.trakt.remote.action.ShowWatchedTask;
import net.simonvt.trakt.remote.action.ShowWatchlistTask;
import net.simonvt.trakt.remote.sync.SyncEpisodeTask;
import net.simonvt.trakt.remote.sync.SyncEpisodeWatchlistTask;
import net.simonvt.trakt.remote.sync.SyncMovieTask;
import net.simonvt.trakt.remote.sync.SyncMoviesCollectionTask;
import net.simonvt.trakt.remote.sync.SyncMoviesTask;
import net.simonvt.trakt.remote.sync.SyncMoviesWatchedTask;
import net.simonvt.trakt.remote.sync.SyncMoviesWatchlistTask;
import net.simonvt.trakt.remote.sync.SyncSeasonTask;
import net.simonvt.trakt.remote.sync.SyncShowTask;
import net.simonvt.trakt.remote.sync.SyncShowsCollectionTask;
import net.simonvt.trakt.remote.sync.SyncShowsTask;
import net.simonvt.trakt.remote.sync.SyncShowsWatchedTask;
import net.simonvt.trakt.remote.sync.SyncShowsWatchlistTask;
import net.simonvt.trakt.remote.sync.SyncTask;
import net.simonvt.trakt.remote.sync.SyncUpdatedMovies;
import net.simonvt.trakt.remote.sync.SyncUpdatedShows;
import net.simonvt.trakt.scheduler.EpisodeTaskScheduler;
import net.simonvt.trakt.scheduler.MovieTaskScheduler;
import net.simonvt.trakt.scheduler.SeasonTaskScheduler;
import net.simonvt.trakt.scheduler.ShowTaskScheduler;
import net.simonvt.trakt.settings.Settings;
import net.simonvt.trakt.ui.HomeActivity;
import net.simonvt.trakt.ui.PhoneController;
import net.simonvt.trakt.ui.adapter.EpisodeWatchlistAdapter;
import net.simonvt.trakt.ui.adapter.MovieSearchAdapter;
import net.simonvt.trakt.ui.adapter.MoviesAdapter;
import net.simonvt.trakt.ui.adapter.SeasonAdapter;
import net.simonvt.trakt.ui.adapter.SeasonsAdapter;
import net.simonvt.trakt.ui.adapter.ShowSearchAdapter;
import net.simonvt.trakt.ui.adapter.ShowsAdapter;
import net.simonvt.trakt.ui.dialog.RatingDialog;
import net.simonvt.trakt.ui.fragment.EpisodeFragment;
import net.simonvt.trakt.ui.fragment.EpisodesWatchlistFragment;
import net.simonvt.trakt.ui.fragment.LoginFragment;
import net.simonvt.trakt.ui.fragment.MovieCollectionFragment;
import net.simonvt.trakt.ui.fragment.MovieFragment;
import net.simonvt.trakt.ui.fragment.MovieWatchlistFragment;
import net.simonvt.trakt.ui.fragment.SearchMovieFragment;
import net.simonvt.trakt.ui.fragment.SearchShowFragment;
import net.simonvt.trakt.ui.fragment.SeasonFragment;
import net.simonvt.trakt.ui.fragment.ShowInfoFragment;
import net.simonvt.trakt.ui.fragment.ShowsCollectionFragment;
import net.simonvt.trakt.ui.fragment.ShowsWatchlistFragment;
import net.simonvt.trakt.ui.fragment.UpcomingShowsFragment;
import net.simonvt.trakt.ui.fragment.WatchedMoviesFragment;
import net.simonvt.trakt.ui.fragment.WatchedShowsFragment;
import net.simonvt.trakt.util.LogWrapper;
import net.simonvt.trakt.util.MovieSearchHandler;
import net.simonvt.trakt.util.ShowSearchHandler;
import net.simonvt.trakt.widget.PhoneEpisodeView;
import net.simonvt.trakt.widget.RemoteImageView;

public class TraktApp extends Application {

  public static final boolean DEBUG = BuildConfig.DEBUG;

  private ObjectGraph objectGraph;

  @Override
  public void onCreate() {
    super.onCreate();
    objectGraph = ObjectGraph.create(new AppModule(this));
    objectGraph.plus(new TraktModule());
  }

  public static void inject(Context context) {
    ((TraktApp) context.getApplicationContext()).objectGraph.inject(context);
  }

  public static void inject(Context context, Object object) {
    ((TraktApp) context.getApplicationContext()).objectGraph.inject(object);
  }

  public static Account getAccount(Context context) {
    return new Account(context.getString(R.string.accountName),
        context.getString(R.string.accountType));
  }

  public static void setupAccount(Context context) {
    AccountManager manager = AccountManager.get(context);
    Account account = getAccount(context);
    manager.addAccountExplicitly(account, null, null);

    ContentResolver.setIsSyncable(account, CalendarContract.AUTHORITY, 1);
    ContentResolver.setSyncAutomatically(account, CalendarContract.AUTHORITY, true);
  }

  public static void removeAccount(Context context) {
    AccountManager am = AccountManager.get(context);
    Account[] accounts = am.getAccountsByType(context.getString(R.string.accountType));
    for (Account account : accounts) {
      am.removeAccount(account, null, null);
    }
  }

  @Module(
      includes = {
          TraktModule.class
      },

      injects = {
          // Task queues
          PriorityTraktTaskQueue.class, TraktTaskQueue.class,

          // Task schedulers
          EpisodeTaskScheduler.class, MovieTaskScheduler.class, SeasonTaskScheduler.class,
          ShowTaskScheduler.class,

          // Activities
          HomeActivity.class,

          // Fragments
          SearchShowFragment.class, EpisodeFragment.class, EpisodesWatchlistFragment.class,
          LoginFragment.class, MovieCollectionFragment.class, MovieFragment.class,
          MovieWatchlistFragment.class, SearchMovieFragment.class, SeasonFragment.class,
          ShowInfoFragment.class, ShowsCollectionFragment.class, ShowsWatchlistFragment.class,
          UpcomingShowsFragment.class, WatchedMoviesFragment.class, WatchedShowsFragment.class,

          // Dialogs
          RatingDialog.class,

          // ListAdapters
          EpisodeWatchlistAdapter.class, SeasonAdapter.class, SeasonsAdapter.class,
          ShowsAdapter.class, ShowSearchAdapter.class, MoviesAdapter.class,
          MovieSearchAdapter.class,

          // Views
          PhoneEpisodeView.class, RemoteImageView.class,

          // Services
          TraktTaskService.class,

          // Tasks
          EpisodeCollectionTask.class, EpisodeRateTask.class, EpisodeWatchedTask.class,
          EpisodeWatchlistTask.class, MovieCollectionTask.class, MovieRateTask.class,
          MovieWatchedTask.class, MovieWatchlistTask.class, ShowRateTask.class,
          ShowCollectionTask.class, ShowWatchlistTask.class, SyncShowsCollectionTask.class,
          SyncEpisodeTask.class, SyncEpisodeWatchlistTask.class, SyncMovieTask.class,
          SyncMoviesTask.class, SyncMoviesCollectionTask.class, SyncMoviesWatchedTask.class,
          SyncMoviesWatchlistTask.class, SyncSeasonTask.class, SyncShowTask.class,
          SyncShowsTask.class, SyncShowsWatchlistTask.class, SyncShowsWatchedTask.class,
          ShowWatchedTask.class, SyncUpdatedMovies.class, SyncUpdatedShows.class,
          SyncTask.class,

          // Misc
          PhoneController.class, ResponseParser.class, ShowSearchHandler.class,
          ShowSearchHandler.SearchThread.class, MovieSearchHandler.class,
          MovieSearchHandler.SearchThread.class
      })
  static class AppModule {

    private final Context appContext;

    AppModule(Context appContext) {
      this.appContext = appContext;
    }

    @Provides @Singleton TraktTaskQueue provideTaskQueue(Gson gson) {
      TraktTaskQueue queue = TraktTaskQueue.create(appContext, gson);
      if (DEBUG) {
        queue.setListener(new ObjectQueue.Listener<TraktTask>() {
          @Override
          public void onAdd(ObjectQueue<TraktTask> queue, TraktTask entry) {
            LogWrapper.i("TraktTaskQueue", "Queue size: " + queue.size());
          }

          @Override
          public void onRemove(ObjectQueue<TraktTask> queue) {
            LogWrapper.i("TraktTaskQueue", "Queue size: " + queue.size());
          }
        });
      }
      return queue;
    }

    @Provides @Singleton PriorityTraktTaskQueue providePriorityTaskQueue(Gson gson) {
      PriorityTraktTaskQueue queue = PriorityTraktTaskQueue.create(appContext, gson);
      if (DEBUG) {
        queue.setListener(new ObjectQueue.Listener<TraktTask>() {
          @Override
          public void onAdd(ObjectQueue<TraktTask> queue, TraktTask entry) {
            LogWrapper.i("PriorityTraktTaskQueue", "Queue size: " + queue.size());
          }

          @Override
          public void onRemove(ObjectQueue<TraktTask> queue) {
            LogWrapper.i("PriorityTraktTaskQueue", "Queue size: " + queue.size());
          }
        });
      }
      return queue;
    }

    @Provides @Singleton ShowSearchHandler provideShowSearchHandler(Bus bus) {
      return new ShowSearchHandler(appContext, bus);
    }

    @Provides @Singleton MovieSearchHandler provideMovieSearchHandler(Bus bus) {
      return new MovieSearchHandler(appContext, bus);
    }

    @Provides @ApiKey String provideApiKey() {
      return appContext.getString(R.string.apikey);
    }

    @Provides @Singleton Picasso providePicasso() {
      return new Picasso.Builder(appContext).build();
    }

    @Provides @Singleton Bus provideBus() {
      return new Bus();
    }

    @Provides @Singleton Gson provideGson() {
      GsonBuilder builder = new GsonBuilder();
      builder.registerTypeAdapter(TraktTask.class, new TraktTaskSerializer());
      return builder.create();
    }

    @Provides @Singleton EpisodeTaskScheduler provideEpisodeScheduler() {
      return new EpisodeTaskScheduler(appContext);
    }

    @Provides @Singleton SeasonTaskScheduler provideSeasonScheduler() {
      return new SeasonTaskScheduler(appContext);
    }

    @Provides @Singleton ShowTaskScheduler provideShowScheduler() {
      return new ShowTaskScheduler(appContext);
    }

    @Provides @Singleton MovieTaskScheduler provideMovieScheduler() {
      return new MovieTaskScheduler(appContext);
    }

    @Provides @Singleton UserCredentials provideCredentials() {
      SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(appContext);
      final String username = settings.getString(Settings.USERNAME, null);
      final String password = settings.getString(Settings.PASSWORD, null);
      return new UserCredentials(username, password);
    }
  }
}
