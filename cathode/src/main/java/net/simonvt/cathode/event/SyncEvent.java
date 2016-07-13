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

package net.simonvt.cathode.event;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import net.simonvt.cathode.util.MainHandler;

public final class SyncEvent {

  public interface OnSyncListener {

    void onSyncChanged(int authSyncCount, int jobSyncCount);
  }

  private static final List<WeakReference<OnSyncListener>> LISTENERS = new ArrayList<>();

  private static int authSyncCount = 0;

  private static int jobSyncCount = 0;

  public static void registerListener(final OnSyncListener listener) {
    synchronized (LISTENERS) {
      LISTENERS.add(new WeakReference<>(listener));

      MainHandler.post(new Runnable() {
        @Override public void run() {
          listener.onSyncChanged(authSyncCount, jobSyncCount);
        }
      });
    }
  }

  public static void unregisterListener(OnSyncListener listener) {
    synchronized (LISTENERS) {
      for (int i = LISTENERS.size() - 1; i >= 0; i--) {
        WeakReference<OnSyncListener> ref = LISTENERS.get(i);
        OnSyncListener l = ref.get();
        if (l == null || l == listener) {
          LISTENERS.remove(ref);
        }
      }
    }
  }

  private SyncEvent() {
  }

  public static void authServiceStarted() {
    authSyncCount++;
    postSyncEvent();
  }

  public static void authServiceStopped() {
    authSyncCount--;
    postSyncEvent();
  }

  public static void jobServiceStarted() {
    jobSyncCount++;
    postSyncEvent();
  }

  public static void jobServiceStopped() {
    jobSyncCount--;
    postSyncEvent();
  }

  private static void postSyncEvent() {
    MainHandler.post(new Runnable() {
      @Override public void run() {
        synchronized (LISTENERS) {
          for (int i = LISTENERS.size() - 1; i >= 0; i--) {
            WeakReference<OnSyncListener> ref = LISTENERS.get(i);
            OnSyncListener l = ref.get();
            if (l == null) {
              LISTENERS.remove(ref);
              continue;
            }

            l.onSyncChanged(authSyncCount, jobSyncCount);
          }
        }
      }
    });
  }
}
