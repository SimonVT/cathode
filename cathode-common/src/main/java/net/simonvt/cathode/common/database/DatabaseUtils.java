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
package net.simonvt.cathode.common.database;

import android.text.TextUtils;

public final class DatabaseUtils {

  private DatabaseUtils() {
  }

  public static String removeLeadingArticle(String string) {
    if (TextUtils.isEmpty(string)) {
      return string;
    }

    final long length = string.length();

    if (length > 4 && (string.startsWith("The ") || string.startsWith("the "))) {
      return string.substring(4);
    }

    if (length > 3 && (string.startsWith("An ") || string.startsWith("an "))) {
      return string.substring(3);
    }

    if (length > 2 && (string.startsWith("A ") || string.startsWith("a "))) {
      return string.substring(2);
    }

    return string;
  }
}
