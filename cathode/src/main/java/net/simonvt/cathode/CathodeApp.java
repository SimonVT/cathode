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
package net.simonvt.cathode;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;
import com.squareup.tape.ObjectQueue;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.simonvt.cathode.api.ApiKey;
import net.simonvt.cathode.api.TraktModule;
import net.simonvt.cathode.api.UserToken;
import net.simonvt.cathode.event.AuthFailedEvent;
import net.simonvt.cathode.remote.DeserializationFailedTask;
import net.simonvt.cathode.remote.PriorityQueue;
import net.simonvt.cathode.remote.TraktTask;
import net.simonvt.cathode.remote.TraktTaskQueue;
import net.simonvt.cathode.remote.TraktTaskSerializer;
import net.simonvt.cathode.remote.TraktTaskService;
import net.simonvt.cathode.remote.action.CancelCheckin;
import net.simonvt.cathode.remote.action.movies.CheckInMovieTask;
import net.simonvt.cathode.remote.action.movies.DismissMovieRecommendation;
import net.simonvt.cathode.remote.action.movies.MovieCollectionTask;
import net.simonvt.cathode.remote.action.movies.MovieRateTask;
import net.simonvt.cathode.remote.action.movies.MovieWatchedTask;
import net.simonvt.cathode.remote.action.movies.MovieWatchlistTask;
import net.simonvt.cathode.remote.action.shows.CheckInEpisodeTask;
import net.simonvt.cathode.remote.action.shows.DismissShowRecommendation;
import net.simonvt.cathode.remote.action.shows.EpisodeCollectionTask;
import net.simonvt.cathode.remote.action.shows.EpisodeRateTask;
import net.simonvt.cathode.remote.action.shows.EpisodeWatchedTask;
import net.simonvt.cathode.remote.action.shows.EpisodeWatchlistTask;
import net.simonvt.cathode.remote.action.shows.SeasonCollectionTask;
import net.simonvt.cathode.remote.action.shows.SeasonWatchedTask;
import net.simonvt.cathode.remote.action.shows.ShowRateTask;
import net.simonvt.cathode.remote.action.shows.ShowWatchedTask;
import net.simonvt.cathode.remote.action.shows.ShowWatchlistTask;
import net.simonvt.cathode.remote.sync.PurgeTask;
import net.simonvt.cathode.remote.sync.SyncActivityStreamTask;
import net.simonvt.cathode.remote.sync.SyncPersonTask;
import net.simonvt.cathode.remote.sync.SyncTask;
import net.simonvt.cathode.remote.sync.SyncUserActivityTask;
import net.simonvt.cathode.remote.sync.SyncUserSettingsTask;
import net.simonvt.cathode.remote.sync.SyncWatchingTask;
import net.simonvt.cathode.remote.sync.movies.SyncMovieCrew;
import net.simonvt.cathode.remote.sync.movies.SyncMovieRecommendations;
import net.simonvt.cathode.remote.sync.movies.SyncMovieTask;
import net.simonvt.cathode.remote.sync.movies.SyncMoviesCollectionTask;
import net.simonvt.cathode.remote.sync.movies.SyncMoviesRatings;
import net.simonvt.cathode.remote.sync.movies.SyncMoviesWatchedTask;
import net.simonvt.cathode.remote.sync.movies.SyncMoviesWatchlistTask;
import net.simonvt.cathode.remote.sync.movies.SyncTrendingMoviesTask;
import net.simonvt.cathode.remote.sync.movies.SyncUpdatedMovies;
import net.simonvt.cathode.remote.sync.shows.SyncEpisodeTask;
import net.simonvt.cathode.remote.sync.shows.SyncEpisodeWatchlistTask;
import net.simonvt.cathode.remote.sync.shows.SyncEpisodesRatings;
import net.simonvt.cathode.remote.sync.shows.SyncSeasonTask;
import net.simonvt.cathode.remote.sync.shows.SyncSeasonsRatings;
import net.simonvt.cathode.remote.sync.shows.SyncSeasonsTask;
import net.simonvt.cathode.remote.sync.shows.SyncShowCast;
import net.simonvt.cathode.remote.sync.shows.SyncShowCollectedStatus;
import net.simonvt.cathode.remote.sync.shows.SyncShowRecommendations;
import net.simonvt.cathode.remote.sync.shows.SyncShowTask;
import net.simonvt.cathode.remote.sync.shows.SyncShowWatchedStatus;
import net.simonvt.cathode.remote.sync.shows.SyncShowsCollectionTask;
import net.simonvt.cathode.remote.sync.shows.SyncShowsRatings;
import net.simonvt.cathode.remote.sync.shows.SyncShowsWatchedTask;
import net.simonvt.cathode.remote.sync.shows.SyncShowsWatchlistTask;
import net.simonvt.cathode.remote.sync.shows.SyncTrendingShowsTask;
import net.simonvt.cathode.remote.sync.shows.SyncUpdatedShows;
import net.simonvt.cathode.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.scheduler.SearchTaskScheduler;
import net.simonvt.cathode.scheduler.SeasonTaskScheduler;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.service.AccountAuthenticator;
import net.simonvt.cathode.service.CathodeSyncAdapter;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.HomeActivity;
import net.simonvt.cathode.ui.PhoneController;
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
import net.simonvt.cathode.ui.dialog.CheckInDialog;
import net.simonvt.cathode.ui.dialog.LogoutDialog;
import net.simonvt.cathode.ui.dialog.RatingDialog;
import net.simonvt.cathode.ui.fragment.EpisodeFragment;
import net.simonvt.cathode.ui.fragment.LoginFragment;
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
import net.simonvt.cathode.util.DateUtils;
import net.simonvt.cathode.util.MovieSearchHandler;
import net.simonvt.cathode.util.ShowSearchHandler;
import net.simonvt.cathode.widget.PhoneEpisodeView;
import net.simonvt.cathode.widget.RemoteImageView;
import retrofit.ErrorHandler;
import retrofit.RetrofitError;
import retrofit.client.Response;
import timber.log.Timber;

