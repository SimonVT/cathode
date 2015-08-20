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

import android.util.Log;
import com.crashlytics.android.Crashlytics;
import timber.log.Timber;

public class CrashlyticsTree extends Timber.Tree {

  @Override protected void log(int priority, String tag, String message, Throwable t) {
    String type;
    switch (priority) {
      case Log.VERBOSE:
      case Log.DEBUG:
        return;

      case Log.INFO:
        type = "INFO";
        break;

      case Log.WARN:
        type = "WARN";
        break;

      case Log.ERROR:
        type = "ERROR";
        break;

      case Log.ASSERT:
        type = "WTF";
        break;

      default:
        type = "UNKNOWN";
        break;
    }

    String msg = type + ": ";

    if (tag != null) {
      msg += "[" + tag + "] ";
    }

    msg += message;

    Crashlytics.log(msg);
    if (t != null) {
      Crashlytics.logException(t);
    }
  }
}
