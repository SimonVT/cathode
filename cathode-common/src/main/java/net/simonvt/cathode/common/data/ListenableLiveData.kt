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

package net.simonvt.cathode.common.data

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import java.util.ArrayList
import java.util.HashMap

abstract class ListenableLiveData<D>(val context: Context) : AsyncLiveData<D>(),
  ThrottleContentObserver.Callback {

  private val notificationUris = ArrayList<Uri>()
  private val observers = HashMap<Uri, ContentObserver>()

  override fun onActive() {
    super.onActive()
    synchronized(notificationUris) {
      for (uri in notificationUris) {
        registerUri(uri)
      }
    }
  }

  override fun onInactive() {
    super.onInactive()
    synchronized(notificationUris) {
      for (uri in notificationUris) {
        unregisterUri(uri)
      }
    }
  }

  fun addNotificationUri(uri: Uri) {
    synchronized(notificationUris) {
      notificationUris.add(uri)
      if (hasActiveObservers()) {
        registerUri(uri)
      }
    }
  }

  fun clearNotificationUris() {
    synchronized(notificationUris) {
      for (uri in notificationUris) {
        unregisterUri(uri)
      }

      notificationUris.clear()
    }
  }

  fun removeNotificationUri(uri: Uri) {
    synchronized(notificationUris) {
      notificationUris.remove(uri)
      if (hasActiveObservers()) {
        unregisterUri(uri)
      }
    }
  }

  protected fun registerUri(uri: Uri) {
    synchronized(observers) {
      val observer = ThrottleContentObserver(this)
      observers[uri] = observer
      context.contentResolver.registerContentObserver(uri, true, observer)
    }
  }

  protected fun unregisterUri(uri: Uri) {
    synchronized(observers) {
      if (observers.containsKey(uri)) {
        val observer = observers[uri]
        observers.remove(uri)
        context.contentResolver.unregisterContentObserver(observer!!)
      }
    }
  }

  override fun onContentChanged() {
    loadData()
  }
}
