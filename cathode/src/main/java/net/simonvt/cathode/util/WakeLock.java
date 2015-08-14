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

package net.simonvt.cathode.util;

import android.content.Context;
import android.os.PowerManager;
import java.util.HashMap;
import java.util.Map;
import timber.log.Timber;

public final class WakeLock {

  private static final Map<String, PowerManager.WakeLock> WAKELOCKS = new HashMap<>();

  private WakeLock() {
  }

  private static PowerManager.WakeLock getLock(Context context, String tag) {
    PowerManager.WakeLock wakeLock = WAKELOCKS.get(tag);
    if (wakeLock == null) {
      PowerManager mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
      wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag);
      WAKELOCKS.put(tag, wakeLock);
    }

    return wakeLock;
  }

  static boolean hasLock(Context context, String tag) {
    PowerManager.WakeLock lock = getLock(context, tag);
    return lock.isHeld();
  }

  public static void acquire(Context context, String tag) {
    PowerManager.WakeLock lock = getLock(context, tag);
    if (!lock.isHeld()) {
      Timber.d("Acquiring wakelock for tag: " + tag);
      lock.acquire();
    }
  }

  public static void release(Context context, String tag) {
    PowerManager.WakeLock lock = getLock(context, tag);
    if (lock.isHeld()) {
      Timber.d("Releasing wakelock for tag: " + tag);
      lock.release();
    }
  }
}
