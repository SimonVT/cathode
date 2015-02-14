/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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

package net.simonvt.cathode.api.util;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public final class TimeUtils {

  private TimeUtils() {
  }

  public static long getMillis(String iso) {
    if (iso != null) {
      final int length = iso.length();
      DateTimeFormatter fmt;

      if (length <= 20) {
        fmt = ISODateTimeFormat.dateTimeNoMillis();
      } else {
        fmt = ISODateTimeFormat.dateTime();
      }

      return fmt.parseDateTime(iso).getMillis();
    }

    return 0L;
  }

  public static String getIsoTime() {
    DateTime dt = new DateTime();
    DateTimeFormatter fmt = ISODateTimeFormat.dateTime().withZoneUTC();
    return fmt.print(dt);
  }

  public static String getIsoTime(long millis) {
    DateTime dt = new DateTime(millis);
    DateTimeFormatter fmt = ISODateTimeFormat.dateTime().withZoneUTC();
    return fmt.print(dt);
  }
}
