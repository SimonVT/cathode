package net.simonvt.trakt.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.api.entity.Movie;
import net.simonvt.trakt.api.service.SearchService;
import net.simonvt.trakt.event.MovieSearchResult;
import net.simonvt.trakt.event.SearchFailureEvent;
import net.simonvt.trakt.provider.MovieWrapper;
import net.simonvt.trakt.remote.TraktTaskQueue;
import net.simonvt.trakt.remote.sync.SyncMovieTask;
import retrofit.RetrofitError;

public class MovieSearchHandler {

  private static final String TAG = "MovieSearchHandler";

  private Context context;

  private Bus bus;

  private List<Long> movieIds;

  private SearchThread thread;

  public MovieSearchHandler(Context context, Bus bus) {
    this.context = context;
    this.bus = bus;
    bus.register(this);
  }

  @Produce
  public MovieSearchResult produceSearchResult() {
    if (movieIds != null) {
      return new MovieSearchResult(movieIds);
    }

    return null;
  }

  public boolean isSearching() {
    return thread != null;
  }

  public void deliverResult(List<Long> movieIds) {
    this.movieIds = movieIds;
    bus.post(new MovieSearchResult(movieIds));
  }

  public void deliverFailure() {
    bus.post(new SearchFailureEvent(SearchFailureEvent.Type.MOVIE));
  }

  public void search(final String query) {
    movieIds = null;

    if (thread != null) {
      thread.unregister();
    }
    thread = new SearchThread(context, query);
    thread.start();
  }

  public static final class SearchThread extends Thread {

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    @Inject MovieSearchHandler handler;

    @Inject SearchService searchService;

    @Inject TraktTaskQueue queue;

    private Context context;

    private String query;

    private SearchThread(Context context, String query) {
      this.context = context;
      this.query = query;

      TraktApp.inject(context, this);
    }

    public void unregister() {
      handler = null;
    }

    @Override
    public void run() {
      try {
        List<Movie> movies = searchService.movies(query);

        final List<Long> movieIds = new ArrayList<Long>(movies.size());

        for (Movie movie : movies) {
          if (!TextUtils.isEmpty(movie.getTitle())) {
            final boolean exists =
                MovieWrapper.exists(context.getContentResolver(), movie.getTmdbId());

            final long movieId =
                MovieWrapper.updateOrInsertMovie(context.getContentResolver(), movie);
            movieIds.add(movieId);

            if (!exists) queue.add(new SyncMovieTask(movie.getTmdbId()));
          }
        }

        MAIN_HANDLER.post(new Runnable() {
          @Override
          public void run() {
            if (handler != null) handler.deliverResult(movieIds);
          }
        });
      } catch (RetrofitError e) {
        e.printStackTrace();
        MAIN_HANDLER.post(new Runnable() {
          @Override
          public void run() {
            if (handler != null) handler.deliverFailure();
          }
        });
      }
    }
  }
}
