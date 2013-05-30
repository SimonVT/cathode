package net.simonvt.trakt;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.otto.Bus;
import com.squareup.picasso.Picasso;
import com.squareup.tape.ObjectQueue;

import net.simonvt.trakt.api.ApiKey;
import net.simonvt.trakt.api.ResponseParser;
import net.simonvt.trakt.api.TraktModule;
import net.simonvt.trakt.api.UserCredentials;
import net.simonvt.trakt.scheduler.EpisodeTaskScheduler;
import net.simonvt.trakt.scheduler.MovieTaskScheduler;
import net.simonvt.trakt.scheduler.SeasonTaskScheduler;
import net.simonvt.trakt.scheduler.ShowTaskScheduler;
import net.simonvt.trakt.settings.Settings;
import net.simonvt.trakt.sync.PriorityTraktTaskQueue;
import net.simonvt.trakt.sync.TraktTaskQueue;
import net.simonvt.trakt.sync.TraktTaskSerializer;
import net.simonvt.trakt.sync.TraktTaskService;
import net.simonvt.trakt.sync.task.EpisodeCollectionTask;
import net.simonvt.trakt.sync.task.EpisodeWatchedTask;
import net.simonvt.trakt.sync.task.SyncEpisodeTask;
import net.simonvt.trakt.sync.task.SyncEpisodeWatchlist;
import net.simonvt.trakt.sync.task.SyncMovieTask;
import net.simonvt.trakt.sync.task.SyncMoviesTask;
import net.simonvt.trakt.sync.task.SyncMoviesWatchlistTask;
import net.simonvt.trakt.sync.task.SyncSeasonTask;
import net.simonvt.trakt.sync.task.SyncShowTask;
import net.simonvt.trakt.sync.task.SyncShowsCollectionTask;
import net.simonvt.trakt.sync.task.SyncShowsTask;
import net.simonvt.trakt.sync.task.SyncShowsWatchlistTask;
import net.simonvt.trakt.sync.task.SyncTask;
import net.simonvt.trakt.sync.task.SyncWatchedStatusTask;
import net.simonvt.trakt.sync.task.TraktTask;
import net.simonvt.trakt.sync.task.UpdateSeasonCountTask;
import net.simonvt.trakt.sync.task.UpdateShowCountTask;
import net.simonvt.trakt.ui.HomeActivity;
import net.simonvt.trakt.ui.adapter.EpisodeWatchlistAdapter;
import net.simonvt.trakt.ui.adapter.MoviesAdapter;
import net.simonvt.trakt.ui.adapter.SeasonsAdapter;
import net.simonvt.trakt.ui.adapter.ShowsAdapter;
import net.simonvt.trakt.ui.fragment.AddShowFragment;
import net.simonvt.trakt.ui.fragment.EpisodeFragment;
import net.simonvt.trakt.ui.fragment.EpisodesWatchlistFragment;
import net.simonvt.trakt.ui.fragment.LoginFragment;
import net.simonvt.trakt.ui.fragment.MovieCollectionFragment;
import net.simonvt.trakt.ui.fragment.MovieWatchlistFragment;
import net.simonvt.trakt.ui.fragment.SearchMovieFragment;
import net.simonvt.trakt.ui.fragment.SeasonFragment;
import net.simonvt.trakt.ui.fragment.SeasonsFragment;
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

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import javax.inject.Singleton;

public class TraktApp extends Application {

    public static final boolean DEBUG = BuildConfig.DEBUG;

    private ObjectGraph mObjectGraph;

    @Override
    public void onCreate() {
        super.onCreate();

        mObjectGraph = ObjectGraph.create(new AppModule(this));
        mObjectGraph.plus(new TraktModule());
    }

    public static void inject(Context context) {
        ((TraktApp) context.getApplicationContext()).mObjectGraph.inject(context);
    }

    public static void inject(Context context, Object object) {
        ((TraktApp) context.getApplicationContext()).mObjectGraph.inject(object);
    }

