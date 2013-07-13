package net.simonvt.trakt.util;

import retrofit.RetrofitError;

import com.squareup.otto.Bus;
import com.squareup.otto.Produce;

import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.api.entity.Movie;
import net.simonvt.trakt.api.service.SearchService;
import net.simonvt.trakt.event.MovieSearchResult;
import net.simonvt.trakt.event.SearchFailureEvent;
import net.simonvt.trakt.provider.MovieWrapper;
import net.simonvt.trakt.remote.TraktTaskQueue;
import net.simonvt.trakt.remote.sync.SyncMovieTask;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class MovieSearchHandler {

    private static final String TAG = "MovieSearchHandler";

    private Context mContext;

    private Bus mBus;

    private List<Long> mMovieIds;

    private SearchThread mThread;

    public MovieSearchHandler(Context context, Bus bus) {
        mContext = context;
        mBus = bus;
        bus.register(this);
    }

    @Produce
    public MovieSearchResult produceSearchResult() {
        if (mMovieIds != null) {
            return new MovieSearchResult(mMovieIds);
        }

        return null;
    }

    public boolean isSearching() {
        return mThread != null;
    }

    public void deliverResult(List<Long> movieIds) {
        mMovieIds = movieIds;
        mBus.post(new MovieSearchResult(movieIds));
    }

    public void deliverFailure() {
        mBus.post(new SearchFailureEvent(SearchFailureEvent.Type.MOVIE));
    }

    public void search(final String query) {
        mMovieIds = null;

        if (mThread != null) {
            mThread.unregister();
        }
        mThread = new SearchThread(mContext, query);
        mThread.start();

    }

    public static final class SearchThread extends Thread {

        private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

        @Inject MovieSearchHandler mHandler;

        @Inject SearchService mSearchService;

        @Inject TraktTaskQueue mQueue;

        private Context mContext;

        private String mQuery;

        private SearchThread(Context context, String query) {
            mContext = context;
            mQuery = query;

            TraktApp.inject(context, this);
        }

        public void unregister() {
            mHandler = null;
        }

        @Override
        public void run() {
            try {
                List<Movie> movies = mSearchService.movies(mQuery);

                final List<Long> movieIds = new ArrayList<Long>(movies.size());

                for (Movie movie : movies) {
                    if (!TextUtils.isEmpty(movie.getTitle())) {
                        final boolean exists = MovieWrapper.exists(mContext.getContentResolver(), movie.getTmdbId());

                        final long movieId = MovieWrapper.updateOrInsertMovie(mContext.getContentResolver(), movie);
                        movieIds.add(movieId);

                        if (!exists) mQueue.add(new SyncMovieTask(movie.getTmdbId()));
                    }
                }

                MAIN_HANDLER.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mHandler != null) mHandler.deliverResult(movieIds);
                    }
                });
            } catch (RetrofitError e) {
                e.printStackTrace();
                MAIN_HANDLER.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mHandler != null) mHandler.deliverFailure();
                    }
                });
            }
        }
    }
}
