/*
 * Copyright (C) 2018 Simon Vig Therkildsen
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

package net.simonvt.cathode.common.data;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ListenableLiveData<D> extends AsyncLiveData<D>
    implements ThrottleContentObserver.Callback {

  private Context context;

  private final List<Uri> notificationUris = new ArrayList<>();
  private final Map<Uri, ContentObserver> observers = new HashMap<>();

  public ListenableLiveData(Context context) {
    this.context = context;
  }

  public Context getContext() {
    return context;
  }

  public void addNotificationUri(Uri uri) {
    synchronized (notificationUris) {
      notificationUris.add(uri);
      registerUri(uri);
    }
  }

  public void clearNotificationUris() {
    synchronized (notificationUris) {
      for (Uri uri : notificationUris) {
        unregisterUri(uri);
      }

      notificationUris.clear();
    }
  }

  public void removeNotificationUri(Uri uri) {
    synchronized (notificationUris) {
      unregisterUri(uri);
      notificationUris.remove(uri);
    }
  }

  protected void registerUri(Uri uri) {
    ContentObserver observer = new ThrottleContentObserver(this);
    observers.put(uri, observer);
    context.getContentResolver().registerContentObserver(uri, true, observer);
  }

  protected void unregisterUri(Uri uri) {
    ContentObserver observer = observers.get(uri);
    context.getContentResolver().unregisterContentObserver(observer);
  }

  @Override public void onContentChanged() {
    loadData();
  }
}
