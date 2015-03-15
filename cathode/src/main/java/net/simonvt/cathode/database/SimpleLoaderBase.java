/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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

package net.simonvt.cathode.database;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.AsyncTaskLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import timber.log.Timber;

public abstract class SimpleLoaderBase<T extends AbsSimpleCursor> extends AsyncTaskLoader<T> {

  private final List<Uri> notificationUris = new ArrayList<>();

  private final Map<Uri, ContentObserver> observers = new HashMap<>();

  private T cursor;

  private long waitUntil;

  private Handler handler = new Handler(Looper.getMainLooper());

  private Runnable postResult = new Runnable() {
    @Override public void run() {
      deliverResult(cursor);
    }
  };

  public SimpleLoaderBase(Context context) {
    super(context);
  }

  public void addNotificationUri(Uri uri) {
    notificationUris.add(uri);
    registerUri(uri);
  }

  public void removeNotificationUri(Uri uri) {
    unregisterUri(uri);
    notificationUris.remove(uri);
  }

  public void clearNotificationUris() {
    for (Uri uri : notificationUris) {
      unregisterUri(uri);
    }

    notificationUris.clear();
  }

  private void registerUri(Uri uri) {
    ContentObserver observer = new ForceLoadContentObserver();
    observers.put(uri, observer);

    if (isStarted()) {
      getContext().getContentResolver().registerContentObserver(uri, true, observer);
    }
  }

  private void unregisterUri(Uri uri) {
    ContentObserver observer = observers.get(uri);
    getContext().getContentResolver().unregisterContentObserver(observer);
  }

  public void throttle(long ms) {
    waitUntil = System.currentTimeMillis() + ms;
  }

  @Override public void deliverResult(T cursor) {
    if (isReset()) {
      return;
    }

    this.cursor = cursor;
    handler.removeCallbacks(postResult);

    final long now = System.currentTimeMillis();
    if (now < waitUntil) {
      handler.postDelayed(postResult, waitUntil - now + 250);
      return;
    }

    if (isStarted()) {
      super.deliverResult(cursor);
    }
  }

  @Override protected void onStartLoading() {
    if (cursor != null) {
      deliverResult(cursor);
    }

    for (Uri uri : notificationUris) {
      ContentObserver observer = observers.get(uri);
      getContext().getContentResolver().registerContentObserver(uri, true, observer);
      Timber.d("Registering observer");
    }

    //if (takeContentChanged() || cursor == null) {
    forceLoad();
    //}
  }

  @Override protected void onStopLoading() {
    cancelLoad();

    for (ContentObserver observer : observers.values()) {
      getContext().getContentResolver().unregisterContentObserver(observer);
      Timber.d("unregistering observer");
    }
  }

  @Override protected void onReset() {
    super.onReset();
    onStopLoading();
    cursor = null;
  }

  public final class ForceLoadContentObserver extends ContentObserver {
    public ForceLoadContentObserver() {
      super(new Handler(Looper.getMainLooper()));
    }

    @Override
    public boolean deliverSelfNotifications() {
      return true;
    }

    @Override
    public void onChange(boolean selfChange) {
      onContentChanged();
    }
  }
}
