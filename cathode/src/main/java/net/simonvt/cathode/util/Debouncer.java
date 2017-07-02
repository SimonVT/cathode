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

package net.simonvt.cathode.util;

import java.util.HashMap;
import java.util.Map;
import net.simonvt.cathode.common.util.MainHandler;
import timber.log.Timber;

public class Debouncer {

  private static final Debouncer INSTANCE = new Debouncer();

  private final Map<String, Runnable> runnables = new HashMap<>();

  private Debouncer() {
  }

  public static void debounce(final String key, final Runnable r, final long delayMillis) {
    Timber.d("Debouncing key %s", key);
    if (INSTANCE.runnables.containsKey(key)) {
      Timber.d("Key %s already exists", key);

      Runnable runnable = INSTANCE.runnables.get(key);
      MainHandler.removeCallbacks(runnable);

      INSTANCE.runnables.remove(key);
    }

    INSTANCE.runnables.put(key, r);

    Runnable runner = new Runnable() {
      @Override public void run() {
        Timber.d("Running key %s", key);
        remove(key);
        r.run();
      }
    };
    MainHandler.postDelayed(runner, delayMillis);
  }

  public static void remove(final String key) {
    Timber.d("Removing key");
    INSTANCE.runnables.remove(key);
  }
}
