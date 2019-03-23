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

package net.simonvt.cathode.api.util

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimeUtils {

  @JvmStatic
  fun getMillis(iso: String?): Long {
    if (iso != null) {
      val dateFormat: DateFormat

      val length = iso.length

      if (length <= 20) {
        dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
      } else {
        dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
      }

      dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))

      try {
        return dateFormat.parse(iso).time
      } catch (e: ParseException) {
        throw IllegalArgumentException(e)
      }
    }

    return 0L
  }

  /**
   * @param month The first month is 0.
   */
  @JvmStatic
  fun getMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.YEAR, year)
    calendar.set(Calendar.MONTH, month)
    calendar.set(Calendar.DAY_OF_MONTH, day)
    calendar.set(Calendar.HOUR_OF_DAY, hour)
    calendar.set(Calendar.MINUTE, minute)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
  }

  @JvmStatic
  fun getIsoTime(): String = getIsoTime(System.currentTimeMillis())

  @JvmStatic
  fun getIsoTime(millis: Long): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    return dateFormat.format(Date(millis))
  }
}
