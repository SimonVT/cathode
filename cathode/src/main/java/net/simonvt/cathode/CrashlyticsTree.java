/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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
package net.simonvt.cathode;

import com.crashlytics.android.Crashlytics;
import timber.log.Timber;

public class CrashlyticsTree extends Timber.HollowTree implements Timber.TaggedTree {

  private String nextTag;

  @Override public void tag(String tag) {
    nextTag = tag;
  }

  private String getTag() {
    String tag = nextTag;
    if (tag != null) {
      nextTag = null;
    }

    return tag;
  }

  @Override public void i(String message, Object... args) {
    log("INFO", message, args);
  }

  @Override public void i(Throwable t, String message, Object... args) {
    log("INFO", message, args);
  }

  @Override public void w(String message, Object... args) {
    log("WARN", message, args);
  }

  @Override public void w(Throwable t, String message, Object... args) {
    log("WARN", message, args);
  }

  @Override public void e(String message, Object... args) {
    log("ERROR", message, args);
  }

  @Override public void e(Throwable t, String message, Object... args) {
    e(message, args);
    Crashlytics.logException(t);
  }

  private void log(String caller, String message, Object... args) {
    if (message == null) return;
    StringBuilder s = new StringBuilder();
    String tag = getTag();
    if (tag != null) {
      s.append("[").append(tag).append("] ");
    }
    s.append(caller).append(" - ").append(String.format(message, args));
    Crashlytics.log(s.toString());
  }
}
