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

import java.util.Locale;

public final class TextUtils {

  private TextUtils() {
  }

  public static int wordCount(String s) {
    if (s == null) {
      return 0;
    }

    String trimmed = s.trim();
    trimmed = trimmed.replaceAll("[^a-zA-Z0-9 ]", "");
    if (trimmed.isEmpty()) {
      return 0;
    }

    String[] split = trimmed.split("\\s+");
    return split.length;
  }

  public static String upperCaseFirstLetter(String string) {
    if (android.text.TextUtils.isEmpty(string)) {
      return string;
    }

    return string.substring(0, 1).toUpperCase(Locale.US) + string.substring(1);
  }
}
