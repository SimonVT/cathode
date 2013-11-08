package net.simonvt.cathode.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.api.entity.TvShow;
import net.simonvt.cathode.api.service.SearchService;
import net.simonvt.cathode.event.SearchFailureEvent;
import net.simonvt.cathode.event.ShowSearchResult;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTaskQueue;
import net.simonvt.cathode.remote.sync.SyncShowTask;
import retrofit.RetrofitError;

public class ShowSearchHandler {

  private static final String TAG = "ShowSearchHandler";

  private Context context;

  private Bus bus;

  private List<Long> showIds;

  private SearchThread thread;

  public ShowSearchHandler(Context context, Bus bus) {
    this.context = context;
    this.bus = bus;
    bus.register(this);
  }

  @Produce public ShowSearchResult produceSearchResult() {
    if (showIds != null) {
      return new ShowSearchResult(showIds);
    }

    return null;
  }

  public boolean isSearching() {
    return thread != null;
  }

  public void deliverResult(List<Long> showIds) {
    LogWrapper.v(TAG, "[deliverResult]");
    this.showIds = showIds;
    bus.post(new ShowSearchResult(showIds));
  }

  public void deliverFailure() {
    LogWrapper.v(TAG, "[deliverFailure]");
    bus.post(new SearchFailureEvent(SearchFailureEvent.Type.SHOW));
  }

  public void search(final String query) {
    LogWrapper.v(TAG, "[search] Query: " + query);
    showIds = null;

    if (thread != null) {
      thread.unregister();
    }
    thread = new SearchThread(context, query);
    thread.start();
  }

  public static final class SearchThread extends Thread {

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    @Inject ShowSearchHandler handler;

    @Inject SearchService searchService;

    @Inject TraktTaskQueue queue;

    private Context context;

    private String query;

    private SearchThread(Context context, String query) {
      this.context = context;
      this.query = query;

      CathodeApp.inject(context, this);
    }

    public void unregister() {
      handler = null;
    }

    @Override public void run() {
      try {
        List<TvShow> shows = searchService.shows(query);

        final List<Long> showIds = new ArrayList<Long>(shows.size());

        for (TvShow show : shows) {
          if (!TextUtils.isEmpty(show.getTitle())) {
            final boolean exists =
                ShowWrapper.exists(context.getContentResolver(), show.getTvdbId());

            final long showId = ShowWrapper.updateOrInsertShow(context.getContentResolver(), show);
            showIds.add(showId);

            if (!exists) queue.add(new SyncShowTask(show.getTvdbId()));
          }
        }

        MAIN_HANDLER.post(new Runnable() {
          @Override public void run() {
            if (handler != null) handler.deliverResult(showIds);
          }
        });
      } catch (RetrofitError e) {
        e.printStackTrace();
        MAIN_HANDLER.post(new Runnable() {
          @Override public void run() {
            if (handler != null) handler.deliverFailure();
          }
        });
      }
    }
  }
}
