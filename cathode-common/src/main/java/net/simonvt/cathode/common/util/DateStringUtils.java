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
package net.simonvt.cathode.common.util;

import android.content.Context;
import android.content.res.Resources;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import net.simonvt.cathode.common.R;

public final class DateStringUtils {

  private static final long HOUR_IN_MINUTES = 60;

  private static final long DAY_IN_MINUTES = 24 * HOUR_IN_MINUTES;

  private static final long YEAR_IN_MINUTES = 365 * DAY_IN_MINUTES;

  private DateStringUtils() {
  }

  public static long timeUntilUpdate(long airtimeMillis) {
    return timeUntilUpdate(System.currentTimeMillis(), airtimeMillis);
  }

  /**
   * Calculate time until the timestamp string returned by
   * {@link #getAirdateInterval(android.content.Context, long, boolean)} should be updated.
   */
  public static long timeUntilUpdate(long fromMillis, long airtimeMillis) {
    Calendar fromCalendar = Calendar.getInstance();
    fromCalendar.setTimeInMillis(fromMillis);

    Calendar nextYear = Calendar.getInstance();
    final int year = fromCalendar.get(Calendar.YEAR);
    nextYear.set(year + 1, Calendar.JANUARY, 1, 0, 0, 0);
    nextYear.set(Calendar.MILLISECOND, 0);
    final long nextYearMillis = nextYear.getTimeInMillis();

    // If show aired more than 24 hours ago, update next year
    if (airtimeMillis < fromMillis - 24 * DateUtils.HOUR_IN_MILLIS) {
      return nextYearMillis - fromMillis;
    }

    // If millis is next year
    if (airtimeMillis > nextYearMillis) {
      if (airtimeMillis - 24 * DateUtils.HOUR_IN_MILLIS < nextYearMillis) {
        return airtimeMillis - 24 * DateUtils.HOUR_IN_MILLIS - fromMillis;
      } else {
        return nextYearMillis - fromMillis;
      }
    }

    // Airing now (+/- 2 min
    if (airtimeMillis >= fromMillis - 2 * DateUtils.MINUTE_IN_MILLIS
        && airtimeMillis <= fromMillis + 2 * DateUtils.MINUTE_IN_MILLIS) {
      return Math.max(airtimeMillis + 2 * DateUtils.MINUTE_IN_MILLIS + 1L - fromMillis, 1L);
    }

    // If millis is in more than 24 hours
    if (airtimeMillis > fromMillis + 24 * DateUtils.HOUR_IN_MILLIS) {
      return airtimeMillis - 24 * DateUtils.HOUR_IN_MILLIS - fromMillis;
    }

    // If millis is within next hour
    if (airtimeMillis <= fromMillis + DateUtils.HOUR_IN_MILLIS && airtimeMillis >= fromMillis) {
      final long milliDiff = airtimeMillis - fromMillis;
      final long inMinutes = milliDiff / DateUtils.MINUTE_IN_MILLIS;
      final long updateIn = milliDiff - inMinutes * DateUtils.MINUTE_IN_MILLIS;
      return Math.max(updateIn, 1L);
    }

    // If millis was within last hour
    if (airtimeMillis < fromMillis && airtimeMillis >= fromMillis - DateUtils.HOUR_IN_MILLIS) {
      final long milliDiff = fromMillis - airtimeMillis;
      final long minutesSince = milliDiff / DateUtils.MINUTE_IN_MILLIS;
      final long updateIn = milliDiff - minutesSince * DateUtils.MINUTE_IN_MILLIS;
      return Math.max(updateIn, 1L);
    }

    // If millis is within next 24 hours
    if (airtimeMillis > fromMillis && airtimeMillis <= fromMillis + 24 * DateUtils.HOUR_IN_MILLIS) {
      final long milliDiff = airtimeMillis - fromMillis;
      final long inHours = milliDiff / DateUtils.HOUR_IN_MILLIS;
      final long updateIn = milliDiff - inHours * DateUtils.HOUR_IN_MILLIS;
      return Math.max(updateIn, 1L);
    }

    // If millis was within last 24 hours
    if (airtimeMillis < fromMillis && airtimeMillis >= fromMillis - 24 * DateUtils.HOUR_IN_MILLIS) {
      final long milliDiff = fromMillis - airtimeMillis;
      final long hoursSince = milliDiff / DateUtils.HOUR_IN_MILLIS;
      final long updateIn = milliDiff - hoursSince * DateUtils.HOUR_IN_MILLIS;
      return Math.max(updateIn, 1L);
    }

    throw new RuntimeException(
        "Unable to get update time for millis " + airtimeMillis + " at time " + fromMillis);
  }

  public static String getTimeString(Context context, long millis) {
    return getTimeString(millis, DateFormat.is24HourFormat(context));
  }

  public static String getTimeString(long millis, boolean is24HourFormat) {
    StringBuilder sb = new StringBuilder();
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(millis);

    if (is24HourFormat) {
      sb.append(cal.get(Calendar.HOUR_OF_DAY));
    } else {
      sb.append(cal.get(Calendar.HOUR));
    }

    sb.append(":").append(String.format(Locale.US, "%02d", cal.get(Calendar.MINUTE)));

    if (!is24HourFormat) {
      sb.append(" ");
      final int ampm = cal.get(Calendar.AM_PM);
      if (ampm == Calendar.AM) {
        sb.append("am");
      } else {
        sb.append("pm");
      }
    }

    return sb.toString();
  }

