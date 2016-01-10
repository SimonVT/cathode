/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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

package net.simonvt.cathode.search;

import android.content.Context;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.api.service.SearchService;
import net.simonvt.cathode.util.MainHandler;
import timber.log.Timber;

public abstract class SearchHandler {

  public interface SearchListener {

    void onSearchFailure();

    void onSearchSuccess(List<Long> resultIds);
  }

  private final List<WeakReference<SearchListener>> listeners = new ArrayList<>();

  private List<Long> resultIds;

  private boolean lastSearchFailed = false;

  private SearchThread thread;

  private Context context;

  public SearchHandler(Context context) {
    this.context = context;
    CathodeApp.inject(context, this);
  }

  public void addListener(SearchListener listener) {
    WeakReference<SearchListener> listenerRef = new WeakReference<>(listener);
    listeners.add(listenerRef);

    if (resultIds != null) {
      listener.onSearchSuccess(resultIds);
    } else if (lastSearchFailed) {
      listener.onSearchFailure();
    }
  }

  public void removeListener(SearchListener listener) {
    for (int i = listeners.size() - 1; i >= 0; i--) {
      WeakReference<SearchListener> listenerRef = listeners.get(i);
      SearchListener searchListener = listenerRef.get();
      if (searchListener == null || listener == searchListener) {
        listeners.remove(listenerRef);
      }
    }
  }

  public List<Long> getResultIds() {
    return resultIds;
  }

  public void clear() {
    resultIds = null;
    lastSearchFailed = false;

    if (thread != null) {
      thread.unregister();
      thread = null;
    }
  }

  public void postOnFailure() {
    thread = null;
    lastSearchFailed = true;
    resultIds = null;

    for (int i = listeners.size() - 1; i >= 0; i--) {
      WeakReference<SearchListener> listenerRef = listeners.get(i);
      SearchListener listener = listenerRef.get();

      if (listener == null) {
        listeners.remove(listenerRef);
      } else {
        listener.onSearchFailure();
      }
    }
  }

  public void publishResult(List<Long> resultIds) {
    Timber.d("Publishing results");
    thread = null;
    this.resultIds = resultIds;
    lastSearchFailed = false;

    for (int i = listeners.size() - 1; i >= 0; i--) {
      WeakReference<SearchListener> listenerRef = listeners.get(i);
      SearchListener listener = listenerRef.get();

      if (listener == null) {
        listeners.remove(listenerRef);
      } else {
        listener.onSearchSuccess(resultIds);
      }
    }
  }

  public void search(String query) {
    resultIds = null;
    lastSearchFailed = false;

    if (thread != null) {
      thread.unregister();
    }

    thread = new SearchThread(context, this, query);
    thread.start();
  }

  public boolean isSearching() {
    return thread != null;
  }

  public boolean noResults() {
    return !lastSearchFailed && resultIds == null;
  }

  protected abstract List<Long> performSearch(String query) throws SearchFailedException;

  public static final class SearchThread extends Thread {

    @Inject SearchService searchService;

    private SearchHandler handler;

    private Context context;

    private String query;

    private SearchThread(Context context, SearchHandler handler, String query) {
      this.context = context;
      this.handler = handler;
      this.query = query;

      CathodeApp.inject(context, this);
    }

    public void unregister() {
      handler = null;
    }

    @Override public void run() {
      try {
        if (handler != null) {
          final List<Long> resultIds = handler.performSearch(query);

          MainHandler.post(new Runnable() {
            @Override public void run() {
              if (handler != null) handler.publishResult(resultIds);
            }
          });
        }
      } catch (SearchFailedException e) {
        Timber.d(e, "Search failed");

        MainHandler.post(new Runnable() {
          @Override public void run() {
            if (handler != null) handler.postOnFailure();
          }
        });
      }
    }
  }
}