    @Module(
            includes = {
                    TraktModule.class
            },

            injects = {
                    // Task queues
                    PriorityTraktTaskQueue.class,
                    TraktTaskQueue.class,

                    // Task schedulers
                    EpisodeTaskScheduler.class,
                    MovieTaskScheduler.class,
                    SeasonTaskScheduler.class,
                    ShowTaskScheduler.class,

                    // Activities
                    HomeActivity.class,

                    // Fragments
                    AddShowFragment.class,
                    EpisodeFragment.class,
                    EpisodesWatchlistFragment.class,
                    LoginFragment.class,
                    MovieCollectionFragment.class,
                    MovieWatchlistFragment.class,
                    SearchMovieFragment.class,
                    SeasonFragment.class,
                    SeasonsFragment.class,
                    ShowInfoFragment.class,
                    ShowsCollectionFragment.class,
                    ShowsWatchlistFragment.class,
                    UpcomingShowsFragment.class,
                    WatchedMoviesFragment.class,
                    WatchedShowsFragment.class,

                    // ListAdapters
                    EpisodeWatchlistAdapter.class,
                    SeasonsAdapter.class,
                    ShowsAdapter.class,
                    MoviesAdapter.class,

                    // Views
                    PhoneEpisodeView.class,
                    RemoteImageView.class,

                    // Services
                    TraktTaskService.class,

                    // Tasks
                    EpisodeCollectionTask.class,
                    EpisodeWatchedTask.class,
                    SyncShowsCollectionTask.class,
                    SyncEpisodeTask.class,
                    SyncEpisodeWatchlist.class,
                    SyncMovieTask.class,
                    SyncMoviesTask.class,
                    SyncMoviesWatchlistTask.class,
                    SyncSeasonTask.class,
                    SyncShowTask.class,
                    SyncShowsTask.class,
                    SyncShowsWatchlistTask.class,
                    SyncWatchedStatusTask.class,
                    UpdateSeasonCountTask.class,
                    UpdateShowCountTask.class,
                    SyncTask.class,

                    // Misc
                    ResponseParser.class,
                    ShowSearchHandler.class,
                    ShowSearchHandler.SearchThread.class,
                    MovieSearchHandler.class,
                    MovieSearchHandler.SearchThread.class
            }
    )
    static class AppModule {

        private final Context mAppContext;

        AppModule(Context appContext) {
            this.mAppContext = appContext;
        }

        @Provides
        @Singleton
        TraktTaskQueue provideTaskQueue(Gson gson) {
            TraktTaskQueue queue = TraktTaskQueue.create(mAppContext, gson);
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

        @Provides
        @Singleton
        PriorityTraktTaskQueue providePriorityTaskQueue(Gson gson) {
            PriorityTraktTaskQueue queue = PriorityTraktTaskQueue.create(mAppContext, gson);
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

        @Provides
        @Singleton
        ShowSearchHandler provideShowSearchHandler(Bus bus) {
            return new ShowSearchHandler(mAppContext, bus);
        }

        @Provides
        @Singleton
        MovieSearchHandler provideMovieSearchHandler(Bus bus) {
            return new MovieSearchHandler(mAppContext, bus);
        }

        @Provides
        @ApiKey
        String provideApiKey() {
            return mAppContext.getString(R.string.apikey);
        }

        @Provides
        @Singleton
        Picasso providePicasso() {
            return new Picasso.Builder(mAppContext).build();
        }

        @Provides
        @Singleton
        Bus provideBus() {
            return new Bus();
        }

        @Provides
        @Singleton
        Gson provideGson() {
            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(TraktTask.class, new TraktTaskSerializer());
            return builder.create();
        }

        @Provides
        @Singleton
        EpisodeTaskScheduler provideEpisodeScheduler() {
            return new EpisodeTaskScheduler(mAppContext);
        }

        @Provides
        @Singleton
        SeasonTaskScheduler provideSeasonScheduler() {
            return new SeasonTaskScheduler(mAppContext);
        }

        @Provides
        @Singleton
        ShowTaskScheduler provideShowScheduler() {
            return new ShowTaskScheduler(mAppContext);
        }

        @Provides
        @Singleton
        UserCredentials provideCredentials() {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mAppContext);
            final String username = settings.getString(Settings.USERNAME, null);
            final String password = settings.getString(Settings.PASSWORD, null);
            return new UserCredentials(username, password);
        }

        //@Provides
        //@Singleton
        //MovieTaskScheduler provideMovieScheduler() {
        //    return new MovieTaskScheduler(mAppContext);
        //}
    }
}
