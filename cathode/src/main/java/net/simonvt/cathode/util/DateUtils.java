package net.simonvt.cathode.util;

import android.content.Context;
import android.text.format.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import net.simonvt.cathode.R;

public final class DateUtils {

  private static final String TAG = "DateUtils";

  public static final long MINUTE_IN_SECONDS = 60;

  public static final long HOUR_IN_SECONDS = 60 * MINUTE_IN_SECONDS;

  public static final long DAY_IN_SECONDS = 24 * HOUR_IN_SECONDS;

  public static final long YEAR_IN_SECONDS = 365 * DAY_IN_SECONDS;

  public static final long MINUTE_IN_MILLIS = android.text.format.DateUtils.MINUTE_IN_MILLIS;

  public static final long HOUR_IN_MILLIS = android.text.format.DateUtils.HOUR_IN_MILLIS;

  public static final long DAY_IN_MILLIS = android.text.format.DateUtils.DAY_IN_MILLIS;

  public static final long YEAR_IN_MILLIS = android.text.format.DateUtils.YEAR_IN_MILLIS;

  private DateUtils() {
  }

  public static long getMillis(String iso) {
    try {
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
      Date date = formatter.parse(iso);
      return date.getTime();
    } catch (ParseException e) {
      e.printStackTrace();
    }

    return 0L;
  }

  /** Formats milliseconds (UTC) as a String. */
  public static String millisToString(Context context, long millis, boolean extended) {
    if (millis < android.text.format.DateUtils.YEAR_IN_MILLIS) {
      return context.getResources().getString(R.string.airdate_unknown);
    }

    Calendar cal = Calendar.getInstance();

    final long rightNow = cal.getTimeInMillis();

    if (rightNow >= millis - 2 * MINUTE_IN_MILLIS && rightNow <= millis + 2 * MINUTE_IN_MILLIS) {
      return context.getString(R.string.now);
    }

    if (rightNow > millis && rightNow < millis + 1 * HOUR_IN_MILLIS) {
      final long minutes = (rightNow - millis) / MINUTE_IN_MILLIS;
      return context.getString(R.string.minutes_ago, minutes);
    }

    if (rightNow < millis && rightNow > millis - 1 * HOUR_IN_MILLIS) {
      final long minutes = (millis - rightNow) / MINUTE_IN_MILLIS;
      return context.getString(R.string.in_minutes, minutes);
    }

    if (rightNow > millis && rightNow <= millis + 24 * HOUR_IN_MILLIS) {
      final long hours = (rightNow - millis) / HOUR_IN_MILLIS;
      return context.getString(R.string.hours_ago, hours);
    }

    if (rightNow < millis && rightNow >= millis - 24 * HOUR_IN_MILLIS) {
      final long hours = (millis - rightNow) / HOUR_IN_MILLIS;
      return context.getString(R.string.in_hours, hours);
    }

    final int year = cal.get(Calendar.YEAR);
    cal.setTimeInMillis(millis);
    final int millisYear = cal.get(Calendar.YEAR);

    String month = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault());
    final int day = cal.get(Calendar.DAY_OF_MONTH);
    StringBuilder sb = new StringBuilder();

    sb.append(month).append(" ").append(day);

    if (millisYear != year) {
      sb.append(", ").append(millisYear);
    } else if (extended && millis > rightNow - 24 * HOUR_IN_MILLIS) {
      final boolean twentyFourHourFormat = DateFormat.is24HourFormat(context);

      sb.append(" ");

      if (twentyFourHourFormat) {
        sb.append(cal.get(Calendar.HOUR_OF_DAY));
      } else {
        sb.append(cal.get(Calendar.HOUR));
      }

      sb.append(":").append(String.format("%02d", cal.get(Calendar.MINUTE)));

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
}