  public static String getDateString(long millis) {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(millis);

    SimpleDateFormat formatter = new SimpleDateFormat("EEEE, MMMM d", Locale.US);
    return formatter.format(cal.getTime());
  }

  public static String getAirdateInterval(Context context, long airdateMillis, boolean extended) {
    return getAirdateInterval(context, airdateMillis, System.currentTimeMillis(), extended);
  }

  public static String getAirdateInterval(Context context, long airdateMillis, long fromMillis,
      boolean extended) {
    return getAirdateInterval(context, airdateMillis, fromMillis, extended,
        DateFormat.is24HourFormat(context));
  }

  /** Formats milliseconds (UTC) as a String. */
  public static String getAirdateInterval(Context context, long airdateMillis, long fromMillis,
      boolean extended, boolean is24HourFormat) {
    if (airdateMillis == 0L) {
      return context.getResources().getString(R.string.airdate_unknown);
    }

    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(fromMillis);

    if (fromMillis >= airdateMillis - 2 * DateUtils.MINUTE_IN_MILLIS
        && fromMillis <= airdateMillis + 2 * DateUtils.MINUTE_IN_MILLIS) {
      return context.getString(R.string.now);
    }

    if (fromMillis > airdateMillis && fromMillis < airdateMillis + DateUtils.HOUR_IN_MILLIS) {
      final int minutes = (int) ((fromMillis - airdateMillis) / DateUtils.MINUTE_IN_MILLIS);
      return context.getResources().getQuantityString(R.plurals.minutes_ago, minutes, minutes);
    }

    if (fromMillis < airdateMillis && fromMillis > airdateMillis - DateUtils.HOUR_IN_MILLIS) {
      final int minutes = (int) ((airdateMillis - fromMillis) / DateUtils.MINUTE_IN_MILLIS);
      return context.getResources().getQuantityString(R.plurals.in_minutes, minutes, minutes);
    }

    if (fromMillis > airdateMillis && fromMillis <= airdateMillis + 24 * DateUtils.HOUR_IN_MILLIS) {
      final int hours = (int) ((fromMillis - airdateMillis) / DateUtils.HOUR_IN_MILLIS);
      return context.getResources().getQuantityString(R.plurals.hours_ago, hours, hours);
    }

    if (fromMillis < airdateMillis && fromMillis >= airdateMillis - 24 * DateUtils.HOUR_IN_MILLIS) {
      final int hours = (int) ((airdateMillis - fromMillis) / DateUtils.HOUR_IN_MILLIS);
      return context.getResources().getQuantityString(R.plurals.in_hours, hours, hours);
    }

    final int currentYear = cal.get(Calendar.YEAR);

    cal.setTimeInMillis(airdateMillis);
    final int millisYear = cal.get(Calendar.YEAR);
    final String month = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault());
    final int day = cal.get(Calendar.DAY_OF_MONTH);

    StringBuilder sb = new StringBuilder();

    if (millisYear >= currentYear || extended) {
      sb.append(month).append(" ").append(day);
    }

    if (millisYear < currentYear && !extended) {
      sb.append(millisYear);
    } else if (millisYear != currentYear) {
      sb.append(", ").append(millisYear);
    }

    if (extended) {
      sb.append(" ");

      if (is24HourFormat) {
        sb.append(cal.get(Calendar.HOUR_OF_DAY));
      } else {
        sb.append(cal.get(Calendar.HOUR));
      }

      sb.append(":").append(String.format(Locale.US, "%02d", cal.get(Calendar.MINUTE)));

      if (!is24HourFormat) {
        sb.append(" ");
        final int ampm = cal.get(Calendar.AM_PM);
        if (ampm == Calendar.AM) {
          sb.append("am");
        } else {
          sb.append("pm");
        }
      }
    }

    return sb.toString();
  }

  public static String getStatsString(Context context, long timeInMinutes) {
    Resources res = context.getResources();

    if (timeInMinutes == 0L) {
      return res.getString(R.string.stats_minutes, 0);
    }

    final int years = (int) (timeInMinutes / YEAR_IN_MINUTES);
    long timeLeft = timeInMinutes - years * YEAR_IN_MINUTES;
    final int days = (int) (timeLeft / DAY_IN_MINUTES);
    timeLeft = timeLeft - days * DAY_IN_MINUTES;
    final int hours = (int) (timeLeft / HOUR_IN_MINUTES);
    timeLeft = timeLeft - hours * HOUR_IN_MINUTES;
    final int minutes = (int) timeLeft;

    StringBuilder builder = new StringBuilder();
    if (years > 0) {
      String yearString = res.getString(R.string.stats_years, years);
      builder.append(yearString);
    }

    if (days > 0) {
      if (years > 0) {
        builder.append(" ");
      }

      String daysString = res.getString(R.string.stats_days, days);
      builder.append(daysString);
    }

    if (hours > 0) {
      if (days > 0 || years > 0) {
        builder.append(" ");
      }

      String hourString = res.getString(R.string.stats_hours, hours);
      builder.append(hourString);
    }

    if (minutes > 0 && years == 0) {
      if (hours > 0 || days > 0) {
        builder.append(" ");
      }

      String minuteString = res.getString(R.string.stats_minutes, minutes);
      builder.append(minuteString);
    }

    return builder.toString();
  }
}