public class CathodeApp extends Application {

  private static final String TAG = "CathodeApp";

  public static final boolean DEBUG = BuildConfig.DEBUG;

  private static final int AUTH_NOTIFICATION = 2;

  private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

  private ObjectGraph objectGraph;

  @Inject Bus bus;

  @Override public void onCreate() {
    super.onCreate();
    if (BuildConfig.DEBUG) {
      Timber.plant(new Timber.DebugTree());

      StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
      StrictMode.setThreadPolicy(
          new StrictMode.ThreadPolicy.Builder().detectAll().permitDiskReads().penaltyLog().build());
    } else {
      Crashlytics.start(this);
      Timber.plant(new CrashlyticsTree());
    }

    upgrade();

    if (!accountExists(this)) {
      setupAccount(this);
    }

    objectGraph = ObjectGraph.create(new AppModule(this));
    objectGraph.plus(new TraktModule());

    objectGraph.inject(this);

    bus.register(this);
  }

  private void upgrade() {
    // TODO: Delete CathodeDatabase
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    final int currentVersion = settings.getInt(Settings.VERSION_CODE, -1);

    if (currentVersion == -1) {
      settings.edit().putInt(Settings.VERSION_CODE, BuildConfig.VERSION_CODE).apply();
      return;
    }

    if (currentVersion != BuildConfig.VERSION_CODE) {
      if (currentVersion < 20000) {
        removeAccount(this);
      }

      settings.edit().putInt(Settings.VERSION_CODE, BuildConfig.VERSION_CODE).apply();
    }
  }

  @Subscribe public void onAuthFailure(AuthFailedEvent event) {
    Timber.tag(TAG).i("onAuthFailure");
    if (!accountExists(this)) return; // User has logged out, ignore.

    Account account = getAccount(this);

    Intent intent = new Intent(this, HomeActivity.class);
    intent.setAction(HomeActivity.ACTION_LOGIN);
    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

    PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);

    Notification.Builder builder = new Notification.Builder(this) //
        .setSmallIcon(R.drawable.ic_noti_error)
        .setTicker(getString(R.string.auth_failed))
        .setContentTitle(getString(R.string.auth_failed))
        .setContentText(getString(R.string.auth_failed_desc, account.name))
        .setContentIntent(pi)
        .setPriority(Notification.PRIORITY_HIGH);

