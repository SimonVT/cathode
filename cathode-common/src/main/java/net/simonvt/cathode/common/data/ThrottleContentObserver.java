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

import android.database.ContentObserver;
import android.net.Uri;
import net.simonvt.cathode.common.util.MainHandler;

public class ThrottleContentObserver extends ContentObserver {

  public static final int DELAY = 1000;

  public interface Callback {

    void onContentChanged();
  }

  private Callback callback;

  private Runnable notifyChange;

  private boolean pendingChange;

  public ThrottleContentObserver(Callback callback) {
    super(null);
    this.callback = callback;
  }

  @Override public void onChange(boolean selfChange) {
    if (notifyChange != null) {
      pendingChange = true;
    } else {
      postNotifyChange();
    }
  }

  private void postNotifyChange() {
    notifyChange = new Runnable() {
      @Override public void run() {
        callback.onContentChanged();
        notifyChange = null;

        if (pendingChange) {
          postNotifyChange();
          pendingChange = false;
        }
      }
    };
    MainHandler.postDelayed(notifyChange, DELAY);
  }

  @Override public void onChange(boolean selfChange, Uri uri) {
    onChange(selfChange);
  }
}
