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
import net.simonvt.cathode.api.entity.SearchResult;
import net.simonvt.cathode.api.entity.Show;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.api.service.SearchService;
import net.simonvt.cathode.event.SearchFailureEvent;
import net.simonvt.cathode.event.ShowSearchResult;
import net.simonvt.cathode.provider.ShowWrapper;
import retrofit.RetrofitError;
import timber.log.Timber;

public class ShowSearchHandler {

  private Context context;

  private Bus bus;

  public static List<Long> showIds;

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
    Timber.d("[deliverResult]");
    ShowSearchHandler.showIds = showIds;
    bus.post(new ShowSearchResult(showIds));
  }

  public void deliverFailure() {
    Timber.d("[deliverFailure]");
    bus.post(new SearchFailureEvent(SearchFailureEvent.Type.SHOW));
  }

  public void search(final String query) {
    Timber.d("[search] Query: " + query);
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
        List<SearchResult> results = searchService.query(ItemType.SHOW, query);

        final List<Long> showIds = new ArrayList<Long>(results.size());

        for (SearchResult result : results) {
          Show show = result.getShow();
          if (!TextUtils.isEmpty(show.getTitle())) {
            final long showId = ShowWrapper.updateOrInsertShow(context.getContentResolver(), show);
            showIds.add(showId);
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
