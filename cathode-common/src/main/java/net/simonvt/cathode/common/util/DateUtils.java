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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import net.simonvt.cathode.common.R;

public final class DateUtils {

  public static final long MINUTE_IN_SECONDS = 60;

  public static final long HOUR_IN_SECONDS = 60 * MINUTE_IN_SECONDS;

  public static final long DAY_IN_SECONDS = 24 * HOUR_IN_SECONDS;

  public static final long YEAR_IN_SECONDS = 365 * DAY_IN_SECONDS;

  public static final long MINUTE_IN_MILLIS = android.text.format.DateUtils.MINUTE_IN_MILLIS;

  public static final long HOUR_IN_MILLIS = android.text.format.DateUtils.HOUR_IN_MILLIS;

  public static final long DAY_IN_MILLIS = android.text.format.DateUtils.DAY_IN_MILLIS;

  public static final long YEAR_IN_MILLIS = android.text.format.DateUtils.YEAR_IN_MILLIS;

  public static final long HOUR_IN_MINUTES = 60;

  public static final long DAY_IN_MINUTES = 24 * HOUR_IN_MINUTES;

  public static final long YEAR_IN_MINUTES = 365 * DAY_IN_MINUTES;

  private DateUtils() {
  }

  public static long getMillis(String iso) {
    try {
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
      Date date = formatter.parse(iso);
      return date.getTime();
    } catch (ParseException e) {
      e.printStackTrace();
    }

    return 0L;
  }

  /**
   * Calculate time until the timestamp string returned by
   * {@link #millisToString(android.content.Context, long, boolean)} should be updated.
   */
  public static long timeUntilUpdate(long millis) {
    Calendar rightNow = Calendar.getInstance();
    final long rightNowMillis = rightNow.getTimeInMillis();

    Calendar nextYear = Calendar.getInstance();
    final int year = rightNow.get(Calendar.YEAR);
    nextYear.set(year + 1, Calendar.JANUARY, 1, 0, 0, 0);
    final long nextYearMillis = nextYear.getTimeInMillis();

    // If show aired more than 24 hours ago, update next year
    if (millis < rightNowMillis - 24 * HOUR_IN_MILLIS) {
      return nextYearMillis - rightNowMillis;
    }

    // If millis is next year
    if (millis > nextYearMillis) {
      if (millis - 24 * HOUR_IN_MILLIS < nextYearMillis) {
        return millis - 24 * HOUR_IN_MILLIS - rightNowMillis;
      } else {
        return nextYearMillis - rightNowMillis;
      }
    }

    // If millis is in more than 24 hours
    if (millis > rightNowMillis + 24 * HOUR_IN_MILLIS) {
      return millis - 24 * HOUR_IN_MILLIS - rightNowMillis;
    }

    // If millis is within next hour
    if (millis <= rightNowMillis + HOUR_IN_MILLIS && millis >= rightNowMillis) {
      final long nextMin = millis % MINUTE_IN_MILLIS;
      return nextMin > 0 ? nextMin : MINUTE_IN_MILLIS;
    }

    // If millis was within last hour
    if (millis < rightNowMillis && millis >= rightNowMillis - HOUR_IN_MILLIS) {
      final long lastMin = millis % MINUTE_IN_MILLIS;
      return lastMin > 0 ? MINUTE_IN_MILLIS - lastMin : MINUTE_IN_MILLIS;
    }

    // If millis is within next 24 hours
    if (millis > rightNowMillis && millis <= rightNowMillis + 24 * HOUR_IN_MILLIS) {
      final long nextHour = millis % HOUR_IN_MILLIS;
      return nextHour > 0 ? nextHour : HOUR_IN_MILLIS;
    }

    // If millis was within last 24 hours
    if (millis < rightNowMillis && millis >= rightNowMillis - 24 * HOUR_IN_MILLIS) {
      final long lastHour = millis % HOUR_IN_MILLIS;
      return lastHour > 0 ? HOUR_IN_MILLIS - lastHour : HOUR_IN_MILLIS;
    }

    throw new RuntimeException(
        "Unable to get update time for millis " + millis + " at time " + rightNowMillis);
  }

  public static String getTimeString(Context context, long millis) {
    StringBuilder sb = new StringBuilder();
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(millis);
    final boolean twentyFourHourFormat = DateFormat.is24HourFormat(context);

    if (twentyFourHourFormat) {
      sb.append(cal.get(Calendar.HOUR_OF_DAY));
    } else {
      sb.append(cal.get(Calendar.HOUR));
    }

    sb.append(":").append(String.format(Locale.US, "%02d", cal.get(Calendar.MINUTE)));

    if (!twentyFourHourFormat) {
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

  /** Formats milliseconds (UTC) as a String. */
  public static String millisToString(Context context, long millis, boolean extended) {
    if (millis == 0L) {
      return context.getResources().getString(R.string.airdate_unknown);
    }

    Calendar cal = Calendar.getInstance();

    final long rightNow = cal.getTimeInMillis();

    if (rightNow >= millis - 2 * MINUTE_IN_MILLIS && rightNow <= millis + 2 * MINUTE_IN_MILLIS) {
      return context.getString(R.string.now);
    }

    if (rightNow > millis && rightNow < millis + 1 * HOUR_IN_MILLIS) {
      final int minutes = (int) ((rightNow - millis) / MINUTE_IN_MILLIS);
      return context.getResources().getQuantityString(R.plurals.minutes_ago, minutes, minutes);
    }

    if (rightNow < millis && rightNow > millis - 1 * HOUR_IN_MILLIS) {
      final int minutes = (int) ((millis - rightNow) / MINUTE_IN_MILLIS);
      return context.getResources().getQuantityString(R.plurals.in_minutes, minutes, minutes);
    }

    if (rightNow > millis && rightNow <= millis + 24 * HOUR_IN_MILLIS) {
      final int hours = (int) ((rightNow - millis) / HOUR_IN_MILLIS);
      return context.getResources().getQuantityString(R.plurals.hours_ago, hours, hours);
    }

    if (rightNow < millis && rightNow >= millis - 24 * HOUR_IN_MILLIS) {
      final int hours = (int) ((millis - rightNow) / HOUR_IN_MILLIS);
      return context.getResources().getQuantityString(R.plurals.in_hours, hours, hours);
    }

    final int currentYear = cal.get(Calendar.YEAR);

    cal.setTimeInMillis(millis);
    final int millisYear = cal.get(Calendar.YEAR);
    final String month = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault());
    final int day = cal.get(Calendar.DAY_OF_MONTH);

    StringBuilder sb = new StringBuilder();

    if (millisYear >= currentYear || extended) {
      sb.append(month).append(" ").append(day);
    }

    if (millisYear < currentYear && !extended) {
      sb.append(millisYear);
    } else if (millisYear != currentYear && extended) {
      sb.append(", ").append(millisYear);
    }

    if (extended) {
      final boolean twentyFourHourFormat = DateFormat.is24HourFormat(context);

      sb.append(" ");

      if (twentyFourHourFormat) {
        sb.append(cal.get(Calendar.HOUR_OF_DAY));
      } else {
        sb.append(cal.get(Calendar.HOUR));
      }

      sb.append(":").append(String.format(Locale.US, "%02d", cal.get(Calendar.MINUTE)));

      if (!twentyFourHourFormat) {
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
