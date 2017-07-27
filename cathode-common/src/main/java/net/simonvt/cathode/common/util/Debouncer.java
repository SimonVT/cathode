/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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

package net.simonvt.cathode.common.util;

import java.util.HashMap;
import java.util.Map;
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
      remove(key);
    }

    Runnable runner = new Runnable() {
      @Override public void run() {
        Timber.d("Running key %s", key);
        INSTANCE.runnables.remove(key);
        r.run();
      }
    };

    INSTANCE.runnables.put(key, runner);
    MainHandler.postDelayed(runner, delayMillis);
  }

  public static void remove(final String key) {
    Timber.d("Removing key");
    Runnable runner = INSTANCE.runnables.get(key);
    MainHandler.removeCallbacks(runner);
    INSTANCE.runnables.remove(key);
  }
}
