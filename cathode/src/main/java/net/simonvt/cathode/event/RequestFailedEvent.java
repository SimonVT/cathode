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
package net.simonvt.cathode.event;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import net.simonvt.cathode.util.MainHandler;

public final class RequestFailedEvent {

  public interface OnRequestFailedListener {

    void onRequestFailed(RequestFailedEvent event);
  }

  private static final List<WeakReference<OnRequestFailedListener>> LISTENERS = new ArrayList<>();

  public static void registerListener(OnRequestFailedListener listener) {
    synchronized (LISTENERS) {
      LISTENERS.add(new WeakReference<>(listener));
    }
  }

  public static void unregisterListener(OnRequestFailedListener listener) {
    synchronized (LISTENERS) {
      for (int i = LISTENERS.size() - 1; i >= 0; i--) {
        WeakReference<OnRequestFailedListener> ref = LISTENERS.get(i);
        OnRequestFailedListener l = ref.get();
        if (l == null || l == listener) {
          LISTENERS.remove(ref);
        }
      }
    }
  }

  public static void post(final int errorMessage) {
    MainHandler.post(new Runnable() {
      @Override public void run() {
        synchronized (LISTENERS) {
          RequestFailedEvent event = new RequestFailedEvent(errorMessage);

          for (int i = LISTENERS.size() - 1; i >= 0; i--) {
            WeakReference<OnRequestFailedListener> ref = LISTENERS.get(i);
            OnRequestFailedListener l = ref.get();
            if (l == null) {
              LISTENERS.remove(ref);
              continue;
            }

            l.onRequestFailed(event);
          }
        }
      }
    });
  }

  private int errorMessage;

  private RequestFailedEvent(int errorMessage) {
    this.errorMessage = errorMessage;
  }

  public int getErrorMessage() {
    return errorMessage;
  }
}