    NotificationManager nm = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
    nm.notify(AUTH_NOTIFICATION, builder.build());
  }

  public static void inject(Context context) {
    ((CathodeApp) context.getApplicationContext()).objectGraph.inject(context);
  }

  public static void inject(Context context, Object object) {
    ((CathodeApp) context.getApplicationContext()).objectGraph.inject(object);
  }

  public static boolean accountExists(Context context) {
    AccountManager am = AccountManager.get(context);
    Account[] accounts = am.getAccountsByType(context.getString(R.string.accountType));

    return accounts.length > 0;
  }

  public static Account getAccount(Context context) {
    AccountManager accountManager = AccountManager.get(context);
    Account[] accounts = accountManager.getAccountsByType(context.getString(R.string.accountType));
    return accounts.length > 0 ? accounts[0] : null;
  }

  public static void setupAccount(Context context) {
    removeAccount(context);

    AccountAuthenticator.allowRemove = false;
    AccountManager manager = AccountManager.get(context);

    // TODO: Can I get the username?
    Account account = new Account("Cathode", context.getString(R.string.accountType));

    manager.addAccountExplicitly(account, null, null);

    ContentResolver.setIsSyncable(account, BuildConfig.PROVIDER_AUTHORITY, 1);
    ContentResolver.setSyncAutomatically(account, BuildConfig.PROVIDER_AUTHORITY, true);
    ContentResolver.addPeriodicSync(account, BuildConfig.PROVIDER_AUTHORITY, new Bundle(),
        12 * DateUtils.HOUR_IN_SECONDS);

    ContentResolver.setIsSyncable(account, CalendarContract.AUTHORITY, 1);
    ContentResolver.setSyncAutomatically(account, CalendarContract.AUTHORITY, true);
    ContentResolver.addPeriodicSync(account, CalendarContract.AUTHORITY, new Bundle(),
        12 * DateUtils.HOUR_IN_SECONDS);
  }

  public static void removeAccount(Context context) {
    AccountAuthenticator.allowRemove = true;
    AccountManager am = AccountManager.get(context);
    Account[] accounts = am.getAccountsByType(context.getString(R.string.accountType));
    for (Account account : accounts) {
      ContentResolver.removePeriodicSync(account, BuildConfig.PROVIDER_AUTHORITY, new Bundle());
      ContentResolver.removePeriodicSync(account, CalendarContract.AUTHORITY, new Bundle());
      am.removeAccount(account, null, null);
    }
  }

  @Module(
      includes = {
          TraktModule.class
      },

      injects = {
          CathodeApp.class,

          // Task queue
          TraktTaskQueue.class,

          // Task schedulers
          EpisodeTaskScheduler.class, MovieTaskScheduler.class, SeasonTaskScheduler.class,
          ShowTaskScheduler.class, SearchTaskScheduler.class,

          // Activities
          HomeActivity.class,

          // Fragments
          SearchShowFragment.class, EpisodeFragment.class, LoginFragment.class, LogoutDialog.class,
          MovieCollectionFragment.class, MovieFragment.class, MovieRecommendationsFragment.class,
          MovieWatchlistFragment.class, SearchMovieFragment.class, SeasonFragment.class,
          ShowFragment.class, ShowsCollectionFragment.class, ShowRecommendationsFragment.class,
          ShowsWatchlistFragment.class, TrendingShowsFragment.class, TrendingMoviesFragment.class,
          UpcomingShowsFragment.class, WatchedMoviesFragment.class, WatchedShowsFragment.class,

          // Dialogs
          RatingDialog.class, CheckInDialog.class, CheckInDialog.Injections.class,

          // ListAdapters
          SeasonAdapter.class, SeasonsAdapter.class, ShowDescriptionAdapter.class,
          MoviesAdapter.class, MovieSearchAdapter.class, ShowRecommendationsAdapter.class,
          MovieRecommendationsAdapter.class, ShowsWithNextAdapter.class, ShowWatchlistAdapter.class,
          UpcomingAdapter.class,

          // Views
          PhoneEpisodeView.class, RemoteImageView.class,

          // Services
          TraktTaskService.class, CathodeSyncAdapter.class,

          // Tasks
          DeserializationFailedTask.class, TraktTaskQueue.class, TraktTaskSerializer.class,
          CancelCheckin.class, CheckInMovieTask.class, DismissMovieRecommendation.class,
          MovieCollectionTask.class, MovieRateTask.class, MovieWatchedTask.class,
          MovieWatchlistTask.class, CheckInEpisodeTask.class, DismissShowRecommendation.class,
          EpisodeCollectionTask.class, EpisodeRateTask.class, EpisodeWatchedTask.class,
          EpisodeWatchlistTask.class, SeasonCollectionTask.class, SeasonWatchedTask.class,
          ShowRateTask.class, ShowWatchedTask.class, ShowWatchlistTask.class, PurgeTask.class,
          SyncActivityStreamTask.class, SyncTask.class, SyncUserActivityTask.class,
          SyncUserSettingsTask.class, SyncWatchingTask.class, SyncMovieCrew.class,
          SyncMovieRecommendations.class, SyncMoviesCollectionTask.class, SyncMoviesRatings.class,
          SyncMoviesWatchedTask.class, SyncMoviesWatchlistTask.class, SyncMovieTask.class,
          SyncTrendingMoviesTask.class, SyncUpdatedMovies.class, SyncEpisodesRatings.class,
          SyncEpisodeTask.class, SyncEpisodeWatchlistTask.class, SyncSeasonsRatings.class,
          SyncSeasonsTask.class, SyncSeasonTask.class, SyncShowCast.class,
          SyncShowCollectedStatus.class, SyncShowRecommendations.class,
          SyncShowsCollectionTask.class, SyncShowsRatings.class, SyncShowsWatchedTask.class,
          SyncShowsWatchlistTask.class, SyncShowTask.class, SyncShowWatchedStatus.class,
          SyncTrendingShowsTask.class, SyncUpdatedShows.class, SyncPersonTask.class,

          // Misc
          PhoneController.class, ShowSearchHandler.class, ShowSearchHandler.SearchThread.class,
          MovieSearchHandler.class, MovieSearchHandler.SearchThread.class
      })
  static class AppModule {

    private final Context appContext;

    AppModule(Context appContext) {
      this.appContext = appContext;
    }

    @Provides @Singleton TraktTaskQueue provideTaskQueue(Gson gson) {
      TraktTaskQueue queue = TraktTaskQueue.create(appContext, gson, "taskQueue");
      if (DEBUG) {
        queue.setListener(new ObjectQueue.Listener<TraktTask>() {
          @Override public void onAdd(ObjectQueue<TraktTask> queue, TraktTask entry) {
            Timber.tag("TraktQueue").d("Queue size: %d", queue.size());
          }

          @Override public void onRemove(ObjectQueue<TraktTask> queue) {
            Timber.tag("TraktQueue").d("Queue size: %d", queue.size());
          }
        });
      }
      return queue;
    }

    @Provides @Singleton @PriorityQueue TraktTaskQueue providePriorityTaskQueue(Gson gson) {
      TraktTaskQueue queue = TraktTaskQueue.create(appContext, gson, "priorityTaskQueue");
      if (DEBUG) {
        queue.setListener(new ObjectQueue.Listener<TraktTask>() {
          @Override public void onAdd(ObjectQueue<TraktTask> queue, TraktTask entry) {
            Timber.tag("PriorityQueue").d("Queue size: %d", queue.size());
          }

          @Override public void onRemove(ObjectQueue<TraktTask> queue) {
            Timber.tag("PriorityQueue").d("Queue size: %d", queue.size());
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

    @Provides @Singleton ErrorHandler provideErrorHandler(final Bus bus) {
      return new ErrorHandler() {
        @Override public Throwable handleError(RetrofitError error) {
          Response response = error.getResponse();
          if (response != null) {
            final int statusCode = response.getStatus();
            if (statusCode == 401) {
              MAIN_HANDLER.post(new Runnable() {
                @Override public void run() {
                  bus.post(new AuthFailedEvent());
                }
              });
            }
          }

          return error;
        }
      };
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

    @Provides @Singleton SearchTaskScheduler provideSearchScheduler() {
      return new SearchTaskScheduler(appContext);
    }

    @Provides @Singleton UserToken provideUserToken() {
      SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(appContext);

      String token = settings.getString(Settings.TRAKT_TOKEN, null);

      return new UserToken(token);
    }

    @Provides @ApiKey String provideClientId() {
      return BuildConfig.TRAKT_CLIENT_ID;
    }
  }
}
