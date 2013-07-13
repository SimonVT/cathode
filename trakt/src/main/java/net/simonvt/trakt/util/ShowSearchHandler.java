package net.simonvt.trakt.util;

import retrofit.RetrofitError;

import com.squareup.otto.Bus;
import com.squareup.otto.Produce;

import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.api.entity.TvShow;
import net.simonvt.trakt.api.service.SearchService;
import net.simonvt.trakt.event.SearchFailureEvent;
import net.simonvt.trakt.event.ShowSearchResult;
import net.simonvt.trakt.provider.ShowWrapper;
import net.simonvt.trakt.remote.TraktTaskQueue;
import net.simonvt.trakt.remote.sync.SyncShowTask;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class ShowSearchHandler {

    private static final String TAG = "ShowSearchHandler";

    private Context mContext;

    private Bus mBus;

    private List<Long> mShowIds;

    private SearchThread mThread;

    public ShowSearchHandler(Context context, Bus bus) {
        mContext = context;
        mBus = bus;
        bus.register(this);
    }

    @Produce
    public ShowSearchResult produceSearchResult() {
        if (mShowIds != null) {
            return new ShowSearchResult(mShowIds);
        }

        return null;
    }

    public boolean isSearching() {
        return mThread != null;
    }

    public void deliverResult(List<Long> showIds) {
        LogWrapper.v(TAG, "[deliverResult]");
        mShowIds = showIds;
        mBus.post(new ShowSearchResult(showIds));
    }

    public void deliverFailure() {
        LogWrapper.v(TAG, "[deliverFailure]");
        mBus.post(new SearchFailureEvent(SearchFailureEvent.Type.SHOW));
    }

    public void search(final String query) {
        LogWrapper.v(TAG, "[search] Query: " + query);
        mShowIds = null;

        if (mThread != null) {
            mThread.unregister();
        }
        mThread = new SearchThread(mContext, query);
        mThread.start();

    }

    public static final class SearchThread extends Thread {

        private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

        @Inject ShowSearchHandler mHandler;

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
                List<TvShow> shows = mSearchService.shows(mQuery);

                final List<Long> showIds = new ArrayList<Long>(shows.size());

                for (TvShow show : shows) {
                    if (!TextUtils.isEmpty(show.getTitle())) {
                        final boolean exists = ShowWrapper.exists(mContext.getContentResolver(), show.getTvdbId());

                        final long showId = ShowWrapper.updateOrInsertShow(mContext.getContentResolver(), show);
                        showIds.add(showId);

                        if (!exists) mQueue.add(new SyncShowTask(show.getTvdbId()));
                    }
                }

                MAIN_HANDLER.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mHandler != null) mHandler.deliverResult(showIds);
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
